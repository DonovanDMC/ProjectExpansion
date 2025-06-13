package cool.furry.mc.neoforge.projectexpansion

import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*

import javax.inject.Inject

abstract class SignJar extends Exec {
    @Input
    final Provider<String> modId
    @Input
    final Provider<String> minecraftVersion
    @Input
    final Provider<String> modVersion
    @InputFile
    final RegularFileProperty inputFile
    @InputFile
    final RegularFileProperty scriptFile
    @InputFile
    final RegularFileProperty secretsFile
    @OutputFile
    final RegularFileProperty outputFile
    SignJar() {
        modId = providerFactory.gradleProperty("mod_id")
        minecraftVersion = providerFactory.gradleProperty("minecraft_version")
        modVersion = providerFactory.gradleProperty("mod_version")
        inputFile = objectFactory.fileProperty().convention(projectLayout.buildDirectory.file("libs/${modId.get()}-${minecraftVersion.get()}-${modVersion.get()}.jar"))
        scriptFile = objectFactory.fileProperty().convention(projectLayout.projectDirectory.file("scripts/sign-jar.sh"))
        secretsFile = objectFactory.fileProperty().convention(projectLayout.projectDirectory.file("scripts/secrets.sh"))
        outputFile = objectFactory.fileProperty().convention(projectLayout.buildDirectory.file("libs/signed/${modId.get()}-${minecraftVersion.get()}-${modVersion.get()}.jar"))
    }

    @Inject
    protected abstract ProviderFactory getProviderFactory()

    @Inject
    protected abstract ObjectFactory getObjectFactory()

    @Inject
    protected abstract ProjectLayout getProjectLayout()

    @TaskAction
    @Override
    void exec() {
        if (!secretsFile.get().asFile.exists()) {
            logger.warn("Secrets file is missing, not signing jars")
            return
        }

        setCommandLine(scriptFile.get(), inputFile.get().asFile.name)

        super.exec()
    }
}
