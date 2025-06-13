package cool.furry.mc.neoforge.projectexpansion

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory

import javax.inject.Inject

abstract class Data extends Exec {
    @InputFile
    final RegularFile script
    @InputDirectory
    final DirectoryProperty inputDirectory
    @OutputDirectory
    final DirectoryProperty outputDirectory
    Data() {
        script = projectLayout.projectDirectory.file("scripts/generate-assets.sh")
        inputDirectory = getObjectFactory().directoryProperty().convention(projectLayout.projectDirectory.dir("src/main/generation"))
        outputDirectory = getObjectFactory().directoryProperty().convention(projectLayout.projectDirectory.dir("src/generated/resources"))
        setCommandLine(script.getAsFile().getAbsolutePath())
    }

    @Inject
    protected abstract ObjectFactory getObjectFactory()

    @Inject
    protected abstract ProjectLayout getProjectLayout()
}