import org.gradle.kotlin.dsl.maven

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
rootProject.name = "omod-visualizer"

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            url = uri("https://repo.osgeo.org/repository/release/")
        }
        maven {
            url = uri("https://repo.osgeo.org/repository/snapshot/")
        }
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}