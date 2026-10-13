package com.alcatrazescapee.mcjunitlib.framework;

import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.commons.lang3.mutable.MutableInt;

/**
 * @see IntegrationTestFactory
 */
public class DynamicIntegrationTest
{
    /**
     * Create a single dynamic integration test with a friendly name
     *
     * @param name The name of the test instance
     * @param test The test to perform
     * @return A dynamic integration test
     */
    public static DynamicIntegrationTest create(String name, Consumer<IntegrationTestHelper> test)
    {
        return new DynamicIntegrationTest(name, test);
    }

    /**
     * Create a sequentially numbered stream of dynamic integration tests
     *
     * @param stream A stream of test instances
     * @return A stream of dynamic integration tests numbered 1, 2, 3...
     */
    public static Stream<DynamicIntegrationTest> create(Stream<Consumer<IntegrationTestHelper>> stream)
    {
        final MutableInt counter = new MutableInt(0);
        return stream.map(test -> {
            counter.add(1);
            return new DynamicIntegrationTest(String.valueOf(counter.intValue()), test);
        });
    }

    private final String name;
    private final Consumer<IntegrationTestHelper> test;

    private DynamicIntegrationTest(String name, Consumer<IntegrationTestHelper> test)
    {
        this.name = name;
        this.test = test;
    }

    String getName()
    {
        return name;
    }

    Consumer<IntegrationTestHelper> getTestAction()
    {
        return test;
    }
}
