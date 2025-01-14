package com.enhancedplayerlist.data;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;

public class PlayerStatsData {
    private String playerName;
    private String uuid;
    private boolean isOnline;
    
    // Stats from minecraft:custom
    private long playTime;
    private int deaths;
    private long timeSinceDeath;
    private int mobKills;
    private long blocksWalked;
    private long blocksMined;
    private int jumps;
    private float damageDealt;
    private float damageTaken;
    
    public void loadFromJson(JsonObject json) {
        JsonObject customStats = json.getAsJsonObject("stats")
                                   .getAsJsonObject("minecraft:custom");
        
        this.playTime = getOrDefault(customStats, "minecraft:play_time", 0L);
        this.deaths = getOrDefault(customStats, "minecraft:deaths", 0);
        this.timeSinceDeath = getOrDefault(customStats, "minecraft:time_since_death", 0L);
        this.mobKills = getOrDefault(customStats, "minecraft:mob_kills", 0);
        this.blocksWalked = getOrDefault(customStats, "minecraft:walk_one_cm", 0L) / 100L;
        this.jumps = getOrDefault(customStats, "minecraft:jump", 0);
        this.damageDealt = getOrDefault(customStats, "minecraft:damage_dealt", 0f);
        this.damageTaken = getOrDefault(customStats, "minecraft:damage_taken", 0f);
        
        // Calculate total blocks mined from all mined stats
        JsonObject minedStats = json.getAsJsonObject("stats")
                                  .getAsJsonObject("minecraft:mined");
        if (minedStats != null) {
            this.blocksMined = minedStats.entrySet().stream()
                    .mapToLong(entry -> entry.getValue().getAsLong())
                    .sum();
        }
    }
    
    private <T> T getOrDefault(JsonObject json, String key, T defaultValue) {
        if (json == null || !json.has(key)) return defaultValue;
        if (defaultValue instanceof Long) {
            return (T) Long.valueOf(json.get(key).getAsLong());
        } else if (defaultValue instanceof Integer) {
            return (T) Integer.valueOf(json.get(key).getAsInt());
        } else if (defaultValue instanceof Float) {
            return (T) Float.valueOf(json.get(key).getAsFloat());
        }
        return defaultValue;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(playerName != null ? playerName : "");
        buf.writeUtf(uuid);
        buf.writeBoolean(isOnline);
        buf.writeLong(playTime);
        buf.writeInt(deaths);
        buf.writeLong(timeSinceDeath);
        buf.writeInt(mobKills);
        buf.writeLong(blocksWalked);
        buf.writeLong(blocksMined);
        buf.writeInt(jumps);
        buf.writeFloat(damageDealt);
        buf.writeFloat(damageTaken);
    }

    public void decode(FriendlyByteBuf buf) {
        this.playerName = buf.readUtf();
        this.uuid = buf.readUtf();
        this.isOnline = buf.readBoolean();
        this.playTime = buf.readLong();
        this.deaths = buf.readInt();
        this.timeSinceDeath = buf.readLong();
        this.mobKills = buf.readInt();
        this.blocksWalked = buf.readLong();
        this.blocksMined = buf.readLong();
        this.jumps = buf.readInt();
        this.damageDealt = buf.readFloat();
        this.damageTaken = buf.readFloat();
    }

    // Getters and setters
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public String getUuid() { return uuid; }
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