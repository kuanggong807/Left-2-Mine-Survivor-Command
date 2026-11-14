package com.qiusheng.leftmineext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.thecallunxz.left2mine.combat.Carryables;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-authoritative hold-to-fill channel. The client sends a hold packet
 * while right-click is down; no packets for a few ticks cancels the fill.
 */
public final class GasFillManager {
    private static final Map<UUID, Channel> ACTIVE = new HashMap<>();
    private static final int HOLD_GRACE_TICKS = 10;
    private static final int SOUND_POUR_START = 0;
    private static final int SOUND_POUR_STOP = 1;
    private static final int SOUND_FILL_COMPLETE = 2;

    public static void broadcastPort(ServerLevel level, GasPortData.GasPort port, boolean visible) {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            GasNetwork.sendPort(player, visible, visible ? port.delivered : 0, visible ? port.required : 0);
        }
    }

    public static void sendActivePort(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        GasPortData.GasPort port = GasPortData.get(level).activePort();
        GasNetwork.sendPort(player, port != null,
                port == null ? 0 : port.delivered,
                port == null ? 0 : port.required);
    }

    private static void startPourSound(ServerLevel level, ServerPlayer source, Channel channel) {
        channel.playerId = source.getId();
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() == source.level() && player.distanceToSqr(source) <= 36.0D) {
                GasNetwork.sendSound(player, SOUND_POUR_START, channel.playerId);
                channel.soundReceivers.add(player.getUUID());
            }
        }
    }

    private static void stopPourSound(Channel channel) {
        if (channel.soundReceivers.isEmpty()) {
            return;
        }
        MinecraftServer server = serverOf(channel.player);
        if (server != null) {
            for (UUID id : channel.soundReceivers) {
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                if (player != null) {
                    GasNetwork.sendSound(player, SOUND_POUR_STOP, channel.playerId);
                }
            }
        }
        channel.soundReceivers.clear();
    }

    private static void refreshPourSound(ServerLevel level, ServerPlayer source, Channel channel) {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            boolean nearby = player.level() == source.level() && player.distanceToSqr(source) <= 36.0D;
            boolean receiving = channel.soundReceivers.contains(player.getUUID());
            if (nearby && !receiving) {
                GasNetwork.sendSound(player, SOUND_POUR_START, channel.playerId);
                channel.soundReceivers.add(player.getUUID());
            } else if (!nearby && receiving) {
                GasNetwork.sendSound(player, SOUND_POUR_STOP, channel.playerId);
                channel.soundReceivers.remove(player.getUUID());
            }
        }
    }

    private static void broadcastCompleteSound(ServerLevel level, ServerPlayer source) {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            GasNetwork.sendSound(player, SOUND_FILL_COMPLETE, source.getId());
        }
    }

    public static boolean tryStart(ServerPlayer player, ServerLevel level, BlockPos pos) {
        GasPortData data = GasPortData.get(level);
        GasPortData.GasPort port = data.find(level, pos);
        if (port == null || port.remaining() <= 0) {
            return false;
        }
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(LeftMineRegistry.QUEST_GASCAN.get())) {
            return false;
        }

        String dimension = level.dimension().location().toString();
        Channel existing = ACTIVE.get(player.getUUID());
        if (existing != null && existing.dimension.equals(dimension) && existing.portPos.equals(pos)) {
            existing.lastHoldTick = level.getGameTime();
            return true;
        }

        // One gas port can only be serviced by one player at a time.
        for (Channel other : ACTIVE.values()) {
            if (!other.player.equals(player.getUUID())
                    && other.dimension.equals(dimension)
                    && other.portPos.equals(pos)) {
                player.displayClientMessage(Component.translatable("leftmineext.gas.busy"), true);
                return false;
            }
        }

        stop(player.getUUID(), false);
        Channel channel = new Channel(player.getUUID(), dimension, pos.immutable());
        channel.startTick = level.getGameTime();
        channel.lastHoldTick = channel.startTick;
        channel.durationTicks = Math.max(20, data.fillSeconds() * 20);
        ACTIVE.put(player.getUUID(), channel);
        startPourSound(level, player, channel);
        GasNetwork.sendState(player, true, 0.0F, false, port.delivered, port.required);
        return true;
    }

    public static void setHolding(ServerPlayer player, boolean holding) {
        Channel channel = ACTIVE.get(player.getUUID());
        if (channel == null) {
            return;
        }
        if (!holding) {
            stop(player.getUUID(), false);
            return;
        }
        channel.lastHoldTick = player.level().getGameTime();
    }

    public static void interrupt(ServerPlayer player) {
        stop(player.getUUID(), false);
    }

    public static void stop(UUID playerId, boolean complete) {
        Channel channel = ACTIVE.remove(playerId);
        if (channel == null) {
            return;
        }
        stopPourSound(channel);
        MinecraftServer server = serverOf(playerId);
        if (server != null) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                GasNetwork.sendState(player, false, complete ? 1.0F : 0.0F, complete, 0, 0);
            }
        }
    }

    public static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Set<UUID> remove = new HashSet<>();
        for (Map.Entry<UUID, Channel> entry : ACTIVE.entrySet()) {
            Channel channel = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || player.isRemoved() || player.isDeadOrDying()) {
                remove.add(entry.getKey());
                continue;
            }
            if (!(player.level() instanceof ServerLevel level)
                    || !level.dimension().location().toString().equals(channel.dimension)) {
                remove.add(entry.getKey());
                continue;
            }
            GasPortData data = GasPortData.get(level);
            GasPortData.GasPort port = data.find(level, channel.portPos);
            if (port == null || port.remaining() <= 0) {
                remove.add(entry.getKey());
                continue;
            }
            if (!player.getMainHandItem().is(LeftMineRegistry.QUEST_GASCAN.get())) {
                remove.add(entry.getKey());
                continue;
            }
            long now = level.getGameTime();
            if (now - channel.lastHoldTick > HOLD_GRACE_TICKS) {
                remove.add(entry.getKey());
                continue;
            }
            if (!lookingAt(player, port)) {
                remove.add(entry.getKey());
                continue;
            }

            float progress = Math.min(1.0F, (now - channel.startTick) / (float) channel.durationTicks);
            if (progress >= 1.0F) {
                stopPourSound(channel);
                boolean completed = data.deliver(level, port);
                GasNetwork.sendState(player, false, 1.0F, true, port.delivered, port.required);
                ACTIVE.remove(entry.getKey());
                consumeQuestCan(player);
                broadcastPort(level, port, !completed);
                broadcastCompleteSound(level, player);
                if (completed) {
                    player.displayClientMessage(Component.translatable("leftmineext.gas.complete"), false);
                } else {
                    player.displayClientMessage(Component.translatable(
                            "leftmineext.gas.unit_added", port.delivered, port.required), false);
                }
                continue;
            }

            if (now % 2L == 0L) {
                GasNetwork.sendState(player, true, progress, false, port.delivered, port.required);
            }
            if (now % 5L == 0L) {
                refreshPourSound(level, player, channel);
            }
        }

        for (UUID id : remove) {
            stop(id, false);
        }
    }

    /**
     * Each completed fill uses one quest gas can. Carryables keeps a Held
     * reference to the exact ItemStack; after shrinking it to empty we release
     * that reference so the next can can be picked up/observed normally.
     */
    private static void consumeQuestCan(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(LeftMineRegistry.QUEST_GASCAN.get()) || stack.isEmpty()) {
            return;
        }
        boolean last = stack.getCount() <= 1;
        stack.shrink(1);
        if (last) {
            Carryables.release(player, false);
            player.inventoryMenu.broadcastChanges();
        }
    }

    private static boolean lookingAt(ServerPlayer player, GasPortData.GasPort port) {
        Vec3 blockCenter = Vec3.atCenterOf(port.pos).add(0.0D, 0.5D, 0.0D);
        Vec3 markerCenter = Vec3.atCenterOf(port.pos).add(0.0D, 1.35D, 0.0D);
        if (player.distanceToSqr(blockCenter) > 36.0D) {
            return false;
        }
        // Accept looking at either the port block or the floating marker. The
        // old exact block ray failed when the player naturally looked at the
        // marker just above the port and made the bar reset immediately.
        Vec3 look = player.getViewVector(1.0F);
        Vec3 eye = player.getEyePosition();
        double toBlock = look.dot(blockCenter.subtract(eye).normalize());
        double toMarker = look.dot(markerCenter.subtract(eye).normalize());
        return Math.max(toBlock, toMarker) >= 0.65D;
    }

    private static MinecraftServer serverOf(UUID playerId) {
        return net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
    }

    private static final class Channel {
        private final UUID player;
        private int playerId;
        private final String dimension;
        private final BlockPos portPos;
        private final Set<UUID> soundReceivers = new HashSet<>();
        private long startTick;
        private long lastHoldTick;
        private int durationTicks;

        private Channel(UUID player, String dimension, BlockPos portPos) {
            this.player = player;
            this.dimension = dimension;
            this.portPos = portPos;
        }
    }

    private GasFillManager() {
    }
}
