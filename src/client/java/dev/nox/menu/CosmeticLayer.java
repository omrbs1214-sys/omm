package dev.nox.menu;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;

import java.util.List;
import java.util.Map;

/** Draws your Nox cosmetics, emote effects and custom cape on the local player. */
public class CosmeticLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	private static final long START = System.nanoTime();

	public CosmeticLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
		super(parent);
	}

	private static void draw(PoseStack ps, SubmitNodeCollector c, ModelPart host, Data.Part pd, Data.Cos cos, Map<String, String> cols, float t, float xOff, float yOff) {
		ps.pushPose();
		host.translateAndRotate(ps);
		c.submitCustomGeometry(ps, RenderTypes.debugFilledBox(), (pose, buf) -> Draw.part(pose, buf, pd, cos, cols, t, xOff, yOff, 1f));
		ps.popPose();
	}

	/** host == null: world-fixed prop anchored at the model origin (neck line, feet at y = -24) */
	private static void drawFx(PoseStack ps, SubmitNodeCollector c, ModelPart host, Data.Part pd, float t, float xOff, float scale) {
		ps.pushPose();
		if (host != null) host.translateAndRotate(ps);
		c.submitCustomGeometry(ps, RenderTypes.debugFilledBox(), (pose, buf) -> Draw.part(pose, buf, pd, null, null, t, xOff, 0f, scale));
		ps.popPose();
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
		Boolean local = state.getData(NoxMenuClient.LOCAL);
		if (local == null || !local) return;
		Data.Loadout lo = Data.loadout;
		Data.Emote em = Emotes.active();
		if (lo.equipped.isEmpty() && lo.cape == null && em == null) return;

		ModelPart root = this.getParentModel().root();
		ModelPart head = root.getChild("head"), body = root.getChild("body"), ra = root.getChild("right_arm"), la = root.getChild("left_arm");
		float t = (System.nanoTime() - START) / 1_000_000_000f;

		for (Data.Equipped eq : lo.equipped) {
			Data.Cos cos = Data.COS.get(eq.id);
			if (cos == null) continue;
			float yOff = ("face".equals(cos.slot) && !"ninjamask".equals(cos.id)) ? lo.faceDy : 0f;
			for (Map.Entry<String, Data.Part> en : cos.parts.entrySet()) {
				switch (en.getKey()) {
					case "head" -> draw(poseStack, collector, head, en.getValue(), cos, eq.colors, t, 0f, yOff);
					case "body" -> draw(poseStack, collector, body, en.getValue(), cos, eq.colors, t, 0f, 0f);
					case "rArm" -> draw(poseStack, collector, ra, en.getValue(), cos, eq.colors, t, -1f, 0f);
					case "lArm" -> draw(poseStack, collector, la, en.getValue(), cos, eq.colors, t, 1f, 0f);
					default -> {}
				}
			}
		}

		if (em != null && !em.fx.isEmpty()) {
			float te = Emotes.time(), stopAt = Emotes.stopAt();
			for (Map.Entry<String, List<Data.Part>> en : em.fx.entrySet()) {
				for (Data.Part pd : en.getValue()) {
					// props pop up when they appear and pop out when they leave (or when the emote ends)
					float p0 = pd.vis != null ? pd.vis[0] : 0f, p1 = pd.vis != null ? pd.vis[1] : stopAt, pp = Math.max(0.05f, pd.pop);
					float sc = (te < p0 ? 0f : Emotes.easeBack((te - p0) / pp)) * (te > p1 ? 1f - Emotes.smooth((te - p1) / pp) : 1f);
					if (sc <= 0.002f) continue;
					switch (en.getKey()) {
						case "head" -> drawFx(poseStack, collector, head, pd, te, 0f, sc);
						case "body" -> drawFx(poseStack, collector, body, pd, te, 0f, sc);
						case "rArm" -> drawFx(poseStack, collector, ra, pd, te, -1f, sc);
						case "lArm" -> drawFx(poseStack, collector, la, pd, te, 1f, sc);
						case "root" -> drawFx(poseStack, collector, null, pd, te, 0f, sc);
						default -> {}
					}
				}
			}
		}

		if (lo.cape != null && lo.cape.length > 0) {
			int[] frame = lo.cape[(int) (t * lo.capeFps) % lo.cape.length];
			float tilt = 0.12f + 0.04f * (float) Math.sin(t * 1.8f);
			poseStack.pushPose();
			body.translateAndRotate(poseStack);
			collector.submitCustomGeometry(poseStack, RenderTypes.debugFilledBox(), (pose, buf) -> Draw.cape(pose, buf, frame, tilt));
			poseStack.popPose();
		}
	}
}
