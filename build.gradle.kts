plugins {
    id("java")
}

group = "com.snazzah"
version = "1.0.0"

repositories {
    mavenCentral()
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

val isLinux = System.getProperty("os.name").lowercase().contains("linux")

val hytaleBasePath = System.getenv("HYTALE_PATH")
    ?: (System.getProperty("user.home") + if (isLinux)
        "/.var/app/com.hypixel.HytaleLauncher/data/Hytale"
    else
        "/AppData/Roaming/Hytale")

val patchline = System.getenv("HYTALE_PATCHLINE") ?: "release"

val hytaleJar = "$hytaleBasePath/install/$patchline/package/game/latest/Server/HytaleServer.jar"
//val hytaleSourcesJar = "$hytaleBasePath/install/$patchline/package/game/latest/Server/HytaleServer-Sources.jar"

dependencies {
    // Local HytaleServer jar
    compileOnly(files(file("libs/HytaleServer.jar")))
//    compileOnly(files(hytaleJar))
}