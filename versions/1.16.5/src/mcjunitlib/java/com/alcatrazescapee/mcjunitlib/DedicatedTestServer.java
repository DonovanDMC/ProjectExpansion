/*
 * Part of MCJUnitLib by AlcatrazEscapee
 * Work under Copyright. See the project LICENSE.md for details.
 */

package com.alcatrazescapee.mcjunitlib;

import java.io.File;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.ReportedException;
import net.minecraft.network.ServerStatusResponse;
import net.minecraft.profiler.IProfiler;
import net.minecraft.profiler.LongTickDetector;
import net.minecraft.profiler.TimeTracker;
import net.minecraft.resources.DataPackRegistries;
import net.minecraft.resources.ResourcePackList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerPropertiesProvider;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.Util;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.chunk.listener.IChunkStatusListenerFactory;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.IServerConfiguration;
import net.minecraft.world.storage.SaveFormat;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestManager;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.datafixers.DataFixer;

public class DedicatedTestServer extends DedicatedServer
{
    private static final Level UNIT_TEST = Level.forName("UNITTEST", 50);
    private static final Logger LOGGER = LogManager.getLogger();

    // Reflection to allow access of profiler
    private static final Field PROFILER_FIELD = ObfuscationReflectionHelper.findField(MinecraftServer.class, "field_71304_b"); // profiler
    private static final Field CONTINUOUS_PROFILER_FIELD = ObfuscationReflectionHelper.findField(MinecraftServer.class, "field_240769_m_"); // continuousProfiler

    private static <T> T uncheck(Callable<T> action)
    {
        try
        {
            return action.call();
        }
        catch (Throwable t)
        {
            throw new RuntimeException(t);
        }
    }

    private final boolean crashOnFailedTests;

    private int delayTicks;
    private boolean allTestsFinished;
    private boolean crashed;

    // Shadow from DedicatedServer
    private long lastOverloadWarning;
    private boolean mayHaveDelayedTasks;
    private long delayedTasksMaxNextTickTime;
    private boolean delayProfilerStart;
    private volatile boolean isReady;

    public DedicatedTestServer(Thread thread, DynamicRegistries.Impl dynamicRegistries, SaveFormat.LevelSave saveFormat, ResourcePackList resourcePacks, DataPackRegistries dataPacks, IServerConfiguration serverConfiguration, ServerPropertiesProvider serverProperties, DataFixer dataFixer, MinecraftSessionService service, GameProfileRepository profileRepository, PlayerProfileCache profileCache, IChunkStatusListenerFactory chunkStatusListenerFactory, boolean crashOnFailedTests)
    {
        super(thread, dynamicRegistries, saveFormat, resourcePacks, dataPacks, serverConfiguration, serverProperties, dataFixer, service, profileRepository, profileCache, chunkStatusListenerFactory);

        this.allTestsFinished = false;
        this.crashOnFailedTests = crashOnFailedTests;
        this.delayTicks = 0;
    }

    public boolean crashed()
    {
        return crashed;
    }

