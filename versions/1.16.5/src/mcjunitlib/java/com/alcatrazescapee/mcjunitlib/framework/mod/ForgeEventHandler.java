package com.alcatrazescapee.mcjunitlib.framework.mod;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestManager;

/**
 * This is part of the mod features of the integration tests
 */
public final class ForgeEventHandler
{
    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event)
    {
        LOGGER.debug("Registering 'integrationTest' command");
        IntegrationTestCommands.registerCommands(event.getDispatcher());
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event)
    {
        final World world = event.world;
        if (event.phase == TickEvent.Phase.END && world instanceof ServerWorld && event.world.dimension() == World.OVERWORLD)
        {
            IntegrationTestManager.INSTANCE.tick((ServerWorld) world);
        }
    }

    @SubscribeEvent
    public void onCreateWorldSpawn(WorldEvent.CreateSpawnPosition event)
    {
        // Spawn just beneath the test area
        LOGGER.debug("Setting spawn location for integration tests");
        event.getSettings().setSpawn(new BlockPos(0, 4, -10), 0.0F);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event)
    {
        if (event.getWorld() instanceof ServerWorld)
        {
            LOGGER.debug("Setting up game rules for integration tests");

            final ServerWorld world = (ServerWorld) event.getWorld();
            final MinecraftServer server = world.getServer();
            final GameRules rules = world.getGameRules();

            // Game rules for best test setup
            rules.getRule(GameRules.RULE_DAYLIGHT).set(false, server);
            rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);

            rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
            rules.getRule(GameRules.RULE_DOINSOMNIA).set(false, server);
            rules.getRule(GameRules.RULE_DO_PATROL_SPAWNING).set(false, server);
            rules.getRule(GameRules.RULE_DO_TRADER_SPAWNING).set(false, server);
        }
    }
}
