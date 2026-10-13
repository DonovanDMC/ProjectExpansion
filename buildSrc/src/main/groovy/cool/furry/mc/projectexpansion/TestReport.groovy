package cool.furry.mc.projectexpansion

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskAction

import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Turns the results of the test runs in this build into reports/<time>/report.md and report.json, one section per version
// Runs after any version's tests, alone or together through gameTest, and fails the build if a version's tests did
abstract class TestReport extends DefaultTask {
    static final Map<String, String> LOADERS = [forge: "Forge", neoforge: "NeoForge"]
    static final Map<String, String> FRAMEWORKS = [gametest: "Game tests", mcjunitlib: "MCJUnitLib"]
    static final Map<String, String> STATUS_ICONS = [passed: "✅", failed: "❌", aborted: "❌", optional_failed: "⚠️", skipped: "⏭️"]
    static final Map<String, String> RESULTS = [passed: "✅ Passed", failed: "❌ Failed", crashed: "💥 Crashed", not_run: "⏭️ Not run"]

    static class Run {
        String version
        String loader
        JavaExec task
        File results
    }

    @Internal
    final List<Run> runs = []

    @Internal
    abstract DirectoryProperty getReportsDirectory()

    TestReport() {
        outputs.upToDateWhen { false }
    }

    void run(String version, String loader, JavaExec task, File results) {
        runs << new Run(version: version, loader: loader, task: task, results: results)
    }

