package cool.furry.mc.projectexpansion

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

// Runs the bun asset generator (src/generation) for one Minecraft version
abstract class Data extends Exec {
    @InputFile
    abstract RegularFileProperty getScriptFile()
    @InputFiles
    abstract ConfigurableFileCollection getInputFiles()
    @Input
    abstract Property<String> getMinecraftVersion()
    @Input
    abstract Property<String> getLoader()
    @OutputDirectory
    abstract DirectoryProperty getOutputDirectory()

    @TaskAction
    @Override
    void exec() {
        setCommandLine(scriptFile.get(), minecraftVersion.get(), loader.get(), outputDirectory.get())
        super.exec()
    }
}
