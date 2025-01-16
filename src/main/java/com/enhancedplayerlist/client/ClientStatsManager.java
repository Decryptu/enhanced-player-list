// src/main/java/com/enhancedplayerlist/client/ClientStatsManager.java
package com.enhancedplayerlist.client;

import com.enhancedplayerlist.data.PlayerStatsData;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientStatsManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, PlayerStatsData> playerStats = new HashMap<>();

    public static void updatePlayerStats(Map<UUID, PlayerStatsData> newStats) {
        LOGGER.debug("ClientStatsManager: Received {} player stats", newStats.size());
        newStats.forEach((uuid, stats) -> {
            LOGGER.debug("Player {}: playtime={}, deaths={}, jumps={}, dmgDealt={}, dmgTaken={}, blocksWalked={}",
                stats.getPlayerName(),
                stats.getPlayTime(),
                stats.getDeaths(),
                stats.getJumps(),
                stats.getDamageDealt(),
                stats.getDamageTaken(),
                stats.getBlocksWalked()
            );
        });
        
        playerStats.clear();
        playerStats.putAll(newStats);
    }

    public static Map<UUID, PlayerStatsData> getPlayerStats() {
        return playerStats;
    }
}