package pl.nebula.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import pl.nebula.module.Module;
import pl.nebula.module.ModuleManager;
import pl.nebula.setting.Setting;

import java.nio.file.Files;
import java.nio.file.Path;

public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private Config() {}

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("nebula.json");
    }

    public static void save() {
        try {
            JsonObject root = new JsonObject();
            for (Module m : ModuleManager.all()) {
                JsonObject o = new JsonObject();
                o.addProperty("bind", m.getKey());
                JsonObject s = new JsonObject();
                for (Setting<?> st : m.settings) s.add(st.name, st.toJson());
                o.add("settings", s);
                root.add(m.name, o);
            }
            Files.writeString(path(), GSON.toJson(root));
        } catch (Exception e) {
            System.err.println("[Nebula] Could not save config: " + e);
        }
    }

    public static void load() {
        Path p = path();
        if (!Files.exists(p)) return;
        try {
            JsonObject root = GSON.fromJson(Files.readString(p), JsonObject.class);
            if (root == null) return;
            for (Module m : ModuleManager.all()) {
                JsonElement me = root.get(m.name);
                if (me == null || !me.isJsonObject()) continue;
                JsonObject o = me.getAsJsonObject();
                if (o.has("bind")) m.setKey(o.get("bind").getAsInt());
                if (o.has("settings")) {
                    JsonObject s = o.getAsJsonObject("settings");
                    for (Setting<?> st : m.settings) {
                        try {
                            if (s.has(st.name)) st.fromJson(s.get(st.name));
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[Nebula] Could not load config: " + e);
        }
    }
}
