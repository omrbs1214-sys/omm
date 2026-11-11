package dev.nox.menu;

import net.minecraft.client.CameraType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Free-Fire style 8 slot emote wheel made of pictures only (no buttons, no text).
 * Pictures live in the mod jar: assets/nox/textures/gui/emotes/<emote id>.png  (512x512, replace them with your own art).
 * Slots are chosen in Nox Launcher, Emotes tab.
 */
public class EmoteWheelScreen extends Screen {
	private static final int TEX = 512;
	private static final Map<String, Identifier> CACHE = new HashMap<>();
	private final long opened = System.nanoTime();
	private final int[] px = new int[8], py = new int[8];
	private int size = 80;
	private int hover = -1;

	public EmoteWheelScreen() {
		super(Component.literal("Emotes"));
	}

	private static Identifier icon(String id) {
		return CACHE.computeIfAbsent(id, k -> Identifier.fromNamespaceAndPath("nox", "textures/gui/emotes/" + k + ".png"));
	}

	@Override
	protected void init() {
		int cx = this.width / 2, cy = this.height / 2;
		int rad = Math.max(70, Math.min(this.width, this.height) / 3);
		size = Math.max(48, Math.min(this.width, this.height) / 5);
		for (int i = 0; i < 8; i++) {
			double a = Math.toRadians(-90 + i * 45);
			px[i] = cx + (int) Math.round(Math.cos(a) * rad * 1.25);
			py[i] = cy + (int) Math.round(Math.sin(a) * rad);
		}
	}

	private int slotAt(double mx, double my) {
		List<String> slots = Data.loadout.emotes;
		for (int i = 0; i < 8; i++) {
			if (i >= slots.size() || slots.get(i) == null || !Data.EMOTES.containsKey(slots.get(i))) continue;
			if (Math.abs(mx - px[i]) <= size / 2.0 + 2 && Math.abs(my - py[i]) <= size / 2.0 + 2) return i;
		}
		return -1;
	}

	/** called from the mouse-click event registered in NoxMenuClient */
	public void click(double mx, double my) {
		int i = slotAt(mx, my);
		if (i >= 0) {
			String id = Data.loadout.emotes.get(i);
			Emotes.play(id);
			// emotes are best seen from the front; you can still switch with F5 afterwards
			if (this.minecraft != null) this.minecraft.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
		}
		this.onClose();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		// no extractBackground() here: Screen already draws it once per frame (drawing it twice crashes 26.x: blur only once per frame)
		super.extractRenderState(g, mouseX, mouseY, delta);
		hover = slotAt(mouseX, mouseY);
		float age = (System.nanoTime() - opened) / 1_000_000_000f;
		List<String> slots = Data.loadout.emotes;
		for (int i = 0; i < 8; i++) {
			float pop = Emotes.easeBack((age - i * 0.04f) / 0.3f);        // slots pop open one after another
			if (pop <= 0.01f) continue;
			float sz = size * pop * (i == hover ? 1.16f : 1f);
			int x = Math.round(px[i] - sz / 2f), y = Math.round(py[i] - sz / 2f), s = Math.round(sz);
			String id = i < slots.size() ? slots.get(i) : null;
			boolean has = id != null && Data.EMOTES.containsKey(id);
			g.fill(x - 2, y - 2, x + s + 2, y + s + 2, i == hover ? 0xFFFFA500 : 0xFF3A2A14);   // frame (orange when hovered)
			if (has) {
				g.pose().pushMatrix();
				g.pose().translate((float) x, (float) y);
				g.pose().scale(sz / TEX, sz / TEX);
				g.blit(RenderPipelines.GUI_TEXTURED, icon(id), 0, 0, 0, 0, TEX, TEX, TEX, TEX);
				g.pose().popMatrix();
			} else {
				g.fill(x, y, x + s, y + s, 0xFF120C06);
			}
		}
	}

	@Override
	public void onClose() {
		if (this.minecraft != null) this.minecraft.gui.setScreen(null);
	}
}
