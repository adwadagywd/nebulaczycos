package pl.nebula.module;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import pl.nebula.render.Gfx;
import pl.nebula.setting.Setting.*;
import pl.nebula.util.Colors;
import pl.nebula.util.Targets;

import java.util.List;

/**
 * Works by enlarging the "targeting margin" the client uses when ray-casting for the
 * entity under the crosshair (see EntityMixin). The entity's real bounding box is untouched,
 * so ESP, physics and server sync are never affected.
 * Max 1.0 because vanilla pre-selects candidates with a box that is only expanded by 1.0.
 */
public class HitboxExpander extends Module {
    public final NumberSetting expand = add(new NumberSetting("Expand", 0.3, 0.05, 1.0, 0.05));
    public final BoolSetting playersOnly = add(new BoolSetting("Players Only", true));
    public final BoolSetting show = add(new BoolSetting("Show Hitbox", false));
    public final NumberSetting width = add(new NumberSetting("Line Width", 1.5, 0.5, 6.0, 0.5));
    public final ColorSetting color = add(new ColorSetting("Color", 0.0f, 0.9f));

    public HitboxExpander() {
        super("HitboxExpander", "Makes player hitboxes easier to hit");
        width.visibleIf(show::on);
        color.visibleIf(show::on);
    }

    public float marginFor(Entity e) {
        if (!isEnabled() || e == mc.player) return 0f;
        if (e instanceof PlayerEntity) return expand.f();
        if (!playersOnly.on() && e instanceof LivingEntity) return expand.f();
        return 0f;
    }

    @Override
    public void render3d(Gfx g) {
        if (!show.on()) return;
        List<PlayerEntity> players = Targets.players(g.cam, 128, true, false);
        if (players.isEmpty()) return;
        g.begin();
        for (PlayerEntity p : players) {
            Vec3d rel = p.getLerpedPos(g.tickDelta).subtract(g.cam);
            double w = p.getWidth() / 2.0 + expand.d(), h = p.getHeight();
            Box b = new Box(rel.x - w, rel.y - expand.d(), rel.z - w, rel.x + w, rel.y + h + expand.d(), rel.z + w);
            g.box(b, width.f(), Colors.argb(color.rgb(), 0.9f));
        }
        g.end();
    }
}
