package dev.nox.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Draws the animated character sprite sheet that Nox Launcher bakes (skin + cosmetics included). */
public final class AvatarRenderer {
	private static final Identifier TEX = Identifier.fromNamespaceAndPath("nox", "textures/gui/avatar.png");

	private AvatarRenderer() {}

	public static void draw(GuiGraphicsExtractor g, int areaX, int areaY, int areaW, int areaH) {
		if (!NoxState.avatar || NoxState.frames <= 0) return;
		int fw = NoxState.fw, fh = NoxState.fh;
		float sc = Math.min(areaW / (float) fw, areaH / (float) fh);
		if (sc <= 0f) return;
		int dw = (int) (fw * sc), dh = (int) (fh * sc);
		int x = areaX + (areaW - dw) / 2, y = areaY + (areaH - dh) / 2;
		int idx = (int) ((System.currentTimeMillis() / (1000L / NoxState.fps)) % NoxState.frames);
		int u = (idx % NoxState.cols) * fw, v = (idx / NoxState.cols) * fh;
		int tw = NoxState.cols * fw, th = ((NoxState.frames + NoxState.cols - 1) / NoxState.cols) * fh;

		g.pose().pushMatrix();
		g.pose().translate((float) x, (float) y);
		g.pose().scale(sc, sc);
		g.blit(RenderPipelines.GUI_TEXTURED, TEX, 0, 0, u, v, fw, fh, tw, th);
		g.pose().popMatrix();

		// name tag
		Minecraft mc = Minecraft.getInstance();
		String n = NoxState.name.toUpperCase();
		int tagW = mc.font.width(n) + 16, tagX = areaX + (areaW - tagW) / 2, tagY = Math.max(4, y - 8);
		g.fill(tagX - 1, tagY - 1, tagX + tagW + 1, tagY + 15, 0xFF19D3C5);
		g.fill(tagX, tagY, tagX + tagW, tagY + 14, 0xE0050C0E);
		g.centeredText(mc.font, n, tagX + tagW / 2, tagY + 3, 0xFF19D3C5);
	}
}
