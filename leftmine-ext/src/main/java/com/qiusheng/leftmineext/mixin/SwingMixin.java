package com.qiusheng.leftmineext.mixin;

import com.qiusheng.leftmineext.NpcOptional;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * EntityAIAttackTarget calls npc.swing(MAIN_HAND) before doHurtTarget. Even
 * when the damage is blocked, that leaves the visible left-click melee swing.
 * Suppress it for NPCs that are using an L2M gun or throwable.
 */
@Mixin(LivingEntity.class)
public abstract class SwingMixin {
    @Inject(method = "swing", at = @At("HEAD"), cancellable = true)
    private void leftmineext$noNpcMeleeSwing(InteractionHand hand, CallbackInfo cir) {
        if (NpcOptional.isNpc(this) && NpcOptional.hasRangedWeapon((LivingEntity) (Object) this)) {
            cir.cancel();
        }
    }
}
