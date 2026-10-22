package com.qiusheng.leftmineext;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.thecallunxz.left2mine.bot.SurvivorActor;
import net.thecallunxz.left2mine.bot.SurvivorBot;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 让 Left 2 Mine 的 SurvivorBot 跟随玩家进出载具。
 *
 * <p>MTR 的乘客不是原版 passenger，且 MTR 只移动 Player，不移动 SurvivorBot。
 * 因此 bot 进入载具后不再贴着某个玩家：它们被分配到车厢内的局部位置，保存
 * 自己的局部偏移并做小范围前后左右移动；电梯只更新垂直位置，火车则由玩家
 * 群体的平均位置带动。玩家全部离开后恢复 AI，并传送到车外玩家身旁。</p>
 */
public final class BotVehicleFollow {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int ENTER_DELAY = 4;
    private static final int EXIT_DELAY = 4;
    private static final double DEFAULT_HALF_WIDTH = 1.1;
    private static final double DEFAULT_HALF_DEPTH = 2.8;

    private static final Map<UUID, BotState> FOLLOWING = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, Integer> INSIDE_TICKS = new HashMap<>();
    private static final Map<ResourceKey<Level>, Integer> OUTSIDE_TICKS = new HashMap<>();
    private static final Map<ResourceKey<Level>, CabinAnchor> ANCHORS = new HashMap<>();

    private BotVehicleFollow() {
    }

    public static int followingCount() {
        return FOLLOWING.size();
    }

    public static boolean isFollowing(Entity entity) {
        return entity != null && FOLLOWING.containsKey(entity.getUUID());
    }

    public static void tick(ServerLevel level) {
        List<ServerPlayer> players = level.players().stream()
                .filter(SurvivorActor::valid)
                .toList();
        List<SurvivorBot> bots = bots(level);

        if (players.isEmpty()) {
            release(level, bots, List.of(), false);
            reset(level);
            ANCHORS.remove(level.dimension());
            return;
        }

        List<ServerPlayer> inside = players.stream().filter(VehicleGuard::isInVehicle).toList();
        var key = level.dimension();

        if (inside.size() == players.size()) {
            int ticks = INSIDE_TICKS.merge(key, 1, Integer::sum);
            OUTSIDE_TICKS.remove(key);
            Cabin cabin = cabin(level, inside);
            if (ticks >= ENTER_DELAY) {
                int marked = markAll(bots, cabin);
                if (marked > 0) {
                    LOGGER.info("[leftmineext] bots entered vehicle: {} ({} players, cabin {}x{})",
                            marked, inside.size(),
                            String.format("%.2f", cabin.halfWidth * 2),
                            String.format("%.2f", cabin.halfDepth * 2));
                }
            }
            followMarked(bots, cabin);
        } else if (inside.isEmpty()) {
            int ticks = OUTSIDE_TICKS.merge(key, 1, Integer::sum);
            INSIDE_TICKS.remove(key);
            if (ticks >= EXIT_DELAY) {
                int released = release(level, bots, players, true);
                if (released > 0) {
                    LOGGER.info("[leftmineext] bots left vehicle: {} ({} players outside)", released, players.size());
                }
                ANCHORS.remove(key);
            }
        } else {
            INSIDE_TICKS.remove(key);
            OUTSIDE_TICKS.remove(key);
            // 部分玩家仍在车内：已进入的 bot 继续在车厢内活动，不重新贴到玩家身上。
            Cabin cabin = cabin(level, inside);
            followMarked(bots, cabin);
        }
    }

