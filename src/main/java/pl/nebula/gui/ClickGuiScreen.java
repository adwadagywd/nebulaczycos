package pl.nebula.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import pl.nebula.config.Config;
import pl.nebula.module.Module;
import pl.nebula.module.ModuleManager;
import pl.nebula.setting.Setting;
import pl.nebula.setting.Setting.*;

import java.util.ArrayList;
import java.util.List;

public class ClickGuiScreen extends Screen {
    private static final int PW = 440, LIST_W = 112, RH = 16, GAP = 2;

    private Module selected = ModuleManager.all().get(0);
    private Module listening;
    private int scroll;
    private Row dragRow;

    public ClickGuiScreen() {
        super(Text.literal("Nebula"));
    }

    @Override public boolean shouldPause() { return false; }
    @Override public void removed() { Config.save(); }

    private int px() { return (width - PW) / 2; }
    private int ph() { return Math.min(310, height - 20); }
    private int py() { return (height - ph()) / 2; }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static String keyName(int k) {
        if (k <= 0) return "NONE";
        return InputUtil.fromKeyCode(k, 0).getLocalizedText().getString().toUpperCase();
    }

    // ------------------------------------------------------------------ rows
    private abstract class Row {
        int x, w;
        boolean draggable;
        abstract void draw(DrawContext c, int x, int y, int w, int mx, int my);
        void click(double mx, int btn) {}
        void drag(double mx) {}
        void base(DrawContext c, int x, int y, int w, int mx, int my) {
            this.x = x; this.w = w;
            c.fill(x, y, x + w, y + RH, in(mx, my, x, y, w, RH) ? 0xFF262634 : 0xFF1A1A24);
        }
    }

    private class EnabledRow extends Row {
        final Module m;
        EnabledRow(Module m) { this.m = m; }
        @Override void draw(DrawContext c, int x, int y, int w, int mx, int my) {
            base(c, x, y, w, mx, my);
            c.drawText(textRenderer, "Enabled", x + 6, y + 4, 0xFFFFFFFF, true);
            String s = m.isEnabled() ? "ON" : "OFF";
            c.drawText(textRenderer, s, x + w - 6 - textRenderer.getWidth(s), y + 4, m.isEnabled() ? 0xFF55FF55 : 0xFFFF5555, true);
        }
        @Override void click(double mx, int btn) { m.toggle(); }
    }

    private class BindRow extends Row {
        final Module m;
        BindRow(Module m) { this.m = m; }
        @Override void draw(DrawContext c, int x, int y, int w, int mx, int my) {
            base(c, x, y, w, mx, my);
            c.drawText(textRenderer, "Bind", x + 6, y + 4, 0xFFFFFFFF, true);
            String s = listening == m ? "press a key..." : keyName(m.getKey());
            c.drawText(textRenderer, s, x + w - 6 - textRenderer.getWidth(s), y + 4, listening == m ? 0xFFFFDD55 : 0xFFB388FF, true);
        }
        @Override void click(double mx, int btn) {
            if (btn == 1) { m.setKey(GLFW.GLFW_KEY_UNKNOWN); listening = null; }
            else listening = m;
        }
    }

    private class BoolRow extends Row {
        final BoolSetting s;
        BoolRow(BoolSetting s) { this.s = s; }
        @Override void draw(DrawContext c, int x, int y, int w, int mx, int my) {
            base(c, x, y, w, mx, my);
            c.drawText(textRenderer, s.name, x + 6, y + 4, 0xFFFFFFFF, true);
            String t = s.on() ? "ON" : "OFF";
            c.drawText(textRenderer, t, x + w - 6 - textRenderer.getWidth(t), y + 4, s.on() ? 0xFF55FF55 : 0xFFFF5555, true);
        }
        @Override void click(double mx, int btn) { s.toggle(); }
    }

