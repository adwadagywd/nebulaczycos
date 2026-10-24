package pl.nebula.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

public final class Colors {
    private Colors() {}

    public static int rainbow(float offset) {
        float h = ((System.currentTimeMillis() % 6000L) / 6000f + offset) % 1f;
        return MathHelper.hsvToRgb(h, 0.8f, 1f) & 0xFFFFFF;
    }

    public static int argb(int rgb, float alpha) {
        int a = MathHelper.clamp((int) (alpha * 255f), 5, 255);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    public static float healthFraction(PlayerEntity p) {
        return MathHelper.clamp((p.getHealth() + p.getAbsorptionAmount()) / Math.max(1f, p.getMaxHealth()), 0f, 1f);
    }

    public static int resolve(ColorMode mode, int baseRgb, PlayerEntity p, double dist) {
        return switch (mode) {
            case STATIC -> baseRgb;
            case RAINBOW -> rainbow(p.getId() * 0.05f);
            case HEALTH -> MathHelper.hsvToRgb(healthFraction(p) * 0.33f, 1f, 1f) & 0xFFFFFF;
            case DISTANCE -> MathHelper.hsvToRgb((float) Math.min(1.0, dist / 80.0) * 0.33f, 1f, 1f) & 0xFFFFFF;
        };
    }
}
