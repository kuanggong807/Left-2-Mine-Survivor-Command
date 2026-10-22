package com.qiusheng.leftmineext;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(LeftMineExt.MOD_ID)
public final class LeftMineExt {
    public static final String MOD_ID = "leftmineext";
    public static final String ADMIN_TAG = AdminManager.ADMIN_TAG;

    public LeftMineExt(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        LeftMineCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AdminManager.onLogin(player);
        }
    }

    @SubscribeEvent
    public void playerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AdminManager.onLogout(player);
        }
    }

    /**
     * 感染者尝试锁定载具内玩家时，改锁载具外的最近玩家；没有则清空目标。
     */
    @SubscribeEvent
    public void changeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide() || !VehicleGuard.isInfected(mob)) {
            return;
        }
        if (event.getNewAboutToBeSetTarget() instanceof LivingEntity target && VehicleGuard.isInVehicle(target)) {
            LivingEntity replacement = VehicleGuard.findAlternative(mob, target);
            event.setNewAboutToBeSetTarget(replacement);
        }
    }

    /**
     * 兜底清理已存在的锁定目标；同时保留 1 秒一次的管理员模式兜底。
     */
    @SubscribeEvent
    public void serverTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 2 == 0) {
            for (ServerLevel level : event.getServer().getAllLevels()) {
                BotVehicleFollow.tick(level);
            }
        }

        if (event.getServer().getTickCount() % 5 == 0) {
            for (ServerLevel level : event.getServer().getAllLevels()) {
                VehicleGuard.retargetAll(level);
                VehicleGuard.cleanupEffects(level);
            }
        }

        if (event.getServer().getTickCount() % 20 != 0) {
            return;
        }
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (AdminManager.isAdmin(player) && !AdminManager.isPlaying(player) && !player.isCreative()) {
                AdminManager.park(player);
            }
        }
    }

    /**
     * 载具内玩家/bot 不吃 Left 2 Mine 感染者的直接伤害或投射物伤害。
     */
    @SubscribeEvent
    public void incomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof LivingEntity target
                && VehicleGuard.isInVehicle(target)
                && VehicleGuard.infectedDamage(event.getSource())) {
            event.setCanceled(true);
        }
    }

    /**
     * 阻止 Smoker/Jockey/Charger/Spitter/Boomer 的控制或胆汁效果落到载具内玩家/bot。
     */
    @SubscribeEvent
    public void applicableEffect(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof LivingEntity target
                && VehicleGuard.isInVehicle(target)
                && VehicleGuard.restraintEffect(event.getEffectInstance())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }
}
