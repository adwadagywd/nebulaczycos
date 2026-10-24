package pl.nebula.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.nebula.module.ModuleManager;
import pl.nebula.module.Nametags;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    /** Hides the vanilla player nametag while custom Nametags are active. */
    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void nebula$hideVanilla(EntityRenderState state, Text text, MatrixStack matrices,
                                    VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        Nametags n = ModuleManager.nametags;
        if (n != null && n.isEnabled() && n.hideVanilla.on() && state instanceof PlayerEntityRenderState) {
            ci.cancel();
        }
    }
}
