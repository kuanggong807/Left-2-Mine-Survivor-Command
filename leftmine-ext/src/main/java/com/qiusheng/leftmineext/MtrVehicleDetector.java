package com.qiusheng.leftmineext;

import mtr.data.Lift;
import mtr.data.LiftServer;
import mtr.data.RailwayData;
import net.minecraft.server.level.ServerPlayer;

/**
 * 仅在 NeoMTR 已加载时触碰 MTR 类，避免未安装 MTR 时类加载失败。
 */
final class MtrVehicleDetector {
    private MtrVehicleDetector() {
    }

    record Bounds(double halfWidth, double halfDepth) {
    }

    static boolean isRiding(ServerPlayer player) {
        try {
            RailwayData data = RailwayData.getInstance(player.serverLevel());
            if (data == null) {
                return false;
            }
            var module = data.railwayDataCoolDownModule;
            return !module.canRide(player)
                    || module.getRidingRoute(player) != null
                    || module.playerInVirtualDrive.contains(player);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * 电梯车厢的半宽/半深；火车或非 MTR 载具返回 null，由调用方使用默认车厢尺寸。
     */
    static Bounds bounds(ServerPlayer player) {
        try {
            RailwayData data = RailwayData.getInstance(player.serverLevel());
            if (data == null || data.lifts.isEmpty()) {
                return null;
            }
            Lift nearest = null;
            double nearestDistance = Double.MAX_VALUE;
            for (LiftServer lift : data.lifts) {
                double dx = lift.getPositionX() - player.getX();
                double dy = lift.getPositionY() - player.getY();
                double dz = lift.getPositionZ() - player.getZ();
                double distance = dx * dx + dy * dy + dz * dz;
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = lift;
                }
            }
            if (nearest == null) {
                return null;
            }
            double maxRange = Math.max(nearest.liftWidth, nearest.liftDepth) / 2.0 + 6.0;
            if (nearestDistance > maxRange * maxRange) {
                return null;
            }
            return new Bounds(
                    Math.max(0.45, nearest.liftWidth / 2.0 - 0.35),
                    Math.max(0.45, nearest.liftDepth / 2.0 - 0.35)
            );
        } catch (Throwable ignored) {
            return null;
        }
    }
}
