package pl.nebula.module;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import pl.nebula.render.Gfx;
import pl.nebula.setting.Setting;
import pl.nebula.setting.Setting.*;
import pl.nebula.util.ColorMode;
import pl.nebula.util.Colors;
import pl.nebula.util.Targets;

import java.util.List;

public class Esp extends Module {
    public enum Mode { BOX, FILLED, FRAME }

    public final EnumSetting<Mode> mode = add(new EnumSetting<>("Mode", Mode.BOX));
    public final BoolSetting skeleton = add(new BoolSetting("Skeleton", false));
    public final NumberSetting width = add(new NumberSetting("Line Width", 2.0, 0.5, 8.0, 0.5));
    public final NumberSetting fill = add(new NumberSetting("Fill Alpha", 0.25, 0.05, 0.8, 0.05));
    public final EnumSetting<ColorMode> colorMode = add(new EnumSetting<>("Color Mode", ColorMode.STATIC));
    public final ColorSetting color = add(new ColorSetting("Color", 0.75f, 0.8f));
    public final NumberSetting range = add(new NumberSetting("Max Distance", 128, 16, 512, 8));
    public final BoolSetting invisible = add(new BoolSetting("Invisible Players", true));
    public final BoolSetting self = add(new BoolSetting("Self (in Freecam)", true));

    public Esp() {
        super("ESP", "Draws boxes / frames / skeletons around players");
        fill.visibleIf(() -> mode.get() == Mode.FILLED);
        color.visibleIf(() -> colorMode.get() == ColorMode.STATIC);
    }

    @Override
    public void render3d(Gfx g) {
        List<PlayerEntity> players = Targets.players(g.cam, range.d(), invisible.on(), self.on());
        if (players.isEmpty()) return;

        g.begin();
        for (PlayerEntity p : players) {
            Vec3d rel = p.getLerpedPos(g.tickDelta).subtract(g.cam);
            // use real dimensions, NOT getBoundingBox() -> never affected by anything else
            double w = p.getWidth(), h = p.getHeight();
            double dist = rel.length();

            int rgb = Colors.resolve(colorMode.get(), color.rgb(), p, dist);
            int solid = Colors.argb(rgb, 1f);
            float lw = width.f();

            Box b = new Box(rel.x - w / 2, rel.y, rel.z - w / 2, rel.x + w / 2, rel.y + h, rel.z + w / 2);

            switch (mode.get()) {
                case BOX -> g.box(b, lw, solid);
                case FILLED -> {
                    g.filledBox(b, Colors.argb(rgb, fill.f()));
                    g.box(b, lw, solid);
                }
                case FRAME -> g.frame(rel.add(0, h / 2, 0), Math.max(w, 0.6) + 0.2, h + 0.1, lw, solid);
            }

            if (skeleton.on()) skeleton(g, p, rel, lw, solid);
        }
        g.end();
    }

    private void skeleton(Gfx g, PlayerEntity p, Vec3d rel, float px, int argb) {
        float yaw = MathHelper.lerpAngleDegrees(g.tickDelta, p.prevBodyYaw, p.bodyYaw);
        double r = Math.toRadians(yaw);
        Vec3d right = new Vec3d(Math.cos(r), 0, Math.sin(r));
        double s = p.getHeight() / 1.8;

        // pt(sideways offset, height in standing-player units)
        java.util.function.BiFunction<Double, Double, Vec3d> pt =
                (a, y) -> rel.add(right.multiply(a * s)).add(0, y * s, 0);

        g.ribbon(pt.apply(0.0, 1.8), pt.apply(0.0, 1.4), px, argb);          // head -> neck
        g.ribbon(pt.apply(-0.3, 1.4), pt.apply(0.3, 1.4), px, argb);         // shoulders
        g.ribbon(pt.apply(0.0, 1.4), pt.apply(0.0, 0.75), px, argb);         // spine
        g.ribbon(pt.apply(-0.125, 0.75), pt.apply(0.125, 0.75), px, argb);   // hips
        for (int side = -1; side <= 1; side += 2) {
            double sd = side;
            g.ribbon(pt.apply(0.3 * sd, 1.4), pt.apply(0.37 * sd, 0.72), px, argb);                     // arm
            g.ribbon(pt.apply(0.125 * sd, 0.75), pt.apply(0.125 * sd, 0.4), px, argb);                  // thigh
            g.ribbon(pt.apply(0.125 * sd, 0.4), pt.apply(0.125 * sd, 0.0), px, argb);                   // shin
        }
    }
}
