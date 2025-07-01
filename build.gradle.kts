plugins {
    kotlin("jvm") version "1.9.23"
}

group = "de.uniwuerzburg.omodvisualizer"
version = "1.0-SNAPSHOT"

val lwjglVersion = "3.3.4"
val jomlVersion = "1.10.7"
val lwjglNatives = "natives-windows"

repositories {
    mavenCentral()
}

dependencies {
    implementation(files("debugJar/omod-2.0.15-all.jar"))
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

    implementation("org.locationtech.jts:jts-core:1.+")
    implementation("org.orbisgis:poly2tri-core:0.+")
    implementation("org.joml", "joml", jomlVersion)

    implementation("com.github.ajalt.clikt:clikt:4.+")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}