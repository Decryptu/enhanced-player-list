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

    private PlayerStatsData() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final PlayerStatsData data = new PlayerStatsData();

        public Builder playerName(String playerName) {
            data.playerName = playerName != null ? playerName : "";
            return this;
        }

        public Builder uuid(String uuid) {
            data.uuid = uuid != null ? uuid : "";
            return this;
        }

        public Builder online(boolean online) {
            data.isOnline = online;
            if (!online) {
                data.lastSeen = System.currentTimeMillis();
            }
            return this;
        }

        public Builder lastSeen(long lastSeen) {
            data.lastSeen = lastSeen;
            return this;
        }

        public Builder playTime(long playTime) {
            data.playTime = playTime;
            return this;
        }

        public Builder deaths(int deaths) {
            data.deaths = deaths;
            return this;
        }

        public Builder timeSinceDeath(long timeSinceDeath) {
            data.timeSinceDeath = timeSinceDeath;
            return this;
        }

        public Builder mobKills(int mobKills) {
            data.mobKills = mobKills;
            return this;
        }

        public Builder blocksWalked(long blocksWalked) {
            data.blocksWalked = blocksWalked;
            return this;
        }

        public Builder blocksMined(long blocksMined) {
            data.blocksMined = blocksMined;
            return this;
        }

        public Builder jumps(int jumps) {
            data.jumps = jumps;
            return this;
        }

        public Builder damageDealt(float damageDealt) {
            data.damageDealt = damageDealt;
            return this;
        }

        public Builder damageTaken(float damageTaken) {
            data.damageTaken = damageTaken;
            return this;
        }

        public PlayerStatsData build() {
            return data;
        }
    }

    // Split codecs for network optimization
    private static final StreamCodec<ByteBuf, PlayerStatsData> BASIC_DATA_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PlayerStatsData::getPlayerName,
            ByteBufCodecs.STRING_UTF8, PlayerStatsData::getUuid,
            ByteBufCodecs.BOOL, PlayerStatsData::isOnline,
            (name, uuid, online) -> builder()
                    .playerName(name)
                    .uuid(uuid)
                    .online(online)
                    .build());

    private static final StreamCodec<ByteBuf, PlayerStatsData> STATS_DATA_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, PlayerStatsData::getPlayTime,
            ByteBufCodecs.VAR_INT, PlayerStatsData::getDeaths,
            ByteBufCodecs.VAR_LONG, PlayerStatsData::getTimeSinceDeath,
            (playTime, deaths, timeSinceDeath) -> builder()
                    .playTime(playTime)
                    .deaths(deaths)
                    .timeSinceDeath(timeSinceDeath)
                    .build());

    private static final StreamCodec<ByteBuf, PlayerStatsData> EXTENDED_STATS_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PlayerStatsData::getJumps,
            ByteBufCodecs.VAR_LONG, PlayerStatsData::getBlocksWalked,
            ByteBufCodecs.FLOAT, PlayerStatsData::getDamageDealt,
            (jumps, blocksWalked, damageDealt) -> builder()
                    .jumps(jumps)
                    .blocksWalked(blocksWalked)
                    .damageDealt(damageDealt)
                    .build());

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
            Builder builder = builder();

            // Decode basic data
            PlayerStatsData basic = BASIC_DATA_CODEC.decode(buf);
            builder.playerName(basic.getPlayerName())
                   .uuid(basic.getUuid())
                   .online(basic.isOnline());

            // Decode stats data
            PlayerStatsData stats = STATS_DATA_CODEC.decode(buf);
            builder.playTime(stats.getPlayTime())
                   .deaths(stats.getDeaths())
                   .timeSinceDeath(stats.getTimeSinceDeath());

            // Decode extended stats
            PlayerStatsData extended = EXTENDED_STATS_CODEC.decode(buf);
            builder.jumps(extended.getJumps())
                   .blocksWalked(extended.getBlocksWalked())
                   .damageDealt(extended.getDamageDealt());

            // Decode remaining fields
            return builder.lastSeen(ByteBufCodecs.VAR_LONG.decode(buf))
                         .blocksMined(ByteBufCodecs.VAR_LONG.decode(buf))
                         .damageTaken(ByteBufCodecs.FLOAT.decode(buf))
                         .build();
        }
    };

    public void loadFromJson(JsonObject json) {
        try {
            JsonObject stats = json.getAsJsonObject("stats");
            if (stats == null) return;

            JsonObject customStats = stats.getAsJsonObject("minecraft:custom");
            if (customStats == null) return;

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

    // Getters
    @Nonnull
    public String getPlayerName() { return playerName; }
    @Nonnull
    public String getUuid() { return uuid; }
    public boolean isOnline() { return isOnline; }
    public long getLastSeen() { return lastSeen; }
    public long getPlayTime() { return playTime; }
    public int getDeaths() { return deaths; }
    public long getTimeSinceDeath() { return timeSinceDeath; }
    public int getMobKills() { return mobKills; }
    public long getBlocksWalked() { return blocksWalked; }
    public long getBlocksMined() { return blocksMined; }
    public int getJumps() { return jumps; }
    public float getDamageDealt() { return damageDealt; }
    public float getDamageTaken() { return damageTaken; }

    // Minimal setters needed for backwards compatibility
    public void setPlayerName(String playerName) {
        this.playerName = playerName != null ? playerName : "";
    }

    public void setUuid(String uuid) {
        this.uuid = uuid != null ? uuid : "";
    }

    public void setOnline(boolean online) {
        this.isOnline = online;
        if (!online) {
            this.lastSeen = System.currentTimeMillis();
        }
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerStatsData that)) return false;

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