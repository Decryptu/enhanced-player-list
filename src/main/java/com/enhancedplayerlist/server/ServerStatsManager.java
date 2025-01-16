// src/main/java/com/enhancedplayerlist/server/ServerStatsManager.java
package com.enhancedplayerlist.server;

import com.enhancedplayerlist.Config;
import com.enhancedplayerlist.data.PlayerStatsData;
import com.enhancedplayerlist.network.NetworkHandler;
import com.enhancedplayerlist.network.PlayerStatsPacket;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.FileReader;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerStatsManager {
    private static final Gson GSON = new Gson();
    private static final Map<UUID, PlayerStatsData> playerStats = new ConcurrentHashMap<>();
    private static final Duration CLEANUP_THRESHOLD = Duration.ofDays(30); // Configurable if needed
    private static MinecraftServer server;

    public static void init(MinecraftServer server) {
        ServerStatsManager.server = server;
        loadAllPlayerStats();
        cleanupOldPlayers();
    }

    private static void cleanupOldPlayers() {
        if (!Config.showOfflinePlayers) {
            long now = System.currentTimeMillis();
            playerStats.values().removeIf(stats -> !stats.isOnline() &&
                    (now - stats.getLastSeen() > CLEANUP_THRESHOLD.toMillis()));
        }
    }

    public static void loadAllPlayerStats() {
        if (server == null)
            return;

        File statsDir = server.getWorldPath(LevelResource.PLAYER_STATS_DIR).toFile();
        if (!statsDir.exists() || !statsDir.isDirectory())
            return;

        File[] statFiles = statsDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (statFiles == null)
            return;

        boolean dataUpdated = false;
        Set<UUID> processedUuids = new HashSet<>();

        for (File statFile : statFiles) {
            try {
                String uuidStr = statFile.getName().replace(".json", "");
                UUID uuid = UUID.fromString(uuidStr);
                processedUuids.add(uuid);

                JsonObject jsonStats = GSON.fromJson(new FileReader(statFile), JsonObject.class);
                PlayerStatsData oldData = playerStats.get(uuid);
                PlayerStatsData newData = new PlayerStatsData();
                newData.setUuid(uuidStr);
                newData.loadFromJson(jsonStats);

                // Get the file's last modified time
                long lastModified = statFile.lastModified();
                newData.setLastSeen(lastModified);

                // Preserve online status and player name if exists
                if (oldData != null) {
                    newData.setOnline(oldData.isOnline());
                    if (!oldData.getPlayerName().isEmpty()) {
                        newData.setPlayerName(oldData.getPlayerName());
                    }
                    // Only update lastSeen if player is offline
                    if (!oldData.isOnline()) {
                        newData.setLastSeen(lastModified);
                    }
                }

                // Update player name for online players
                ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                if (player != null) {
                    newData.setOnline(true);
                    newData.setPlayerName(player.getGameProfile().getName());
                    // For online players, lastSeen should be current time
                    newData.setLastSeen(System.currentTimeMillis());
                } else if (newData.getPlayerName().isEmpty()) {
                    // Only use profile cache for offline players with no name
                    Optional.ofNullable(server.getProfileCache())
                            .flatMap(cache -> cache.get(uuid))
                            .ifPresent(profile -> newData.setPlayerName(profile.getName()));
                }

                playerStats.put(uuid, newData);
                dataUpdated = true;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Remove stats for deleted player files if we're not showing offline players
        if (!Config.showOfflinePlayers) {
            playerStats.keySet().removeIf(uuid -> !processedUuids.contains(uuid));
        }

        if (dataUpdated) {
            syncToClients();
        }
    }

    public static void syncToClients() {
        if (server == null)
            return;

        Map<UUID, PlayerStatsData> visibleStats = playerStats.entrySet().stream()
                .filter(e -> e.getValue().isOnline() || Config.showOfflinePlayers)
                .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);

        if (!visibleStats.isEmpty()) {
            PlayerStatsPacket packet = new PlayerStatsPacket(visibleStats);
            NetworkHandler.sendToAllPlayers(packet);
        }
    }

    public static void forceSync() {
        if (server != null) {
            loadAllPlayerStats();
            syncToClients();
        }
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

    public static void onServerStopping() {
        server = null;
        playerStats.clear();
    }
}