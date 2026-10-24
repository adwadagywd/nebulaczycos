package pl.nebula.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.nebula.module.Fullbright;
import pl.nebula.module.ModuleManager;

@Mixin(SimpleOption.class)
public abstract class SimpleOptionMixin<T> {
    /** Fullbright (gamma mode): gamma option reports a huge value while enabled. */
    @SuppressWarnings("unchecked")
    @Inject(method = "getValue", at = @At("RETURN"), cancellable = true)
    private void nebula$gamma(CallbackInfoReturnable<T> cir) {
        Fullbright fb = ModuleManager.fullbright;
        if (fb == null || !fb.gammaActive()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.options == null) return;
        if ((Object) this == mc.options.getGamma()) {
            cir.setReturnValue((T) (Object) Double.valueOf(16.0));
        }
    }
}
