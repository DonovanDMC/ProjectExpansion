package com.alcatrazescapee.mcjunitlib.framework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to mark dynamically created integration tests.
 * These are tests which are conducted by loading a world, building a structure from a saved structure NBT, and referencing a integration test class and method which set out requirements for what a success or fail look like.
 *
 * Any test method this is used on MUST take no parameters, and return an {@code Stream<DynamicIntegrationTest>}
 *
 * @see IntegrationTest
 * @see DynamicIntegrationTest
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface IntegrationTestFactory
{
    /**
     * If omitted the method name will be used instead
     * The NBT structure representing the test will be searched for at {testClass}/{testName}.nbt
     *
     * @return The name of this test.
     */
    String value() default "";

    /**
     * How long before this test will be deemed failed by time out.
     *
     * @return A number of ticks > 0, or -1 to indicate there is no timeout.
     */
    int timeoutTicks() default 200;

    /**
     * How often this test's conditions should be checked.
     * By default, they will be re-evaluated every 10 ticks.
     *
     * @return A number of ticks > 0
     */
    int refreshTicks() default 10;
}
