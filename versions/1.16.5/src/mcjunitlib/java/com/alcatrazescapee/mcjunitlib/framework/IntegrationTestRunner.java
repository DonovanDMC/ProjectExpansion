package com.alcatrazescapee.mcjunitlib.framework;

import java.util.function.Consumer;

import net.minecraft.util.ResourceLocation;

class IntegrationTestRunner
{
    private final Class<?> clazz;
    private final Consumer<IntegrationTestHelper> testAction;
    private final String testName;
    private final ResourceLocation templateName;

    private final int refreshTicks;
    private final int timeoutTicks;

    IntegrationTestRunner(Class<?> clazz, Consumer<IntegrationTestHelper> testAction, String testName, ResourceLocation templateName, int refreshTicks, int timeoutTicks)
    {
        this.clazz = clazz;
        this.testAction = testAction;
        this.testName = testName;
        this.templateName = templateName;
        this.refreshTicks = refreshTicks;
        this.timeoutTicks = timeoutTicks;
    }

    String getName()
    {
        return testName;
    }

    ResourceLocation getTemplateName()
    {
        return templateName;
    }

    String getClassName()
    {
        return clazz.getName();
    }

    int getTimeoutTicks()
    {
        return timeoutTicks;
    }

    int getRefreshTicks()
    {
        return refreshTicks;
    }

    Consumer<IntegrationTestHelper> getTestAction()
    {
        return testAction;
    }
}
