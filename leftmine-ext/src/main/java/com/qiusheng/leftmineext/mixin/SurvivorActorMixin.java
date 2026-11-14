package com.qiusheng.leftmineext.mixin;

import com.qiusheng.leftmineext.NpcOptional;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.thecallunxz.left2mine.bot.SurvivorActor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes armed CustomNPCs valid Left 2 Mine survivor targets, while keeping
 * unarmed NPCs out of the infected target selection. The helper methods also
 * prevent the Player/SurvivorBot-only SurvivorActor helpers from crashing when
 * GunItem.act() runs on an EntityNPCInterface.
 */
@Mixin(SurvivorActor.class)
public abstract class SurvivorActorMixin {
    @Inject(method = "valid", at = @At("RETURN"), cancellable = true)
    private static void leftmineext$validArmedNpc(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && NpcOptional.isArmedNpc(entity)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Left 2 Mine 2.0.6 uses SurvivorActor.targetable for the infected target
     * goal (and SpecialInfected's private validator delegates to it). Extend
     * both predicates so armed NPCs remain valid targets after the update.
     */
    @Inject(method = "targetable", at = @At("RETURN"), cancellable = true)
    private static void leftmineext$targetableArmedNpc(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && NpcOptional.isArmedNpc(entity)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "selected", at = @At("HEAD"), cancellable = true)
    private static void leftmineext$selectedNpc(LivingEntity actor, CallbackInfoReturnable<Integer> cir) {
        if (NpcOptional.isNpc(actor)) {
            cir.setReturnValue(0);
        }
    }

    @Inject(
            method = "item(Lnet/minecraft/world/entity/LivingEntity;I)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void leftmineext$getItemNpc(LivingEntity actor, int slot, CallbackInfoReturnable<ItemStack> cir) {
        if (NpcOptional.isNpc(actor)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }

    @Inject(
            method = "item(Lnet/minecraft/world/entity/LivingEntity;ILnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void leftmineext$setItemNpc(LivingEntity actor, int slot, ItemStack stack, CallbackInfo ci) {
        if (NpcOptional.isNpc(actor)) {
            ci.cancel();
        }
    }

    @Inject(method = "select", at = @At("HEAD"), cancellable = true)
    private static void leftmineext$selectNpc(LivingEntity actor, int slot, CallbackInfo ci) {
        if (NpcOptional.isNpc(actor)) {
            ci.cancel();
        }
    }

    @Inject(method = "sync", at = @At("HEAD"), cancellable = true)
    private static void leftmineext$syncNpc(LivingEntity actor, CallbackInfo ci) {
        if (NpcOptional.isNpc(actor)) {
            ci.cancel();
        }
    }
}
