// src/main/java/com/enhancedplayerlist/network/NetworkHandler.java
package com.enhancedplayerlist.network;

import com.enhancedplayerlist.EnhancedPlayerList;
import com.enhancedplayerlist.client.ClientStatsManager;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;

public class NetworkHandler {
    public static final ResourceLocation PLAYER_STATS_ID = new ResourceLocation(EnhancedPlayerList.MODID, "player_stats");

    @SubscribeEvent
    public static void register(RegisterPayloadHandlerEvent event) {
        IPayloadRegistrar registrar = event.registrar(EnhancedPlayerList.MODID)
            .versioned("1.0");

        registrar.play(
            PlayerStatsPacket.TYPE,
            PlayerStatsPacket::new,
            handler -> handler.client((packet, context) -> {
                context.workHandler().execute(() -> {
                    ClientStatsManager.updatePlayerStats(packet.playerStats());
                });
            })
        );
    }

    public static void sendToServer(PlayerStatsPacket packet) {
        PacketDistributor.SERVER.noArg().send(packet);
    }

    public static void sendToAllPlayers(PlayerStatsPacket packet) {
        PacketDistributor.ALL.noArg().send(packet);
    }
}