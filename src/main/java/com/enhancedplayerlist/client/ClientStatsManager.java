// src/main/java/com/enhancedplayerlist/client/ClientStatsManager.java
package com.enhancedplayerlist.client;

import com.enhancedplayerlist.data.PlayerStatsData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientStatsManager {
    private static final Map<UUID, PlayerStatsData> playerStats = new HashMap<>();

    public static void updatePlayerStats(Map<UUID, PlayerStatsData> newStats) {
        playerStats.clear();
        playerStats.putAll(newStats);
    }

    public static Map<UUID, PlayerStatsData> getPlayerStats() {
        return playerStats;
    }
}