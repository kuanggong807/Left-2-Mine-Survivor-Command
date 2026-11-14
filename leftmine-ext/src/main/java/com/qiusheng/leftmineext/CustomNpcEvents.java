package com.qiusheng.leftmineext;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import noppes.npcs.entity.EntityNPCInterface;

/**
 * All direct CustomNPCs event listeners live here and are registered only when
 * the customnpcs mod is present.
 */
public final class CustomNpcEvents {
    public static void register() {
        NeoForge.EVENT_BUS.addListener(CustomNpcEvents::npcEntityTick);
        NeoForge.EVENT_BUS.addListener(CustomNpcEvents::entityInteract);
    }

    private static void npcEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof EntityNPCInterface npc
                && npc.level() instanceof ServerLevel level
                && npc.isAlive()
                && level.getGameTime() % 2L == 0L) {
            NpcGunCompat.tickNpc(level, npc);
        }
    }

    private static void entityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.gameMode.getGameModeForPlayer() == GameType.ADVENTURE
                && event.getTarget() instanceof EntityNPCInterface npc
                && npc.isAttacking()) {
            npc.setTarget(null);
        }
    }

    private CustomNpcEvents() {
    }
}
