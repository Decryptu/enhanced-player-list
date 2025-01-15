// src/main/java/com/enhancedplayerlist/network/PlayerStatsPacket.java
package com.enhancedplayerlist.network;

import com.enhancedplayerlist.data.PlayerStatsData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record PlayerStatsPacket(Map<UUID, PlayerStatsData> playerStats) implements CustomPacketPayload {
    public static final ResourceLocation ID = NetworkHandler.PLAYER_STATS_ID;
    public static final CustomPacketPayload.Type<PlayerStatsPacket> TYPE = new CustomPacketPayload.Type<>(ID);

    public PlayerStatsPacket(FriendlyByteBuf buf) {
        this(readStats(buf));
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(playerStats.size());
        playerStats.forEach((uuid, data) -> {
            buf.writeUUID(uuid);
            data.encode(buf);
        });
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public CustomPacketPayload.Type<?> type() {
        return TYPE;
    }

    private static Map<UUID, PlayerStatsData> readStats(FriendlyByteBuf buf) {
        Map<UUID, PlayerStatsData> stats = new HashMap<>();
        int size = buf.readVarInt();
        for (int i = 0; i < size; i++) {
            UUID uuid = buf.readUUID();
            PlayerStatsData data = new PlayerStatsData();
            data.decode(buf);
            stats.put(uuid, data);
        }
        return stats;
    }
}