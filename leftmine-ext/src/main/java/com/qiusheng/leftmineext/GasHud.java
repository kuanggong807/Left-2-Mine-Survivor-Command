package com.qiusheng.leftmineext;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

public final class GasHud {
    private static final ResourceLocation LAYER =
            ResourceLocation.fromNamespaceAndPath(LeftMineExt.MOD_ID, "gas_progress");

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(GasHud::onRegister);
        GasClientEvents.register();
    }

    @SubscribeEvent
    public static void onRegister(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER, (graphics, deltaTracker) -> GasClientState.render(graphics));
    }

    private GasHud() {
    }
}
