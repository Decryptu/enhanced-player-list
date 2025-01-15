// src/main/java/com/enhancedplayerlist/data/PlayerStatsData.java
package com.enhancedplayerlist.data;

import com.google.gson.JsonObject;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import io.netty.buffer.ByteBuf;
import javax.annotation.Nonnull;

public class PlayerStatsData {
  private String playerName;
  private String uuid;
  private boolean isOnline;
  
  private long playTime;
  private int deaths;
  private long timeSinceDeath;
  private int mobKills;
  private long blocksWalked;
  private long blocksMined;
  private int jumps;
  private float damageDealt;
  private float damageTaken;

  private static final StreamCodec<ByteBuf, PlayerStatsData> BASIC_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8, PlayerStatsData::getPlayerName,
      ByteBufCodecs.STRING_UTF8, PlayerStatsData::getUuid,
      ByteBufCodecs.BOOL, PlayerStatsData::isOnline,
      ByteBufCodecs.VAR_LONG, PlayerStatsData::getPlayTime,
      ByteBufCodecs.VAR_INT, PlayerStatsData::getDeaths,
      ByteBufCodecs.VAR_LONG, PlayerStatsData::getTimeSinceDeath,
      (playerName, uuid, online, playTime, deaths, timeSinceDeath) -> {
          PlayerStatsData data = new PlayerStatsData();
          data.setPlayerName(playerName);
          data.setUuid(uuid);
          data.setOnline(online);
          data.playTime = playTime;
          data.deaths = deaths;
          data.timeSinceDeath = timeSinceDeath;
          return data;
      }
  );

  private static final StreamCodec<ByteBuf, PlayerStatsData> STATS_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, PlayerStatsData::getMobKills,
      ByteBufCodecs.VAR_LONG, PlayerStatsData::getBlocksWalked,
      ByteBufCodecs.VAR_LONG, PlayerStatsData::getBlocksMined,
      ByteBufCodecs.VAR_INT, PlayerStatsData::getJumps,
      ByteBufCodecs.FLOAT, PlayerStatsData::getDamageDealt,
      ByteBufCodecs.FLOAT, PlayerStatsData::getDamageTaken,
      (mobKills, blocksWalked, blocksMined, jumps, damageDealt, damageTaken) -> {
          PlayerStatsData data = new PlayerStatsData();
          data.mobKills = mobKills;
          data.blocksWalked = blocksWalked;
          data.blocksMined = blocksMined;
          data.jumps = jumps;
          data.damageDealt = damageDealt;
          data.damageTaken = damageTaken;
          return data;
      }
  );

    public static final StreamCodec<ByteBuf, PlayerStatsData> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(@Nonnull ByteBuf buf, @Nonnull PlayerStatsData value) {
            BASIC_CODEC.encode(buf, value);
            STATS_CODEC.encode(buf, value);
        }

        @Override
        public @Nonnull PlayerStatsData decode(@Nonnull ByteBuf buf) {
            PlayerStatsData basic = BASIC_CODEC.decode(buf);
            PlayerStatsData stats = STATS_CODEC.decode(buf);
            
            basic.mobKills = stats.mobKills;
            basic.blocksWalked = stats.blocksWalked;
            basic.blocksMined = stats.blocksMined;
            basic.jumps = stats.jumps;
            basic.damageDealt = stats.damageDealt;
            basic.damageTaken = stats.damageTaken;
            
            return basic;
        }
    };
  
  public PlayerStatsData() {}

  public void loadFromJson(JsonObject json) {
      JsonObject customStats = json.getAsJsonObject("stats")
                                 .getAsJsonObject("minecraft:custom");
      
      this.playTime = getStatLong(customStats, "minecraft:play_time", 0L);
      this.deaths = getStatInt(customStats, "minecraft:deaths", 0);
      this.timeSinceDeath = getStatLong(customStats, "minecraft:time_since_death", 0L);
      this.mobKills = getStatInt(customStats, "minecraft:mob_kills", 0);
      this.blocksWalked = getStatLong(customStats, "minecraft:walk_one_cm", 0L) / 100L;
      this.jumps = getStatInt(customStats, "minecraft:jump", 0);
      this.damageDealt = getStatFloat(customStats, "minecraft:damage_dealt", 0f);
      this.damageTaken = getStatFloat(customStats, "minecraft:damage_taken", 0f);
      
      JsonObject minedStats = json.getAsJsonObject("stats")
                                .getAsJsonObject("minecraft:mined");
      if (minedStats != null) {
          this.blocksMined = minedStats.entrySet().stream()
                  .mapToLong(entry -> entry.getValue().getAsLong())
                  .sum();
      }
  }
  
  private long getStatLong(JsonObject json, String key, long defaultValue) {
      return json != null && json.has(key) ? json.get(key).getAsLong() : defaultValue;
  }

  private int getStatInt(JsonObject json, String key, int defaultValue) {
      return json != null && json.has(key) ? json.get(key).getAsInt() : defaultValue;
  }

  private float getStatFloat(JsonObject json, String key, float defaultValue) {
      return json != null && json.has(key) ? json.get(key).getAsFloat() : defaultValue;
  }

  public String getPlayerName() { return playerName != null ? playerName : ""; }
  public void setPlayerName(String playerName) { this.playerName = playerName; }
  public String getUuid() { return uuid != null ? uuid : ""; }
  public void setUuid(String uuid) { this.uuid = uuid; }
  public boolean isOnline() { return isOnline; }
  public void setOnline(boolean online) { isOnline = online; }
  public long getPlayTime() { return playTime; }
  public int getDeaths() { return deaths; }
  public long getTimeSinceDeath() { return timeSinceDeath; }
  public int getMobKills() { return mobKills; }
  public long getBlocksWalked() { return blocksWalked; }
  public long getBlocksMined() { return blocksMined; }
  public int getJumps() { return jumps; }
  public float getDamageDealt() { return damageDealt; }
  public float getDamageTaken() { return damageTaken; }
}