package com.cibernet.splatcraft;

import com.cibernet.splatcraft.data.SplatcraftTags;
import com.cibernet.splatcraft.handlers.ScoreboardHandler;
import com.cibernet.splatcraft.handlers.client.ClientSetupHandler;
import com.cibernet.splatcraft.handlers.client.SplatcraftKeyHandler;
import com.cibernet.splatcraft.network.SplatcraftPacketHandler;
import com.cibernet.splatcraft.registries.*;
import com.cibernet.splatcraft.world.gen.SplatcraftOreGen;
import net.minecraft.block.Block;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Splatcraft.MODID)
public class Splatcraft
{
    public static final String MODID = "splatcraft";
    public static final String MODNAME = "Splatcraft";
    public static final String SHORT = "SC";
    public static final String VERSION = "2.3.0";
    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    public Splatcraft()
    {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SplatcraftConfig.clientConfig);
        SplatcraftConfig.loadConfig(SplatcraftConfig.clientConfig, FMLPaths.CONFIGDIR.get().resolve(Splatcraft.MODID + "-client.toml").toString());

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        SplatcraftCapabilities.registerCapabilities();
        SplatcraftPacketHandler.registerMessages();

        event.enqueueWork(() ->
        {
            SplatcraftEntities.setEntityAttributes();
            SplatcraftGameRules.registerGamerules();
        });

        SplatcraftTags.register();
        SplatcraftStats.register();
        ScoreboardHandler.register();
        SplatcraftCommands.registerArguments();

        SplatcraftOreGen.registerOres();
    }

    private void clientSetup(final FMLClientSetupEvent event)
    {
        SplatcraftEntities.bindRenderers();
        SplatcraftKeyHandler.registerKeys();
        SplatcraftBlocks.setRenderLayers();
        SplatcraftTileEntitites.bindTESR();


        event.enqueueWork(() ->
        {
            SplatcraftItems.registerModelProperties();
            SplatcraftItems.registerArmorModels();
            ClientSetupHandler.bindScreenContainers();
        });
    }

    @SubscribeEvent
    public void onServerAboutToStart(FMLServerAboutToStartEvent event)
    {

    }

    @SubscribeEvent
    public void onServerStarted(FMLServerStartedEvent event)
    {
        SplatcraftGameRules.booleanRules.replaceAll((k, v) -> event.getServer().getGameRules().getBoolean(SplatcraftGameRules.getRuleFromIndex(k)));
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents
    {
        @SubscribeEvent
        public static void onBlocksRegistry(final RegistryEvent.Register<Block> blockRegistryEvent)
        {
        }
    }
}