    @TaskAction
    void generate() {
        List<Run> ran = runs.findAll { project.gradle.taskGraph.hasTask(it.task) }
        if (ran.isEmpty()) return
        List<Map> versions = ran.sort { a, b -> compareVersions(a.version, b.version) }.collect { version(it) }

        Map report = [
            generatedAt: Instant.now().toString(),
            result     : versions.every { it.result == "passed" } ? "passed" : "failed",
            summary    : summarize(versions*.summary) + [versions: versions.size()],
            versions   : versions,
        ]
        File dir = reportsDirectory.dir(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))).get().asFile
        dir.mkdirs()
        File jsonFile = new File(dir, "report.json"), markdownFile = new File(dir, "report.md")
        jsonFile.setText(JsonOutput.prettyPrint(JsonOutput.toJson(report)) + "\n", "UTF-8")
        markdownFile.setText(markdown(report), "UTF-8")
        logger.lifecycle("Test report: ${markdownFile.toURI()}")

        List<Map> failed = versions.findAll { it.result != "passed" }
        if (failed) throw new GradleException("Tests didn't pass for ${failed.collect { "${it.version} (${it.result.replace('_', ' ')})" }.join(', ')}, see ${markdownFile}")
    }

    static Map version(Run run) {
        Integer exitCode = run.task.executionResult.getOrNull()?.exitValue
        Map results = run.results.isFile() ? new JsonSlurper().parse(run.results) as Map : null
        List<Map> tests = (results?.tests ?: []) as List<Map>
        Map summary = summarize(tests)
        String result
        if (exitCode == null) result = "not_run"
        else if (summary.failed > 0) result = "failed"
        else if (exitCode != 0 || results == null || !results.complete) result = "crashed"
        else result = "passed"

        List<Map> groups = tests.groupBy { it.group }.collect { name, groupTests ->
            [name: name, kind: groupTests[0].kind, summary: summarize(groupTests), tests: groupTests.sort { it.name }]
        }.sort { it.name }
        return [
            version   : run.version,
            loader    : run.loader,
            framework : results?.framework,
            result    : result,
            exitCode  : exitCode,
            complete  : results?.complete ?: false,
            startedAt : results?.startedAt,
            finishedAt: results?.finishedAt,
            durationMs: results ? Duration.between(Instant.parse(results.startedAt as String), Instant.parse(results.finishedAt as String)).toMillis() : null,
            summary   : summary,
            failures  : tests.findAll { it.status in ["failed", "aborted", "optional_failed"] },
            groups    : groups,
        ]
    }

    // works for both lists of tests and lists of summaries
    static Map summarize(List<Map> items) {
        if (items.every { it.containsKey("total") }) {
            return [total: 0, passed: 0, failed: 0, optionalFailed: 0, skipped: 0].collectEntries { key, value -> [key, items.sum { it[key] } ?: 0] }
        }
        return [
            total         : items.size(),
            passed        : items.count { it.status == "passed" },
            failed        : items.count { it.status in ["failed", "aborted"] },
            optionalFailed: items.count { it.status == "optional_failed" },
            skipped       : items.count { it.status == "skipped" },
        ]
    }

    static String markdown(Map report) {
        List<Map> versions = report.versions as List<Map>
        Map summary = report.summary as Map
        StringBuilder out = new StringBuilder("# Test report\n\n")
        String headline = report.result == "passed" ? "✅ **All ${summary.total} tests passed**" : "❌ **${summary.failed} of ${summary.total} tests failed**"
        if (report.result != "passed" && summary.failed == 0) headline = "💥 **A test run didn't finish**"
        out << "${headline} across ${versions.size()} version${versions.size() == 1 ? '' : 's'}, generated ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}\n\n"

        out << "| Version | Loader | Result | Passed | Failed | Total | Time |\n| --- | --- | --- | ---: | ---: | ---: | ---: |\n"
        versions.each { Map version ->
            Map s = version.summary as Map
            out << "| [${version.version}](#${anchor(version)}) | ${loader(version)} | ${RESULTS[version.result]} | ${s.passed} | ${s.failed} | ${s.total} | ${time(version.durationMs as Long)} |\n"
        }

        List<Map> failing = versions.findAll { it.failures || it.result in ["crashed", "not_run"] }
        if (failing) {
            out << "\n## Failures\n"
            failing.each { Map version ->
                if (version.result == "crashed") out << "\n### ${version.version}\n\n${crashed(version)}\n"
                if (version.result == "not_run") out << "\n### ${version.version}\n\nThe tests didn't run, as something they depend on failed first.\n"
                (version.failures as List<Map>).each { Map test ->
                    out << "\n### ${version.version} › ${test.group} › `${test.name}`${test.status == 'optional_failed' ? ' (optional)' : ''}\n\n"
                    out << "```\n${(test.message ?: "No message").toString().trim()}\n```\n"
                }
            }
        }

        versions.each { Map version ->
            Map s = version.summary as Map
            out << "\n## ${version.version} · ${loader(version)}\n\n"
            out << "${RESULTS[version.result]} · ${FRAMEWORKS[version.framework] ?: 'No results'} · ${s.passed}/${s.total} passed"
            if (s.optionalFailed) out << " · ${s.optionalFailed} optional failed"
            if (s.skipped) out << " · ${s.skipped} skipped"
            out << " · ${time(version.durationMs as Long)}\n"
            if (version.result == "crashed") out << "\n${crashed(version)}\n"
            (version.groups as List<Map>).each { Map group ->
                Map g = group.summary as Map
                out << "\n### ${group.name} (${g.passed}/${g.total})\n\n| Test | Result | Time |\n| --- | --- | ---: |\n"
                (group.tests as List<Map>).each { Map test ->
                    String note = test.status == "passed" ? "" : " ${firstLine(test.message as String)}"
                    out << "| `${test.name}` | ${STATUS_ICONS[test.status] ?: test.status}${note} | ${time(test.durationMs as Long)} |\n"
                }
            }
        }
        return out.toString()
    }

    static String crashed(Map version) {
        String exit = version.exitCode == null ? "" : " with exit code ${version.exitCode}"
        return version.complete ? "The run exited${exit}, but no test failed." : "The run stopped${exit} before every test finished, the tests listed are the ones that did."
    }

    static String loader(Map version) {
        return LOADERS[version.loader] ?: version.loader
    }

    // github style heading anchors
    static String anchor(Map version) {
        return "${version.version} · ${loader(version)}".toLowerCase().replaceAll(/[^a-z0-9 \-]/, "").replace(" ", "-")
    }

    // keeps a failure message to one line, so it fits in a table cell
    static String firstLine(String message) {
        if (!message) return ""
        String line = message.trim().readLines()[0]
        return line.replace("|", "\\|").take(200)
    }

    static String time(Long ms) {
        if (ms == null) return "–"
        if (ms < 1000) return "${ms} ms"
        if (ms < 60_000) return String.format("%.1f s", ms / 1000d)
        return "${ms.intdiv(60_000)} m ${(ms % 60_000).intdiv(1000)} s"
    }

    static int compareVersions(String a, String b) {
        List<Integer> x = a.tokenize(".")*.toInteger(), y = b.tokenize(".")*.toInteger()
        for (int i = 0; i < Math.max(x.size(), y.size()); i++) {
            int compared = (x[i] ?: 0) <=> (y[i] ?: 0)
            if (compared != 0) return compared
        }
        return 0
    }
}
