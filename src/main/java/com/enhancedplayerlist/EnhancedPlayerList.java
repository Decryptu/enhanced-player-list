package com.enhancedplayerlist;

import com.enhancedplayerlist.network.NetworkHandler;
import com.enhancedplayerlist.server.ServerStatsManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(EnhancedPlayerList.MODID)
public class EnhancedPlayerList {
    public static final String MODID = "enhancedplayerlist";
    private static final Logger LOGGER = LogUtils.getLogger();

    public EnhancedPlayerList(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerPayloads);
        
        // Register ourselves for server and other game events
        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Enhanced Player List initialized");
    }

    private void registerPayloads(RegisterPayloadHandlerEvent event) {
        NetworkHandler.register(event);
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        ServerStatsManager.init(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        // Clean up if needed
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            ServerStatsManager.onPlayerJoin((ServerPlayer)event.getEntity());
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            ServerStatsManager.onPlayerLeave((ServerPlayer)event.getEntity());
        }
    }
}