package com.enhancedplayerlist.client.event;

import com.enhancedplayerlist.Config;
import com.enhancedplayerlist.client.PlayerListRenderer;
import com.enhancedplayerlist.client.ClientStatsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientEventHandler {
    
    private static final Minecraft minecraft = Minecraft.getInstance();
    private static int lastListWidth = 0;

    public static void init() {
        NeoForge.EVENT_BUS.register(ClientEventHandler.class);
    }
    
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (minecraft.player == null || !minecraft.isWindowActive() || !minecraft.options.keyPlayerList.isDown()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        
        var onlinePlayers = minecraft.player.connection;
        if (onlinePlayers == null) return;
        
        var players = onlinePlayers.getListedOnlinePlayers();

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

        // Use AtomicInteger for y coordinate to handle lambda effectively final requirement
        AtomicInteger yPos = new AtomicInteger(10);

        // Render player stats
        for (var playerInfo : players) {
            UUID playerId = playerInfo.getProfile().getId();
            List<Component> stats = PlayerListRenderer.getPlayerStats(playerId);
            if (!stats.isEmpty()) {
                int baseX = screenWidth - extraWidth - 5;
                int statY = yPos.get();

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
            yPos.addAndGet(9);
        }

        // Handle offline players
        if (Config.showOfflinePlayers) {
            ClientStatsManager.getPlayerStats().forEach((uuid, statsData) -> {
                if (!statsData.isOnline()) {
                    List<Component> stats = PlayerListRenderer.getPlayerStats(uuid);
                    if (!stats.isEmpty()) {
                        int baseX = screenWidth - extraWidth - 5;
                        int statY = yPos.get();

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
                        yPos.addAndGet(9);
                    }
                }
            });
        }
    }

    public static int getLastListWidth() {
        return lastListWidth;
    }
}