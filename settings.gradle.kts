pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.neoforged.net/releases/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.kikugie.stonecutter") version "0.9.8"
}

rootProject.name = "biggerreactorsrework"

stonecutter {
    create(rootProject) {
        versions("1.21.1", "26.1.2", "26.3")
        vcsVersion = "26.1.2"
        centralScript = "build.gradle"
    }
}
