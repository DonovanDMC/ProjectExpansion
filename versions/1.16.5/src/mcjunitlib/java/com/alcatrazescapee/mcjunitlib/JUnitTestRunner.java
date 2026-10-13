/*
 * Part of MCJUnitLib by AlcatrazEscapee
 * Work under Copyright. See the project LICENSE.md for details.
 */

package com.alcatrazescapee.mcjunitlib;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraftforge.fml.ModList;

import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.engine.support.descriptor.PackageSource;
import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

public class JUnitTestRunner implements TestExecutionListener
{
    private static final Level UNIT_TEST = Level.forName("UNITTEST", 50);
    private static final Logger LOGGER = LogManager.getLogger("UnitTests");
    private static final String HR = "--------------------------------------------------";
    // classes with an annotation from here are treated as test classes, such as @Test or @ParameterizedTest
    private static final String JUNIT_PACKAGE = "org.junit.";

    private int testCounter;
    private boolean failedTests;

    public void runAllTests()
    {
        testCounter = 1;
        failedTests = false;

        // Test classes are found through FML's scan data, the same as integration tests.
        // Selecting whole classpath roots made JUnit load every class in them, including client only ones that can't load on a dedicated server.
        Set<String> testClasses = ModList.get().getAllScanData().stream()
            .flatMap(scanData -> scanData.getAnnotations().stream())
            .filter(annotation -> annotation.getAnnotationType().getClassName().startsWith(JUNIT_PACKAGE))
            .map(annotation -> annotation.getClassType().getClassName())
            .collect(Collectors.toCollection(TreeSet::new));
        LOGGER.debug("Found test classes {}", testClasses);

        // Forge gradle sets MOD_MODULES to an empty string when there are none, which isn't a module name
        String moduleNamesString = Optional.ofNullable(System.getenv("MOD_MODULES")).orElse("");
        LOGGER.debug("Got module names {} from env", moduleNamesString);
        Set<String> moduleNames = Arrays.stream(moduleNamesString.split(File.pathSeparator))
                        .map(mod -> {
                            String[] splitString = mod.split("%%", 2);
                            return splitString[splitString.length - 1];
                        })
                        .filter(mod -> !mod.trim().isEmpty())
                        .collect(Collectors.toSet());

        if(!moduleNames.isEmpty())
            LOGGER.debug("Found supplied module names [{}]", moduleNames);

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
            .selectors(testClasses.stream().map(DiscoverySelectors::selectClass).collect(Collectors.toList()))
            .selectors(DiscoverySelectors.selectModules(moduleNames))
            .build();

        Launcher launcher = LauncherFactory.create();
        TestPlan testPlan = launcher.discover(request);

        if (testPlan.containsTests())
        {
            SummaryGeneratingListener summaryListener = new SummaryGeneratingListener();
            launcher.execute(request, this, summaryListener);
            TestExecutionSummary summary = summaryListener.getSummary();
            long timeMillis = summary.getTimeFinished() - summary.getTimeStarted();

            if (!summary.getFailures().isEmpty())
            {
                LOGGER.log(UNIT_TEST, HR);
                LOGGER.log(UNIT_TEST, "Failures:");
                for (TestExecutionSummary.Failure failure : summary.getFailures())
                {
                    LOGGER.log(UNIT_TEST, "Test {} Failed: {}", failure.getTestIdentifier().getDisplayName(), failure.getException().toString());
                    LOGGER.debug("Failure Reason:", failure.getException());
                }
            }

            LOGGER.log(UNIT_TEST, HR);
            LOGGER.log(UNIT_TEST, "Summary");
            LOGGER.log(UNIT_TEST, "Found {} Tests", summary.getTestsFoundCount());
            LOGGER.log(UNIT_TEST, " - {} / {} Passed ({})", summary.getTestsSucceededCount(), summary.getTestsFoundCount(), String.format("%02d%%", (int) (100f * summary.getTestsSucceededCount() / summary.getTestsFoundCount())));
            if (summary.getTestsFailedCount() > 0)
            {
                LOGGER.log(UNIT_TEST, " - {} / {} Failed ({})", summary.getTestsFailedCount(), summary.getTestsFoundCount(), String.format("%02d%%", (int) (100f * summary.getTestsFailedCount() / summary.getTestsFoundCount())));
            }
            if (summary.getTestsSkippedCount() > 0)
            {
                LOGGER.log(UNIT_TEST, " - {} / {} Skipped ({})", summary.getTestsSkippedCount(), summary.getTestsFoundCount(), String.format("%02d%%", (int) (100f * summary.getTestsSkippedCount() / summary.getTestsFoundCount())));
            }
            if (summary.getTestsAbortedCount() > 0)
            {
                LOGGER.log(UNIT_TEST, " - {} / {} Aborted ({})", summary.getTestsAbortedCount(), summary.getTestsFoundCount(), String.format("%02d%%", (int) (100f * summary.getTestsAbortedCount() / summary.getTestsFoundCount())));
            }
            String millis = timeMillis == 0 ? "< 1" : String.valueOf(timeMillis);
            String seconds = timeMillis < 1000 ? "< 1" : String.valueOf(timeMillis / 1000);
            LOGGER.log(UNIT_TEST, "Finished Execution in {} s ({} ms)", seconds, millis);

            failedTests = summary.getTestsFailedCount() > 0;
        }
        else
        {
            LOGGER.log(UNIT_TEST, "Did not discover any tests to run.");
        }
    }

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan)
    {
        LOGGER.log(UNIT_TEST, HR);
        LOGGER.log(UNIT_TEST, "Running Test Plan with {} test(s)", testPlan.countTestIdentifiers(TestIdentifier::isTest));
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan)
    {
        LOGGER.log(UNIT_TEST, "Finished Test Plan");
    }

    @Override
    public void executionSkipped(TestIdentifier testIdentifier, String reason)
    {
        if (testIdentifier.isTest())
        {
            LOGGER.log(UNIT_TEST, "{}. Skipped {} due to {}", testCounter, getDisplayName(testIdentifier), reason);
            testCounter++;
        }
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier)
    {
        if (testIdentifier.isTest())
        {
            LOGGER.log(UNIT_TEST, "{}. Running {}", testCounter, getDisplayName(testIdentifier));
            testCounter++;
        }
    }

    public boolean hasFailedTests()
    {
        return failedTests;
    }

    private String getDisplayName(TestIdentifier testIdentifier)
    {
        TestSource source = testIdentifier.getSource().orElse(null);
        if (source instanceof ClassSource)
        {
            return "Class " + ((ClassSource) source).getClassName();
        }
        else if (source instanceof MethodSource)
        {
            MethodSource method = (MethodSource) source;
            return "Method " + method.getClassName() + "#" + method.getMethodName() + "(" + method.getMethodParameterTypes() + ")";
        }
        else if (source instanceof PackageSource)
        {
            return "Package " + ((PackageSource) source).getPackageName();
        }
        else if (testIdentifier.isContainer())
        {
            return "Container " + testIdentifier.getDisplayName();
        }
        else if (testIdentifier.isTest())
        {
            return "Test " + testIdentifier.getDisplayName();
        }
        else
        {
            return testIdentifier.getDisplayName();
        }
    }
}
