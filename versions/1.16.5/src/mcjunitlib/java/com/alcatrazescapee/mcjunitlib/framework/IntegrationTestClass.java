package com.alcatrazescapee.mcjunitlib.framework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This is an optional annotation which can be applied to a test class to specify the structure folder which is used to locate test structures.
 *
 * @see IntegrationTest
 * @see IntegrationTestHelper
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface IntegrationTestClass
{
    /**
     * If this annotation is omitted the method name will be used instead
     * The NBT structure representing the test will be searched for at {testClass}/{testName}.nbt
     *
     * @return The name of the test class.
     */
    String value();
}
