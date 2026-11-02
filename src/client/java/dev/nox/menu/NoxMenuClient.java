package dev.nox.menu;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoxMenuClient implements ClientModInitializer {
	public static final Logger LOG = LoggerFactory.getLogger("NoxMenu");
	private static KeyMapping openKey;
	private int tick;

	@Override
	public void onInitializeClient() {
		NoxState.load();

		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("nox", "main"));
		openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.nox.menu", GLFW.GLFW_KEY_RIGHT_SHIFT, category));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openKey.consumeClick()) {
				if (client.gui.screen() == null) client.gui.setScreen(new CosmeticsScreen(null));
			}
			// every ~2s: did the launcher bake a new avatar? then reload resources so the new sprite sheet shows up
			if (++tick % 40 == 0) {
				int old = NoxState.ver;
				if (NoxState.load() && old != -1 && NoxState.ver != old) reloadResources(client);
			}
		});

		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			if (screen instanceof TitleScreen || screen.getClass().getSimpleName().equals("PauseScreen")) attach(client, screen);
		});
	}

	private static void attach(Minecraft client, Screen screen) {
		int areaX = 8;
		int areaW = Math.max(110, screen.width / 2 - 130);
		int areaY = 34;
		int areaH = Math.max(80, screen.height - areaY - 52);

		Button b = Button.builder(Component.literal("Cosmetics"), btn -> client.gui.setScreen(new CosmeticsScreen(screen)))
			.pos(areaX + (areaW - 100) / 2, areaY + areaH + 6)
			.size(100, 20)
			.build();
		Screens.getWidgets(screen).add(b);

		ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, tickDelta) ->
			AvatarRenderer.draw(graphics, areaX, areaY, areaW, areaH));
	}

	/** Reflection keeps this compile-safe if Mojang renames the method; worst case the new sprite shows after F3+T / restart. */
	private static void reloadResources(Minecraft client) {
		try {
			client.getClass().getMethod("reloadResourcePacks").invoke(client);
		} catch (Throwable t) {
			LOG.warn("Could not reload resources automatically (press F3+T)", t);
		}
	}
}
