package com.enhancedplayerlist.client.event;

import com.enhancedplayerlist.Config;
import com.enhancedplayerlist.client.PlayerListRenderer;
import com.enhancedplayerlist.client.ClientStatsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientEventHandler {
    private static final Minecraft minecraft = Minecraft.getInstance();
    private static int lastListWidth = 0;
    private static final int ROW_HEIGHT = 9;
    private static final int HEADER_HEIGHT = ROW_HEIGHT + 1;
    private static final int START_Y = HEADER_HEIGHT + 5;

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
        
        Collection<PlayerInfo> players = player.connection.getListedOnlinePlayers();
        if (players.isEmpty() && !Config.showOfflinePlayers) return;

        List<String> statColumns = new ArrayList<>(Config.visibleStats);
        Map<String, Integer> columnWidths = new HashMap<>();
        
        // Calculate column widths
        for (String stat : statColumns) {
            Component header = Component.literal(formatStatHeader(stat));
            columnWidths.put(stat, minecraft.font.width(header));
        }

        // Calculate widths from online players
        players.forEach(playerInfo -> {
            UUID playerId = playerInfo.getProfile().getId();
            Map<String, Component> stats = PlayerListRenderer.getPlayerStatsMap(playerId);
            
            for (String stat : statColumns) {
                Component value = stats.get(stat);
                if (value != null) {
                    columnWidths.put(stat, Math.max(columnWidths.get(stat), minecraft.font.width(value)));
                }
            }
        });

        // Draw column headers
        AtomicInteger xPos = new AtomicInteger(screenWidth - 5);
        for (int i = statColumns.size() - 1; i >= 0; i--) {
            String stat = statColumns.get(i);
            int width = columnWidths.get(stat);
            xPos.set(xPos.get() - width - 10);
            
            graphics.drawString(
                minecraft.font,
                formatStatHeader(stat),
                xPos.get(),
                5, // Move headers up
                0xFFFFFF
            );
        }

        AtomicInteger yPos = new AtomicInteger(START_Y);

        // Draw values for online players
        players.forEach(playerInfo -> {
            UUID playerId = playerInfo.getProfile().getId();
            Map<String, Component> stats = PlayerListRenderer.getPlayerStatsMap(playerId);
            
            // Draw player name
            graphics.drawString(
                minecraft.font,
                playerInfo.getProfile().getName(),
                5,
                yPos.get(),
                0xFFFFFF
            );
            
            // Draw stats
            xPos.set(screenWidth - 5);
            for (int i = statColumns.size() - 1; i >= 0; i--) {
                String stat = statColumns.get(i);
                Component value = stats.get(stat);
                int width = columnWidths.get(stat);
                xPos.set(xPos.get() - width - 10);
                
                if (value != null) {
                    graphics.drawString(
                        minecraft.font,
                        value,
                        xPos.get(),
                        yPos.get(),
                        0xFFFFFF
                    );
                }
            }
            yPos.addAndGet(ROW_HEIGHT);
        });

        // Handle offline players
        if (Config.showOfflinePlayers) {
            ClientStatsManager.getPlayerStats().forEach((uuid, statsData) -> {
                if (!statsData.isOnline()) {
                    Map<String, Component> stats = PlayerListRenderer.getPlayerStatsMap(uuid);
                    
                    // Draw offline player name
                    graphics.drawString(
                        minecraft.font,
                        Component.literal(statsData.getPlayerName()),
                        5,
                        yPos.get(),
                        Config.grayOutOffline ? 0x808080 : 0xFFFFFF
                    );
                    
                    xPos.set(screenWidth - 5);
                    for (int i = statColumns.size() - 1; i >= 0; i--) {
                        String stat = statColumns.get(i);
                        Component value = stats.get(stat);
                        int width = columnWidths.get(stat);
                        xPos.set(xPos.get() - width - 10);
                        
                        if (value != null) {
                            graphics.drawString(
                                minecraft.font,
                                value,
                                xPos.get(),
                                yPos.get(),
                                Config.grayOutOffline ? 0x808080 : 0xFFFFFF
                            );
                        }
                    }
                    yPos.addAndGet(ROW_HEIGHT);
                }
            });
        }
    }

    private static String formatStatHeader(String stat) {
        return Config.compactMode ? stat.substring(0, Math.min(3, stat.length())).toUpperCase() 
                                : stat.substring(0, 1).toUpperCase() + stat.substring(1);
    }

    public static int getLastListWidth() {
        return lastListWidth;
    }
}