    private static List<SurvivorBot> bots(ServerLevel level) {
        List<SurvivorBot> result = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof SurvivorBot bot && bot.isAlive() && !bot.isRemoved()) {
                result.add(bot);
            }
        }
        return result;
    }

    /**
     * 进入载具时按 Bot 数量在车厢里分配局部位置，适配多人/多 bot。
     */
    private static int markAll(List<SurvivorBot> bots, Cabin cabin) {
        int total = bots.size();
        int columns = Math.max(1, (int) Math.ceil(Math.sqrt(total)));
        int rows = Math.max(1, (int) Math.ceil((double) total / columns));
        int marked = 0;

        for (int i = 0; i < total; i++) {
            SurvivorBot bot = bots.get(i);
            if (FOLLOWING.containsKey(bot.getUUID())) {
                continue;
            }
            double gridX = columns == 1 ? 0.0 : ((double) (i % columns) / (columns - 1) * 2.0 - 1.0);
            double gridZ = rows == 1 ? 0.0 : ((double) (i / columns) / (rows - 1) * 2.0 - 1.0);
            double baseX = gridX * Math.max(0.0, cabin.halfWidth - 0.35);
            double baseZ = gridZ * Math.max(0.0, cabin.halfDepth - 0.35);

            BotState state = new BotState(BotFlags.of(bot), baseX, baseZ, i * 0.71);
            if (FOLLOWING.putIfAbsent(bot.getUUID(), state) != null) {
                continue;
            }
            bot.stopRiding();
            bot.setNoAi(true);
            bot.setNoGravity(true);
            bot.setInvulnerable(true);
            bot.setTarget(null);
            bot.getNavigation().stop();
            marked++;
        }
        return marked;
    }

    private static void followMarked(List<SurvivorBot> bots, Cabin cabin) {
        for (SurvivorBot bot : bots) {
            BotState state = FOLLOWING.get(bot.getUUID());
            if (state == null) {
                continue;
            }
            state.phase += 0.045;
            double margin = 0.18;
            double maxX = Math.max(0.0, cabin.halfWidth - margin);
            double maxZ = Math.max(0.0, cabin.halfDepth - margin);
            double x = clamp(state.baseX + Math.sin(state.phase) * 0.10, -maxX, maxX);
            double z = clamp(state.baseZ + Math.cos(state.phase * 0.85) * 0.10, -maxZ, maxZ);
            bot.teleportTo(cabin.x + x, cabin.y, cabin.z + z);
            bot.setDeltaMovement(Vec3.ZERO);
            bot.fallDistance = 0.0F;
        }
    }

    /**
     * @return 释放的 bot 数量
     */
    private static int release(ServerLevel level, List<SurvivorBot> bots, List<ServerPlayer> outside, boolean teleport) {
        int released = 0;
        for (SurvivorBot bot : bots) {
            BotState state = FOLLOWING.remove(bot.getUUID());
            if (state == null) {
                continue;
            }
            state.flags.restore(bot);
            bot.setTarget(null);
            bot.setDeltaMovement(Vec3.ZERO);
            ServerPlayer target = outside.isEmpty() ? null : nearest(outside, bot);
            if (teleport && target != null) {
                bot.teleportTo(target.getX(), target.getY(), target.getZ());
            }
            released++;
        }

        // 清理已经卸载/移除的 bot 状态，避免 UUID 泄漏；只清理所有维度都不存在的实体。
        FOLLOWING.keySet().removeIf(uuid -> {
            for (ServerLevel other : level.getServer().getAllLevels()) {
                if (other.getEntity(uuid) != null) {
                    return false;
                }
            }
            return true;
        });
        return released;
    }

    private static Cabin cabin(ServerLevel level, List<ServerPlayer> inside) {
        double x = inside.stream().mapToDouble(ServerPlayer::getX).average().orElse(0.0);
        double y = inside.stream().mapToDouble(ServerPlayer::getY).average().orElse(0.0);
        double z = inside.stream().mapToDouble(ServerPlayer::getZ).average().orElse(0.0);
        double halfWidth = DEFAULT_HALF_WIDTH;
        double halfDepth = DEFAULT_HALF_DEPTH;
        boolean elevator = false;

        if (ModList.get().isLoaded("mtr")) {
            for (ServerPlayer player : inside) {
                MtrVehicleDetector.Bounds bounds = MtrVehicleDetector.bounds(player);
                if (bounds != null) {
                    halfWidth = bounds.halfWidth();
                    halfDepth = bounds.halfDepth();
                    elevator = true;
                    break;
                }
            }
        }

        if (elevator) {
            CabinAnchor anchor = ANCHORS.computeIfAbsent(level.dimension(), key -> new CabinAnchor(x, z));
            // 电梯只上下移动，水平位置固定在首次进入时的车厢中心，避免跟随玩家在车厢里走动。
            return new Cabin(anchor.x, y, anchor.z, halfWidth, halfDepth);
        }
        ANCHORS.remove(level.dimension());
        return new Cabin(x, y, z, halfWidth, halfDepth);
    }

    private static void reset(ServerLevel level) {
        INSIDE_TICKS.remove(level.dimension());
        OUTSIDE_TICKS.remove(level.dimension());
    }

    private static ServerPlayer nearest(List<ServerPlayer> players, Entity origin) {
        return players.stream()
                .min(Comparator.comparingDouble(origin::distanceToSqr))
                .orElse(null);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record Cabin(double x, double y, double z, double halfWidth, double halfDepth) {
    }

    private record CabinAnchor(double x, double z) {
    }

    private record BotFlags(boolean noAi, boolean noGravity, boolean invulnerable) {
        static BotFlags of(SurvivorBot bot) {
            return new BotFlags(bot.isNoAi(), bot.isNoGravity(), bot.isInvulnerable());
        }

        void restore(SurvivorBot bot) {
            bot.setNoAi(noAi);
            bot.setNoGravity(noGravity);
            bot.setInvulnerable(invulnerable);
        }
    }

    private static final class BotState {
        private final BotFlags flags;
        private final double baseX;
        private final double baseZ;
        private double phase;

        private BotState(BotFlags flags, double baseX, double baseZ, double phase) {
            this.flags = flags;
            this.baseX = baseX;
            this.baseZ = baseZ;
            this.phase = phase;
        }
    }
}
