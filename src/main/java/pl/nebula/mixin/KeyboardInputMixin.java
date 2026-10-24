package pl.nebula.mixin;

import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.nebula.module.Freecam;
import pl.nebula.module.ModuleManager;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {
    /** Freecam: the real player must not move while the camera flies. */
    @Inject(method = "tick", at = @At("TAIL"))
    private void nebula$freeze(CallbackInfo ci) {
        Freecam f = ModuleManager.freecam;
        if (f != null && f.isEnabled()) {
            Input in = (Input) (Object) this;
            in.playerInput = new PlayerInput(false, false, false, false, false, false, false);
            in.movementForward = 0f;
            in.movementSideways = 0f;
        }
    }
}
