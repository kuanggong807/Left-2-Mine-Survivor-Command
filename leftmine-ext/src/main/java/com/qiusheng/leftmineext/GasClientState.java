package com.qiusheng.leftmineext;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client-side render/input state for the gas-fill channel. The server remains
 * authoritative; this class only sends "right-click still held" and draws the
 * centered progress widget.
 */
public final class GasClientState {
    private static final ResourceLocation BORDER =
            ResourceLocation.fromNamespaceAndPath(LeftMineExt.MOD_ID, "textures/gui/gas_progress_border.png");
    private static final ResourceLocation BODY =
            ResourceLocation.fromNamespaceAndPath(LeftMineExt.MOD_ID, "textures/gui/gas_progress_body.png");
    private static final ResourceLocation FULL =
            ResourceLocation.fromNamespaceAndPath(LeftMineExt.MOD_ID, "textures/gui/gas_progress_full.png");
    private static final ResourceLocation COUNTER_BG =
            ResourceLocation.fromNamespaceAndPath(LeftMineExt.MOD_ID, "textures/gui/gas_counter_bg.png");
    private static final ResourceLocation COUNTER_DIGITS =
            ResourceLocation.fromNamespaceAndPath(LeftMineExt.MOD_ID, "textures/gui/gas_counter_digits.png");
    private static final String COUNTER_GLYPHS = "0123456789/";

    private static boolean active;
    private static float progress;
    private static int completeTicks;
    private static boolean wasDown;
    private static int delivered;
    private static int required;
    private static boolean portVisible;
    private static int portDelivered;
    private static int portRequired;
    private static CameraType previousCamera;
    private static boolean cameraLocked;

    public static void acceptPort(GasNetwork.PortPayload payload) {
        portVisible = payload.visible();
        portDelivered = payload.delivered();
        portRequired = payload.required();
    }

    public static void accept(GasNetwork.StatePayload payload) {
        delivered = payload.delivered();
        required = payload.required();
        if (payload.active()) {
            if (!active) {
                lockCamera();
            }
            active = true;
            progress = Math.max(0.0F, Math.min(1.0F, payload.progress()));
            completeTicks = 0;
        } else {
            active = false;
            progress = Math.max(0.0F, Math.min(1.0F, payload.progress()));
            restoreCamera();
            completeTicks = payload.complete() ? 20 : 0;
        }
    }

    public static void tick() {
        if (completeTicks > 0) {
            completeTicks--;
        }
        if (!active) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        if (minecraft.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        boolean down = minecraft.options.keyUse.isDown() || minecraft.mouseHandler.isRightPressed();
        if (down || wasDown) {
            PacketDistributor.sendToServer(new GasNetwork.HoldPayload(down));
        }
        wasDown = down;
    }

    public static void render(GuiGraphics graphics) {
        if (!active && completeTicks <= 0 && !portVisible) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        int progressWidth = Math.max(160, Math.min(560, Math.round(screenWidth * 0.34F)));

        if (portVisible) {
            drawCounter(graphics, screenWidth, screenHeight, progressWidth);
        }
        if (!active && completeTicks <= 0) {
            return;
        }

        // Empty frame first, then the full image clipped to the growing gold
        // bar. At 100% the whole progressbar_2 is drawn, so it is exact.
        int width = progressWidth;
        float scale = width / 512.0F;
        int height = Math.round(128.0F * scale);
        int x = (screenWidth - width) / 2;
        int y = screenHeight / 2 - Math.round(55.5F * scale);
        graphics.blit(BORDER, x, y, width, height, 0.0F, 0.0F, 512, 128, 512, 128);

        int barX = x + Math.round(76.0F * scale);
        int barY = y + Math.round(49.0F * scale);
        int barWidth = Math.round(398.0F * scale);
        int barHeight = Math.round(13.0F * scale);
        int filled = Math.round(barWidth * Math.max(0.0F, Math.min(1.0F, progress)));
        if (filled > 0 && progress < 1.0F) {
            graphics.enableScissor(barX, barY, barX + filled, barY + barHeight);
            graphics.blit(FULL, x, y, width, height, 0.0F, 0.0F, 512, 128, 512, 128);
            graphics.disableScissor();
        }
        if (progress >= 1.0F) {
            graphics.blit(FULL, x, y, width, height, 0.0F, 0.0F, 512, 128, 512, 128);
        }

        Component label = Component.translatable("leftmineext.gas.hud");
        int textX = x + Math.round(76.0F * scale);
        int textY = Math.max(0, barY - 11);
        graphics.drawString(minecraft.font, label, textX, textY, 0xFFFFFFFF, true);
    }

    private static void drawCounter(GuiGraphics graphics, int screenWidth, int screenHeight, int progressWidth) {
        // sl.png is 246x119. Keep the counter at roughly 24% of the progress
        // bar width, matching the video ratio, with a readable minimum size.
        int bgWidth = Math.max(64, Math.min(110, Math.round(progressWidth * 0.24F)));
        float scale = bgWidth / 246.0F;
        int bgHeight = Math.round(119.0F * scale);
        int bx = (screenWidth - bgWidth) / 2;
        int by = Math.max(4, Math.round(screenHeight * 0.04F));
        graphics.blit(COUNTER_BG, bx, by, bgWidth, bgHeight,
                0.0F, 0.0F, 246, 119, 246, 119);

        String text = portDelivered + " / " + portRequired;
        int total = 0;
        for (int i = 0; i < text.length(); i++) {
            total += counterAdvance(text.charAt(i), scale);
        }
        int right = bx + Math.round(230.0F * scale);
        int tx = right - total;
        int ty = by + Math.round(55.0F * scale);
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            int advance = counterAdvance(ch, scale);
            int glyph = COUNTER_GLYPHS.indexOf(ch);
            if (glyph >= 0) {
                graphics.blit(COUNTER_DIGITS, tx, ty, advance, Math.round(40.0F * scale),
                        glyph * 30, 0, 30, 40, 330, 40);
            }
            tx += advance;
        }
    }

    private static int counterAdvance(char ch, float scale) {
        if (ch == ' ') {
            return Math.max(1, Math.round(10.0F * scale));
        }
        return Math.max(1, Math.round((ch == '/' ? 20.0F : 24.0F) * scale));
    }

    private static void lockCamera() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!cameraLocked) {
            previousCamera = minecraft.options.getCameraType();
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            cameraLocked = true;
        }
    }

    private static void restoreCamera() {
        if (cameraLocked) {
            Minecraft.getInstance().options.setCameraType(
                    previousCamera == null ? CameraType.FIRST_PERSON : previousCamera);
            cameraLocked = false;
            previousCamera = null;
        }
    }

    private GasClientState() {
    }
}
