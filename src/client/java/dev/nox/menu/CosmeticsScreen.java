package dev.nox.menu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** In-game cosmetics picker (Right Shift, or the Cosmetics button on the title / pause screen). */
public class CosmeticsScreen extends Screen {
	private final Screen parent;
	private final List<String> eq = new ArrayList<>(NoxState.equipped);
	private final Map<String, Button> buttons = new LinkedHashMap<>();
	private int leftW;

	public CosmeticsScreen(Screen parent) {
		super(Component.literal("Nox Cosmetics"));
		this.parent = parent;
	}

	private Component label(NoxState.Cos c) {
		return Component.literal((eq.contains(c.id()) ? "[x] " : "[ ] ") + c.name());
	}

	@Override
	protected void init() {
		NoxState.load();
		eq.clear();
		eq.addAll(NoxState.equipped);
		buttons.clear();
		leftW = Math.max(140, this.width / 3);
		int gap = 4, bh = 20, top = 40;
		int n = Math.max(1, NoxState.catalog.size());
		int maxRows = Math.max(1, (this.height - top - 40) / (bh + gap));
		int cols = (int) Math.ceil(n / (double) maxRows);
		int bw = Math.max(70, Math.min(130, (this.width - leftW - 12) / cols - gap));
		int i = 0;
		for (NoxState.Cos c : NoxState.catalog) {
			int col = i / maxRows, row = i % maxRows;
			Button b = Button.builder(label(c), btn -> toggle(c))
				.pos(leftW + col * (bw + gap), top + row * (bh + gap))
				.size(bw, bh)
				.build();
			this.addRenderableWidget(b);
			buttons.put(c.id(), b);
			i++;
		}
		this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
			.pos(this.width / 2 - 50, this.height - 26)
			.size(100, 20)
			.build());
	}

	private void toggle(NoxState.Cos c) {
		if (eq.contains(c.id())) {
			eq.remove(c.id());
		} else {
			for (NoxState.Cos o : NoxState.catalog) if (o.slot().equals(c.slot())) eq.remove(o.id());
			eq.add(c.id());
		}
		for (NoxState.Cos o : NoxState.catalog) {
			Button b = buttons.get(o.id());
			if (b != null) b.setMessage(label(o));
		}
		NoxState.request(eq);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		this.extractBackground(g, mouseX, mouseY, delta);
		g.centeredText(this.font, "Nox Cosmetics", this.width / 2, 14, 0xFF19D3C5);
		if (NoxState.catalog.isEmpty()) {
			g.centeredText(this.font, "Start the game from Nox Launcher to load cosmetics.", this.width / 2, this.height / 2, 0xFFFFFFFF);
		} else {
			g.centeredText(this.font, "Your 3D preview updates a few seconds after a change (launcher must stay open).", this.width / 2, this.height - 40, 0xFF7F9D9B);
		}
		AvatarRenderer.draw(g, 10, 34, leftW - 20, this.height - 80);
		super.extractRenderState(g, mouseX, mouseY, delta);
	}

	@Override
	public void onClose() {
		if (this.minecraft != null) this.minecraft.setScreen(parent);
	}
}
