package pl.nebula.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.nebula.module.Freecam;
import pl.nebula.module.ModuleManager;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setPos(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow private boolean thirdPerson;

    @Inject(method = "update", at = @At("TAIL"))
    private void nebula$freecam(BlockView area, Entity focused, boolean third, boolean inverse, float tickDelta, CallbackInfo ci) {
        Freecam f = ModuleManager.freecam;
        if (f != null && f.isEnabled()) {
            Vec3d p = f.interpolated(tickDelta);
            this.setRotation(f.yaw, f.pitch);
            this.setPos(p.x, p.y, p.z);
            this.thirdPerson = true; // makes the real player model render
        }
    }
}
