package com.qiusheng.leftmineext;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import net.thecallunxz.left2mine.bot.SurvivorActor;
import net.thecallunxz.left2mine.bot.SurvivorBot;
import net.thecallunxz.left2mine.combat.CommonInfected;
import net.thecallunxz.left2mine.combat.GrenadeItem;
import net.thecallunxz.left2mine.combat.GunItem;
import net.thecallunxz.left2mine.combat.SpecialInfected;
import net.thecallunxz.left2mine.combat.ThrownWeapon;
import net.thecallunxz.left2mine.combat.WeaponSpec;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.ItemStackWrapper;
import noppes.npcs.entity.EntityNPCInterface;

/**
 * CustomNPCs integration.
 *
 * An NPC with a Left 2 Mine gun fires that gun (never melee); an NPC with a
 * Left 2 Mine throwable throws it every three seconds without consuming it.
 * Infected only consider gun-holding NPCs as targets. All NPC damage to
 * players and SurvivorBots is filtered out in LeftMineExt.
 */
public final class NpcGunCompat {
    private static final double NPC_RANGE = 32.0D;
    private static final double INFECTED_RANGE = 24.0D;
    private static final int THROW_INTERVAL = 60;

    public static boolean isRangedItem(ItemStack stack) {
        return stack.getItem() instanceof GunItem || stack.getItem() instanceof GrenadeItem;
    }

    public static boolean isArmedNpc(LivingEntity entity) {
        if (!(entity instanceof EntityNPCInterface npc) || !npc.isAlive()) {
            return false;
        }
        return isRangedItem(weaponStack(npc));
    }

    public static boolean hasRangedWeapon(LivingEntity entity) {
        if (!(entity instanceof EntityNPCInterface npc) || !npc.isAlive()) {
            return false;
        }
        return isRangedItem(weaponStack(npc));
    }

    /**
     * Role companions and job-override NPCs may not expose the inventory
     * right-hand weapon through getMainHandItem() until they are attacking.
     * Read the right-hand inventory slot directly as a fallback.
     */
    private static ItemStack weaponStack(EntityNPCInterface npc) {
        ItemStack hand = npc.getMainHandItem();
        if (isRangedItem(hand)) {
            return hand;
        }
        IItemStack right = npc.inventory.getRightHand();
        if (right != null) {
            ItemStack inventory = ItemStackWrapper.MCItem(right);
            if (isRangedItem(inventory)) {
                return inventory;
            }
        }
        return hand;
    }

    public static boolean isSurvivorTarget(LivingEntity entity) {
        return entity instanceof Player || entity instanceof SurvivorBot || SurvivorActor.is(entity);
    }

    public static boolean isNpcSource(DamageSource source) {
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        if (isNpc(attacker) || isNpc(direct)) {
            return true;
        }
        return direct instanceof Projectile projectile && isNpc(projectile.getOwner());
    }

    private static boolean isNpc(Entity entity) {
        return entity instanceof EntityNPCInterface;
    }

    public static void tick(ServerLevel level) {
        long gameTime = level.getGameTime();
        // NPCs are driven from EntityTickEvent.Post; this only maintains the
        // infected-side target selection.
        if (gameTime % 10L == 0L) {
            retargetInfected(level);
        }
    }

    public static void tickNpc(ServerLevel level, EntityNPCInterface npc) {
        ItemStack stack = weaponStack(npc);
        if (!isRangedItem(stack)) {
            return;
        }

        // Do not keep a survivor/bot as a CustomNPCs attack target. Infected
        // targets are kept so the NPC visibly locks on and can expose a role
        // companion's inventory weapon through getMainHandItem().
        LivingEntity currentTarget = npc.getTarget();
        if (currentTarget != null && !isInfected(currentTarget)) {
            npc.setTarget(null);
        }

        LivingEntity target = findInfectedTarget(level, npc);

        if (stack.getItem() instanceof GunItem gun) {
            npc.swinging = false;
            if (GunItem.ammo(stack) == 0 && GunItem.reserve(stack) == 0) {
                fillAllAmmo(stack, gun);
            }
            gun.inventoryTick(stack, level, npc, 0, true);
            if (gun.spec == WeaponSpec.GRENADE_LAUNCHER && GunItem.reloadTicks(stack) > 0) {
                gun.inventoryTick(stack, level, npc, 0, true);
            }
            if (target == null) {
                return;
            }

            // Setting the target first makes EntityNPCInterface.getMainHandItem
            // return the inventory right hand for role/job NPCs in L2M 2.0.3.
            npc.setTarget(target);
            ItemStack hand = npc.getMainHandItem();
            if (isRangedItem(hand)) {
                stack = hand;
                gun = (GunItem) stack.getItem();
            }
            npc.swinging = false;
            aimAt(npc, target, false);
            fireGun(npc, stack, gun);
        } else if (stack.getItem() instanceof GrenadeItem grenade) {
            if (target == null) {
                return;
            }
            npc.setTarget(target);
            aimAt(npc, target, true);
            throwThrowable(level, npc, grenade, target);
        }
    }

