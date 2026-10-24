package pl.nebula.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

public final class Notifications {
    private static final long LIFE = 2400L;

    private static class Toast {
        final String module; final Text text; final boolean on; final long time;
        Toast(String module, Text text, boolean on, long time) {
            this.module = module; this.text = text; this.on = on; this.time = time;
        }
    }

    private static final List<Toast> toasts = new ArrayList<>();

    private Notifications() {}

    public static void push(String module, boolean on) {
        toasts.removeIf(t -> t.module.equals(module));
        MutableText t = Text.literal(module + " ").formatted(Formatting.WHITE)
                .append(Text.literal(on ? "enabled" : "disabled").formatted(on ? Formatting.GREEN : Formatting.RED));
        toasts.add(new Toast(module, t, on, System.currentTimeMillis()));
    }

    public static void render(DrawContext ctx) {
        if (toasts.isEmpty()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        long now = System.currentTimeMillis();
        toasts.removeIf(t -> now - t.time > LIFE);

        int y = 8;
        for (Toast t : toasts) {
            long age = now - t.time;
            int w = tr.getWidth(t.text) + 18;
            int h = 18;

            float in = MathHelper.clamp(age / 250f, 0f, 1f);
            in = 1f - (1f - in) * (1f - in) * (1f - in);
            float out = age > LIFE - 350 ? (LIFE - age) / 350f : 1f;
            int alpha = MathHelper.clamp((int) (out * 255), 8, 255);

            int x = ctx.getScaledWindowWidth() - (int) ((w + 8) * in);
            int bg = ((int) (alpha * 0.75f) << 24) | 0x101018;
            int bar = (alpha << 24) | (t.on ? 0x55FF55 : 0xFF5555);

            ctx.fill(x, y, x + w, y + h, bg);
            ctx.fill(x, y, x + 2, y + h, bar);
            ctx.drawText(tr, t.text, x + 8, y + 5, (alpha << 24) | 0xFFFFFF, true);
            y += h + 4;
        }
    }
}
