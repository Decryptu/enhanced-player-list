package com.enhancedplayerlist.server;

import com.enhancedplayerlist.Config;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.util.thread.EffectiveSide;

public class ServerScheduler {
    private static int tickCounter = 0;

    public static void onServerTick(MinecraftServer server) {
        if (EffectiveSide.get() != LogicalSide.SERVER) return;

        tickCounter++;
        if (tickCounter >= Config.updateFrequency) {
            tickCounter = 0;
            ServerStatsManager.loadAllPlayerStats();
            ServerStatsManager.syncToClients();
        }
    }
}