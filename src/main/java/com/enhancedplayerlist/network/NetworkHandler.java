package com.enhancedplayerlist.network;

import com.enhancedplayerlist.EnhancedPlayerList;
import com.enhancedplayerlist.client.ClientStatsManager;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;

public class NetworkHandler {
    public static final ResourceLocation PLAYER_STATS_ID = new ResourceLocation(EnhancedPlayerList.MODID, "player_stats");

    public static void register(RegisterPayloadHandlerEvent event) {
        IPayloadRegistrar registrar = event.registrar(EnhancedPlayerList.MODID);
        
        registrar.play(PLAYER_STATS_ID, PlayerStatsPacket::new,
            handler -> handler.client((packet, context) -> {
                context.workHandler().execute(() -> {
                    ClientStatsManager.updatePlayerStats(packet.getPlayerStats());
                });
            })
        );
    }
}