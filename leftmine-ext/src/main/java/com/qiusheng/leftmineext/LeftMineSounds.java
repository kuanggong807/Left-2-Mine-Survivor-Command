package com.qiusheng.leftmineext;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LeftMineSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, LeftMineExt.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> GAS_POUR = SOUNDS.register(
            "gas_can_fill_pour_01",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(LeftMineExt.MOD_ID, "gas_can_fill_pour_01")));
    public static final DeferredHolder<SoundEvent, SoundEvent> FILL_COMPLETE = SOUNDS.register(
            "survival_teamrec",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(LeftMineExt.MOD_ID, "survival_teamrec")));

    public static void register(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
    }

    private LeftMineSounds() {
    }
}
