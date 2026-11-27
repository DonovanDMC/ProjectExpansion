plugins {
    java
    id("org.jetbrains.intellij.platform") version "2.20.1"
}

group = "cool.furry.mc.projectexpansion"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// built against the IntelliJ that's installed, rather than downloading another, -PideaHome=... for one somewhere else
val ideaHome = providers.gradleProperty("ideaHome").getOrElse("${System.getProperty("user.home")}/.local/share/JetBrains/Toolbox/apps/intellij-idea-ultimate")

dependencies {
    intellijPlatform {
        local(ideaHome)
        bundledPlugin("com.intellij.java")
        bundledPlugin("com.intellij.gradle")
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

intellijPlatform {
    // nothing here needs them, and they're slow to build
    buildSearchableOptions = false
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "253"
            untilBuild = provider { null }
        }
    }
}
