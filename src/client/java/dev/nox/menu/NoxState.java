package dev.nox.menu;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Shared state written by Nox Launcher into <gameDir>/nox/state.json; cosmetic requests go back via request.json. */
public final class NoxState {
	public record Cos(String id, String name, String slot, String rarity) {}

	public static final Path DIR = FabricLoader.getInstance().getGameDir().resolve("nox");
	public static String name = "Player";
	public static int ver = -1;
	public static boolean avatar = false;
	public static int fw = 224, fh = 336, cols = 16, frames = 192, fps = 8;
	public static final List<Cos> catalog = new ArrayList<>();
	public static final List<String> equipped = new ArrayList<>();
	private static long lastMod = -1;

	private NoxState() {}

	private static String str(JsonObject o, String k, String d) {
		return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : d;
	}

	private static int num(JsonObject o, String k, int d) {
		return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsInt() : d;
	}

	/** @return true if the file changed and was re-read */
	public static boolean load() {
		try {
			Path f = DIR.resolve("state.json");
			if (!Files.exists(f)) return false;
			long m = Files.getLastModifiedTime(f).toMillis();
			if (m == lastMod) return false;
			lastMod = m;
			JsonObject o = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
			name = str(o, "name", "Player");
			ver = num(o, "ver", 0);
			avatar = o.has("avatar") && o.get("avatar").getAsBoolean();
			fw = num(o, "fw", 224); fh = num(o, "fh", 336); cols = num(o, "cols", 16); frames = num(o, "frames", 192); fps = Math.max(1, num(o, "fps", 8));
			catalog.clear();
			if (o.has("catalog")) for (JsonElement e : o.getAsJsonArray("catalog")) {
				JsonObject c = e.getAsJsonObject();
				catalog.add(new Cos(str(c, "id", ""), str(c, "name", "?"), str(c, "slot", ""), str(c, "rarity", "common")));
			}
			equipped.clear();
			if (o.has("equipped")) for (JsonElement e : o.getAsJsonArray("equipped")) equipped.add(e.getAsString());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static void request(List<String> ids) {
		try {
			Files.createDirectories(DIR);
			JsonObject o = new JsonObject();
			JsonArray a = new JsonArray();
			for (String id : ids) a.add(id);
			o.add("equipped", a);
			o.addProperty("ts", System.currentTimeMillis());
			Files.writeString(DIR.resolve("request.json"), o.toString());
		} catch (Exception e) {
			NoxMenuClient.LOG.warn("Could not write request.json", e);
		}
	}
}
