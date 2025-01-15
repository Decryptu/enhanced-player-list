// src/main/java/com/enhancedplayerlist/client/event/ClientEventHandler.java
package com.enhancedplayerlist.client.event;

import com.enhancedplayerlist.Config;
import com.enhancedplayerlist.client.PlayerListRenderer;
import com.enhancedplayerlist.client.ClientStatsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Objective;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEventHandler {
    
    private static final Minecraft minecraft = Minecraft.getInstance();
    private static int lastListWidth = 0;

    @SubscribeEvent
    public static void onRenderPlayerList(CustomizeGuiOverlayEvent.PlayerList event) {
        if (!minecraft.isWindowActive() || minecraft.options.keyPlayerList.isDown()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = event.getWindow().getGuiScaledWidth();
        int screenHeight = event.getWindow().getGuiScaledHeight();

        // Get scoreboard objective if any
        Scoreboard scoreboard = minecraft.level.getScoreboard();
        Objective objective = scoreboard != null ? scoreboard.getDisplayObjective(0) : null;

        // Get player list width including stats
        PlayerTabOverlay playerList = minecraft.gui.getTabList();
        List<UUID> players = event.getPlayerInfo().stream()
                .map(info -> info.getProfile().getId())
                .toList();

        // Calculate extra width needed for stats
        int extraWidth = 0;
        for (UUID playerId : players) {
            List<Component> stats = PlayerListRenderer.getPlayerStats(playerId);
            if (!stats.isEmpty()) {
                for (Component stat : stats) {
                    int statWidth = minecraft.font.width(stat);
                    extraWidth = Math.max(extraWidth, statWidth);
                }
            }
        }

        // Add padding
        extraWidth += 20;
        
        // Store the final list width for other renderers
        lastListWidth = event.getMaxPlayerNameWidth() + extraWidth;
        
        // Modify player list entries to include stats
        int y = 0;
        for (UUID playerId : players) {
            List<Component> stats = PlayerListRenderer.getPlayerStats(playerId);
            if (!stats.isEmpty()) {
                int baseX = screenWidth - extraWidth - 5;
                int statY = y * 9 + event.getHeaderHeight();

                // Render each stat line
                for (Component stat : stats) {
                    graphics.drawString(
                        minecraft.font,
                        stat,
                        baseX,
                        statY,
                        0xFFFFFF
                    );
                    statY += 9;
                }
            }
            y++;
        }

        // Handle offline players if enabled
        if (Config.showOfflinePlayers) {
            ClientStatsManager.getPlayerStats().forEach((uuid, statsData) -> {
                if (!statsData.isOnline()) {
                    List<Component> stats = PlayerListRenderer.getPlayerStats(uuid);
                    if (!stats.isEmpty()) {
                        int baseX = screenWidth - extraWidth - 5;
                        int statY = y * 9 + event.getHeaderHeight();

                        // Render offline player name
                        graphics.drawString(
                            minecraft.font,
                            Component.literal(statsData.getPlayerName()),
                            5,
                            statY,
                            Config.grayOutOffline ? 0x808080 : 0xFFFFFF
                        );

                        // Render offline player stats
                        for (Component stat : stats) {
                            graphics.drawString(
                                minecraft.font,
                                stat,
                                baseX,
                                statY,
                                Config.grayOutOffline ? 0x808080 : 0xFFFFFF
                            );
                            statY += 9;
                        }
                        y++;
                    }
                }
            });
        }

        // Update list height
        event.setFooterHeight(event.getFooterHeight() + y * 9);
    }

    public static int getLastListWidth() {
        return lastListWidth;
    }
}