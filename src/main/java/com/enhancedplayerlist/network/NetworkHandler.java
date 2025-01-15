// src/main/java/com/enhancedplayerlist/network/NetworkHandler.java
package com.enhancedplayerlist.network;

import com.enhancedplayerlist.EnhancedPlayerList;
import com.enhancedplayerlist.client.ClientStatsManager;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public class NetworkHandler {
    public static final ResourceLocation PLAYER_STATS_ID = ResourceLocation.fromNamespaceAndPath(EnhancedPlayerList.MODID, "player_stats");

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        final var registrar = event.registrar(EnhancedPlayerList.MODID)
            .versioned("1.0");

        registrar.playToClient(
            PlayerStatsPacket.TYPE,
            PlayerStatsPacket.STREAM_CODEC,
            (packet, context) -> {
                context.enqueueWork(() -> {
                    ClientStatsManager.updatePlayerStats(packet.playerStats());
                });
            }
        );
    }

    public static void sendToServer(PlayerStatsPacket packet) {
        PacketDistributor.sendToServer(packet);
    }

    public static void sendToAllPlayers(PlayerStatsPacket packet) {
        PacketDistributor.sendToAllPlayers(packet);
    }
}