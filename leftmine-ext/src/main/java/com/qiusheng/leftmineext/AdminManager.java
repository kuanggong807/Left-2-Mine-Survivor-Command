package com.qiusheng.leftmineext;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.thecallunxz.left2mine.session.GameSessions;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理员状态管理。
 *
 * <p>核心策略：管理员标签存在时，未参战的管理员保持创造模式。
 * Left 2 Mine 的自动加入、BotRoster、安全屋人数、团灭检测都显式排除
 * 创造模式玩家，因此无需改动前置模组的大量内部列表。</p>
 */
public final class AdminManager {
    public static final String ADMIN_TAG = "leftmine_admin";
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String PREVIOUS_GAME_MODE = "leftmineext_previous_game_mode";
    private static final Set<UUID> PLAYING = ConcurrentHashMap.newKeySet();

    private AdminManager() {
    }

    public static boolean isAdmin(ServerPlayer player) {
        return player.getTags().contains(ADMIN_TAG);
    }

    public static boolean isPlaying(ServerPlayer player) {
        return PLAYING.contains(player.getUUID());
    }

    public static void markPlaying(ServerPlayer player) {
        PLAYING.add(player.getUUID());
    }

    public static void onLogin(ServerPlayer player) {
        PLAYING.remove(player.getUUID());
        if (isAdmin(player)) {
            park(player);
        }
    }

    public static void onLogout(ServerPlayer player) {
        PLAYING.remove(player.getUUID());
    }

    public static void add(ServerPlayer player) {
        boolean alreadyAdmin = isAdmin(player);
        player.addTag(ADMIN_TAG);
        if (!alreadyAdmin) {
            // 新管理员立即退出对局；已经是管理员且正在游戏中的，不打断。
            PLAYING.remove(player.getUUID());
        }
        park(player);
        LOGGER.info("[leftmineext] admin add {} already={} mode={} playing={}",
                player.getGameProfile().getName(), alreadyAdmin,
                player.gameMode.getGameModeForPlayer(), isPlaying(player));
    }

    public static void remove(ServerPlayer player) {
        player.removeTag(ADMIN_TAG);
        PLAYING.remove(player.getUUID());
        restore(player);
        LOGGER.info("[leftmineext] admin remove {} mode={}",
                player.getGameProfile().getName(), player.gameMode.getGameModeForPlayer());
    }

    /**
     * 未参战管理员进入创造模式。创造模式是前置模组所有玩家筛选的统一旁路。
     */
    public static void park(ServerPlayer player) {
        if (!isAdmin(player) || isPlaying(player)) {
            return;
        }

        CompoundTag data = player.getPersistentData();
        if (!data.contains(PREVIOUS_GAME_MODE)) {
            data.putInt(PREVIOUS_GAME_MODE, player.gameMode.getGameModeForPlayer().getId());
        }
        if (!player.isCreative()) {
            player.setGameMode(GameType.CREATIVE);
        }
    }

    /**
     * 移除管理员标签后恢复此前的游戏模式。
     */
    public static void restore(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (data.contains(PREVIOUS_GAME_MODE)) {
            int id = data.getInt(PREVIOUS_GAME_MODE);
            data.remove(PREVIOUS_GAME_MODE);
            GameType type = GameType.byId(id);
            player.setGameMode(type == null ? GameType.SURVIVAL : type);
        } else if (player.isCreative()) {
            player.setGameMode(GameType.SURVIVAL);
        }
    }

    /**
     * 管理员手动加入对局：切入冒险模式并复用前置模组的玩家重置逻辑。
     */
    public static void join(ServerPlayer player, ServerLevel level, BlockPos spawn) {
        PLAYING.add(player.getUUID());
        player.setGameMode(GameType.ADVENTURE);
        GameSessions.resetPlayer(player, level, spawn, true);
        LOGGER.info("[leftmineext] admin join {} mode={}",
                player.getGameProfile().getName(), player.gameMode.getGameModeForPlayer());
    }

    /**
     * 管理员退出对局：取消参战标记并回到创造模式，避免计入安全屋/团灭等人数。
     */
    public static void leave(ServerPlayer player) {
        PLAYING.remove(player.getUUID());
        park(player);
        LOGGER.info("[leftmineext] admin leave {} mode={}",
                player.getGameProfile().getName(), player.gameMode.getGameModeForPlayer());
    }
}