    /**
     * GunItem.act() can silently reject a shot for an NPC (swinging state,
     * weapon timing, or a CustomNPCs hook). If it did not consume a round,
     * perform one direct L2M hitscan/launcher shot so the NPC still attacks.
     */
    private static void fireGun(EntityNPCInterface npc, ItemStack stack, GunItem gun) {
        int before = GunItem.ammo(stack);
        GunItem.act(npc, 0);
        if (before <= 0 || GunItem.ammo(stack) < before) {
            return;
        }

        CompoundTag tag = GunItem.state(stack);
        tag.putInt("ammo", Math.max(0, before - 1));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        float spread = GunItem.spread(stack);
        if (gun.spec == WeaponSpec.GRENADE_LAUNCHER) {
            net.thecallunxz.left2mine.combat.LauncherGrenade.fire(
                    npc, spread, net.thecallunxz.left2mine.combat.ShotLagCompensation.current(npc));
        } else {
            net.thecallunxz.left2mine.combat.Hitscan.fire(npc, spread);
        }
    }

    /**
     * NPCs do not carry ammo piles, so when their gun is fully dry give them a
     * complete magazine and a complete reserve. This writes the same custom-data
     * fields GunItem itself uses, rather than relying on ammo-pile refill flags.
     */
    private static void fillAllAmmo(ItemStack stack, GunItem gun) {
        GunItem.initialize(stack);
        CompoundTag tag = GunItem.state(stack);
        tag.putInt("ammo", gun.spec.ammo.magazine());
        tag.putInt("storage", gun.spec.ammo.reserve());
        tag.putInt("reloadTicks", 0);
        tag.remove("shellReloadClock");
        tag.remove("shellReloadPhase");
        tag.remove("shellPump");
        tag.remove("queuedShot");
        tag.remove("queuedShotHeld");
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static void throwThrowable(ServerLevel level, EntityNPCInterface npc, GrenadeItem item, LivingEntity target) {
        long now = level.getGameTime();
        long nextThrow = npc.getPersistentData().getLong("leftmineextNpcThrowAt");
        if (now < nextThrow) {
            return;
        }
        npc.getPersistentData().putLong("leftmineextNpcThrowAt", now + THROW_INTERVAL);

        ThrownWeapon thrown = (ThrownWeapon) item.kind.entity().create(level);
        if (thrown == null) {
            return;
        }
        thrown.setOwner(npc);
        thrown.setPos(npc.getEyePosition().add(0.0D, -0.1D, 0.0D));
        // Aim at the infected's feet, with a little upward arc so molotovs,
        // pipe bombs and bile jars land on them even at close range.
        Vec3 feet = target.position().add(0.0D, 0.1D, 0.0D);
        Vec3 delta = feet.subtract(npc.getEyePosition());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        Vec3 velocity = delta.normalize().scale(2.0D);
        velocity = velocity.add(0.0D, 0.18D + horizontal * 0.035D, 0.0D);
        thrown.setDeltaMovement(velocity.add(npc.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D)));
        thrown.setYRot(-npc.getYRot());
        thrown.yRotO = thrown.getYRot();
        level.addFreshEntity(thrown);
    }

    private static LivingEntity findInfectedTarget(ServerLevel level, EntityNPCInterface npc) {
        LivingEntity current = npc.getTarget();
        if (isInfected(current) && current.isAlive()) {
            return current;
        }
        LivingEntity best = null;
        double bestDistance = NPC_RANGE * NPC_RANGE;
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof LivingEntity living) || !isInfected(living) || !living.isAlive() || living.isSpectator()) {
                continue;
            }
            double distance = npc.distanceToSqr(living);
            if (distance < bestDistance) {
                best = living;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static void aimAt(EntityNPCInterface npc, LivingEntity target, boolean feet) {
        Vec3 aimPoint = feet ? target.position().add(0.0D, 0.1D, 0.0D) : target.getEyePosition();
        Vec3 delta = aimPoint.subtract(npc.getEyePosition());
        float yaw = (float) (Mth.atan2(delta.z, delta.x) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) (-Mth.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)) * 180.0D / Math.PI);
        npc.setYRot(yaw);
        npc.setXRot(pitch);
        npc.yHeadRot = yaw;
        npc.yBodyRot = yaw;
    }

    private static void retargetInfected(ServerLevel level) {
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof CommonInfected) && !(entity instanceof SpecialInfected)) {
                continue;
            }
            if (!(entity instanceof Mob mob)) {
                continue;
            }

            LivingEntity current = mob.getTarget();
            if (current instanceof EntityNPCInterface npc && !isArmedNpc(npc)) {
                mob.setTarget(null);
                current = null;
            }
            if (current != null && current.isAlive()) {
                continue;
            }

            EntityNPCInterface npc = findNearestArmedNpc(level, mob);
            if (npc != null) {
                mob.setTarget(npc);
            }
        }
    }

    private static EntityNPCInterface findNearestArmedNpc(ServerLevel level, Mob infected) {
        EntityNPCInterface best = null;
        double bestDistance = INFECTED_RANGE * INFECTED_RANGE;
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof EntityNPCInterface npc) || !isArmedNpc(npc)) {
                continue;
            }
            double distance = infected.distanceToSqr(npc);
            if (distance < bestDistance && infected.hasLineOfSight(npc)) {
                best = npc;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static boolean isInfected(LivingEntity entity) {
        return entity instanceof CommonInfected || entity instanceof SpecialInfected;
    }

    private NpcGunCompat() {
    }
}
