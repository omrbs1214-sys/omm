package dev.nox.menu;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads <gameDir>/nox/models.json (shapes + emotes) and loadout.json (what you wear) written by Nox Launcher. */
public final class Data {
	public static final class Anim {
		public String t = "", ax = "y";
		public float amp, sp, ph, r, y;
		public float[] pv = {0, 0, 0};
	}

	public static final class Box {
		public float[] p, s, r;
		public String c = "#ffffff";
		public boolean glow;
		public float alpha = 1f;
		public Anim a;
	}

	public static final class Part {
		public float[] tp, tr;
		public List<Box> boxes = new ArrayList<>();
	}

	public static final class Cos {
		public String id, name, slot = "";
		public Map<String, String> defColors = new HashMap<>();
		public Map<String, Part> parts = new HashMap<>();
	}

	/** joint curve: value(deg) = base + amp * sin(speed * t + phase) */
	public static final class Emote {
		public String id, name, icon;
		public float dur = 4f;
		public boolean loop;
		public Map<String, Map<String, float[]>> joints = new HashMap<>();
		public float[] dy;
		public Map<String, Part> fx = new HashMap<>();
	}

	public static final class Equipped {
		public String id;
		public Map<String, String> colors = new HashMap<>();
	}

	public static final class Loadout {
		public String name = "Player";
		public float faceDy;
		public List<Equipped> equipped = new ArrayList<>();
		public int[][] cape;       // frames of 160 ARGB colours
		public float capeFps = 8f;
		public List<String> emotes = new ArrayList<>();
	}

	public static final Path DIR = FabricLoader.getInstance().getGameDir().resolve("nox");
	public static final Map<String, Cos> COS = new HashMap<>();
	public static final Map<String, Emote> EMOTES = new LinkedHashMap<>();
	public static volatile Loadout loadout = new Loadout();
	private static long modelsMod = -1, loadoutMod = -1;

	private Data() {}

	private static float[] arr(JsonElement e) {
		if (e == null || !e.isJsonArray()) return null;
		JsonArray a = e.getAsJsonArray();
		float[] f = new float[a.size()];
		for (int i = 0; i < f.length; i++) f[i] = a.get(i).getAsFloat();
		return f;
	}

	private static String str(JsonObject o, String k, String d) {
		return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : d;
	}

	private static Part part(JsonObject o) {
		Part p = new Part();
		if (o.has("T")) {
			JsonObject t = o.getAsJsonObject("T");
			p.tp = arr(t.get("p"));
			p.tr = arr(t.get("r"));
		}
		for (JsonElement e : o.getAsJsonArray("b")) {
			JsonObject b = e.getAsJsonObject();
			Box x = new Box();
			x.p = arr(b.get("p"));
			x.s = arr(b.get("s"));
			x.r = arr(b.get("r"));
			x.c = str(b, "c", "#ffffff");
			x.glow = b.has("g");
			x.alpha = b.has("o") ? b.get("o").getAsFloat() : 1f;
			if (b.has("a")) {
				JsonObject a = b.getAsJsonObject("a");
				Anim an = new Anim();
				an.t = str(a, "t", "");
				an.ax = str(a, "ax", "y");
				an.amp = a.has("amp") ? a.get("amp").getAsFloat() : 0;
				an.sp = a.has("sp") ? a.get("sp").getAsFloat() : 0;
				an.ph = a.has("ph") ? a.get("ph").getAsFloat() : 0;
				an.r = a.has("r") ? a.get("r").getAsFloat() : 0;
				an.y = a.has("y") ? a.get("y").getAsFloat() : 0;
				float[] pv = arr(a.get("pv"));
				if (pv != null) an.pv = pv;
				x.a = an;
			}
			p.boxes.add(x);
		}
		return p;
	}

	private static Map<String, Part> parts(JsonObject o) {
		Map<String, Part> m = new HashMap<>();
		for (Map.Entry<String, JsonElement> en : o.entrySet()) m.put(en.getKey(), part(en.getValue().getAsJsonObject()));
		return m;
	}

	private static int argb(String hex) {
		try {
			return 0xFF000000 | Integer.parseInt(hex.substring(1), 16);
		} catch (Exception e) {
			return 0xFFFFFFFF;
		}
	}

