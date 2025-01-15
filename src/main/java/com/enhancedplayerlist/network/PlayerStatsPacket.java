// src/main/java/com/enhancedplayerlist/network/PlayerStatsPacket.java
package com.enhancedplayerlist.network;

import com.enhancedplayerlist.data.PlayerStatsData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record PlayerStatsPacket(Map<UUID, PlayerStatsData> playerStats) implements CustomPacketPayload {
    
    public static final ResourceLocation ID = NetworkHandler.PLAYER_STATS_ID;
    public static final CustomPacketPayload.Type<PlayerStatsPacket> TYPE = new CustomPacketPayload.Type<>(ID);
    
    public static final StreamCodec<FriendlyByteBuf, PlayerStatsPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.map(
            HashMap::new,
            // Use string encoding for UUID
            ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString),
            PlayerStatsData.STREAM_CODEC
        ),
        PlayerStatsPacket::playerStats,
        PlayerStatsPacket::new
    );

    @Override
    public CustomPacketPayload.Type<?> type() {
        return TYPE;
    }
}