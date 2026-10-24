package pl.nebula.module;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import pl.nebula.render.Gfx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModuleManager {
    private static final List<Module> modules = new ArrayList<>();
    private static final List<Module> renderOrder = new ArrayList<>();

    public static Esp esp;
    public static Tracers tracers;
    public static Nametags nametags;
    public static Fullbright fullbright;
    public static HitboxExpander hitbox;
    public static Freecam freecam;
    public static TriggerBot triggerBot;
    public static ClickGui clickGui;

    private ModuleManager() {}

    public static void init() {
        esp = new Esp();
        tracers = new Tracers();
        nametags = new Nametags();
        fullbright = new Fullbright();
        hitbox = new HitboxExpander();
        freecam = new Freecam();
        triggerBot = new TriggerBot();
        clickGui = new ClickGui();

        Collections.addAll(modules, esp, tracers, nametags, fullbright, hitbox, freecam, triggerBot, clickGui);
        // order matters: nametags last (flushes vertex consumers)
        Collections.addAll(renderOrder, hitbox, esp, tracers, nametags);
    }

    public static List<Module> all() { return Collections.unmodifiableList(modules); }

    public static void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        for (Module m : modules) if (m.isEnabled()) m.onTick();
    }

    /** Called every frame so even very short key taps are caught. */
    public static void pollKeys() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.getWindow() == null) return;
        long handle = mc.getWindow().getHandle();
        for (Module m : modules) {
            int k = m.getKey();
            if (k <= 0) { m.keyDown = false; continue; }
            boolean down = InputUtil.isKeyPressed(handle, k);
            if (down && !m.keyDown && mc.currentScreen == null) m.toggle();
            m.keyDown = down;
        }
    }

    public static void render3d(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        boolean any = false;
        for (Module m : renderOrder) if (m.isEnabled()) { any = true; break; }
        if (!any) return;

        Gfx g = Gfx.create(ctx);
        g.state(true);
        try {
            for (Module m : renderOrder) if (m.isEnabled()) m.render3d(g);
        } finally {
            g.end();
            g.state(false);
        }
    }

    public static void onDisconnect() {
        if (freecam != null && freecam.isEnabled()) freecam.setEnabled(false, false);
    }
}
