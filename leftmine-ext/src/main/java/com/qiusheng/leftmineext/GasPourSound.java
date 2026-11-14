package com.qiusheng.leftmineext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * A looping pour sound attached to the fueling player. It starts from the
 * beginning on every new channel and is stopped by the server when the player
 * releases right click or the channel is cancelled.
 */
public final class GasPourSound extends AbstractTickableSoundInstance {
    private final int playerId;

    public GasPourSound(int playerId) {
        super(LeftMineSounds.GAS_POUR.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.playerId = playerId;
        this.looping = true;
        this.volume = 1.0F;
        this.pitch = 1.0F;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
        updatePosition();
    }

    @Override
    public void tick() {
        if (!updatePosition()) {
            stopNow();
        }
    }

    private boolean updatePosition() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !(minecraft.level.getEntity(playerId) instanceof Player player)) {
            return false;
        }
        x = player.getX();
        y = player.getY() + 0.8D;
        z = player.getZ();
        return true;
    }

    public void stopNow() {
        stop();
    }
}
