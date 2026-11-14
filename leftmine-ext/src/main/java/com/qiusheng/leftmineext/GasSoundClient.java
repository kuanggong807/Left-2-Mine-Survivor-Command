package com.qiusheng.leftmineext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;

import java.util.HashMap;
import java.util.Map;

public final class GasSoundClient {
    public static final int POUR_START = 0;
    public static final int POUR_STOP = 1;
    public static final int FILL_COMPLETE = 2;

    private static final Map<Integer, GasPourSound> POURING = new HashMap<>();

    public static void handle(int action, int playerId) {
        switch (action) {
            case POUR_START -> startPour(playerId);
            case POUR_STOP -> stopPour(playerId);
            case FILL_COMPLETE -> Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(LeftMineSounds.FILL_COMPLETE.get(), 1.0F));
            default -> {
            }
        }
    }

    private static void startPour(int playerId) {
        stopPour(playerId);
        GasPourSound sound = new GasPourSound(playerId);
        POURING.put(playerId, sound);
        Minecraft.getInstance().getSoundManager().play(sound);
    }

    private static void stopPour(int playerId) {
        GasPourSound old = POURING.remove(playerId);
        if (old != null) {
            old.stopNow();
        }
    }

    private GasSoundClient() {
    }
}
