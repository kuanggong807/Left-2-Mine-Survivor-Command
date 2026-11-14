package com.qiusheng.leftmineext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Global quest gas-port definitions. Stored in the overworld so a port can be
 * set from any dimension and survives restarts.
 */
public final class GasPortData extends SavedData {
    private static final String DATA_NAME = "leftmineext_gas_ports";
    private static final Factory<GasPortData> FACTORY = new Factory<>(
            GasPortData::new, (tag, provider) -> load(tag));

    private final Map<String, GasPort> ports = new LinkedHashMap<>();
    private int fillSeconds = 5;
    private String activeKey;

    public static GasPortData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public GasPort activePort() {
        return activeKey == null ? null : ports.get(activeKey);
    }

    public void setActive(GasPort port) {
        activeKey = key(port.dimension, port.pos);
        setDirty();
    }

    public void clearActive() {
        activeKey = null;
        setDirty();
    }

    public int fillSeconds() {
        return fillSeconds;
    }

    public void setFillSeconds(int seconds) {
        fillSeconds = Math.max(1, seconds);
        setDirty();
    }

    public GasPort setPort(ServerLevel level, BlockPos pos, int required, BlockPos redstone) {
        String dimension = level.dimension().location().toString();
        String key = key(dimension, pos);
        GasPort previous = ports.remove(key);
        if (previous != null) {
            discardMarker(level, previous);
        }
        GasPort port = new GasPort(dimension, pos.immutable(), Math.max(1, required), redstone.immutable());
        ports.put(key, port);
        activeKey = key;
        spawnOrUpdateMarker(level, port);
        setDirty();
        return port;
    }

    public GasPort find(ServerLevel level, BlockPos pos) {
        return ports.get(key(level.dimension().location().toString(), pos));
    }

    public Collection<GasPort> ports() {
        return new ArrayList<>(ports.values());
    }

    public void remove(ServerLevel level, GasPort port) {
        String removedKey = key(port.dimension, port.pos);
        ports.remove(removedKey);
        if (removedKey.equals(activeKey)) {
            activeKey = null;
        }
        discardMarker(level, port);
        setDirty();
    }

    /**
     * Adds one oil unit. Returns true when the port has received all required
     * units and redstone has been placed.
     */
    public boolean deliver(ServerLevel level, GasPort port) {
        port.delivered++;
        if (port.delivered < port.required) {
            spawnOrUpdateMarker(level, port);
            setDirty();
            return false;
        }

        level.setBlockAndUpdate(port.redstone, Blocks.REDSTONE_BLOCK.defaultBlockState());
        discardMarker(level, port);
        String deliveredKey = key(port.dimension, port.pos);
        ports.remove(deliveredKey);
        if (deliveredKey.equals(activeKey)) {
            activeKey = null;
        }
        setDirty();
        return true;
    }

    public void updateMarker(ServerLevel level, GasPort port) {
        spawnOrUpdateMarker(level, port);
        setDirty();
    }

    public void spawnOrUpdateMarker(ServerLevel level, GasPort port) {
        ServerLevel target = level.getServer().getLevel(ResourceKey.create(
                Registries.DIMENSION, ResourceLocation.parse(port.dimension)));
        if (target == null) {
            return;
        }
        Entity existing = port.marker == null ? null : target.getEntity(port.marker);
        if (existing instanceof Display display) {
            if (existing instanceof net.minecraft.world.entity.Display.ItemDisplay itemDisplay) {
                itemDisplay.load(markerTag(target, port));
                itemDisplay.setPos(port.pos.getX() + 0.5D, port.pos.getY() + 1.35D, port.pos.getZ() + 0.5D);
            }
            // load() can reset Glowing/NoGravity to the NBT defaults. Apply the
            // marker flags after loading so the highlight survives every barrel.
            display.setGlowingTag(true);
            display.setNoGravity(true);
            display.setInvulnerable(true);
            return;
        }

        net.minecraft.world.entity.Display.ItemDisplay display =
                EntityType.ITEM_DISPLAY.create(target);
        if (display == null) {
            return;
        }
        display.load(markerTag(target, port));
        display.setPos(port.pos.getX() + 0.5D, port.pos.getY() + 1.35D, port.pos.getZ() + 0.5D);
        display.setGlowingTag(true);
        display.setNoGravity(true);
        display.setInvulnerable(true);
        target.addFreshEntity(display);
        port.marker = display.getUUID();
        setDirty();
    }

