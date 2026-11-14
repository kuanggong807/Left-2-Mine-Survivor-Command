package com.qiusheng.leftmineext.mixin;

import com.qiusheng.leftmineext.NpcGunCompat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * EntityNPCInterface overrides Mob.doHurtTarget, so a Mob-level guard is not
 * enough. This is the actual method CustomNPCs uses for its melee/left-click
 * attack; cancel it whenever the NPC has an L2M gun or throwable in hand.
 */
@Pseudo
@Mixin(EntityNPCInterface.class)
public abstract class EntityNPCAttackMixin {
    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void leftmineext$noNpcMelee(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof LivingEntity npc && NpcGunCompat.hasRangedWeapon(npc)) {
            cir.setReturnValue(false);
        }
    }
}
