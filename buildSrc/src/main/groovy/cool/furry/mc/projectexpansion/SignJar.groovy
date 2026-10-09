package cool.furry.mc.projectexpansion

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

abstract class SignJar extends Exec {
    @InputFile
    abstract RegularFileProperty getInputFile()
    @InputFile
    abstract RegularFileProperty getScriptFile()
    // not an @InputFile, a missing secrets file just skips signing
    @Internal
    abstract RegularFileProperty getSecretsFile()
    @OutputFile
    abstract RegularFileProperty getOutputFile()

    @TaskAction
    @Override
    void exec() {
        if (!secretsFile.get().asFile.exists()) {
            logger.warn("Secrets file is missing, not signing jars")
            return
        }

        setCommandLine(scriptFile.get(), secretsFile.get(), inputFile.get(), outputFile.get())
        // jarsigner isn't always on PATH, fall back to the jdk gradle is running on
        if (!environment.containsKey("JAVA_HOME")) {
            environment("JAVA_HOME", System.getProperty("java.home"))
        }

        super.exec()
    }
}
