// src/main/java/com/enhancedplayerlist/EnhancedPlayerList.java
package com.enhancedplayerlist;

import com.enhancedplayerlist.network.NetworkHandler;
import com.enhancedplayerlist.server.ServerStatsManager;
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
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModContainer;

@Mod(EnhancedPlayerList.MODID)
public class EnhancedPlayerList {
   public static final String MODID = "enhancedplayerlist";
   private static final Logger LOGGER = LogUtils.getLogger();

   public EnhancedPlayerList(IEventBus modEventBus) {
    // Get the mod container 
    ModContainer modContainer = ModLoadingContext.get().getActiveContainer();
    
    // Register config with both bus and container
    Config.register(modEventBus, modContainer);
       
       // Register mod events
       modEventBus.addListener(this::commonSetup);
       modEventBus.addListener(NetworkHandler::register);
       NeoForge.EVENT_BUS.register(this);
   }

   private void commonSetup(final FMLCommonSetupEvent event) {
       LOGGER.info("Enhanced Player List initialized");
   }

   @SubscribeEvent
   public void onServerStarted(ServerStartedEvent event) {
       ServerStatsManager.init(event.getServer());
   }

   @SubscribeEvent
   public void onServerStopping(ServerStoppingEvent event) {
       ServerStatsManager.onServerStopping();
   }

   @SubscribeEvent
   public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
       if (event.getEntity() instanceof ServerPlayer player) {
           ServerStatsManager.onPlayerJoin(player);
       }
   }

   @SubscribeEvent
   public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
       if (event.getEntity() instanceof ServerPlayer player) {
           ServerStatsManager.onPlayerLeave(player);
       }
   }
}