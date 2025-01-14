package com.enhancedplayerlist.client;

import com.enhancedplayerlist.Config;
import com.enhancedplayerlist.data.PlayerStatsData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@OnlyIn(Dist.CLIENT)
public class PlayerListRenderer {
    public static List<Component> getPlayerStats(UUID playerId) {
        PlayerStatsData stats = ClientStatsManager.getPlayerStats().get(playerId);
        if (stats == null) return new ArrayList<>();

        List<Component> statLines = new ArrayList<>();
        boolean compact = Config.compactMode;

        for (String stat : Config.visibleStats) {
            MutableComponent statComponent = switch (stat) {
                case "playtime" -> formatStat(compact ? "PT" : "Playtime",
                        formatTime(stats.getPlayTime()));
                case "deaths" -> formatStat(compact ? "D" : "Deaths",
                        String.valueOf(stats.getDeaths()));
                case "lastDeath" -> formatStat(compact ? "LD" : "Last Death",
                        formatTime(stats.getTimeSinceDeath()));
                case "mobKills" -> formatStat(compact ? "MK" : "Mob Kills",
                        String.valueOf(stats.getMobKills()));
                case "blocksWalked" -> formatStat(compact ? "BW" : "Blocks Walked",
                        String.format("%,d", stats.getBlocksWalked()));
                case "blocksMined" -> formatStat(compact ? "BM" : "Blocks Mined",
                        String.format("%,d", stats.getBlocksMined()));
                case "jumps" -> formatStat(compact ? "J" : "Jumps",
                        String.valueOf(stats.getJumps()));
                case "damageDealt" -> formatStat(compact ? "DD" : "Damage Dealt",
                        String.format("%.1f", stats.getDamageDealt()));
                case "damageTaken" -> formatStat(compact ? "DT" : "Damage Taken",
                        String.format("%.1f", stats.getDamageTaken()));
                default -> null;
            };

            if (statComponent != null) {
                if (!stats.isOnline() && Config.grayOutOffline) {
                    statComponent.withStyle(ChatFormatting.GRAY);
                }
                statLines.add(statComponent);
            }
        }

        return statLines;
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

    private static MutableComponent formatStat(String name, String value) {
        return Component.literal(String.format("%s: %s", name, value));
    }
}