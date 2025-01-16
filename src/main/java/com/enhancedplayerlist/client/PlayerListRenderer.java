// src/main/java/com/enhancedplayerlist/client/PlayerListRenderer.java
package com.enhancedplayerlist.client;

import com.enhancedplayerlist.Config;
import com.enhancedplayerlist.data.PlayerStatsData;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

@OnlyIn(Dist.CLIENT)
public class PlayerListRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static Map<String, Component> getPlayerStatsMap(UUID playerId) {
        Map<String, Component> statMap = new HashMap<>();
        if (playerId == null) {
            LOGGER.debug("PlayerListRenderer: playerId is null");
            return statMap;
        }

        PlayerStatsData stats = ClientStatsManager.getPlayerStats().get(playerId);
        if (stats == null) {
            LOGGER.debug("PlayerListRenderer: No stats found for player {}", playerId);
            return statMap;
        }

        LOGGER.debug("PlayerListRenderer: Processing stats for player {} with visible stats: {}", 
                    stats.getPlayerName(), Config.visibleStats);
        LOGGER.debug("PlayerListRenderer: Raw stats - Playtime: {}, Deaths: {}, Jumps: {}, Damage Dealt: {}, Damage Taken: {}, Blocks Walked: {}", 
                    stats.getPlayTime(), stats.getDeaths(), stats.getJumps(), 
                    stats.getDamageDealt(), stats.getDamageTaken(), stats.getBlocksWalked());

        for (String stat : Config.visibleStats) {
            LOGGER.debug("PlayerListRenderer: Processing stat: {}", stat);
            try {
                Component value = switch (stat.toLowerCase()) {
                    case "playtime" -> {
                        long playTime = stats.getPlayTime();
                        LOGGER.debug("Processing playtime: {}", playTime);
                        yield formatValue(formatTime(playTime));
                    }
                    case "deaths" -> {
                        int deaths = stats.getDeaths();
                        LOGGER.debug("Processing deaths: {}", deaths);
                        yield formatValue(String.valueOf(deaths));
                    }
                    case "distance" -> {
                        long blocks = stats.getBlocksWalked();
                        LOGGER.debug("Processing distance: {}", blocks);
                        double km = blocks / 1000.0;
                        yield formatValue(Config.compactMode ? 
                            String.format("%.1f", km) : 
                            String.format("%.1f km", km));
                    }
                    case "jumps" -> {
                        int jumps = stats.getJumps();
                        LOGGER.debug("Processing jumps: {}", jumps);
                        yield formatValue(String.valueOf(jumps));
                    }
                    case "dmgdealt" -> {
                        float damage = stats.getDamageDealt();
                        LOGGER.debug("Processing damage dealt: {}", damage);
                        yield formatValue(Config.compactMode ? 
                            String.format("%.0f", damage) : 
                            String.format("%.0f ♥", damage / 2));
                    }
                    case "dmgtaken" -> {
                        float damage = stats.getDamageTaken();
                        LOGGER.debug("Processing damage taken: {}", damage);
                        yield formatValue(Config.compactMode ? 
                            String.format("%.0f", damage) : 
                            String.format("%.0f ♥", damage / 2));
                    }
                    case "lastseen" -> {
                        boolean online = stats.isOnline();
                        LOGGER.debug("Processing last seen (online: {})", online);
                        yield online ? 
                            Component.literal("Online").withStyle(ChatFormatting.GREEN) : 
                            formatValue(formatLastSeen(stats.getLastSeen()));
                    }
                    default -> {
                        LOGGER.debug("Unknown stat type: {}", stat);
                        yield Component.empty();
                    }
                };

                if (value != Component.empty()) {
                    LOGGER.debug("Adding stat {} with value {}", stat, value.getString());
                    if (!stats.isOnline() && Config.grayOutOffline) {
                        value = value.copy().withStyle(ChatFormatting.GRAY);
                    }
                    statMap.put(stat, value);
                }
            } catch (Exception e) {
                LOGGER.error("Error processing stat {}: {}", stat, e.getMessage());
            }
        }

        LOGGER.debug("Final stat map contains keys: {}", statMap.keySet());
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