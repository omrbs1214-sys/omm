package dev.nox.menu;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;

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
		c.submitCustomGeometry(ps, RenderTypes.debugFilledBox(), (pose, buf) -> Draw.part(pose, buf, pd, cos, cols, t, xOff, yOff));
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
			float te = Emotes.time();
			for (Map.Entry<String, Data.Part> en : em.fx.entrySet()) {
				switch (en.getKey()) {
					case "head" -> draw(poseStack, collector, head, en.getValue(), null, null, te, 0f, 0f);
					case "body" -> draw(poseStack, collector, body, en.getValue(), null, null, te, 0f, 0f);
					case "rArm" -> draw(poseStack, collector, ra, en.getValue(), null, null, te, -1f, 0f);
					case "lArm" -> draw(poseStack, collector, la, en.getValue(), null, null, te, 1f, 0f);
					default -> {}
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
