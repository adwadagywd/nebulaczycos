package pl.nebula.module;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import pl.nebula.render.Gfx;
import pl.nebula.setting.Setting;
import pl.nebula.util.Notifications;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    public final String name;
    public final String description;
    public final List<Setting<?>> settings = new ArrayList<>();

    private boolean enabled;
    private int key = GLFW.GLFW_KEY_UNKNOWN;
    public boolean keyDown;

    protected Module(String name, String description) {
        this.name = name;
        this.description = description;
    }

    protected <T extends Setting<?>> T add(T s) {
        settings.add(s);
        return s;
    }

    public boolean isEnabled() { return enabled; }
    public boolean hasToggle() { return true; }
    public int getKey() { return key; }
    public void setKey(int key) { this.key = key; }

    public void toggle() { setEnabled(!enabled, true); }
    public void setEnabled(boolean v) { setEnabled(v, true); }

    public void setEnabled(boolean v, boolean notify) {
        if (enabled == v) return;
        enabled = v;
        if (v) onEnable(); else onDisable();
        if (notify) Notifications.push(name, v);
    }

    protected void onEnable() {}
    protected void onDisable() {}
    public void onTick() {}
    public void render3d(Gfx g) {}
}
