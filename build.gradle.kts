plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    id("com.gradleup.shadow") version "8.+"
    application
}

group = "de.uniwuerzburg.omosimvisualizer"
version = "1.0"

val lwjglVersion = "3.3.4"
val jomlVersion = "1.10.7"
val lwjglNatives = "natives-windows"

dependencies {
    implementation("org.geotools:gt-epsg-hsql:31.+")
    implementation("org.geotools:gt-main:31.+")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.openstreetmap.osmosis:osmosis-pbf:0.48.+")
    implementation("org.openstreetmap.osmosis:osmosis-areafilter:0.48.+")
    implementation("org.locationtech.jts:jts-core:1.+")

    implementation("com.github.L-Strobel:omosim:v2.2.1")
    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))

    implementation("org.lwjgl:lwjgl")
    implementation("org.lwjgl:lwjgl-assimp")
    implementation("org.lwjgl:lwjgl-glfw")
    implementation("org.lwjgl:lwjgl-openal")
    implementation("org.lwjgl:lwjgl-opengl")
    implementation("org.lwjgl:lwjgl-stb")

    runtimeOnly("org.lwjgl:lwjgl::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-assimp::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-glfw::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-openal::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-opengl::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-stb::$lwjglNatives")

    implementation("org.joml", "joml", jomlVersion)

    implementation("org.orbisgis:poly2tri-core:0.+")

    implementation("com.github.ajalt.clikt:clikt:4.+")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.shadowJar {
    archiveBaseName.set("omosim-visualizer-${lwjglNatives}")
    mergeServiceFiles()
}

application {
    mainClass.set("de.uniwuerzburg.omosimvisualizer.MainKt")
}
