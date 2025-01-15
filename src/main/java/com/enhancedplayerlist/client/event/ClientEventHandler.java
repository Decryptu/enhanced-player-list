// src/main/java/com/enhancedplayerlist/client/event/ClientEventHandler.java
package com.enhancedplayerlist.client.event;

import com.enhancedplayerlist.Config;
import com.enhancedplayerlist.EnhancedPlayerList;
import com.enhancedplayerlist.client.PlayerListRenderer;
import com.enhancedplayerlist.client.ClientStatsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.DisplaySlot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.gui.overlay.IGuiOverlay;

import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = EnhancedPlayerList.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEventHandler {
    
    private static final Minecraft minecraft = Minecraft.getInstance();
    private static int lastListWidth = 0;
    
    private static final IGuiOverlay PLAYER_LIST_OVERLAY = ((gui, graphics, partialTick, screenWidth, screenHeight) -> {
        if (!minecraft.isWindowActive() || !minecraft.options.keyPlayerList.isDown()) {
            return;
        }

        // Get scoreboard objective if any
        Scoreboard scoreboard = minecraft.level.getScoreboard();
        Objective objective = scoreboard != null ? scoreboard.getDisplayObjective(DisplaySlot.LIST) : null;

        PlayerTabOverlay playerList = minecraft.gui.getTabList();
        
        // Get online players via player connection
        var players = minecraft.player.connection.getListedOnlinePlayers();

        // Calculate extra width for stats
        int extraWidth = 0;
        for (var playerInfo : players) {
            UUID playerId = playerInfo.getProfile().getId();
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
        lastListWidth = minecraft.font.width("Player") + extraWidth;

        // Render player stats
        int y = 10;
        for (var playerInfo : players) {
            UUID playerId = playerInfo.getProfile().getId();
            List<Component> stats = PlayerListRenderer.getPlayerStats(playerId);
            if (!stats.isEmpty()) {
                int baseX = screenWidth - extraWidth - 5;
                int statY = y;

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
            y += 9;
        }

        // Handle offline players
        if (Config.showOfflinePlayers) {
            ClientStatsManager.getPlayerStats().forEach((uuid, statsData) -> {
                if (!statsData.isOnline()) {
                    List<Component> stats = PlayerListRenderer.getPlayerStats(uuid);
                    if (!stats.isEmpty()) {
                        int baseX = screenWidth - extraWidth - 5;
                        int statY = y;

                        graphics.drawString(
                            minecraft.font,
                            Component.literal(statsData.getPlayerName()),
                            5,
                            statY,
                            Config.grayOutOffline ? 0x808080 : 0xFFFFFF
                        );

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
                        y += 9;
                    }
                }
            });
        }
    });

    public static void register() {
        net.neoforged.neoforge.client.gui.overlay.VanillaGuiOverlay.PLAYER_LIST.insert(PLAYER_LIST_OVERLAY);
    }

    public static int getLastListWidth() {
        return lastListWidth;
    }
}