    @Override
    protected void runServer()
    {
        LOGGER.log(UNIT_TEST, "Server Thread Starting");
        crashed = false;
        try
        {
            if (initServer())
            {
                // Additional initServer() extras
                // This is the equivalent of online-mode=false, which allows in-dev clients to connect
                setUsesAuthentication(false);
                PlayerProfileCache.setUsesAuthentication(false);

                ServerLifecycleHooks.handleServerStarted(this);

                // Before ticking actions
                // 1. Run all JUnit unit tests
                // 2. Setup all integration tests (running the /integrationTest setup command)

                final JUnitTestRunner unitTestRunner = new JUnitTestRunner();
                unitTestRunner.runAllTests();

                final ServerWorld overworld = overworld();
                final BiConsumer<String, Boolean> logger = (message, success) -> LOGGER.info((success ? "" : "ERROR : ") + message);
                final boolean testsVerified = IntegrationTestManager.INSTANCE.verifyAllTests(overworld, logger);
                if (!testsVerified)
                {
                    LOGGER.log(UNIT_TEST, "Unable to verify all tests.");
                }

                // Ticking actions
                // Tick along as per normal
                nextTickTime = Util.getMillis();

                final ServerStatusResponse status = getStatus();
                final IProfiler profiler = getProfiler();

                status.setDescription(new StringTextComponent("Test Server"));
                status.setVersion(new ServerStatusResponse.Version(SharedConstants.getCurrentVersion().getName(), SharedConstants.getCurrentVersion().getProtocolVersion()));

                while (isRunning())
                {
                    long tickDelta = Util.getMillis() - nextTickTime;
                    if (tickDelta > 2000L && nextTickTime - lastOverloadWarning >= 15000L)
                    {
                        long bigTickDelta = tickDelta / 50L;
                        LOGGER.warn("Can't keep up! Is the server overloaded? Running {}ms or {} ticks behind", tickDelta, bigTickDelta);
                        nextTickTime += bigTickDelta * 50L;
                        lastOverloadWarning = this.nextTickTime;
                    }

                    nextTickTime += 50L;
                    startProfilerTick();
                    profiler.startTick();
                    profiler.push("tick");
                    tickServer(this::haveTimeShadow);

                    if (testsVerified)
                    {
                        delayTicks++;
                        if (delayTicks == 20)
                        {
                            LOGGER.log(UNIT_TEST, "Running test setup...");
                            IntegrationTestManager.INSTANCE.setupAllTests(overworld, logger);
                        }
                        else if (delayTicks == 40)
                        {
                            LOGGER.log(UNIT_TEST, "Running tests...");
                            IntegrationTestManager.INSTANCE.runAllTests(overworld, logger);
                        }
                        else if (delayTicks > 40 && !allTestsFinished && IntegrationTestManager.INSTANCE.isComplete())
                        {
                            // Check test completions, and if so, stop server
                            allTestsFinished = true;
                            LOGGER.log(UNIT_TEST, "All tests finished.");
                            boolean failures = unitTestRunner.hasFailedTests() || IntegrationTestManager.INSTANCE.hasFailedTests();
                            if (!failures)
                            {
                                halt(false); // All tests passed, exit gracefully
                            }
                            else if (crashOnFailedTests)
                            {
                                throw new ReportedException(new CrashReport("Some tests have failed!", new Exception())); // Some tests failed, and we've specified to hard crash
                            }
                            // Otherwise, just continue running. This is for debugging test failure states.
                        }
                    }

                    profiler.popPush("nextTickWait");
                    mayHaveDelayedTasks = true;
                    delayedTasksMaxNextTickTime = Math.max(Util.getMillis() + 50L, this.nextTickTime);
                    waitUntilNextTick();
                    profiler.pop();
                    profiler.endTick();
                    endProfilerTick();
                    isReady = true;
                }

                ServerLifecycleHooks.handleServerStopping(this);
            }
            ServerLifecycleHooks.expectServerStopped(); // has to come before finalTick to avoid race conditions
        }
        catch (Throwable e)
        {
            LOGGER.log(UNIT_TEST, "Encountered an unexpected exception", e);
            crashed = true;
            CrashReport crashReport;
            if (e instanceof ReportedException)
            {
                crashReport = this.fillReport(((ReportedException) e).getReport());
            }
            else
            {
                crashReport = this.fillReport(new CrashReport("Exception in server tick loop", e));
            }

            File crashReportFile = new File(new File(this.getServerDirectory(), "crash-reports"), "crash-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + "-server.txt");
            if (crashReport.saveToFile(crashReportFile))
            {
                LOGGER.log(UNIT_TEST, "This crash report has been saved to: {}", crashReportFile.getAbsolutePath());
            }
            else
            {
                LOGGER.log(UNIT_TEST, "We were unable to save this crash report to disk.");
            }

            ServerLifecycleHooks.expectServerStopped(); // has to come before finalTick to avoid race conditions
        }
        finally
        {
            try
            {
                ObfuscationReflectionHelper.setPrivateValue(MinecraftServer.class, this, true, "field_71316_v"); // stopped
                stopServer();
            }
            catch (Throwable t)
            {
                LOGGER.log(UNIT_TEST, "Exception stopping the server", t);
            }
            finally
            {
                ServerLifecycleHooks.handleServerStopped(this);
                onServerExit();
            }

            // Set -1 exit code on crash, so gradle can detect test failures
            if (crashed)
            {
                System.exit(1);
            }
        }
    }

    @Override
    public long getMaxTickLength()
    {
        return 0; // Override to prevent server watchdog thread from starting and crashing long running tests
    }

    /**
     * Override to use shadowed {@code haveTime()}
     */
    @Override
    protected void waitUntilNextTick()
    {
        runAllTasks();
        managedBlock(() -> !haveTimeShadow());
    }

    /**
     * Override to set shadowed mayHaveDelayedTasks
     */
    @Override
    public boolean pollTask()
    {
        final boolean flag = super.pollTask();
        this.mayHaveDelayedTasks = flag;
        return flag;
    }

    /**
     * Use shadowed field
     */
    @Override
    public boolean isReady()
    {
        return isReady;
    }

    /**
     * Use shadowed fields, and the superclass method is private
     * Hint: There is an issue with Forge de/obfuscation if this method is called "haveTime". https://github.com/alcatrazEscapee/mcjunitlib/issues/2
     */
    private boolean haveTimeShadow()
    {
        return runningTask() || Util.getMillis() < (mayHaveDelayedTasks ? delayedTasksMaxNextTickTime : nextTickTime);
    }

    /**
     * Use shadowed fields, and the superclass method is private
     */
    private void startProfilerTick()
    {
        TimeTracker continuousProfiler = uncheck(() -> (TimeTracker) CONTINUOUS_PROFILER_FIELD.get(this));
        if (delayProfilerStart)
        {
            delayProfilerStart = false;
            uncheck(() -> (TimeTracker) CONTINUOUS_PROFILER_FIELD.get(this)).enable();
        }
        uncheck(() -> {
            PROFILER_FIELD.set(this, LongTickDetector.decorateFiller(continuousProfiler.getFiller(), null));
            return null;
        });
    }

    /**
     * Use shadowed fields, and the superclass method is private
     */
    private void endProfilerTick()
    {
        uncheck(() -> {
            PROFILER_FIELD.set(this, ((TimeTracker) CONTINUOUS_PROFILER_FIELD.get(this)).getFiller());
            return null;
        });
    }
}
