package com.qiusheng.leftmineext;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(LeftMineExt.MOD_ID)
public final class LeftMineExt {
    public static final String MOD_ID = "leftmineext";
    public static final String ADMIN_TAG = AdminManager.ADMIN_TAG;

    public LeftMineExt(IEventBus modEventBus) {
        LeftMineRegistry.register(modEventBus);
        LeftMineSounds.register(modEventBus);
        modEventBus.addListener(GasNetwork::register);
        modEventBus.addListener(this::buildCreativeTab);
        if (FMLEnvironment.dist.isClient()) {
            GasHud.register(modEventBus);
        }
        if (ModList.get().isLoaded("customnpcs")) {
            CustomNpcEvents.register();
        }
        NeoForge.EVENT_BUS.register(this);
    }

    private void buildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        // Only the Left 2 Mine "用品" (combat) creative tab, not any other tab.
        if (event.getTabKey().location().toString().equals("left2mine:combat")) {
            event.accept(LeftMineRegistry.QUEST_GASCAN.get());
        }
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        LeftMineCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AdminManager.onLogin(player);
            GasFillManager.sendActivePort(player);
            sendOptionalModNotice(player);
        }
    }

    private static void sendOptionalModNotice(ServerPlayer player) {
        if (!ModList.get().isLoaded("mtr")) {
            player.sendSystemMessage(optionalLine(
                    "leftmineext.optional.mtr_missing",
                    "https://modrinth.com/mod/neomtr"));
        }
        if (!ModList.get().isLoaded("customnpcs")) {
            player.sendSystemMessage(optionalLine(
                    "leftmineext.optional.customnpcs_missing",
                    "https://www.curseforge.com/minecraft/mc-mods/customnpcs-unofficial"));
        }
    }

    private static net.minecraft.network.chat.Component optionalLine(String key, String url) {
        net.minecraft.network.chat.MutableComponent link =
                net.minecraft.network.chat.Component.literal(url);
        link.setStyle(net.minecraft.network.chat.Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                .withUnderlined(true));
        return net.minecraft.network.chat.Component.translatable(key)
                .append(net.minecraft.network.chat.Component.literal(" "))
                .append(link);
    }

    @SubscribeEvent
    public void playerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GasFillManager.stop(player.getUUID(), false);
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
        GasFillManager.tick(event.getServer());
        if (event.getServer().getTickCount() % 10 == 0) {
            QuestCanHighlighter.tick(event.getServer());
        }
        if (event.getServer().getTickCount() % 2 == 0) {
            for (ServerLevel level : event.getServer().getAllLevels()) {
                BotVehicleFollow.tick(level);
                NpcOptional.tick(level);
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
        if (event.getEntity() instanceof ServerPlayer player) {
            GasFillManager.interrupt(player);
        }
        if (event.getEntity() instanceof LivingEntity target
                && VehicleGuard.isInVehicle(target)
                && VehicleGuard.infectedDamage(event.getSource())) {
            event.setCanceled(true);
            return;
        }
        // CustomNPCs guns, launcher grenades, thrown weapons and melee must
        // never damage players or SurvivorBots, including friendly fire.
        if (event.getEntity() instanceof LivingEntity target
                && NpcOptional.isSurvivorTarget(target)
                && NpcOptional.isNpcSource(event.getSource())) {
            event.setCanceled(true);
        }
    }

    /**
     * CustomNPCs refuses interaction while its NPC has an attack target.
     * Adventure-mode players still need to open dialogue/role GUIs during a
     * Left 2 Mine session, so clear the NPC target before the interaction.
     */
    /**
     * Right-clicking a registered gas port with the quest gas can starts a
     * hold-to-fill channel. The actual hold state is sent by the client tick
     * payload so releasing the mouse cancels it.
     */
    @SubscribeEvent
    public void gasInteract(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof ServerLevel level)
                || !event.getItemStack().is(LeftMineRegistry.QUEST_GASCAN.get())) {
            return;
        }
        if (GasFillManager.tryStart(player, level, event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void breakGasPort(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        GasPortData data = GasPortData.get(level);
        GasPortData.GasPort port = data.find(level, event.getPos());
        if (port == null) {
            return;
        }
        boolean admin = event.getPlayer().hasPermissions(2)
                || (event.getPlayer() instanceof ServerPlayer player && AdminManager.isAdmin(player));
        if (!admin) {
            event.setCanceled(true);
            return;
        }
        boolean wasActive = data.activePort() == port;
        data.remove(level, port);
        if (wasActive) {
            GasFillManager.broadcastPort(level, port, false);
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
