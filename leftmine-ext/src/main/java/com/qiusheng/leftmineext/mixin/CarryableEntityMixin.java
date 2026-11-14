package com.qiusheng.leftmineext.mixin;

import com.qiusheng.leftmineext.LeftMineRegistry;
import net.minecraft.world.damagesource.DamageSource;
import net.thecallunxz.left2mine.combat.CarryableEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The quest gas can is a mission item: once dropped it must not be destroyed
 * by melee, arrows, explosions, fire or lava. It can still be picked up.
 */
@Mixin(CarryableEntity.class)
public abstract class CarryableEntityMixin {
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void leftmineext$questCanCannotBeHurt(DamageSource source, float amount,
                                                  CallbackInfoReturnable<Boolean> cir) {
        CarryableEntity self = (CarryableEntity) (Object) this;
        if (self.item().is(LeftMineRegistry.QUEST_GASCAN.get())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void leftmineext$questCanDoesNotBurn(CallbackInfo ci) {
        CarryableEntity self = (CarryableEntity) (Object) this;
        if (self.item().is(LeftMineRegistry.QUEST_GASCAN.get())) {
            self.clearFire();
        }
    }

    @Inject(method = "shouldRenderAtSqrDistance", at = @At("HEAD"), cancellable = true)
    private void leftmineext$questCanAlwaysVisible(double distance, CallbackInfoReturnable<Boolean> cir) {
        CarryableEntity self = (CarryableEntity) (Object) this;
        if (self.item().is(LeftMineRegistry.QUEST_GASCAN.get())) {
            cir.setReturnValue(true);
        }
    }
}
