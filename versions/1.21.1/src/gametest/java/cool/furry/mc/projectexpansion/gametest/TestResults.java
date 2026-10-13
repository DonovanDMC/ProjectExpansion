package cool.furry.mc.projectexpansion.gametest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import cool.furry.mc.projectexpansion.Main;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.LogTestReporter;
import net.minecraft.gametest.framework.TestReporter;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// writes each game test's result to the file named by the projectexpansion.testResults property, which the build turns into a report
// the file is rewritten after every result, so a run that crashes still leaves what finished
@EventBusSubscriber(modid = Main.MOD_ID)
public class TestResults implements TestReporter {
    private static final String PROPERTY = "projectexpansion.testResults";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().create();
    private static final Type GAME_TEST = Type.getType(GameTest.class);

    private final Path file;
    // logging is what the default reporter does, so it's kept
    private final TestReporter log = new LogTestReporter();
    // test names are the method name in lowercase, these give back where the test came from
    private final Map<String, Source> sources = new HashMap<>();
    private final List<Map<String, Object>> tests = new ArrayList<>();
    private final String startedAt = Instant.now().toString();

    private record Source(String group, String name, String batch) {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        String path = System.getProperty(PROPERTY);
        if (path != null) GlobalTestReporter.replaceWith(new TestResults(Paths.get(path)));
    }

    private TestResults(Path file) {
        this.file = file;
        for (ModFileScanData scanData : ModList.get().getAllScanData()) {
            for (ModFileScanData.AnnotationData annotation : scanData.getAnnotations()) {
                if (!annotation.annotationType().equals(GAME_TEST)) continue;
                String className = annotation.clazz().getClassName();
                String method = annotation.memberName().substring(0, annotation.memberName().indexOf('('));
                Object batch = annotation.annotationData().get("batch");
                sources.put(method.toLowerCase(Locale.ROOT), new Source(className.substring(className.lastIndexOf('.') + 1), method, batch == null ? "defaultBatch" : batch.toString()));
            }
        }
        write(false);
    }

    @Override
    public void onTestSuccess(GameTestInfo info) {
        log.onTestSuccess(info);
        add(info, "passed", null);
    }

    @Override
    public void onTestFailed(GameTestInfo info) {
        log.onTestFailed(info);
        Throwable error = info.getError();
        add(info, info.isRequired() ? "failed" : "optional_failed", error == null ? null : error.getMessage() == null ? error.toString() : error.getMessage());
    }

    @Override
    public void finish() {
        log.finish();
        write(true);
    }

    private void add(GameTestInfo info, String status, String message) {
        Source source = sources.getOrDefault(info.getTestName(), new Source("Unknown", info.getTestName(), "unknown"));
        Map<String, Object> test = new LinkedHashMap<>();
        test.put("kind", "gametest");
        test.put("group", source.group());
        test.put("name", source.name());
        test.put("batch", source.batch());
        test.put("status", status);
        test.put("message", message);
        test.put("durationMs", info.getRunTime());
        tests.add(test);
        write(false);
    }

    private void write(boolean complete) {
        Map<String, Object> results = new LinkedHashMap<>();
        results.put("framework", "gametest");
        results.put("startedAt", startedAt);
        results.put("finishedAt", Instant.now().toString());
        results.put("complete", complete);
        results.put("tests", tests);
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, GSON.toJson(results).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Couldn't write the test results to " + file, e);
        }
    }
}
