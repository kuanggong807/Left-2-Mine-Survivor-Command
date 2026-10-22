package com.qiusheng.leftmineext.mixin.client;

import com.qiusheng.leftmineext.LeftMineLocalization;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 覆盖直接以 String 绘制的 HUD 文本，例如 “Pinned by a Hunter!”。
 */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsStringMixin {
    @ModifyVariable(
            method = "drawCenteredString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private String leftmineext$centered(String text) {
        return LeftMineLocalization.translateTextClient(text);
    }

    @ModifyVariable(
            method = "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)I",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private String leftmineext$draw(String text) {
        return LeftMineLocalization.translateTextClient(text);
    }

    @ModifyVariable(
            method = "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private String leftmineext$drawShadow(String text) {
        return LeftMineLocalization.translateTextClient(text);
    }

    @ModifyVariable(
            method = "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;FFIZ)I",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private String leftmineext$drawFloat(String text) {
        return LeftMineLocalization.translateTextClient(text);
    }
}
