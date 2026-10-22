package com.qiusheng.leftmineext.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.qiusheng.leftmineext.LeftMineLocalization;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 覆盖所有 Component.literal 的硬编码文本：GUI 按钮、标题、tooltip、聊天等。
 * 使用 ThreadLocal 防止 translate 内部创建 Component 时递归。
 */
@Mixin(MutableComponent.class)
public abstract class MutableComponentMixin {
    private static final ThreadLocal<Boolean> LEFTMINEEXT_INTERNAL = ThreadLocal.withInitial(() -> false);

    @ModifyReturnValue(method = "create", at = @At("RETURN"))
    private static MutableComponent leftmineext$translateLiteral(MutableComponent original) {
        if (LEFTMINEEXT_INTERNAL.get()
                || !(original.getContents() instanceof PlainTextContents.LiteralContents literal)) {
            return original;
        }
        String translated = LeftMineLocalization.translateTextClient(literal.text());
        if (translated.equals(literal.text())) {
            return original;
        }

        LEFTMINEEXT_INTERNAL.set(true);
        try {
            MutableComponent result = MutableComponent.create(new PlainTextContents.LiteralContents(translated));
            result.setStyle(original.getStyle());
            for (Component sibling : original.getSiblings()) {
                result.append(sibling);
            }
            return result;
        } finally {
            LEFTMINEEXT_INTERNAL.remove();
        }
    }
}
