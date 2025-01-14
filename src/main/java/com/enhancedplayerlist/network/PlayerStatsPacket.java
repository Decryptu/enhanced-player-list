package com.enhancedplayerlist.network;

import com.enhancedplayerlist.data.PlayerStatsData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStatsPacket implements CustomPacketPayload {
    private final Map<UUID, PlayerStatsData> playerStats;

    public PlayerStatsPacket(Map<UUID, PlayerStatsData> playerStats) {
        this.playerStats = playerStats;
    }

    public PlayerStatsPacket(FriendlyByteBuf buf) {
        this.playerStats = new HashMap<>();
        int size = buf.readVarInt();
        for (int i = 0; i < size; i++) {
            UUID uuid = buf.readUUID();
            PlayerStatsData data = new PlayerStatsData();
            data.decode(buf);
            playerStats.put(uuid, data);
        }
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
        return NetworkHandler.PLAYER_STATS_ID;
    }

    public Map<UUID, PlayerStatsData> getPlayerStats() {
        return playerStats;
    }
}