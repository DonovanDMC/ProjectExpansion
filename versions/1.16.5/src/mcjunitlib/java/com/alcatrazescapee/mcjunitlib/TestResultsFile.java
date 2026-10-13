/*
 * Part of MCJUnitLib by AlcatrazEscapee
 * Work under Copyright. See the project LICENSE.md for details.
 */

package com.alcatrazescapee.mcjunitlib;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Writes each unit and integration test's result to the file named by the mcjunitlib.resultsFile system property, so a build can report on them.
 * The file is rewritten after every result, so a run that crashes still leaves what finished.
 */
public final class TestResultsFile
{
    private static final String PROPERTY = "mcjunitlib.resultsFile";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().create();
    private static final List<Map<String, Object>> TESTS = new ArrayList<>();
    private static final String STARTED_AT = Instant.now().toString();

    public static synchronized void add(String kind, String group, String name, String status, @Nullable String message, long durationMs)
    {
        Map<String, Object> test = new LinkedHashMap<>();
        test.put("kind", kind);
        test.put("group", group);
        test.put("name", name);
        test.put("status", status);
        test.put("message", message);
        test.put("durationMs", durationMs);
        TESTS.add(test);
        write(false);
    }

    public static synchronized void complete()
    {
        write(true);
    }

    private static void write(boolean complete)
    {
        String path = System.getProperty(PROPERTY);
        if (path == null) return;
        Path file = Paths.get(path);
        Map<String, Object> results = new LinkedHashMap<>();
        results.put("framework", "mcjunitlib");
        results.put("startedAt", STARTED_AT);
        results.put("finishedAt", Instant.now().toString());
        results.put("complete", complete);
        results.put("tests", TESTS);
        try
        {
            Files.createDirectories(file.getParent());
            Files.write(file, GSON.toJson(results).getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("Couldn't write the test results to " + file, e);
        }
    }

    private TestResultsFile() {}
}
