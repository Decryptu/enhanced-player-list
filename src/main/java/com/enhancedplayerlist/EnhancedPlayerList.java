// src/main/java/com/enhancedplayerlist/EnhancedPlayerList.java
package com.enhancedplayerlist;

import com.enhancedplayerlist.network.NetworkHandler;
import com.enhancedplayerlist.server.ServerStatsManager;
import com.enhancedplayerlist.server.ServerScheduler;
import com.enhancedplayerlist.client.event.ClientEventHandler;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(EnhancedPlayerList.MODID)
public class EnhancedPlayerList {
    public static final String MODID = "enhancedplayerlist";

    public EnhancedPlayerList(IEventBus modEventBus) {
        // Get the mod container
        ModContainer modContainer = ModLoadingContext.get().getActiveContainer();

        // Register config with both bus and container
        Config.register(modEventBus, modContainer);

        // Register mod events
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(NetworkHandler::register);

        // Initialize client event handler only on client side
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientEventHandler.init();
        }

        // Register this class for game events
        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Initialization
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        ServerStatsManager.init(event.getServer());
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        ServerScheduler.onServerTick(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        ServerStatsManager.onServerStopping();
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MinecraftServer server = player.getServer();
            if (server != null) {
                server.execute(() -> {
                    player.getStats().save();
                    ServerStatsManager.forceSync();
                });
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerStatsManager.onPlayerLeave(player);
        }
    }

}