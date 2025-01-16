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
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

@OnlyIn(Dist.CLIENT)
public class PlayerListRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Object2ObjectMap<UUID, Map<String, Component>> STATS_CACHE = new Object2ObjectOpenHashMap<>();
    private static final Map<Long, String> TIME_FORMAT_CACHE = new HashMap<>();
    private static long lastCacheClean = System.currentTimeMillis();
    private static final long CACHE_CLEANUP_INTERVAL = 30000; // 30 seconds

    public static Map<String, Component> getPlayerStatsMap(UUID playerId) {
        if (playerId == null) {
            LOGGER.debug("PlayerListRenderer: playerId is null");
            return Collections.emptyMap();
        }

        // Clean cache periodically
        long now = System.currentTimeMillis();
        if (now - lastCacheClean > CACHE_CLEANUP_INTERVAL) {
            STATS_CACHE.clear();
            TIME_FORMAT_CACHE.clear();
            lastCacheClean = now;
        }

        // Return cached value if exists
        Map<String, Component> cached = STATS_CACHE.get(playerId);
        if (cached != null) return cached;

        PlayerStatsData stats = ClientStatsManager.getPlayerStats().get(playerId);
        if (stats == null) {
            LOGGER.debug("PlayerListRenderer: No stats found for player {}", playerId);
            return Collections.emptyMap();
        }

        Map<String, Component> statMap = new HashMap<>();
        for (String stat : Config.visibleStats) {
            try {
                Component value = switch (stat.toLowerCase()) {
                    case "playtime" -> formatValue(formatTime(stats.getPlayTime()));
                    case "deaths" -> formatValue(String.valueOf(stats.getDeaths()));
                    case "distance" -> {
                        double km = (stats.getBlocksWalked() / 100.0) / 1000.0;
                        yield formatValue(Config.compactMode ? 
                            String.format("%.1f", km) : 
                            String.format("%.1f km", km));
                    }
                    case "jumps" -> formatValue(String.valueOf(stats.getJumps()));
                    case "dmgdealt" -> formatValue(Config.compactMode ? 
                        String.format("%.0f", stats.getDamageDealt()) :
                        String.format("%.0f ♥", stats.getDamageDealt() / 2));
                    case "dmgtaken" -> formatValue(Config.compactMode ? 
                        String.format("%.0f", stats.getDamageTaken()) :
                        String.format("%.0f ♥", stats.getDamageTaken() / 2));
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
                LOGGER.error("Error processing stat {}: {}", stat, e.getMessage());
            }
        }

        // Cache the result
        STATS_CACHE.put(playerId, statMap);
        return statMap;
    }

    private static String formatTime(long ticks) {
        if (ticks <= 0) return "0h";

        Long cacheKey = ticks;
        String cached = TIME_FORMAT_CACHE.get(cacheKey);
        if (cached != null) return cached;

        long totalSeconds = ticks / 20;
        String result;

        if (Config.compactMode) {
            result = TimeUnit.SECONDS.toHours(totalSeconds) + "h";
        } else {
            long days = TimeUnit.SECONDS.toDays(totalSeconds);
            long hours = TimeUnit.SECONDS.toHours(totalSeconds) % 24;
            long minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60;

            StringBuilder time = new StringBuilder();
            if (days > 0) time.append(days).append("d ");
            if (hours > 0 || days > 0) time.append(hours).append("h ");
            if (minutes > 0) time.append(minutes).append("m");

            result = time.toString().trim();
        }

        TIME_FORMAT_CACHE.put(cacheKey, result);
        return result;
    }

    private static String formatLastSeen(long timestamp) {
        if (timestamp <= 0) return "Never";

        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < 0) return "Unknown";

        String result;
        if (Config.compactMode) {
            if (diff < TimeUnit.MINUTES.toMillis(1)) result = "now";
            else if (diff < TimeUnit.HOURS.toMillis(1)) 
                result = (diff / TimeUnit.MINUTES.toMillis(1)) + "m";
            else if (diff < TimeUnit.DAYS.toMillis(1)) 
                result = (diff / TimeUnit.HOURS.toMillis(1)) + "h";
            else result = (diff / TimeUnit.DAYS.toMillis(1)) + "d";
        } else {
            if (diff < TimeUnit.MINUTES.toMillis(1)) result = "Just now";
            else if (diff < TimeUnit.HOURS.toMillis(1)) 
                result = (diff / TimeUnit.MINUTES.toMillis(1)) + " mins ago";
            else if (diff < TimeUnit.DAYS.toMillis(1)) 
                result = (diff / TimeUnit.HOURS.toMillis(1)) + " hours ago";
            else result = (diff / TimeUnit.DAYS.toMillis(1)) + " days ago";
        }

        return result;
    }

    private static Component formatValue(String value) {
        return value == null || value.isEmpty() ? 
            Component.empty() : 
            Component.literal(value);
    }
}