    private class NumRow extends Row {
        final NumberSetting s;
        NumRow(NumberSetting s) { this.s = s; draggable = true; }
        @Override void draw(DrawContext c, int x, int y, int w, int mx, int my) {
            base(c, x, y, w, mx, my);
            double t = (s.d() - s.min) / (s.max - s.min);
            c.fill(x, y, x + (int) (w * t), y + RH, 0xFF5B3FA0);
            c.drawText(textRenderer, s.name, x + 6, y + 4, 0xFFFFFFFF, true);
            String v = s.display();
            c.drawText(textRenderer, v, x + w - 6 - textRenderer.getWidth(v), y + 4, 0xFFDDDDDD, true);
        }
        @Override void click(double mx, int btn) { drag(mx); }
        @Override void drag(double mx) {
            double t = MathHelper.clamp((mx - x) / (double) w, 0.0, 1.0);
            s.set(s.min + t * (s.max - s.min));
        }
    }

    private class EnumRow<E extends Enum<E>> extends Row {
        final EnumSetting<E> s;
        EnumRow(EnumSetting<E> s) { this.s = s; }
        @Override void draw(DrawContext c, int x, int y, int w, int mx, int my) {
            base(c, x, y, w, mx, my);
            c.drawText(textRenderer, s.name, x + 6, y + 4, 0xFFFFFFFF, true);
            String v = s.get().name();
            c.drawText(textRenderer, v, x + w - 6 - textRenderer.getWidth(v), y + 4, 0xFF66CCFF, true);
        }
        @Override void click(double mx, int btn) { if (btn == 1) s.prev(); else s.next(); }
    }

    private class HueRow extends Row {
        final ColorSetting s;
        HueRow(ColorSetting s) { this.s = s; draggable = true; }
        @Override void draw(DrawContext c, int x, int y, int w, int mx, int my) {
            this.x = x; this.w = w;
            for (int i = 0; i < w; i += 2) {
                int rgb = MathHelper.hsvToRgb(i / (float) w, Math.max(0.5f, s.sat), 1f) & 0xFFFFFF;
                c.fill(x + i, y, Math.min(x + i + 2, x + w), y + RH, 0xFF000000 | rgb);
            }
            int mxp = x + (int) (s.get() * (w - 2));
            c.fill(mxp - 1, y, mxp + 2, y + RH, 0xFFFFFFFF);
            c.drawText(textRenderer, s.name + " (hue)", x + 6, y + 4, 0xFFFFFFFF, true);
            int sw = x + w - 18;
            c.fill(sw - 1, y + 2, sw + 13, y + RH - 2, 0xFFFFFFFF);
            c.fill(sw, y + 3, sw + 12, y + RH - 3, 0xFF000000 | s.rgb());
        }
        @Override void click(double mx, int btn) { drag(mx); }
        @Override void drag(double mx) { s.set((float) MathHelper.clamp((mx - x) / (double) (w - 2), 0.0, 1.0)); }
    }

    private class SatRow extends Row {
        final ColorSetting s;
        SatRow(ColorSetting s) { this.s = s; draggable = true; }
        @Override void draw(DrawContext c, int x, int y, int w, int mx, int my) {
            this.x = x; this.w = w;
            for (int i = 0; i < w; i += 2) {
                int rgb = MathHelper.hsvToRgb(s.get(), i / (float) w, 1f) & 0xFFFFFF;
                c.fill(x + i, y, Math.min(x + i + 2, x + w), y + RH, 0xFF000000 | rgb);
            }
            int mxp = x + (int) (s.sat * (w - 2));
            c.fill(mxp - 1, y, mxp + 2, y + RH, 0xFF000000);
            c.drawText(textRenderer, s.name + " (saturation)", x + 6, y + 4, 0xFF000000, false);
        }
        @Override void click(double mx, int btn) { drag(mx); }
        @Override void drag(double mx) { s.sat = (float) MathHelper.clamp((mx - x) / (double) (w - 2), 0.0, 1.0); }
    }

