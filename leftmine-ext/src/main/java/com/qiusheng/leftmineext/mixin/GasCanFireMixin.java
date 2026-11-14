package com.qiusheng.leftmineext.mixin;

import com.qiusheng.leftmineext.LeftMineRegistry;
import net.thecallunxz.left2mine.combat.CarryableEntity;
import net.thecallunxz.left2mine.combat.GasCanFire;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The quest gas can must never ignite, regardless of damage source, fire,
 * explosions, or being thrown into lava. Its CarryableEntity still behaves
 * exactly like the normal gas can in every other respect.
 */
@Mixin(GasCanFire.class)
public abstract class GasCanFireMixin {
    @Inject(method = "ignite", at = @At("HEAD"), cancellable = true)
    private static void leftmineext$preventQuestIgnite(CarryableEntity can, CallbackInfo ci) {
        if (can.item().is(LeftMineRegistry.QUEST_GASCAN.get())) {
            ci.cancel();
        }
    }
}