	private static void loadModels() throws Exception {
		JsonObject root = JsonParser.parseString(Files.readString(DIR.resolve("models.json"))).getAsJsonObject();
		Map<String, Cos> cos = new HashMap<>();
		for (JsonElement e : root.getAsJsonArray("cosmetics")) {
			JsonObject o = e.getAsJsonObject();
			Cos c = new Cos();
			c.id = o.get("id").getAsString();
			c.name = str(o, "name", c.id);
			c.slot = str(o, "slot", "");
			for (JsonElement ce : o.getAsJsonArray("colors")) {
				JsonArray a = ce.getAsJsonArray();
				c.defColors.put(a.get(0).getAsString(), a.get(2).getAsString());
			}
			c.parts = parts(o.getAsJsonObject("parts"));
			cos.put(c.id, c);
		}
		Map<String, Emote> emo = new LinkedHashMap<>();
		for (JsonElement e : root.getAsJsonArray("emotes")) {
			JsonObject o = e.getAsJsonObject();
			Emote m = new Emote();
			m.id = o.get("id").getAsString();
			m.name = str(o, "name", m.id);
			m.icon = str(o, "icon", "");
			m.dur = o.has("dur") ? o.get("dur").getAsFloat() : 4f;
			m.loop = o.has("loop") && o.get("loop").getAsBoolean();
			for (Map.Entry<String, JsonElement> j : o.getAsJsonObject("joints").entrySet()) {
				Map<String, float[]> axes = new HashMap<>();
				for (Map.Entry<String, JsonElement> a : j.getValue().getAsJsonObject().entrySet()) axes.put(a.getKey(), arr(a.getValue()));
				m.joints.put(j.getKey(), axes);
			}
			m.dy = arr(o.get("dy"));
			if (o.has("fx")) m.fx = parts(o.getAsJsonObject("fx"));
			emo.put(m.id, m);
		}
		synchronized (Data.class) {
			COS.clear();
			COS.putAll(cos);
			EMOTES.clear();
			EMOTES.putAll(emo);
		}
	}

	private static void loadLoadout() throws Exception {
		JsonObject o = JsonParser.parseString(Files.readString(DIR.resolve("loadout.json"))).getAsJsonObject();
		Loadout l = new Loadout();
		l.name = str(o, "name", "Player");
		l.faceDy = o.has("faceDy") ? o.get("faceDy").getAsFloat() : 0f;
		if (o.has("equipped")) for (JsonElement e : o.getAsJsonArray("equipped")) {
			JsonObject q = e.getAsJsonObject();
			Equipped eq = new Equipped();
			eq.id = q.get("id").getAsString();
			if (q.has("c")) for (Map.Entry<String, JsonElement> c : q.getAsJsonObject("c").entrySet()) eq.colors.put(c.getKey(), c.getValue().getAsString());
			l.equipped.add(eq);
		}
		if (o.has("cape") && o.get("cape").isJsonObject()) {
			JsonObject c = o.getAsJsonObject("cape");
			JsonArray fr = c.getAsJsonArray("frames");
			l.capeFps = c.has("fps") ? c.get("fps").getAsFloat() : 8f;
			l.cape = new int[fr.size()][];
			for (int i = 0; i < fr.size(); i++) {
				JsonArray px = fr.get(i).getAsJsonArray();
				l.cape[i] = new int[px.size()];
				for (int k = 0; k < px.size(); k++) l.cape[i][k] = argb(px.get(k).getAsString());
			}
		}
		if (o.has("emotes")) for (JsonElement e : o.getAsJsonArray("emotes")) l.emotes.add(e.getAsString());
		loadout = l;
	}

	/** cheap: only re-reads a file when its modified time changed */
	public static void poll() {
		try {
			Path m = DIR.resolve("models.json");
			if (Files.exists(m)) {
				long t = Files.getLastModifiedTime(m).toMillis();
				if (t != modelsMod) {
					modelsMod = t;
					loadModels();
				}
			}
			Path l = DIR.resolve("loadout.json");
			if (Files.exists(l)) {
				long t = Files.getLastModifiedTime(l).toMillis();
				if (t != loadoutMod) {
					loadoutMod = t;
					loadLoadout();
				}
			}
		} catch (Exception e) {
			NoxMenuClient.LOG.warn("Nox data read failed: {}", e.toString());
		}
	}

	public static int color(String c, Map<String, String> chosen, Cos cos) {
		if (c.startsWith("#")) return argb(c);
		String v = chosen != null ? chosen.get(c) : null;
		if (v == null && cos != null) v = cos.defColors.get(c);
		return v == null ? 0xFFFFFFFF : argb(v);
	}
}
