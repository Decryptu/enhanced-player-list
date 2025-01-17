// src/main/java/com/enhancedplayerlist/server/ServerScheduler.java
package com.enhancedplayerlist.server;

import com.enhancedplayerlist.Config;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.util.thread.EffectiveSide;
import java.util.List;

public class ServerScheduler {
    private static int tickCounter = 0;
    private static long lastStatsSave = 0;
    private static final long STATS_SAVE_COOLDOWN = 1000; // 1 second in ms

    public static void onServerTick(MinecraftServer server) {
        if (EffectiveSide.get() != LogicalSide.SERVER) return;

        tickCounter++;
        if (tickCounter >= Config.updateFrequency) {
            tickCounter = 0;
            
            List<ServerPlayer> players = server.getPlayerList().getPlayers();
            if (!players.isEmpty()) {
                long currentTime = System.currentTimeMillis();
                // Only save stats if enough time has passed
                if (currentTime - lastStatsSave >= STATS_SAVE_COOLDOWN) {
                    server.execute(() -> {
                        // Batch process stats saves
                        for (ServerPlayer player : players) {
                            player.getStats().save();
                        }
                        lastStatsSave = currentTime;
                        
                        // Combine load and sync into one operation
                        ServerStatsManager.forceSync();
                    });
                }
            }
        }
    }
}