package com.qiusheng.leftmineext;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.thecallunxz.left2mine.combat.CarryableItem;
import net.thecallunxz.left2mine.combat.CarryableKind;

import java.util.function.Supplier;

/**
 * Quest gas can uses Left 2 Mine's own CarryableItem/GAS_CAN properties so its
 * stack size, carrying, throwing and entity behavior stay identical. The only
 * difference is the model/texture and the GasCanFire mixin that prevents it
 * from igniting.
 */
public final class LeftMineRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(LeftMineExt.MOD_ID);
    public static final DeferredItem<CarryableItem> QUEST_GASCAN = ITEMS.register(
            "quest_gascan", () -> new CarryableItem(CarryableKind.GAS_CAN));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    private LeftMineRegistry() {
    }
}
