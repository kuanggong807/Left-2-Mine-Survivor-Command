package com.qiusheng.leftmineext;

import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/**
 * 物品栏 tooltip 的硬编码英文/未翻译键在客户端渲染前替换。
 */
@EventBusSubscriber(modid = LeftMineExt.MOD_ID, value = Dist.CLIENT)
public final class ClientLocalizationEvents {
    private ClientLocalizationEvents() {
    }

    @SubscribeEvent
    public static void itemTooltip(ItemTooltipEvent event) {
        List<Component> lines = event.getToolTip();
        for (int i = 0; i < lines.size(); i++) {
            Component translated = LeftMineLocalization.translate(lines.get(i));
            if (translated != lines.get(i)) {
                lines.set(i, translated);
            }
        }
    }
}
