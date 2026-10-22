package com.qiusheng.leftmineext;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.fml.ModList;
import net.thecallunxz.left2mine.combat.CommonInfected;
import net.thecallunxz.left2mine.combat.SpecialContent;
import net.thecallunxz.left2mine.combat.SpecialInfected;
import net.thecallunxz.left2mine.bot.SurvivorBot;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.Comparator;

/**
 * 载具保护与感染者目标重定向。
 *
 * <p>NeoMTR 的乘客不是原版 passenger，而是由 RailwayDataCoolDownModule
 * 的 riding cooldown 状态驱动；canRide=false 表示正在 Riding（或刚下车 2 tick）。</p>
 */
public final class VehicleGuard {
    private static final Logger LOGGER = LogUtils.getLogger();

    private VehicleGuard() {
    }

    public static boolean isInfected(Entity entity) {
        return entity instanceof CommonInfected || entity instanceof SpecialInfected;
    }

    public static boolean isInVehicle(ServerPlayer player) {
        if (player.isPassenger() || player.getVehicle() != null) {
            return true;
        }
        return ModList.get().isLoaded("mtr") && MtrVehicleDetector.isRiding(player);
    }

    /**
     * 玩家和已进入载具的 SurvivorBot 都算受保护实体。
     */
    public static boolean isInVehicle(Entity entity) {
        if (entity instanceof ServerPlayer player) {
            return isInVehicle(player);
        }
        if (entity instanceof SurvivorBot bot) {
            return BotVehicleFollow.isFollowing(bot);
        }
        return entity != null && (entity.isPassenger() || entity.getVehicle() != null);
    }

    public static LivingEntity findAlternative(Mob mob, LivingEntity blocked) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return null;
        }
        return level.players().stream()
                .filter(p -> p != blocked)
                .filter(ServerPlayer::isAlive)
                .filter(p -> !p.isCreative() && !p.isSpectator())
                .filter(p -> !isInVehicle(p))
                .min(Comparator.comparingDouble(mob::distanceToSqr))
                .orElse(null);
    }

    public static void retargetAll(ServerLevel level) {
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof Mob mob) || !isInfected(mob) || !mob.isAlive()) {
                continue;
            }
            if (mob.getTarget() instanceof LivingEntity target && isInVehicle(target)) {
                LivingEntity replacement = findAlternative(mob, target);
                LOGGER.debug("[leftmineext] vehicle retarget {} -> {} ({})",
                        target.getName().getString(),
                        replacement != null ? replacement.getName().getString() : "none",
                        mob.getName().getString());
                mob.setTarget(replacement);
            }
        }
    }

    public static boolean infectedDamage(DamageSource source) {
        boolean direct = isInfected(source.getEntity())
                || isInfected(source.getDirectEntity())
                || source.getDirectEntity() instanceof Projectile projectile && isInfected(projectile.getOwner());
        if (direct) {
            return true;
        }
        // 酸液等无实体来源的 Left 2 Mine 环境伤害也要挡住；玩家枪械/爆炸仍有 source entity，不会误伤友军。
        return source.getEntity() == null
                && source.getDirectEntity() == null
                && source.typeHolder().unwrapKey()
                        .map(key -> key.location().getNamespace().equals("left2mine"))
                        .orElse(false);
    }

    /**
     * 载具内玩家/bot 移除已经挂在身上的捕捉/胆汁效果，避免“先被控再上车”。
     */
    public static void cleanupEffects(LivingEntity entity) {
        if (!isInVehicle(entity)) {
            return;
        }
        for (MobEffectInstance instance : java.util.List.copyOf(entity.getActiveEffects())) {
            if (restraintEffect(instance)) {
                entity.removeEffect(instance.getEffect());
            }
        }
    }

    public static void cleanupEffects(ServerLevel level) {
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof LivingEntity living && isInVehicle(living)) {
                cleanupEffects(living);
            }
        }
    }

    public static boolean restraintEffect(MobEffectInstance instance) {
        MobEffect effect = instance.getEffect().value();
        return effect == SpecialContent.PINNED.get()
                || effect == SpecialContent.SMOKED.get()
                || effect == SpecialContent.CHARGED.get()
                || effect == SpecialContent.JOCKEYED.get()
                || effect == SpecialContent.BILE.get();
    }
}
