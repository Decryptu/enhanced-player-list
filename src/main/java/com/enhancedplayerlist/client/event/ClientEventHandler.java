// src/main/java/com/enhancedplayerlist/client/event/ClientEventHandler.java

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
import java.util.concurrent.atomic.AtomicReference;

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

        var player = minecraft.player; // Store player reference to avoid multiple null checks
        if (player == null || player.connection == null) return;  // Double-check both player and connection

        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        
        var players = player.connection.getListedOnlinePlayers();
        
        // Calculate extra width for stats using AtomicReference
        final AtomicReference<Integer> extraWidth = new AtomicReference<>(0);
        players.forEach(playerInfo -> {
            UUID playerId = playerInfo.getProfile().getId();
            List<Component> stats = PlayerListRenderer.getPlayerStats(playerId);
            if (!stats.isEmpty()) {
                for (Component stat : stats) {
                    int statWidth = minecraft.font.width(stat);
                    extraWidth.set(Math.max(extraWidth.get(), statWidth));
                }
            }
        });

        // Add padding
        final int totalExtraWidth = extraWidth.get() + 20;
        lastListWidth = minecraft.font.width("Player") + totalExtraWidth;

        // Use AtomicInteger for y coordinate
        AtomicInteger yPos = new AtomicInteger(10);

        // Render player stats
        players.forEach(playerInfo -> {
            UUID playerId = playerInfo.getProfile().getId();
            List<Component> stats = PlayerListRenderer.getPlayerStats(playerId);
            if (!stats.isEmpty()) {
                int baseX = screenWidth - totalExtraWidth - 5;
                AtomicInteger statY = new AtomicInteger(yPos.get());

                stats.forEach(stat -> {
                    graphics.drawString(
                        minecraft.font,
                        stat,
                        baseX,
                        statY.get(),
                        0xFFFFFF
                    );
                    statY.addAndGet(9);
                });
            }
            yPos.addAndGet(9);
        });

        // Handle offline players
        if (Config.showOfflinePlayers) {
            ClientStatsManager.getPlayerStats().forEach((uuid, statsData) -> {
                if (!statsData.isOnline()) {
                    List<Component> stats = PlayerListRenderer.getPlayerStats(uuid);
                    if (!stats.isEmpty()) {
                        int baseX = screenWidth - totalExtraWidth - 5;
                        AtomicInteger statY = new AtomicInteger(yPos.get());

                        graphics.drawString(
                            minecraft.font,
                            Component.literal(statsData.getPlayerName()),
                            5,
                            statY.get(),
                            Config.grayOutOffline ? 0x808080 : 0xFFFFFF
                        );

                        stats.forEach(stat -> {
                            graphics.drawString(
                                minecraft.font,
                                stat,
                                baseX,
                                statY.get(),
                                Config.grayOutOffline ? 0x808080 : 0xFFFFFF
                            );
                            statY.addAndGet(9);
                        });
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