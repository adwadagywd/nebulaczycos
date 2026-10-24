package pl.nebula.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import pl.nebula.module.ModuleManager;

import java.util.ArrayList;
import java.util.List;

public final class Targets {
    private Targets() {}

    public static List<PlayerEntity> players(Vec3d cam, double range, boolean invisible, boolean selfInFreecam) {
        MinecraftClient mc = MinecraftClient.getInstance();
        List<PlayerEntity> out = new ArrayList<>();
        if (mc.world == null) return out;
        boolean fc = ModuleManager.freecam != null && ModuleManager.freecam.isEnabled();
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p.isRemoved() || p.isSpectator()) continue;
            if (p == mc.player && !(selfInFreecam && fc)) continue;
            if (!invisible && p.isInvisible()) continue;
            if (p.getPos().distanceTo(cam) > range) continue;
            out.add(p);
        }
        return out;
    }
}
