// src/main/java/com/enhancedplayerlist/client/event/ClientEventHandler.java
package com.enhancedplayerlist.client.event;

import com.enhancedplayerlist.Config;
import com.enhancedplayerlist.client.PlayerListRenderer;
import com.enhancedplayerlist.client.ClientStatsManager;
import com.enhancedplayerlist.data.PlayerStatsData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.*;

public class ClientEventHandler {
    private static final Minecraft minecraft = Minecraft.getInstance();
    private static final int ROW_HEIGHT = 9;
    private static final int STAT_SPACING = 8;
    private static final int NAME_COLUMN_WIDTH = 100; // Fixed width for names
    private static final int PADDING = 5;
    private static final int BACKGROUND_COLOR = 0x80000000;

    public static void init() {
        NeoForge.EVENT_BUS.register(ClientEventHandler.class);
    }
    
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (minecraft.player == null || !minecraft.isWindowActive() || !minecraft.options.keyPlayerList.isDown()) {
            return;
        }

        var player = minecraft.player;
        if (player == null || player.connection == null) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        
        // Get all players (online and offline)
        Collection<PlayerInfo> onlinePlayers = player.connection.getListedOnlinePlayers();
        List<PlayerStatsData> allPlayers = new ArrayList<>();
        
        // Add online players
        for (PlayerInfo info : onlinePlayers) {
            UUID playerId = info.getProfile().getId();
            PlayerStatsData data = ClientStatsManager.getPlayerStats().get(playerId);
            if (data != null) allPlayers.add(data);
        }
        
        // Add offline players if enabled
        if (Config.showOfflinePlayers) {
            ClientStatsManager.getPlayerStats().values().stream()
                .filter(data -> !data.isOnline())
                .forEach(allPlayers::add);
        }

        if (allPlayers.isEmpty()) return;

        // Calculate dimensions
        List<String> statColumns = new ArrayList<>(Config.visibleStats);
        Map<String, Integer> columnWidths = calculateColumnWidths(allPlayers, statColumns);
        
        int totalStatsWidth = calculateTotalWidth(columnWidths);
        int totalWidth = NAME_COLUMN_WIDTH + totalStatsWidth + (PADDING * 3);
        
        // Center everything
        int startX = (screenWidth - totalWidth) / 2;
        int backgroundY = screenHeight / 4;
        
        // Calculate proper background height including all players
        int playerCount = allPlayers.size();
        int backgroundHeight = (playerCount + 1) * ROW_HEIGHT + (PADDING * 2);
        
        // Draw main background
        graphics.fill(startX, backgroundY, startX + totalWidth, backgroundY + backgroundHeight, BACKGROUND_COLOR);

        // Draw headers
        int headerY = backgroundY + PADDING;
        
        // Draw "Players" header
        graphics.drawString(
            minecraft.font,
            "Players",
            startX + PADDING,
            headerY,
            0xFFFFFF
        );

        // Draw stat headers
        int statHeaderX = startX + NAME_COLUMN_WIDTH + (PADDING * 2);
        for (String stat : statColumns) {
            graphics.drawString(
                minecraft.font,
                formatStatHeader(stat),
                statHeaderX,
                headerY,
                0xFFFFFF
            );
            statHeaderX += columnWidths.get(stat) + STAT_SPACING;
        }

        // Draw player rows
        int rowY = headerY + ROW_HEIGHT + PADDING;
        for (PlayerStatsData playerData : allPlayers) {
            // Draw player name
            int nameColor = playerData.isOnline() ? 0xFFFFFF : (Config.grayOutOffline ? 0x808080 : 0xFFFFFF);
            graphics.drawString(
                minecraft.font,
                playerData.getPlayerName(),
                startX + PADDING,
                rowY,
                nameColor
            );

            // Draw stats
            int statX = startX + NAME_COLUMN_WIDTH + (PADDING * 2);
            Map<String, Component> stats = PlayerListRenderer.getPlayerStatsMap(UUID.fromString(playerData.getUuid()));
            
            for (String stat : statColumns) {
                Component value = stats.get(stat);
                if (value != null) {
                    graphics.drawString(
                        minecraft.font,
                        value,
                        statX,
                        rowY,
                        nameColor
                    );
                    statX += columnWidths.get(stat) + STAT_SPACING;
                }
            }
            rowY += ROW_HEIGHT;
        }
    }

    private static Map<String, Integer> calculateColumnWidths(Collection<PlayerStatsData> players, List<String> statColumns) {
        Map<String, Integer> columnWidths = new HashMap<>();
        
        // Initialize with header widths
        for (String stat : statColumns) {
            Component header = Component.literal(formatStatHeader(stat));
            columnWidths.put(stat, minecraft.font.width(header));
        }

        // Calculate widths from player stats
        for (PlayerStatsData playerData : players) {
            Map<String, Component> stats = PlayerListRenderer.getPlayerStatsMap(UUID.fromString(playerData.getUuid()));
            
            for (String stat : statColumns) {
                Component value = stats.get(stat);
                if (value != null) {
                    columnWidths.put(stat, 
                        Math.max(columnWidths.get(stat), minecraft.font.width(value))
                    );
                }
            }
        }

        return columnWidths;
    }

    private static int calculateTotalWidth(Map<String, Integer> columnWidths) {
        return columnWidths.values().stream().mapToInt(Integer::intValue).sum() 
               + (columnWidths.size() * STAT_SPACING);
    }

    private static String formatStatHeader(String stat) {
        return Config.compactMode ? stat.substring(0, Math.min(3, stat.length())).toUpperCase() 
                                : stat.substring(0, 1).toUpperCase() + stat.substring(1);
    }
}