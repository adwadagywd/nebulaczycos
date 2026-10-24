package pl.nebula.module;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import pl.nebula.setting.Setting.*;

public class Fullbright extends Module {
    public enum Mode { GAMMA, NIGHT_VISION }

    public final EnumSetting<Mode> mode = add(new EnumSetting<>("Mode", Mode.GAMMA));
    private boolean addedEffect;

    public Fullbright() {
        super("Fullbright", "Maximum brightness everywhere");
    }

    /** Used by SimpleOptionMixin. */
    public boolean gammaActive() {
        return isEnabled() && mode.get() == Mode.GAMMA;
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        if (mode.get() == Mode.NIGHT_VISION) {
            if (!mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, -1, 0, false, false, false));
                addedEffect = true;
            }
        } else {
            removeEffect();
        }
    }

    @Override
    protected void onDisable() {
        removeEffect();
    }

    private void removeEffect() {
        if (addedEffect && mc.player != null) {
            mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
        addedEffect = false;
    }
}
