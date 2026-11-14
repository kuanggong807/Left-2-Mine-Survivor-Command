package com.qiusheng.leftmineext.mixin;

import com.qiusheng.leftmineext.NpcOptional;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Armed CustomNPCs must use their L2M gun/throwable, never the CustomNPCs
 * melee goal. This is the final guard in case a target is assigned by another
 * AI/faction between NPC controller ticks.
 */
@Mixin(Mob.class)
public abstract class MobAttackMixin {
    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void leftmineext$noNpcMelee(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (NpcOptional.isNpc(this) && NpcOptional.hasRangedWeapon((LivingEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
