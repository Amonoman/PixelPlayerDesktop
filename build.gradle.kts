plugins {
    kotlin("jvm") version "2.4.20"
    kotlin("plugin.serialization") version "2.4.20"
    id("org.jetbrains.compose") version "1.12.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20"
}

val appVersionName: String = providers.gradleProperty("APP_VERSION_NAME").getOrElse("0.1.0")
val windowsVersion: String = run {
    val base = appVersionName.substringBefore("-").trim()
    val parts = base.split(".").mapNotNull { it.toIntOrNull() }
    when {
        parts.size >= 3 -> "${parts[0]}.${parts[1]}.${parts[2]}"
        parts.size == 2 -> "${parts[0]}.${parts[1]}.0"
        parts.size == 1 -> "${parts[0]}.0.0"
        else -> "0.1.0"
    }
}

group = "com.kayoi.pixelplayerdesktop"
version = appVersionName

repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.1")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

    // DI (Koin)
    implementation("io.insert-koin:koin-core:4.0.2")
    implementation("io.insert-koin:koin-compose:4.0.2")

    // Database (SQLite JDBC)
    implementation("org.xerial:sqlite-jdbc:3.47.2.0")

    // Audio Tag Reader & VLCJ Audio Engine
    implementation("net.jthink:jaudiotagger:3.0.1")
    implementation("org.gagravarr:vorbis-java-core:0.8")
    implementation("uk.co.caprica:vlcj:4.8.3")

    // Reorderable LazyList (Same library as OSS)
    implementation("sh.calvin.reorderable:reorderable:2.4.3")

    // MaterialKolor (M3 Dynamic Colors & Quantizer)
    implementation("com.materialkolor:material-kolor:2.0.2")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("runCli") {
    group = "application"
    description = "Runs the CLI debug verification runner"
    mainClass.set("com.kayoi.pixelplayerdesktop.cli.CliRunnerKt")
    classpath = sourceSets["main"].runtimeClasspath
    standardInput = System.`in`
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            modules("java.sql", "java.naming", "java.desktop", "jdk.unsupported")
            appResourcesRootDir.set(project.layout.projectDirectory.dir("src/main/resources-packaged"))

            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe
            )

            packageName = "pixelplayer"
            packageVersion = windowsVersion
            description = "PixelPlayer Desktop - Modern Material 3 Music Player"
            vendor = "Kayoi"
            copyright = "Copyright (C) 2026 Theo Vilardo"

            linux {
                iconFile.set(project.file("src/main/resources/icons/icon.png"))
                debPackageVersion = appVersionName
                debMaintainer = "Kayoi <adlifarizi>"
                menuGroup = "AudioVideo;Player;Audio;"
                appCategory = "AudioVideo"
            }

            windows {
                iconFile.set(project.file("src/main/resources/icons/icon.ico"))
                packageVersion = windowsVersion
                msiPackageVersion = windowsVersion
                exePackageVersion = windowsVersion
                menuGroup = "Kayoi"
                upgradeUuid = "d7bfb9d0-6243-4f9e-a81d-e4b5efc4e723"
            }
        }
    }
}
