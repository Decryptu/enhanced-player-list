package com.enhancedplayerlist.client.event;

import com.enhancedplayerlist.client.PlayerListRenderer;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.CustomizePlayerTabListEvent;

import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientEventHandler {
    @SubscribeEvent
    public static void onRenderPlayerList(CustomizePlayerTabListEvent event) {
        UUID playerId = event.getPlayer().getGameProfile().getId();
        List<Component> stats = PlayerListRenderer.getPlayerStats(playerId);
        event.getEntries().addAll(stats);
    }
}