    private CompoundTag markerTag(ServerLevel level, GasPort port) {
        CompoundTag tag = new CompoundTag();
        tag.put("item", new ItemStack(LeftMineRegistry.QUEST_GASCAN.get()).save(level.registryAccess()));
        tag.putBoolean("Glowing", true);
        tag.putBoolean("NoGravity", true);
        tag.putBoolean("Invulnerable", true);
        tag.putString("billboard", "center");
        tag.putFloat("view_range", 512.0F);
        return tag;
    }

    private Component markerText(GasPort port) {
        return Component.translatable("leftmineext.gas.marker", port.delivered, port.required);
    }

    private void discardMarker(ServerLevel level, GasPort port) {
        if (port.marker == null) {
            return;
        }
        ServerLevel target = level.getServer().getLevel(ResourceKey.create(
                Registries.DIMENSION, ResourceLocation.parse(port.dimension)));
        if (target != null) {
            Entity entity = target.getEntity(port.marker);
            if (entity != null) {
                entity.discard();
            }
        }
        port.marker = null;
    }

    private static String key(String dimension, BlockPos pos) {
        return dimension + "|" + pos.asLong();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (GasPort port : ports.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Dimension", port.dimension);
            entry.putLong("Pos", port.pos.asLong());
            entry.putInt("Required", port.required);
            entry.putInt("Delivered", port.delivered);
            entry.putLong("Redstone", port.redstone.asLong());
            if (port.marker != null) {
                entry.putUUID("Marker", port.marker);
            }
            list.add(entry);
        }
        tag.put("Ports", list);
        tag.putInt("FillSeconds", fillSeconds);
        if (activeKey != null) {
            tag.putString("ActivePort", activeKey);
        }
        tag.putInt("DataVersion", 2);
        return tag;
    }

    private static GasPortData load(CompoundTag tag) {
        GasPortData data = new GasPortData();
        int storedSeconds = tag.contains("FillSeconds") ? tag.getInt("FillSeconds") : 5;
        // Worlds created before the 5-second default stored 8; migrate that
        // old default once so existing test worlds also use 5 seconds.
        if (!tag.contains("DataVersion") && storedSeconds == 8) {
            storedSeconds = 5;
        }
        data.fillSeconds = Math.max(1, storedSeconds);
        ListTag list = tag.getList("Ports", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            String dimension = entry.getString("Dimension");
            if (dimension.isEmpty()) {
                continue;
            }
            GasPort port = new GasPort(
                    dimension,
                    BlockPos.of(entry.getLong("Pos")),
                    Math.max(1, entry.getInt("Required")),
                    BlockPos.of(entry.getLong("Redstone")));
            port.delivered = Math.max(0, entry.getInt("Delivered"));
            if (entry.hasUUID("Marker")) {
                port.marker = entry.getUUID("Marker");
            }
            data.ports.put(key(dimension, port.pos), port);
        }
        data.activeKey = tag.contains("ActivePort") ? tag.getString("ActivePort") : null;
        if (data.activeKey != null && !data.ports.containsKey(data.activeKey)) {
            data.activeKey = null;
        }
        return data;
    }

    public static final class GasPort {
        public final String dimension;
        public final BlockPos pos;
        public final int required;
        public int delivered;
        public final BlockPos redstone;
        public UUID marker;

        private GasPort(String dimension, BlockPos pos, int required, BlockPos redstone) {
            this.dimension = dimension;
            this.pos = pos;
            this.required = required;
            this.redstone = redstone;
        }

        public int remaining() {
            return Math.max(0, required - delivered);
        }
    }
}
