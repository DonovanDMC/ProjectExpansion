package cool.furry.mc.neoforge.projectexpansion

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import javax.inject.Inject

// based off of ProjectE's UpdateJson task
// https://github.com/sinkillerj/projecte/blob/653b0d5b5d0a0b3eb62ac36348320d07d9557bf8/buildSrc/src/main/groovy/moze_intel/projecte/UpdateJson.groovy
abstract class UpdateJson extends DefaultTask {
    @Input
    final Provider<String> minecraftVersion
    @Input
    final Provider<String> modVersion
    @OutputFile
    final RegularFileProperty outputFile

    UpdateJson() {
        minecraftVersion = providerFactory.gradleProperty("minecraft_version")
        modVersion = providerFactory.gradleProperty("mod_version")
        outputFile = objectFactory.fileProperty().convention(projectLayout.projectDirectory.file("updates.json"))
    }

    @Inject
    protected abstract ProviderFactory getProviderFactory()

    @Inject
    protected abstract ObjectFactory getObjectFactory()

    @Inject
    protected abstract ProjectLayout getProjectLayout()

    @TaskAction
    void execute() {
        def updateJsonFile = outputFile.get().asFile
        def updateJson = new JsonSlurper().parse(updateJsonFile) as Map

        String minecraftVersion = this.minecraftVersion.get()
        String modVersion = this.modVersion.get()

        def versionData = updateJson[minecraftVersion]
        if (versionData == null) {
            versionData = [:] as Map
            updateJson.put(minecraftVersion, versionData)
        }

        String version = "${minecraftVersion}-${modVersion}"
        versionData[version] = "https://github.com/DonovanDMC/ProjectExpansion/releases/tag/${version}"

        // Update promos
        updateJson.promos."${minecraftVersion}-latest" = version
        updateJson.promos."${minecraftVersion}-recommended" = version

        updateJsonFile.text = JsonOutput.prettyPrint(JsonOutput.toJson(updateJson))
    }
}