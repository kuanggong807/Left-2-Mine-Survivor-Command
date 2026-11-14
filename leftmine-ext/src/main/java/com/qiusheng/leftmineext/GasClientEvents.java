package com.qiusheng.leftmineext;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client input tick is registered explicitly from GasHud (client-only). Keeping
 * it out of a bus annotation avoids the game-bus/mod-bus ambiguity that made
 * the old build miss hold packets.
 */
public final class GasClientEvents {
    public static void register() {
        NeoForge.EVENT_BUS.addListener(GasClientEvents::tick);
    }

    public static void tick(ClientTickEvent.Post event) {
        GasClientState.tick();
    }

    private GasClientEvents() {
    }
}
