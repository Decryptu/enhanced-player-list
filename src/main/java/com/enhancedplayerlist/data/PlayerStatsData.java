// src/main/java/com/enhancedplayerlist/data/PlayerStatsData.java
package com.enhancedplayerlist.data;

import com.google.gson.JsonObject;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import io.netty.buffer.ByteBuf;
import javax.annotation.Nonnull;
import java.util.Objects;

public class PlayerStatsData {
    private String playerName = "";
    private String uuid = "";
    private boolean isOnline;
    private long lastSeen;
    private long playTime;
    private int deaths;
    private long timeSinceDeath;
    private int mobKills;
    private long blocksWalked;
    private long blocksMined;
    private int jumps;
    private float damageDealt;
    private float damageTaken;

    // Codec is limited to 6 parameters at a time so we split them
    private static final StreamCodec<ByteBuf, PlayerStatsData> BASIC_DATA_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PlayerStatsData::getPlayerName,
            ByteBufCodecs.STRING_UTF8, PlayerStatsData::getUuid,
            ByteBufCodecs.BOOL, PlayerStatsData::isOnline,
            // Create basic data
            (name, uuid, online) -> {
                PlayerStatsData data = new PlayerStatsData();
                data.setPlayerName(name);
                data.setUuid(uuid);
                data.setOnline(online);
                return data;
            });

    private static final StreamCodec<ByteBuf, PlayerStatsData> STATS_DATA_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, PlayerStatsData::getPlayTime,
            ByteBufCodecs.VAR_INT, PlayerStatsData::getDeaths,
            ByteBufCodecs.VAR_LONG, PlayerStatsData::getTimeSinceDeath,
            // Create stats data
            (playTime, deaths, timeSinceDeath) -> {
                PlayerStatsData data = new PlayerStatsData();
                data.playTime = playTime;
                data.deaths = deaths;
                data.timeSinceDeath = timeSinceDeath;
                return data;
            });

    private static final StreamCodec<ByteBuf, PlayerStatsData> EXTENDED_STATS_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PlayerStatsData::getJumps,
            ByteBufCodecs.VAR_LONG, PlayerStatsData::getBlocksWalked,
            ByteBufCodecs.FLOAT, PlayerStatsData::getDamageDealt,
            // Create extended stats data
            (jumps, blocksWalked, damageDealt) -> {
                PlayerStatsData data = new PlayerStatsData();
                data.jumps = jumps;
                data.blocksWalked = blocksWalked;
                data.damageDealt = damageDealt;
                return data;
            });

    public static final StreamCodec<ByteBuf, PlayerStatsData> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(@Nonnull ByteBuf buf, @Nonnull PlayerStatsData value) {
            BASIC_DATA_CODEC.encode(buf, value);
            STATS_DATA_CODEC.encode(buf, value);
            EXTENDED_STATS_CODEC.encode(buf, value);
            ByteBufCodecs.VAR_LONG.encode(buf, value.getLastSeen());
            ByteBufCodecs.VAR_LONG.encode(buf, value.getBlocksMined());
            ByteBufCodecs.FLOAT.encode(buf, value.getDamageTaken());
        }

        @Override
        @Nonnull
        public PlayerStatsData decode(@Nonnull ByteBuf buf) {
            PlayerStatsData result = new PlayerStatsData();

            PlayerStatsData basic = BASIC_DATA_CODEC.decode(buf);
            result.setPlayerName(basic.getPlayerName());
            result.setUuid(basic.getUuid());
            result.setOnline(basic.isOnline());

            PlayerStatsData stats = STATS_DATA_CODEC.decode(buf);
            result.playTime = stats.getPlayTime();
            result.deaths = stats.getDeaths();
            result.timeSinceDeath = stats.getTimeSinceDeath();

            PlayerStatsData extended = EXTENDED_STATS_CODEC.decode(buf);
            result.jumps = extended.getJumps();
            result.blocksWalked = extended.getBlocksWalked();
            result.damageDealt = extended.getDamageDealt();

            result.lastSeen = ByteBufCodecs.VAR_LONG.decode(buf);
            result.blocksMined = ByteBufCodecs.VAR_LONG.decode(buf);
            result.damageTaken = ByteBufCodecs.FLOAT.decode(buf);

            return result;
        }
    };

    public void loadFromJson(JsonObject json) {
        try {
            JsonObject stats = json.getAsJsonObject("stats");
            if (stats == null)
                return;

            JsonObject customStats = stats.getAsJsonObject("minecraft:custom");
            if (customStats == null)
                return;

            this.playTime = getStatLong(customStats, "minecraft:play_time", 0L);
            this.deaths = getStatInt(customStats, "minecraft:deaths", 0);
            this.timeSinceDeath = getStatLong(customStats, "minecraft:time_since_death", 0L);
            this.mobKills = getStatInt(customStats, "minecraft:mob_kills", 0);
            this.blocksWalked = getStatLong(customStats, "minecraft:walk_one_cm", 0L) +
                    getStatLong(customStats, "minecraft:sprint_one_cm", 0L) +
                    getStatLong(customStats, "minecraft:walk_on_water_one_cm", 0L) +
                    getStatLong(customStats, "minecraft:walk_under_water_one_cm", 0L);
            this.jumps = getStatInt(customStats, "minecraft:jump", 0);
            this.damageDealt = getStatFloat(customStats, "minecraft:damage_dealt", 0f);
            this.damageTaken = getStatFloat(customStats, "minecraft:damage_taken", 0f);

            JsonObject minedStats = stats.getAsJsonObject("minecraft:mined");
            if (minedStats != null) {
                this.blocksMined = minedStats.entrySet().stream()
                        .mapToLong(entry -> getStatLong(minedStats, entry.getKey(), 0L))
                        .sum();
            }
        } catch (Exception e) {
            System.err.println("Error loading player stats: " + e.getMessage());
        }
    }

    private static long getStatLong(JsonObject json, String key, long defaultValue) {
        try {
            return json.has(key) ? json.get(key).getAsLong() : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static int getStatInt(JsonObject json, String key, int defaultValue) {
        try {
            return json.has(key) ? json.get(key).getAsInt() : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static float getStatFloat(JsonObject json, String key, float defaultValue) {
        try {
            return json.has(key) ? json.get(key).getAsFloat() : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Nonnull
    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName != null ? playerName : "";
    }

    @Nonnull
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid != null ? uuid : "";
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
        if (!online) {
            lastSeen = System.currentTimeMillis();
        }
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public long getPlayTime() {
        return playTime;
    }

    public int getDeaths() {
        return deaths;
    }

    public long getTimeSinceDeath() {
        return timeSinceDeath;
    }

    public int getMobKills() {
        return mobKills;
    }

    public long getBlocksWalked() {
        return blocksWalked;
    }

    public long getBlocksMined() {
        return blocksMined;
    }

    public int getJumps() {
        return jumps;
    }

    public float getDamageDealt() {
        return damageDealt;
    }

    public float getDamageTaken() {
        return damageTaken;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof PlayerStatsData that))
            return false;

        return playTime == that.playTime &&
                deaths == that.deaths &&
                timeSinceDeath == that.timeSinceDeath &&
                mobKills == that.mobKills &&
                blocksWalked == that.blocksWalked &&
                blocksMined == that.blocksMined &&
                jumps == that.jumps &&
                Float.compare(that.damageDealt, damageDealt) == 0 &&
                Float.compare(that.damageTaken, damageTaken) == 0 &&
                Objects.equals(playerName, that.playerName) &&
                Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerName, uuid, playTime, deaths, timeSinceDeath,
                mobKills, blocksWalked, blocksMined, jumps, damageDealt, damageTaken);
    }
}