package com.qiusheng.leftmineext;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Small payload set for the gas-fill channel:
 * - client -> server: right-click is still held;
 * - server -> client: progress / complete / cancel state.
 */
public final class GasNetwork {
    public static final ResourceLocation HOLD_ID =
            ResourceLocation.fromNamespaceAndPath(LeftMineExt.MOD_ID, "gas_hold");
    public static final ResourceLocation STATE_ID =
            ResourceLocation.fromNamespaceAndPath(LeftMineExt.MOD_ID, "gas_state");
    public static final ResourceLocation PORT_ID =
            ResourceLocation.fromNamespaceAndPath(LeftMineExt.MOD_ID, "gas_port");
    public static final ResourceLocation SOUND_ID =
            ResourceLocation.fromNamespaceAndPath(LeftMineExt.MOD_ID, "gas_sound");

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToServer(HoldPayload.TYPE, HoldPayload.CODEC, GasNetwork::handleHold)
                .playToClient(StatePayload.TYPE, StatePayload.CODEC, GasNetwork::handleState)
                .playToClient(PortPayload.TYPE, PortPayload.CODEC, GasNetwork::handlePort)
                .playToClient(SoundPayload.TYPE, SoundPayload.CODEC, GasNetwork::handleSound);
    }

    public static void sendState(ServerPlayer player, boolean active, float progress, boolean complete,
                                 int delivered, int required) {
        PacketDistributor.sendToPlayer(player, new StatePayload(active, progress, complete, delivered, required));
    }

    public static void sendPort(ServerPlayer player, boolean visible, int delivered, int required) {
        PacketDistributor.sendToPlayer(player, new PortPayload(visible, delivered, required));
    }

    public static void sendSound(ServerPlayer player, int action, int playerId) {
        PacketDistributor.sendToPlayer(player, new SoundPayload(action, playerId));
    }

    private static void handleHold(HoldPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            context.enqueueWork(() -> GasFillManager.setHolding(player, payload.holding()));
        }
    }

    private static void handleState(StatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> GasClientState.accept(payload));
    }

    private static void handlePort(PortPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> GasClientState.acceptPort(payload));
    }

    private static void handleSound(SoundPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> GasSoundClient.handle(payload.action(), payload.playerId()));
    }

    public record SoundPayload(int action, int playerId) implements CustomPacketPayload {
        public static final Type<SoundPayload> TYPE = new Type<>(SOUND_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, SoundPayload> CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeByte(payload.action);
                    buf.writeVarInt(payload.playerId);
                },
                buf -> new SoundPayload(buf.readUnsignedByte(), buf.readVarInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record PortPayload(boolean visible, int delivered, int required) implements CustomPacketPayload {
        public static final Type<PortPayload> TYPE = new Type<>(PORT_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, PortPayload> CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeBoolean(payload.visible);
                    buf.writeInt(payload.delivered);
                    buf.writeInt(payload.required);
                },
                buf -> new PortPayload(buf.readBoolean(), buf.readInt(), buf.readInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record HoldPayload(boolean holding) implements CustomPacketPayload {
        public static final Type<HoldPayload> TYPE = new Type<>(HOLD_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, HoldPayload> CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeBoolean(payload.holding),
                buf -> new HoldPayload(buf.readBoolean()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record StatePayload(boolean active, float progress, boolean complete,
                               int delivered, int required) implements CustomPacketPayload {
        public static final Type<StatePayload> TYPE = new Type<>(STATE_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, StatePayload> CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeBoolean(payload.active);
                    buf.writeFloat(payload.progress);
                    buf.writeBoolean(payload.complete);
                    buf.writeInt(payload.delivered);
                    buf.writeInt(payload.required);
                },
                buf -> new StatePayload(buf.readBoolean(), buf.readFloat(), buf.readBoolean(),
                        buf.readInt(), buf.readInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private GasNetwork() {
    }
}
