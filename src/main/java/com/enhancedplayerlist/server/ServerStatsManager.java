package com.enhancedplayerlist.server;

import com.enhancedplayerlist.Config;
import com.enhancedplayerlist.data.PlayerStatsData;
import com.enhancedplayerlist.network.PlayerStatsPacket;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.FileReader;
import java.util.*;

public class ServerStatsManager {
    private static final Gson GSON = new Gson();
    private static final Map<UUID, PlayerStatsData> playerStats = new HashMap<>();
    private static MinecraftServer server;

    public static void init(MinecraftServer server) {
        ServerStatsManager.server = server;
        loadAllPlayerStats();
    }

    public static void loadAllPlayerStats() {
        if (server == null) return;
        
        File statsDir = new File(server.getWorldPath(LevelResource.PLAYER_STATS_DIR).toFile(), "stats");
        if (!statsDir.exists()) return;

        File[] statFiles = statsDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (statFiles == null) return;

        for (File statFile : statFiles) {
            try {
                String uuid = statFile.getName().replace(".json", "");
                JsonObject jsonStats = GSON.fromJson(new FileReader(statFile), JsonObject.class);
                
                PlayerStatsData statsData = new PlayerStatsData();
                statsData.setUuid(uuid);
                statsData.loadFromJson(jsonStats);
                
                // Set online status
                ServerPlayer player = server.getPlayerList().getPlayer(UUID.fromString(uuid));
                statsData.setOnline(player != null);
                
                // Get player name from server's user cache
                Optional.ofNullable(server.getProfileCache())
                    .flatMap(cache -> cache.get(UUID.fromString(uuid)))
                    .ifPresent(profile -> statsData.setPlayerName(profile.getName()));
                
                playerStats.put(UUID.fromString(uuid), statsData);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void syncToClients() {
        if (server == null) return;
        
        // Only send stats that should be visible according to config
        Map<UUID, PlayerStatsData> visibleStats = new HashMap<>();
        playerStats.forEach((uuid, stats) -> {
            if (stats.isOnline() || Config.showOfflinePlayers) {
                visibleStats.put(uuid, stats);
            }
        });
        
        PlayerStatsPacket packet = new PlayerStatsPacket(visibleStats);
        PacketDistributor.ALL.noArg().send(packet);
    }

    public static void onPlayerJoin(Player player) {
        UUID uuid = player.getUUID();
        PlayerStatsData data = playerStats.computeIfAbsent(uuid, k -> new PlayerStatsData());
        data.setOnline(true);
        data.setPlayerName(player.getGameProfile().getName());
        syncToClients();
    }

    public static void onPlayerLeave(Player player) {
        UUID uuid = player.getUUID();
        PlayerStatsData data = playerStats.get(uuid);
        if (data != null) {
            data.setOnline(false);
            syncToClients();
        }
    }
}