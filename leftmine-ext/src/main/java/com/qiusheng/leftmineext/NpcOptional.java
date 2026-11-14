package com.qiusheng.leftmineext;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;

/**
 * Optional CustomNPCs bridge. No class outside this file may reference the
 * noppes.* classes unless CustomNPCs is loaded; this keeps the rest of the
 * add-on usable without CustomNPCs installed.
 */
public final class NpcOptional {
    private static final String NPC_PACKAGE = "noppes.npcs.";

    public static boolean loaded() {
        return ModList.get().isLoaded("customnpcs");
    }

    public static boolean isNpc(Object entity) {
        return loaded() && entity != null
                && entity.getClass().getName().startsWith(NPC_PACKAGE);
    }

    public static boolean isArmedNpc(LivingEntity entity) {
        return loaded() && NpcGunCompat.isArmedNpc(entity);
    }

    public static boolean hasRangedWeapon(LivingEntity entity) {
        return loaded() && NpcGunCompat.hasRangedWeapon(entity);
    }

    public static boolean isSurvivorTarget(LivingEntity entity) {
        return loaded() && NpcGunCompat.isSurvivorTarget(entity);
    }

    public static boolean isNpcSource(DamageSource source) {
        return loaded() && NpcGunCompat.isNpcSource(source);
    }

    public static void tick(ServerLevel level) {
        if (loaded()) {
            NpcGunCompat.tick(level);
        }
    }

    private NpcOptional() {
    }
}
