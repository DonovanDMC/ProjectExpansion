/*
 * Part of MCJUnitLib by AlcatrazEscapee
 * Work under Copyright. See the project LICENSE.md for details.
 */

package com.alcatrazescapee.mcjunitlib.service;

import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraftforge.userdev.FMLUserdevServerLaunchProvider;

import cpw.mods.modlauncher.api.ITransformingClassLoader;

import static net.minecraftforge.fml.loading.LogMarkers.CORE;

public class FMLTestServerLaunchProvider extends FMLUserdevServerLaunchProvider
{
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String name()
    {
        return "fmltestserver";
    }

    @Override
    public Callable<Void> launchService(String[] arguments, ITransformingClassLoader launchClassLoader)
    {
        return () -> {
            LOGGER.debug(CORE, "Launching test server in {} with arguments {}", launchClassLoader, arguments);
            super.beforeStart(launchClassLoader);
            launchClassLoader.addTargetPackageFilter(getPackagePredicate());
            Thread.currentThread().setContextClassLoader(launchClassLoader.getInstance());
            Class.forName("com.alcatrazescapee.mcjunitlib.TestMain", true, launchClassLoader.getInstance()).getMethod("main", String[].class).invoke(null, (Object)arguments);
            return null;
        };
    }
}
