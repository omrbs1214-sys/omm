package dev.nox.menu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Free-Fire style 8 slot emote wheel (slots are set in Nox Launcher, Emotes tab). */
public class EmoteWheelScreen extends Screen {
	public EmoteWheelScreen() {
		super(Component.literal("Emotes"));
	}

	@Override
	protected void init() {
		List<String> slots = Data.loadout.emotes;
		int cx = this.width / 2, cy = this.height / 2;
		int rad = Math.max(70, Math.min(this.width, this.height) / 3);
		for (int i = 0; i < 8; i++) {
			double a = Math.toRadians(-90 + i * 45);
			int bx = cx + (int) Math.round(Math.cos(a) * rad * 1.35) - 48;
			int by = cy + (int) Math.round(Math.sin(a) * rad) - 10;
			String id = i < slots.size() ? slots.get(i) : null;
			Data.Emote e = id == null ? null : Data.EMOTES.get(id);
			String label = (i + 1) + "  " + (e == null ? "-" : e.name);
			Button b = Button.builder(Component.literal(label), btn -> {
				if (e != null) Emotes.play(e.id);
				this.onClose();
			}).pos(bx, by).size(96, 20).build();
			b.active = e != null;
			this.addRenderableWidget(b);
		}
		this.addRenderableWidget(Button.builder(Component.literal("Stop"), b -> {
			Emotes.stop();
			this.onClose();
		}).pos(cx - 30, cy - 10).size(60, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		// no extractBackground() here: Screen already draws it once per frame (drawing it twice crashes 26.x: blur only once per frame)
		super.extractRenderState(g, mouseX, mouseY, delta);
		g.centeredText(this.font, "Emote wheel", this.width / 2, 14, 0xFF19D3C5);
		if (Data.EMOTES.isEmpty()) g.centeredText(this.font, "Start the game from Nox Launcher to load emotes.", this.width / 2, this.height - 30, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		if (this.minecraft != null) this.minecraft.gui.setScreen(null);
	}
}
