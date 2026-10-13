package com.alcatrazescapee.mcjunitlib;

import java.io.File;
import java.net.Proxy;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.block.Blocks;
import net.minecraft.command.Commands;
import net.minecraft.crash.CrashReport;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerPropertiesProvider;
import net.minecraft.server.dedicated.PropertyManager;
import net.minecraft.server.dedicated.ServerProperties;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.DefaultUncaughtExceptionHandler;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixesManager;
import net.minecraft.util.datafix.codec.DatapackCodec;
import net.minecraft.util.registry.*;
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.chunk.listener.LoggingChunkStatusListener;
import net.minecraft.world.gen.DimensionSettings;
import net.minecraft.world.gen.FlatChunkGenerator;
import net.minecraft.world.gen.FlatGenerationSettings;
import net.minecraft.world.gen.FlatLayerInfo;
import net.minecraft.world.gen.settings.DimensionGeneratorSettings;
import net.minecraft.world.gen.settings.DimensionStructuresSettings;
import net.minecraft.world.storage.FolderName;
import net.minecraft.world.storage.IServerConfiguration;
import net.minecraft.world.storage.SaveFormat;
import net.minecraft.world.storage.ServerWorldInfo;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.server.ServerModLoader;

import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestManager;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.serialization.Lifecycle;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

public class TestMain
{
    private static final Level UNIT_TEST = Level.forName("UNITTEST", 50);
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String TEST_WORLD = "test-world";

    public static void main(String[] args)
    {
        LOGGER.log(UNIT_TEST, "TestMain launching");

        OptionParser spec = new OptionParser();

        // Consume unused options, so they don't error if used inadvertently
        spec.accepts("nogui");
        spec.accepts("initSettings", "Initializes 'server.properties' and 'eula.txt', then quits");
        spec.accepts("demo");
        spec.accepts("bonusChest");
        spec.accepts("forceUpgrade");
        spec.accepts("eraseCache");
        spec.accepts("safeMode", "Loads level with vanilla datapack only");
        spec.accepts("help").forHelp();
        spec.accepts("singleplayer").withRequiredArg();
        OptionSpec<String> universeOption = spec.accepts("universe").withRequiredArg().defaultsTo(".");
        OptionSpec<String> worldOption = spec.accepts("world").withRequiredArg();
        spec.accepts("port").withRequiredArg().ofType(Integer.class).defaultsTo(-1);
        spec.accepts("serverId").withRequiredArg();
        spec.nonOptions();
        spec.accepts("allowUpdates").withRequiredArg().ofType(Boolean.class).defaultsTo(Boolean.TRUE); // Forge: allow mod updates to proceed
        spec.accepts("gameDir").withRequiredArg().ofType(File.class).defaultsTo(new File(".")); //Forge: Consume this argument, we use it in the launcher, and the client side.

        // Additional options, for testing purposes
        OptionSpec<Void> crashOnFailedTestsSpec = spec.accepts("crashOnFailedTests");

        try
        {
            OptionSet options = spec.parse(args);

            CrashReport.preload();
            Bootstrap.bootStrap();
            Bootstrap.validate();
            Util.startTimerHackThread();
            ServerModLoader.load();

            // After mods have loaded, immediately setup for integration tests.
            IntegrationTestManager.setup();

            DynamicRegistries.Impl builtinRegistries = DynamicRegistries.builtin();

            // Delete the old test world, we create a new one each run
            LOGGER.log(UNIT_TEST, "Removing previous test world...");
            FileUtils.deleteDirectory(new File(TEST_WORLD));

            // Edit the server.properties file before force saving it. This requires some minor reflection into the original properties object
            final Path path = Paths.get("server.properties");
            final ServerPropertiesProvider serverPropertiesProvider = new ServerPropertiesProvider(builtinRegistries, path);
            final ServerProperties serverProperties = serverPropertiesProvider.getProperties();
            final Properties properties = ObfuscationReflectionHelper.getPrivateValue(PropertyManager.class, serverProperties, "field_73672_b"); // properties

            Objects.requireNonNull(properties);
            properties.setProperty("difficulty", "normal");
            properties.setProperty("enable-command-block", "true");
            properties.setProperty("gamemode", "creative");
            properties.setProperty("generate-structures", "false");
            properties.setProperty("level-name", TEST_WORLD);
            properties.setProperty("max-tick-time", "0");
            properties.setProperty("online-mode", "false");

            serverPropertiesProvider.forceSave();

            File universeFile = new File(options.valueOf(universeOption));
            YggdrasilAuthenticationService authService = new YggdrasilAuthenticationService(Proxy.NO_PROXY, UUID.randomUUID().toString());
            MinecraftSessionService sessionService = authService.createMinecraftSessionService();
            GameProfileRepository profileRepository = authService.createProfileRepository();
            PlayerProfileCache profileCache = new PlayerProfileCache(profileRepository, new File(universeFile, MinecraftServer.USERID_CACHE_FILE.getName()));

            // The world is not read from either server.properties, or the passed in options
            if (new File(universeFile, TEST_WORLD).getAbsolutePath().equals(new File(TEST_WORLD).getAbsolutePath()))
            {
                LOGGER.log(UNIT_TEST, "Error: Invalid world directory specified, must not be null, empty or the same directory as your universe! " + TEST_WORLD);
                return;
            }
            SaveFormat saveFormat = SaveFormat.createDefault(universeFile.toPath());
            SaveFormat.LevelSave levelSave = saveFormat.createAccess(TEST_WORLD);
            MinecraftServer.convertFromRegionFormatIfNeeded(levelSave);
            DatapackCodec levelDataPacks = levelSave.getDataPacks();

            ResourcePackList resourcePacks = new ResourcePackList(new ServerPackFinder(), new FolderPackFinder(levelSave.getLevelPath(FolderName.DATAPACK_DIR).toFile(), IPackNameDecorator.WORLD));
            DatapackCodec dataPacks = MinecraftServer.configurePackRepository(resourcePacks, levelDataPacks == null ? DatapackCodec.DEFAULT : levelDataPacks, false);
            CompletableFuture<DataPackRegistries> dataPackFuture = DataPackRegistries.loadResources(resourcePacks.openAllSelected(), Commands.EnvironmentType.DEDICATED, serverPropertiesProvider.getProperties().functionPermissionLevel, Util.backgroundExecutor(), Runnable::run);

            DataPackRegistries dataPackRegistries;
            try
            {
                dataPackRegistries = dataPackFuture.get();
            }
            catch (Exception e)
            {
                LOGGER.log(UNIT_TEST, "FATAL: Failed to load datapacks, can't proceed with server load. You can either fix your datapacks or reset to vanilla with --safeMode", e);
                resourcePacks.close();
                return;
            }

            dataPackRegistries.updateGlobals();
            WorldSettingsImport<INBT> worldSettingsImport = WorldSettingsImport.create(NBTDynamicOps.INSTANCE, dataPackRegistries.getResourceManager(), builtinRegistries);

            // Custom world settings, ignoring most of the options in server.properties
            final WorldSettings worldSettings = new WorldSettings(TEST_WORLD, GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), dataPacks);

            // Custom dimension generator settings
            // Modified from DimensionGeneratorSettings#create
            final long testSeed = new Random().nextLong();
            final Registry<DimensionType> dimensionTypeRegistry = builtinRegistries.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY);
            final Registry<Biome> biomeRegistry = builtinRegistries.registryOrThrow(Registry.BIOME_REGISTRY);
            final Registry<DimensionSettings> dimensionSettingsRegistry = builtinRegistries.registryOrThrow(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY);
            final SimpleRegistry<Dimension> dimensionRegistry = DimensionType.defaultDimensions(dimensionTypeRegistry, biomeRegistry, dimensionSettingsRegistry, testSeed);

