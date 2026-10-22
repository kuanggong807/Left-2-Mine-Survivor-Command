package com.qiusheng.leftmineext.mixin;

import com.qiusheng.leftmineext.LeftMineLocalization;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Left 2 Mine 的 MapAuthoring 用 displayClientMessage/sendSystemMessage 发送硬编码英文。
 * 在服务端发包前替换为中文，客户端无需安装额外资源包。
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMessageMixin {
    @ModifyVariable(
            method = "displayClientMessage(Lnet/minecraft/network/chat/Component;Z)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private Component leftmineext$translateActionBar(Component message) {
        return LeftMineLocalization.translateForPlayer(message, (ServerPlayer) (Object) this);
    }

    @ModifyVariable(
            method = "sendSystemMessage(Lnet/minecraft/network/chat/Component;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private Component leftmineext$translateChat(Component message) {
        return LeftMineLocalization.translateForPlayer(message, (ServerPlayer) (Object) this);
    }
}
