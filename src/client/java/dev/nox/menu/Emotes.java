package dev.nox.menu;

import net.minecraft.client.model.geom.ModelPart;

import java.util.Map;

/**
 * Plays the launcher's emotes by overriding the local player's model joints after vanilla animation.
 * Joint angles are relative to the torso: body.x = lean forward from the hips, body.y = twist; head and arms follow the torso.
 */
public final class Emotes {
	private static volatile String current;
	private static volatile long t0;

	private Emotes() {}

	public static void play(String id) {
		if (id != null && Data.EMOTES.containsKey(id)) {
			current = id;
			t0 = System.nanoTime();
		}
	}

	public static void stop() {
		current = null;
	}

	public static Data.Emote active() {
		String c = current;
		return c == null ? null : Data.EMOTES.get(c);
	}

	public static float time() {
		return (System.nanoTime() - t0) / 1_000_000_000f;
	}

	private static float smooth(float x) {
		x = Math.max(0f, Math.min(1f, x));
		return x * x * (3f - 2f * x);
	}

	private static float val(float[] j, float t) {
		return j[0] + j[1] * (float) Math.sin(j[2] * t + j[3]);
	}

	private static float lerp(float a, float b, float r) {
		return a + (b - a) * r;
	}

	private static final float D2R = (float) (Math.PI / 180.0);

	/** launcher angles use three.js conventions: x same, y and z mirrored in the game model */
	private static void joint(ModelPart part, Map<String, float[]> axes, float t, float r, boolean xy) {
		if (axes == null) return;
		if (xy && axes.containsKey("x")) part.xRot = lerp(part.xRot, val(axes.get("x"), t) * D2R, r);
		if (xy && axes.containsKey("y")) part.yRot = lerp(part.yRot, -val(axes.get("y"), t) * D2R, r);
		if (axes.containsKey("z")) part.zRot = lerp(part.zRot, -val(axes.get("z"), t) * D2R, r);
	}

	public static void apply(ModelPart root) {
		Data.Emote e = active();
		if (e == null) return;
		float t = time();
		if ((!e.loop && t > e.dur) || t > 120f) {
			stop();
			return;
		}
		float r = smooth(t / 0.35f);
		ModelPart head = root.getChild("head"), body = root.getChild("body"), ra = root.getChild("right_arm"), la = root.getChild("left_arm");
		ModelPart rl = root.getChild("right_leg"), ll = root.getChild("left_leg");

		float lean = 0f, twist = 0f;
		Map<String, float[]> bj = e.joints.get("body");
		if (bj != null) {
			if (bj.containsKey("x")) lean = val(bj.get("x"), t) * D2R * r;
			if (bj.containsKey("y")) twist = -val(bj.get("y"), t) * D2R * r;
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
			float d = e.dy[0] + e.dy[1] * (e.dy.length > 4 && e.dy[4] > 0 ? Math.abs((float) Math.sin(e.dy[2] * t + e.dy[3])) : (float) Math.sin(e.dy[2] * t + e.dy[3]));
			for (ModelPart p : new ModelPart[]{head, body, ra, la, rl, ll}) p.y -= d * r;
		}
	}
}
