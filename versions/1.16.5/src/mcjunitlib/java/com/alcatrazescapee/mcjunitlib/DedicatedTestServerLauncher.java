/*
 * Part of MCJUnitLib by AlcatrazEscapee
 * Work under Copyright. See the project LICENSE.md for details.
 */

package com.alcatrazescapee.mcjunitlib;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.modlauncher.Launcher;

public class DedicatedTestServerLauncher
{
    private static final Level UNIT_TEST = Level.forName("UNITTEST", 50);
    private static final Logger LOGGER = LogManager.getLogger();

    public static void main(String[] args)
    {
        final String markerSelection = System.getProperty("forge.logging.markers", "");
        Arrays.stream(markerSelection.split(",")).forEach(marker -> System.setProperty("forge.logging.marker."+ marker.toLowerCase(Locale.ROOT), "ACCEPT"));

        LOGGER.log(UNIT_TEST, "TestServerLauncher Starting");
        String[] arguments;
        try
        {
            // Since forge made this class package private, and I don't care to copy paste, here we go with a pile of reflection!
            Class<?> argListClass = Class.forName("net.minecraftforge.userdev.ArgumentList");
            Method constructor = argListClass.getMethod("from", String[].class);
            Method putLazy = argListClass.getMethod("putLazy", String.class, String.class);
            Method getArguments = argListClass.getMethod("getArguments");

            constructor.setAccessible(true);
            putLazy.setAccessible(true);
            getArguments.setAccessible(true);

            Object argumentList = constructor.invoke(null, (Object) args);

            putLazy.invoke(argumentList, "gameDir", ".");
            putLazy.invoke(argumentList, "fml.mcpVersion", System.getenv("MCP_VERSION"));
            putLazy.invoke(argumentList, "fml.mcVersion", System.getenv("MC_VERSION"));
            putLazy.invoke(argumentList, "fml.forgeGroup", System.getenv("FORGE_GROUP"));
            putLazy.invoke(argumentList, "fml.forgeVersion", System.getenv("FORGE_VERSION"));

            // Testing specific options
            putLazy.invoke(argumentList, "launchTarget", "fmltestserver");

            arguments = (String[]) getArguments.invoke(argumentList);
            LOGGER.log(UNIT_TEST, "Launching Launcher with arguments: " + String.join(", ", arguments));
        }
        catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException | ClassNotFoundException e)
        {
            LOGGER.log(UNIT_TEST, "Forge is calling, they want their ArgumentList back. This is a bug!");
            LOGGER.log(UNIT_TEST, "Error: ", e);
            return;
        }

        Launcher.main(arguments);
    }
}
