package dev.nox.menu;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.Map;

/** Draws the launcher's voxel boxes. Data space: px, y up, front +z. Model space: y down, front -z (so y and z flip). */
public final class Draw {
	private Draw() {}

	private static final float D2R = (float) (Math.PI / 180.0);
	// corner bits: 1 = x+, 2 = y+, 4 = z+
	private static final int[][] FACES = {{1, 3, 7, 5}, {0, 4, 6, 2}, {2, 6, 7, 3}, {0, 1, 5, 4}, {4, 5, 7, 6}, {0, 2, 3, 1}};
	private static final float[] SHADE = {0.65f, 0.65f, 1.0f, 0.5f, 0.8f, 0.8f};

	private static float[] rx(float[] v, float a) {
		float c = (float) Math.cos(a), s = (float) Math.sin(a);
		return new float[]{v[0], v[1] * c - v[2] * s, v[1] * s + v[2] * c};
	}

	private static float[] ry(float[] v, float a) {
		float c = (float) Math.cos(a), s = (float) Math.sin(a);
		return new float[]{v[0] * c + v[2] * s, v[1], -v[0] * s + v[2] * c};
	}

	private static float[] rz(float[] v, float a) {
		float c = (float) Math.cos(a), s = (float) Math.sin(a);
		return new float[]{v[0] * c - v[1] * s, v[0] * s + v[1] * c, v[2]};
	}

	/** Euler XYZ like three.js: Rx * Ry * Rz (Rz is applied first). Angles in degrees. */
	private static float[] euler(float[] v, float[] r) {
		return rx(ry(rz(v, r[2] * D2R), r[1] * D2R), r[0] * D2R);
	}

	private static float[] axis(float[] v, String ax, float a) {
		return switch (ax) {
			case "x" -> rx(v, a);
			case "z" -> rz(v, a);
			default -> ry(v, a);
		};
	}

	private static int mul(int argb, float k) {
		int a = argb >>> 24, r = (int) (((argb >> 16) & 255) * k), g = (int) (((argb >> 8) & 255) * k), b = (int) ((argb & 255) * k);
		return (a << 24) | (Math.min(255, r) << 16) | (Math.min(255, g) << 8) | Math.min(255, b);
	}

	private static void v(PoseStack.Pose pose, VertexConsumer vc, float[] p, float xOff, float yOff, int color) {
		vc.addVertex(pose, (p[0] + xOff) / 16f, -(p[1] + yOff) / 16f, -p[2] / 16f).setColor(color);
	}

	public static void part(PoseStack.Pose pose, VertexConsumer vc, Data.Part pd, Data.Cos cos, Map<String, String> chosen, float t, float xOff, float yOff) {
		for (Data.Box b : pd.boxes) {
			float hx = b.s[0] / 2f, hy = b.s[1] / 2f, hz = b.s[2] / 2f;
			float cx = b.p[0], cy = b.p[1], cz = b.p[2];
			Data.Anim a = b.a;
			if (a != null) {
				float k = a.sp * t + a.ph;
				switch (a.t) {
					case "bob" -> cy += a.amp * (float) Math.sin(k);
					case "scl" -> hy *= 1f + a.amp * (float) Math.sin(k);
					case "orbit" -> {
						cx = (float) Math.cos(k) * a.r;
						cy = a.y;
						cz = (float) Math.sin(k) * a.r;
					}
					default -> {}
				}
			}
			float[][] c = new float[8][];
			for (int i = 0; i < 8; i++) {
				float[] q = {(i & 1) != 0 ? hx : -hx, (i & 2) != 0 ? hy : -hy, (i & 4) != 0 ? hz : -hz};
				if (b.r != null) q = euler(q, b.r);
				q = new float[]{q[0] + cx, q[1] + cy, q[2] + cz};
				if (a != null && (a.t.equals("rot") || a.t.equals("spin"))) {
					float ang = a.t.equals("rot") ? a.amp * D2R * (float) Math.sin(a.sp * t + a.ph) : a.sp * t;
					float[] d = {q[0] - a.pv[0], q[1] - a.pv[1], q[2] - a.pv[2]};
					d = axis(d, a.ax, ang);
					q = new float[]{d[0] + a.pv[0], d[1] + a.pv[1], d[2] + a.pv[2]};
				}
				if (pd.tr != null) q = euler(q, pd.tr);
				if (pd.tp != null) q = new float[]{q[0] + pd.tp[0], q[1] + pd.tp[1], q[2] + pd.tp[2]};
				c[i] = q;
			}
			int base = Data.color(b.c, chosen, cos);
			int alpha = (int) (Math.max(0f, Math.min(1f, b.alpha)) * 255f);
			base = (alpha << 24) | (base & 0xFFFFFF);
			for (int f = 0; f < 6; f++) {
				int col = b.glow ? base : mul(base, SHADE[f]);
				int[] ids = FACES[f];
				// emitted twice (both windings) so it is visible regardless of back-face culling
				for (int i = 0; i < 4; i++) v(pose, vc, c[ids[i]], xOff, yOff, col);
				for (int i = 3; i >= 0; i--) v(pose, vc, c[ids[i]], xOff, yOff, col);
			}
		}
	}

	/** 10x16 pixel cape hanging from the neck line (model space, px). */
	public static void cape(PoseStack.Pose pose, VertexConsumer vc, int[] px, float tilt) {
		float st = (float) Math.sin(tilt), ct = (float) Math.cos(tilt);
		for (int j = 0; j < 16; j++) {
			for (int i = 0; i < 10; i++) {
				int col = px[Math.min(px.length - 1, j * 10 + i)];
				float x0 = 5 - i - 1, x1 = 5 - i, y0 = j, y1 = j + 1, z = 2.5f;
				float[][] q = {{x0, y0}, {x1, y0}, {x1, y1}, {x0, y1}};
				for (int pass = 0; pass < 2; pass++) {
					for (int k = 0; k < 4; k++) {
						float[] pt = q[pass == 0 ? k : 3 - k];
						float yy = pt[1] * ct - z * st, zz = pt[1] * st + z * ct;
						vc.addVertex(pose, pt[0] / 16f, yy / 16f, zz / 16f).setColor(col);
					}
				}
			}
		}
	}
}
