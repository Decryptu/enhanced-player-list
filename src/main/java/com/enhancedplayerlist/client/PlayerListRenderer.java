// src/main/java/com/enhancedplayerlist/client/PlayerListRenderer.java
package com.enhancedplayerlist.client;

import com.enhancedplayerlist.Config;
import com.enhancedplayerlist.data.PlayerStatsData;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.*;
import java.util.concurrent.TimeUnit;

@OnlyIn(Dist.CLIENT)
public class PlayerListRenderer {
    
    public static Map<String, Component> getPlayerStatsMap(UUID playerId) {
        PlayerStatsData stats = ClientStatsManager.getPlayerStats().get(playerId);
        if (stats == null) return new HashMap<>();

        Map<String, Component> statMap = new HashMap<>();

        for (String stat : Config.visibleStats) {
            Component value = switch (stat) {
                case "playtime" -> formatValue(formatTime(stats.getPlayTime()));
                case "deaths" -> formatValue(String.valueOf(stats.getDeaths()));
                case "lastDeath" -> formatValue(formatTime(stats.getTimeSinceDeath()));
                case "mobKills" -> formatValue(String.valueOf(stats.getMobKills()));
                case "blocksWalked" -> formatValue(String.format("%,d", stats.getBlocksWalked()));
                default -> null;
            };

            if (value != null) {
                if (!stats.isOnline() && Config.grayOutOffline) {
                    value = value.copy().withStyle(ChatFormatting.GRAY);
                }
                statMap.put(stat, value);
            }
        }

        return statMap;
    }

    private static String formatTime(long ticks) {
        long seconds = ticks / 20;
        if (Config.compactMode) {
            long hours = TimeUnit.SECONDS.toHours(seconds);
            return hours + "h";
        } else {
            long hours = TimeUnit.SECONDS.toHours(seconds);
            long minutes = TimeUnit.SECONDS.toMinutes(seconds) - TimeUnit.HOURS.toMinutes(hours);
            return String.format("%dh %dm", hours, minutes);
        }
    }

    private static Component formatValue(String value) {
        return Component.literal(value);
    }
}