    private List<Row> rows() {
        List<Row> r = new ArrayList<>();
        if (selected.hasToggle()) r.add(new EnabledRow(selected));
        r.add(new BindRow(selected));
        for (Setting<?> s : selected.settings) {
            if (!s.isVisible()) continue;
            if (s instanceof BoolSetting b) r.add(new BoolRow(b));
            else if (s instanceof NumberSetting n) r.add(new NumRow(n));
            else if (s instanceof EnumSetting<?> e) r.add(enumRow(e));
            else if (s instanceof ColorSetting cs) { r.add(new HueRow(cs)); r.add(new SatRow(cs)); }
        }
        return r;
    }

    private <E extends Enum<E>> Row enumRow(EnumSetting<E> e) { return new EnumRow<>(e); }

    // ------------------------------------------------------------------ render
    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        c.fill(0, 0, width, height, 0x80000000);

        int px = px(), py = py(), ph = ph();
        c.fill(px, py, px + PW, py + ph, 0xF0121218);
        c.fill(px, py, px + PW, py + 18, 0xFF1B1B26);
        c.drawText(textRenderer, "Nebula", px + 8, py + 5, 0xFFB388FF, true);
        c.drawText(textRenderer, selected.description, px + LIST_W + 8, py + 5, 0xFF888899, false);

        // module list
        List<Module> ms = ModuleManager.all();
        for (int i = 0; i < ms.size(); i++) {
            Module m = ms.get(i);
            int y = py + 24 + i * (RH + GAP);
            boolean hov = in(mx, my, px + 4, y, LIST_W - 8, RH);
            c.fill(px + 4, y, px + LIST_W - 4, y + RH, m == selected ? 0xFF2D2D44 : hov ? 0xFF22222F : 0xFF181822);
            c.drawText(textRenderer, m.name, px + 10, y + 4, m.isEnabled() ? 0xFF55FF88 : 0xFFCCCCCC, true);
        }
        c.drawText(textRenderer, "LMB select / RMB toggle", px + 6, py + ph - 12, 0xFF666677, false);
        c.fill(px + LIST_W, py + 18, px + LIST_W + 1, py + ph, 0xFF2A2A38);

        // settings
        int ax = px + LIST_W + 8, aw = PW - LIST_W - 16, ay = py + 24;
        List<Row> rows = rows();
        int total = rows.size() * (RH + GAP);
        int maxScroll = Math.max(0, total - (ph - 30));
        scroll = MathHelper.clamp(scroll, 0, maxScroll);

        c.enableScissor(px + LIST_W + 1, py + 20, px + PW, py + ph - 2);
        for (int i = 0; i < rows.size(); i++) {
            int y = ay + i * (RH + GAP) - scroll;
            if (y + RH < py + 20 || y > py + ph) continue;
            rows.get(i).draw(c, ax, y, aw, mx, my);
        }
        c.disableScissor();
    }

    // ------------------------------------------------------------------ input
    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int px = px(), py = py(), ph = ph();

        List<Module> ms = ModuleManager.all();
        for (int i = 0; i < ms.size(); i++) {
            int y = py + 24 + i * (RH + GAP);
            if (in(mx, my, px + 4, y, LIST_W - 8, RH)) {
                Module m = ms.get(i);
                if (btn == 1 && m.hasToggle()) m.toggle();
                else { selected = m; scroll = 0; listening = null; }
                return true;
            }
        }

        if (in(mx, my, px + LIST_W + 1, py + 20, PW - LIST_W - 1, ph - 22)) {
            int ax = px + LIST_W + 8, aw = PW - LIST_W - 16, ay = py + 24;
            List<Row> rows = rows();
            for (int i = 0; i < rows.size(); i++) {
                int y = ay + i * (RH + GAP) - scroll;
                if (in(mx, my, ax, y, aw, RH)) {
                    Row r = rows.get(i);
                    r.x = ax; r.w = aw;
                    r.click(mx, btn);
                    if (r.draggable) dragRow = r;
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (dragRow != null) { dragRow.drag(mx); return true; }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        dragRow = null;
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        scroll -= (int) (v * 14);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (listening != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) { /* cancel */ }
            else if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) listening.setKey(GLFW.GLFW_KEY_UNKNOWN);
            else listening.setKey(key);
            listening = null;
            return true;
        }
        if (key == ModuleManager.clickGui.getKey() || key == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }
}
