package com.qiusheng.leftmineext.client;

import com.qiusheng.leftmineext.LeftMineLocalization;
import net.minecraft.client.Minecraft;

/**
 * 客户端当前语言。英文/其他语言不启用 Left 2 Mine 硬编码英文的汉化，
 * 保持英文原文；中文环境才套用中文词典。
 */
public final class ClientLanguage {
    private ClientLanguage() {
    }

    public static boolean isChinese() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getLanguageManager() == null) {
            return false;
        }
        return LeftMineLocalization.isChinese(minecraft.getLanguageManager().getSelected());
    }
}
