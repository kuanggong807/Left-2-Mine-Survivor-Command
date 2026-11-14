package com.qiusheng.leftmineext.mixin;

import com.qiusheng.leftmineext.LeftMineRegistry;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.thecallunxz.left2mine.combat.CarryableItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Quest gas can is a fuel mission item, not a shove weapon. Cancelling its use
 * on both client and server prevents the Left 2 Mine shove animation/packet
 * from firing while the player holds right-click to fuel.
 */
@Mixin(CarryableItem.class)
public abstract class CarryableItemMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void leftmineext$questCanDoesNotShove(Level level, Player player, InteractionHand hand,
                                                  CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(LeftMineRegistry.QUEST_GASCAN.get())) {
            cir.setReturnValue(InteractionResultHolder.fail(stack));
        }
    }
}
