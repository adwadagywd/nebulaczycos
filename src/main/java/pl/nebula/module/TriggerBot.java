package pl.nebula.module;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import pl.nebula.setting.Setting.*;

import java.util.concurrent.ThreadLocalRandom;

public class TriggerBot extends Module {
    public final BoolSetting onlyCrits = add(new BoolSetting("Only Crits", false));
    public final NumberSetting cooldown = add(new NumberSetting("Cooldown", 0.92, 0.1, 1.0, 0.01));
    public final NumberSetting delay = add(new NumberSetting("Delay (ticks)", 0, 0, 10, 1));
    public final BoolSetting randomize = add(new BoolSetting("Randomize Delay", true));
    public final BoolSetting players = add(new BoolSetting("Players", true));
    public final BoolSetting hostiles = add(new BoolSetting("Hostile Mobs", false));
    public final BoolSetting animals = add(new BoolSetting("Animals", false));
    public final BoolSetting weaponOnly = add(new BoolSetting("Weapon Only", true));
    public final BoolSetting skipUsing = add(new BoolSetting("Skip While Using Item", true));

    private int timer;
    private int wait = -1;

    public TriggerBot() {
        super("TriggerBot", "Attacks automatically when the crosshair is on a target");
    }

    @Override
    protected void onDisable() {
        timer = 0;
        wait = -1;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        if (mc.currentScreen != null) return;

        HitResult hr = mc.crosshairTarget;
        if (!(hr instanceof EntityHitResult ehr)) { timer = 0; wait = -1; return; }
        Entity target = ehr.getEntity();
        if (!validTarget(target)) { timer = 0; wait = -1; return; }

        if (skipUsing.on() && mc.player.isUsingItem()) return;
        if (weaponOnly.on()) {
            ItemStack held = mc.player.getMainHandStack();
            if (!held.isIn(ItemTags.SWORDS) && !held.isIn(ItemTags.AXES)) return;
        }
        if (mc.player.getAttackCooldownProgress(0.5f) < cooldown.f()) return;
        if (onlyCrits.on() && !canCrit()) return;

        if (wait < 0) {
            wait = delay.i();
            if (randomize.on() && wait > 0) wait = Math.max(0, wait + ThreadLocalRandom.current().nextInt(-1, 2));
        }
        if (timer < wait) { timer++; return; }

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        timer = 0;
        wait = -1;
    }

    private boolean validTarget(Entity e) {
        if (!(e instanceof LivingEntity le) || !le.isAlive()) return false;
        if (e instanceof PlayerEntity p) return players.on() && !p.isSpectator();
        if (e instanceof Monster) return hostiles.on();
        if (e instanceof AnimalEntity) return animals.on();
        return false;
    }

    private boolean canCrit() {
        var p = mc.player;
        return p.fallDistance > 0.0f
                && !p.isOnGround()
                && !p.isClimbing()
                && !p.isTouchingWater()
                && !p.isSprinting()
                && !p.hasVehicle()
                && !p.hasStatusEffect(StatusEffects.BLINDNESS);
    }
}
