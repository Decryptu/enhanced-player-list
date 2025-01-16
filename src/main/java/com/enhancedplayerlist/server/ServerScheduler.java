// src/main/java/com/enhancedplayerlist/server/ServerScheduler.java
package com.enhancedplayerlist.server;

import com.enhancedplayerlist.Config;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.util.thread.EffectiveSide;

public class ServerScheduler {
    private static int tickCounter = 0;

    public static void onServerTick(MinecraftServer server) {
        if (EffectiveSide.get() != LogicalSide.SERVER) return;

        tickCounter++;
        if (tickCounter >= Config.updateFrequency) {
            tickCounter = 0;
            
            if (!server.getPlayerList().getPlayers().isEmpty()) {
                server.execute(() -> {
                    // Save stats for all online players
                    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                        player.getStats().save();
                    }
                    
                    // Load and sync stats in one operation
                    ServerStatsManager.loadAllPlayerStats();
                    ServerStatsManager.syncToClients();
                });
            }
        }
    }
}