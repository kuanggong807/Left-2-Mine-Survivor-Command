package com.qiusheng.leftmineext;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.thecallunxz.left2mine.map.MapSavedData;
import net.thecallunxz.left2mine.session.GameSessions;

import java.util.Collection;

public final class LeftMineCommands {
    private static final String[] HELP_KEYS = {
            "leftmineext.help.header",
            "leftmineext.help.fuhuo",
            "leftmineext.help.admin_add",
            "leftmineext.help.admin_remove",
            "leftmineext.help.game_join",
            "leftmineext.help.game_leave",
            "leftmineext.help.gas",
            "leftmineext.help.status",
            "leftmineext.help.tag"
    };

    private LeftMineCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> fuhuo = Commands.literal("fuhuo")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("pos", Vec3Argument.vec3())
                                .executes(LeftMineCommands::fuhuo)));

        LiteralArgumentBuilder<CommandSourceStack> admin = Commands.literal("admin")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("add")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(LeftMineCommands::adminAdd)))
                .then(Commands.literal("remove")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(LeftMineCommands::adminRemove)));

        LiteralArgumentBuilder<CommandSourceStack> game = Commands.literal("game")
                .then(Commands.literal("join")
                        .requires(source -> source.getEntity() instanceof ServerPlayer player
                                && AdminManager.isAdmin(player))
                        .executes(LeftMineCommands::join))
                .then(Commands.literal("leave")
                        .requires(source -> source.getEntity() instanceof ServerPlayer player
                                && AdminManager.isAdmin(player))
                        .executes(LeftMineCommands::leave));

        LiteralArgumentBuilder<CommandSourceStack> gas = Commands.literal("gas")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("set")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                        .then(Commands.argument("redstone", BlockPosArgument.blockPos())
                                                .executes(LeftMineCommands::gasSet)))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(LeftMineCommands::gasRemove)))
                .then(Commands.literal("time")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 60))
                                .executes(LeftMineCommands::gasTime)));

        dispatcher.register(Commands.literal("leftmine")
                .executes(ctx -> help(ctx.getSource()))
                .then(Commands.literal("help")
                        .executes(ctx -> help(ctx.getSource())))
                .then(Commands.literal("status")
                        .executes(ctx -> status(ctx.getSource())))
                .then(fuhuo)
                .then(admin)
                .then(game)
                .then(gas));
    }

    private static int help(CommandSourceStack source) {
        for (String key : HELP_KEYS) {
            source.sendSuccess(() -> Component.translatable(key), false);
        }
        return 1;
    }

    private static int status(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("leftmineext.only_player"));
            return 0;
        }
        String mode = player.gameMode.getGameModeForPlayer().getName();
        source.sendSuccess(() -> Component.translatable(
                "leftmineext.status.line",
                yesNo(AdminManager.isAdmin(player)),
                yesNo(AdminManager.isPlaying(player)),
                yesNo(VehicleGuard.isInVehicle(player)),
                BotVehicleFollow.followingCount(),
                Component.translatable("leftmineext.mode." + mode)
        ), false);
        return 1;
    }

    private static Component yesNo(boolean value) {
        return Component.translatable(value ? "leftmineext.status.yes" : "leftmineext.status.no");
    }

    private static int fuhuo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        Vec3 pos = Vec3Argument.getVec3(ctx, "pos");
        BlockPos blockPos = BlockPos.containing(pos);
        ServerLevel level = ctx.getSource().getLevel();

        int count = 0;
        for (ServerPlayer target : targets) {
            if (AdminManager.isAdmin(target) && !AdminManager.isPlaying(target)) {
                AdminManager.markPlaying(target);
            }
            // GameSessions.resetPlayer skips creative mode; set adventure before reviving.
            target.setGameMode(GameType.ADVENTURE);
            GameSessions.resetPlayer(target, level, blockPos, true);
            target.sendSystemMessage(Component.translatable("leftmineext.fuhuo.revived_self", blockPos.toShortString()));
            count++;
        }

        final int revived = count;
        if (revived == 0) {
            ctx.getSource().sendFailure(Component.translatable("leftmineext.fuhuo.none"));
            return 0;
        }
        ctx.getSource().sendSuccess(
                () -> Component.translatable("leftmineext.fuhuo.summary", revived, blockPos.toShortString()),
                true);
        return revived;
    }

    private static int adminAdd(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        for (ServerPlayer target : targets) {
            boolean already = AdminManager.isAdmin(target);
            AdminManager.add(target);
            target.sendSystemMessage(Component.translatable(
                    already ? "leftmineext.admin.already_self" : "leftmineext.admin.added_self"));
        }
        ctx.getSource().sendSuccess(
                () -> Component.translatable("leftmineext.admin.added_summary", targets.size()), true);
        return targets.size();
    }

    private static int adminRemove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        for (ServerPlayer target : targets) {
            if (!AdminManager.isAdmin(target)) {
                target.sendSystemMessage(Component.translatable("leftmineext.admin.not_admin_self"));
                continue;
            }
            AdminManager.remove(target);
            target.sendSystemMessage(Component.translatable("leftmineext.admin.removed_self"));
        }
        ctx.getSource().sendSuccess(
                () -> Component.translatable("leftmineext.admin.removed_summary", targets.size()), true);
        return targets.size();
    }

    private static int join(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (!AdminManager.isAdmin(player)) {
            ctx.getSource().sendFailure(Component.translatable("leftmineext.game.only_admin"));
            return 0;
        }

        ServerLevel level = ctx.getSource().getServer().overworld();
        MapSavedData data = MapSavedData.get(level);
        if (!data.inGame()) {
            ctx.getSource().sendFailure(Component.translatable("leftmineext.game.no_session"));
            return 0;
        }

        BlockPos spawn = data.position("respawn");
        AdminManager.join(player, level, spawn);
        player.sendSystemMessage(Component.translatable("leftmineext.game.joined_self", spawn.toShortString()));
        ctx.getSource().sendSuccess(
                () -> Component.translatable("leftmineext.game.joined_summary", player.getGameProfile().getName()), true);
        return 1;
    }

    private static int leave(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (!AdminManager.isAdmin(player)) {
            ctx.getSource().sendFailure(Component.translatable("leftmineext.game.only_admin"));
            return 0;
        }

        AdminManager.leave(player);
        player.sendSystemMessage(Component.translatable("leftmineext.game.left_self"));
        ctx.getSource().sendSuccess(
                () -> Component.translatable("leftmineext.game.left_summary", player.getGameProfile().getName()), true);
        return 1;
    }

    private static int gasSet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        BlockPos redstone = BlockPosArgument.getLoadedBlockPos(ctx, "redstone");
        ServerLevel level = ctx.getSource().getLevel();
        GasPortData data = GasPortData.get(level);
        GasPortData.GasPort port = data.setPort(level, pos, amount, redstone);
        data.setActive(port);
        GasFillManager.broadcastPort(level, port, true);
        ctx.getSource().sendSuccess(
                () -> Component.translatable("leftmineext.gas.set", pos.toShortString(), amount, redstone.toShortString()),
                true);
        return 1;
    }

    private static int gasRemove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        ServerLevel level = ctx.getSource().getLevel();
        GasPortData data = GasPortData.get(level);
        GasPortData.GasPort port = data.find(level, pos);
        if (port == null) {
            ctx.getSource().sendFailure(Component.translatable("leftmineext.gas.not_found"));
            return 0;
        }
        boolean wasActive = data.activePort() == port;
        data.remove(level, port);
        if (wasActive) {
            GasFillManager.broadcastPort(level, port, false);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("leftmineext.gas.removed", pos.toShortString()), true);
        return 1;
    }

    private static int gasTime(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        int seconds = IntegerArgumentType.getInteger(ctx, "seconds");
        for (ServerLevel level : ctx.getSource().getServer().getAllLevels()) {
            GasPortData.get(level).setFillSeconds(seconds);
        }
        GasPortData.get(ctx.getSource().getLevel()).setFillSeconds(seconds);
        ctx.getSource().sendSuccess(() -> Component.translatable("leftmineext.gas.time", seconds), true);
        return seconds;
    }
}
