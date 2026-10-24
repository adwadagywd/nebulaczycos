package pl.nebula.module;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import pl.nebula.render.Gfx;
import pl.nebula.setting.Setting.*;
import pl.nebula.util.ColorMode;
import pl.nebula.util.Colors;
import pl.nebula.util.Targets;

import java.util.List;

public class Tracers extends Module {
    public enum Target { FEET, BODY, HEAD }

    public final NumberSetting thickness = add(new NumberSetting("Thickness", 1.5, 0.5, 8.0, 0.25));
    public final NumberSetting range = add(new NumberSetting("Max Distance", 256, 16, 512, 8));
    public final NumberSetting alpha = add(new NumberSetting("Opacity", 0.9, 0.1, 1.0, 0.05));
    public final EnumSetting<Target> target = add(new EnumSetting<>("Target", Target.BODY));
    public final EnumSetting<ColorMode> colorMode = add(new EnumSetting<>("Color Mode", ColorMode.DISTANCE));
    public final ColorSetting color = add(new ColorSetting("Color", 0.55f, 0.8f));
    public final BoolSetting invisible = add(new BoolSetting("Invisible Players", true));

    public Tracers() {
        super("Tracers", "Lines from the crosshair to players");
        color.visibleIf(() -> colorMode.get() == ColorMode.STATIC);
    }

    @Override
    public void render3d(Gfx g) {
        List<PlayerEntity> players = Targets.players(g.cam, range.d(), invisible.on(), false);
        if (players.isEmpty()) return;

        g.begin();
        for (PlayerEntity p : players) {
            Vec3d rel = p.getLerpedPos(g.tickDelta).subtract(g.cam);
            double dist = rel.length();
            double yOff = switch (target.get()) {
                case FEET -> 0.0;
                case BODY -> p.getHeight() * 0.5;
                case HEAD -> p.getHeight() * 0.9;
            };
            Vec3d end = rel.add(0, yOff, 0);

            int rgb = Colors.resolve(colorMode.get(), color.rgb(), p, dist);
            g.ribbon(g.crosshair, end, thickness.f(), Colors.argb(rgb, alpha.f()));
        }
        g.end();
    }
}