            // Flat chunk generator. Modified from FlatPresetsScreen#<cinit>
            LOGGER.log(UNIT_TEST, "Setting random seed: " + testSeed);
            final List<FlatLayerInfo> layers = new ArrayList<>(Arrays.asList(new FlatLayerInfo(1, Blocks.BEDROCK), new FlatLayerInfo(2, Blocks.DIRT), new FlatLayerInfo(1, Blocks.GRASS_BLOCK)));
            final FlatChunkGenerator chunkGenerator = new FlatChunkGenerator(new FlatGenerationSettings(biomeRegistry, new DimensionStructuresSettings(Optional.empty(), new HashMap<>()), layers, false, false, Optional.of(() -> biomeRegistry.getOrThrow(Biomes.PLAINS))));
            final DimensionGeneratorSettings testDimensionGeneratorSettings = new DimensionGeneratorSettings(testSeed, false, false, DimensionGeneratorSettings.withOverworld(dimensionTypeRegistry, dimensionRegistry, chunkGenerator));

            final IServerConfiguration serverConfiguration = new ServerWorldInfo(worldSettings, testDimensionGeneratorSettings, Lifecycle.stable());
            levelSave.saveDataTag(builtinRegistries, serverConfiguration);

            final boolean crashOnFailedTests = options.has(crashOnFailedTestsSpec);
            final DedicatedTestServer server = MinecraftServer.spin(threadIn -> new DedicatedTestServer(threadIn, builtinRegistries, levelSave, resourcePacks, dataPackRegistries, serverConfiguration, serverPropertiesProvider, DataFixesManager.getDataFixer(), sessionService, profileRepository, profileCache, LoggingChunkStatusListener::new, crashOnFailedTests));

            Thread thread = new Thread("Server Shutdown Thread")
            {
                public void run()
                {
                    server.halt(!server.crashed());
                    LogManager.shutdown(); // we're manually managing the logging shutdown on the server. Make sure we do it here at the end.
                }
            };
            thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
            Runtime.getRuntime().addShutdownHook(thread);
        }
        catch (Exception e)
        {
            LOGGER.log(UNIT_TEST, "FATAL: Failed to start the minecraft server", e);
        }
    }
}
