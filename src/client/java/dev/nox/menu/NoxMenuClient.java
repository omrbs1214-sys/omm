package dev.nox.menu;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoxMenuClient implements ClientModInitializer {
	public static final Logger LOG = LoggerFactory.getLogger("NoxMenu");
	public static final RenderStateDataKey<Boolean> LOCAL = RenderStateDataKey.create(() -> "nox local player");
	private int tick;

	@Override
	public void onInitializeClient() {
		Data.poll();

		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("nox", "main"));
		KeyMapping wheelKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.nox.emotes", GLFW.GLFW_KEY_B, category));
		KeyMapping stopKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.nox.stop", GLFW.GLFW_KEY_X, category));

		LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
			if (entityRenderer instanceof AvatarRenderer) {
				registrationHelper.register(new CosmeticLayer((AvatarRenderer) entityRenderer));
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (++tick % 20 == 0) Data.poll();
			while (wheelKey.consumeClick()) {
				if (client.player != null && client.gui.screen() == null) client.gui.setScreen(new EmoteWheelScreen());
			}
			while (stopKey.consumeClick()) Emotes.stop();
			// walking away cancels an emote
			if (Emotes.active() != null && client.player != null && client.player.getDeltaMovement().horizontalDistanceSqr() > 0.0036) Emotes.stop();
		});
		LOG.info("Nox Menu ready");
	}
}
