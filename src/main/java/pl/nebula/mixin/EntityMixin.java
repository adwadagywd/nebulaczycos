package pl.nebula.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.nebula.module.Freecam;
import pl.nebula.module.HitboxExpander;
import pl.nebula.module.ModuleManager;

@Mixin(Entity.class)
public abstract class EntityMixin {

    /** Freecam: mouse moves the camera instead of the player. */
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void nebula$look(double dx, double dy, CallbackInfo ci) {
        Freecam f = ModuleManager.freecam;
        if (f != null && f.isEnabled() && (Object) this == MinecraftClient.getInstance().player) {
            f.rotate(dx, dy);
            ci.cancel();
        }
    }

    /** Hitbox expander: vanilla crosshair ray-cast uses getBoundingBox().expand(getTargetingMargin()). */
    @Inject(method = "getTargetingMargin", at = @At("HEAD"), cancellable = true)
    private void nebula$margin(CallbackInfoReturnable<Float> cir) {
        HitboxExpander h = ModuleManager.hitbox;
        if (h != null && h.isEnabled()) {
            float m = h.marginFor((Entity) (Object) this);
            if (m > 0f) cir.setReturnValue(m);
        }
    }
}
