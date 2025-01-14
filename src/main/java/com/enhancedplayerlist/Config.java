package com.enhancedplayerlist;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Arrays;
import java.util.List;

@Mod.EventBusSubscriber(modid = EnhancedPlayerList.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue SHOW_OFFLINE_PLAYERS = BUILDER
            .comment("Whether to show offline players in the player list")
            .define("showOfflinePlayers", true);

    private static final ModConfigSpec.BooleanValue GRAY_OUT_OFFLINE = BUILDER
            .comment("Whether to gray out offline players in the player list")
            .define("grayOutOffline", true);

    private static final ModConfigSpec.ConfigValue<List<? extends String>> VISIBLE_STATS = BUILDER
            .comment("Stats to display in the player list",
                    "Available stats:",
                    "playtime - Total play time",
                    "deaths - Number of deaths",
                    "lastDeath - Time since last death",
                    "mobKills - Total mob kills",
                    "playerKills - Player kills",
                    "blocksWalked - Blocks walked",
                    "blocksMined - Blocks mined",
                    "jumps - Jump count",
                    "damageDealt - Damage dealt",
                    "damageTaken - Damage taken")
            .defineList("visibleStats", 
                Arrays.asList(
                    "playtime",
                    "deaths",
                    "lastDeath",
                    "mobKills",
                    "blocksWalked"
                ),
                entry -> entry instanceof String && isValidStat((String) entry));

    private static final ModConfigSpec.IntValue UPDATE_FREQUENCY = BUILDER
            .comment("How often to update player stats (in ticks, 20 ticks = 1 second)")
            .defineInRange("updateFrequency", 100, 20, 6000);

    private static final ModConfigSpec.BooleanValue COMPACT_MODE = BUILDER
            .comment("Whether to use compact mode (shorter stat names and values)")
            .define("compactMode", false);

    static final ModConfigSpec SPEC = BUILDER.build();

    // Runtime configuration access
    public static boolean showOfflinePlayers;
    public static boolean grayOutOffline;
    public static List<? extends String> visibleStats;
    public static int updateFrequency;
    public static boolean compactMode;

    private static boolean isValidStat(String stat) {
        return Arrays.asList(
                "playtime",
                "deaths",
                "lastDeath",
                "mobKills",
                "playerKills",
                "blocksWalked",
                "blocksMined",
                "jumps",
                "damageDealt",
                "damageTaken"
        ).contains(stat);
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent.Loading event) {
        showOfflinePlayers = SHOW_OFFLINE_PLAYERS.get();
        grayOutOffline = GRAY_OUT_OFFLINE.get();
        visibleStats = VISIBLE_STATS.get();
        updateFrequency = UPDATE_FREQUENCY.get();
        compactMode = COMPACT_MODE.get();
    }
    
    @SubscribeEvent
    static void onReload(final ModConfigEvent.Reloading event) {
        showOfflinePlayers = SHOW_OFFLINE_PLAYERS.get();
        grayOutOffline = GRAY_OUT_OFFLINE.get();
        visibleStats = VISIBLE_STATS.get();
        updateFrequency = UPDATE_FREQUENCY.get();
        compactMode = COMPACT_MODE.get();
    }
}