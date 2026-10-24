package pl.nebula.module;

import net.minecraft.client.render.Camera;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import pl.nebula.setting.Setting.*;

public class Freecam extends Module {
    public final NumberSetting speed = add(new NumberSetting("Speed", 1.0, 0.1, 5.0, 0.1));

    public Vec3d pos = Vec3d.ZERO;
    public Vec3d prev = Vec3d.ZERO;
    public float yaw, pitch;

    public Freecam() {
        super("Freecam", "Detached camera, your player stays in place");
    }

    @Override
    protected void onEnable() {
        if (mc.player == null) return;
        Camera c = mc.gameRenderer.getCamera();
        pos = prev = c.getPos();
        yaw = c.getYaw();
        pitch = c.getPitch();
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        var o = mc.options;
        double f = (o.forwardKey.isPressed() ? 1 : 0) - (o.backKey.isPressed() ? 1 : 0);
        double s = (o.leftKey.isPressed() ? 1 : 0) - (o.rightKey.isPressed() ? 1 : 0);
        double u = (o.jumpKey.isPressed() ? 1 : 0) - (o.sneakKey.isPressed() ? 1 : 0);

        Vec3d look = Vec3d.fromPolar(pitch, yaw);
        Vec3d left = new Vec3d(look.z, 0, -look.x).normalize();
        Vec3d move = look.multiply(f).add(left.multiply(s)).add(0, u, 0);

        prev = pos;
        if (move.lengthSquared() > 1e-6) {
            double spd = speed.d() * (o.sprintKey.isPressed() ? 2.0 : 1.0);
            pos = pos.add(move.normalize().multiply(spd));
        }
    }

    public Vec3d interpolated(float tickDelta) {
        return prev.lerp(pos, tickDelta);
    }

    /** Mouse look (called from EntityMixin instead of rotating the real player). */
    public void rotate(double dx, double dy) {
        yaw += (float) dx * 0.15f;
        pitch = MathHelper.clamp(pitch + (float) dy * 0.15f, -90f, 90f);
    }
}
