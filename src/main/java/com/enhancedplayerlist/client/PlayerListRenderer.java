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
        Map<String, Component> statMap = new HashMap<>();
        if (playerId == null) return statMap;

        PlayerStatsData stats = ClientStatsManager.getPlayerStats().get(playerId);
        if (stats == null) return statMap;

        // Get the raw values first with null checks
        for (String stat : Config.visibleStats) {
            try {
                Component value = switch (stat.toLowerCase()) {
                    case "playtime" -> {
                        long playTime = stats.getPlayTime();
                        if (playTime < 0) playTime = 0;
                        yield formatValue(formatTime(playTime));
                    }
                    case "deaths" -> {
                        int deaths = stats.getDeaths();
                        if (deaths < 0) deaths = 0;
                        yield formatValue(String.valueOf(deaths));
                    }
                    case "distance" -> {
                        long blocks = stats.getBlocksWalked();
                        if (blocks < 0) blocks = 0;
                        double km = blocks / 1000.0;
                        yield formatValue(Config.compactMode ? 
                            String.format("%.1f", km) : 
                            String.format("%.1f km", km));
                    }
                    case "jumps" -> {
                        int jumps = stats.getJumps();
                        if (jumps < 0) jumps = 0;
                        yield formatValue(String.valueOf(jumps));
                    }
                    case "dmgdealt" -> {
                        float damage = stats.getDamageDealt();
                        if (damage < 0) damage = 0;
                        yield formatValue(Config.compactMode ? 
                            String.format("%.0f", damage) : 
                            String.format("%.0f ♥", damage / 2)); // Convert to hearts
                    }
                    case "dmgtaken" -> {
                        float damage = stats.getDamageTaken();
                        if (damage < 0) damage = 0;
                        yield formatValue(Config.compactMode ? 
                            String.format("%.0f", damage) : 
                            String.format("%.0f ♥", damage / 2)); // Convert to hearts
                    }
                    case "lastseen" -> stats.isOnline() ? 
                        Component.literal("Online").withStyle(ChatFormatting.GREEN) : 
                        formatValue(formatLastSeen(stats.getLastSeen()));
                    default -> Component.empty();
                };

                if (value != Component.empty()) {
                    if (!stats.isOnline() && Config.grayOutOffline) {
                        value = value.copy().withStyle(ChatFormatting.GRAY);
                    }
                    statMap.put(stat, value);
                }
            } catch (Exception e) {
                // If any stat fails, skip it gracefully
                continue;
            }
        }

        return statMap;
    }

    private static String formatTime(long ticks) {
        if (ticks <= 0) return "0h";
        
        long totalSeconds = ticks / 20; // Convert Minecraft ticks to seconds
        
        if (Config.compactMode) {
            long hours = TimeUnit.SECONDS.toHours(totalSeconds);
            return hours + "h";
        } else {
            long days = TimeUnit.SECONDS.toDays(totalSeconds);
            long hours = TimeUnit.SECONDS.toHours(totalSeconds) % 24;
            long minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60;
            
            StringBuilder time = new StringBuilder();
            if (days > 0) time.append(days).append("d ");
            if (hours > 0 || days > 0) time.append(hours).append("h ");
            if (minutes > 0) time.append(minutes).append("m");
            
            return time.toString().trim();
        }
    }

    private static String formatLastSeen(long timestamp) {
        if (timestamp <= 0) return "Never";
        
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        
        // Guard against future timestamps
        if (diff < 0) return "Unknown";
        
        if (Config.compactMode) {
            if (diff < TimeUnit.MINUTES.toMillis(1)) return "now";
            if (diff < TimeUnit.HOURS.toMillis(1)) return (diff / TimeUnit.MINUTES.toMillis(1)) + "m";
            if (diff < TimeUnit.DAYS.toMillis(1)) return (diff / TimeUnit.HOURS.toMillis(1)) + "h";
            return (diff / TimeUnit.DAYS.toMillis(1)) + "d";
        } else {
            if (diff < TimeUnit.MINUTES.toMillis(1)) return "Just now";
            if (diff < TimeUnit.HOURS.toMillis(1)) return (diff / TimeUnit.MINUTES.toMillis(1)) + " mins ago";
            if (diff < TimeUnit.DAYS.toMillis(1)) return (diff / TimeUnit.HOURS.toMillis(1)) + " hours ago";
            return (diff / TimeUnit.DAYS.toMillis(1)) + " days ago";
        }
    }

    private static Component formatValue(String value) {
        return value == null || value.isEmpty() ? 
            Component.empty() : 
            Component.literal(value);
    }
}