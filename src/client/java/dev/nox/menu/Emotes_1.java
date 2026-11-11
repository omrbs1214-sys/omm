package dev.nox.menu;

import net.minecraft.client.model.geom.ModelPart;

import java.util.Map;

/**
 * Plays the launcher's emotes by overriding the local player's model joints after vanilla animation.
 * Joint angles are relative to the torso: body.x = lean forward from the hips, body.y = twist, root.y = whole-body spin.
 * Every emote blends in over 0.5s and blends out over 0.5s (also when you stop it), props pop in / out.
 */
public final class Emotes {
	private static volatile String current;
	private static volatile long t0;
	private static volatile float stopAt = Float.POSITIVE_INFINITY;
	private static final float D2R = (float) (Math.PI / 180.0);

	private Emotes() {}

	public static void play(String id) {
		Data.Emote e = id == null ? null : Data.EMOTES.get(id);
		if (e == null) return;
		current = id;
		t0 = System.nanoTime();
		stopAt = e.loop ? Float.POSITIVE_INFINITY : Math.max(0.6f, e.dur - 0.5f);
	}

	/** smooth stop: starts the 0.5s blend-out */
	public static void stop() {
		if (current != null) stopAt = Math.min(stopAt, time());
	}

	public static Data.Emote active() {
		String c = current;
		return c == null ? null : Data.EMOTES.get(c);
	}

	public static float time() {
		return (System.nanoTime() - t0) / 1_000_000_000f;
	}

	public static float stopAt() {
		return stopAt;
	}

	public static float smooth(float x) {
		x = Math.max(0f, Math.min(1f, x));
		return x * x * (3f - 2f * x);
	}

	public static float easeBack(float x) {
		x = Math.max(0f, Math.min(1f, x));
		float c1 = 1.70158f, c3 = c1 + 1f;
		return 1f + c3 * (float) Math.pow(x - 1f, 3) + c1 * (float) Math.pow(x - 1f, 2);
	}

	/** blend weight (0..1); ends the emote when the blend-out is finished */
	public static float weight() {
		if (current == null) return 0f;
		float t = time();
		if (t - stopAt >= 0.5f) {
			current = null;
			return 0f;
		}
		return smooth(t / 0.5f) * (1f - smooth((t - stopAt) / 0.5f));
	}

	private static float lerp(float a, float b, float r) {
		return a + (b - a) * r;
	}

	/** launcher angles use three.js conventions: x same, y and z mirrored in the game model */
	private static void joint(ModelPart part, Map<String, Data.Curve> axes, float t, float r, boolean xy) {
		if (axes == null) return;
		if (xy && axes.containsKey("x")) part.xRot = lerp(part.xRot, axes.get("x").eval(t) * D2R, r);
		if (xy && axes.containsKey("y")) part.yRot = lerp(part.yRot, -axes.get("y").eval(t) * D2R, r);
		if (axes.containsKey("z")) part.zRot = lerp(part.zRot, -axes.get("z").eval(t) * D2R, r);
	}

	public static void apply(ModelPart root) {
		Data.Emote e = active();
		if (e == null) return;
		float r = weight();
		if (current == null) return;
		float t = time();
		ModelPart head = root.getChild("head"), body = root.getChild("body"), ra = root.getChild("right_arm"), la = root.getChild("left_arm");
		ModelPart rl = root.getChild("right_leg"), ll = root.getChild("left_leg");

		float lean = 0f, twist = 0f;
		Map<String, Data.Curve> bj = e.joints.get("body");
		if (bj != null) {
			if (bj.containsKey("x")) lean = bj.get("x").eval(t) * D2R * r;
			if (bj.containsKey("y")) twist = -bj.get("y").eval(t) * D2R * r;
		}
		joint(head, e.joints.get("head"), t, r, true);
		joint(body, bj, t, r, false);
		joint(ra, e.joints.get("rArm"), t, r, true);
		joint(la, e.joints.get("lArm"), t, r, true);
		joint(rl, e.joints.get("rLeg"), t, r, true);
		joint(ll, e.joints.get("lLeg"), t, r, true);

		// torso chain: body, head and arms are carried by the lean (about the hips) and the twist (about the neck)
		if (lean != 0f || twist != 0f) {
			float ct = (float) Math.cos(twist), st = (float) Math.sin(twist), cl = (float) Math.cos(lean), sl = (float) Math.sin(lean);
			for (ModelPart p : new ModelPart[]{head, body, ra, la}) {
				float x1 = p.x * ct + p.z * st, z1 = -p.x * st + p.z * ct;
				float ry = p.y - 12f;
				p.x = x1;
				p.y = ry * cl - z1 * sl + 12f;
				p.z = ry * sl + z1 * cl;
				p.xRot += lean;
				p.yRot += twist;
			}
		}
		if (e.dy != null) {
			float d = e.dy.eval(t) * r;
			for (ModelPart p : new ModelPart[]{head, body, ra, la, rl, ll}) p.y -= d;
		}
		// whole-body spin about the vertical axis (Siuuu jump)
		Map<String, Data.Curve> rj = e.joints.get("root");
		if (rj != null && rj.containsKey("y")) {
			float deg = rj.get("y").eval(t);
			deg = ((deg + 180f) % 360f + 360f) % 360f - 180f;
			float phi = -deg * D2R * r, c = (float) Math.cos(phi), s = (float) Math.sin(phi);
			for (ModelPart p : new ModelPart[]{head, body, ra, la, rl, ll}) {
				float x = p.x * c + p.z * s, z = -p.x * s + p.z * c;
				p.x = x;
				p.z = z;
				p.yRot += phi;
			}
		}
	}
}
