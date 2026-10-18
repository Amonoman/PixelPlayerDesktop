import java.net.URI

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

    // HTTP Client (OkHttp)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // DI (Koin)
    implementation("io.insert-koin:koin-core:4.0.2")
    implementation("io.insert-koin:koin-compose:4.0.2")

    // Database (SQLite JDBC)
    implementation("org.xerial:sqlite-jdbc:3.47.2.0")

    // Audio Tag Reader & VLCJ Audio Engine
    implementation("net.jthink:jaudiotagger:3.0.1")
    implementation("org.gagravarr:vorbis-java-core:0.8")
    implementation("uk.co.caprica:vlcj:4.8.3")

    // Reorderable LazyList
    implementation("sh.calvin.reorderable:reorderable:2.4.3")

    // MaterialKolor (M3 Dynamic Colors & Quantizer)
    implementation("com.materialkolor:material-kolor:2.0.2")

    // Lyrics Romanization
    implementation("com.atilika.kuromoji:kuromoji-ipadic:0.9.0")
    implementation("com.belerweb:pinyin4j:2.5.1")
    implementation("com.ibm.icu:icu4j:76.1")

    // FileKit (Native File & Directory Picker)
    implementation("io.github.vinceglb:filekit-dialogs:0.16.0")

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

        jvmArgs(
            "-Xmx256m",
            "-XX:+UseG1GC",
            "-XX:G1PeriodicGCInterval=3000",
            "-XX:-ShrinkHeapInSteps"
        )

        buildTypes.release.proguard {
            configurationFiles.from(project.file("proguard-rules.pro"))
        }

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

tasks.register("setupVlcWindows") {
    group = "vlc"
    description = "Downloads and bundles stripped LibVLC for Windows"
    val targetDir = layout.projectDirectory.dir("src/main/resources-packaged/windows/vlc").asFile
    val vlcUrl = "https://download.videolan.org/pub/videolan/vlc/3.0.21/win64/vlc-3.0.21-win64.zip"
    val downloadDir = layout.buildDirectory.dir("downloads").get().asFile
    val zipFile = downloadDir.resolve("vlc-3.0.21-win64.zip")

    outputs.dir(targetDir)
    onlyIf { !targetDir.resolve("libvlc.dll").exists() }

    doLast {
        downloadDir.mkdirs()
        if (!zipFile.exists()) {
            println("Downloading LibVLC Windows x64 from $vlcUrl...")
            URI(vlcUrl).toURL().openStream().use { input ->
                zipFile.outputStream().use { output -> input.copyTo(output) }
            }
        }

        println("Extracting and stripping LibVLC to $targetDir...")
        targetDir.mkdirs()
        copy {
            from(zipTree(zipFile)) {
                include("vlc-3.0.21/libvlc.dll")
                include("vlc-3.0.21/libvlccore.dll")
                include("vlc-3.0.21/plugins/**")
                exclude("**/plugins/plugins.dat")

                // 1. GUI, video rendering & 3D engines
                exclude("**/plugins/gui/**")
                exclude("**/plugins/video_output/**")
                exclude("**/plugins/video_filter/**")
                exclude("**/plugins/video_chroma/**")
                exclude("**/plugins/video_splitter/**")
                exclude("**/plugins/spu/**")
                exclude("**/plugins/text_renderer/**")
                exclude("**/plugins/visualization/**")
                exclude("**/plugins/control/**")
                exclude("**/plugins/d3d9/**")
                exclude("**/plugins/d3d11/**")

                // 2. Transcoding, server streaming, recording & unused utilities
                exclude("**/plugins/access_output/**")
                exclude("**/plugins/stream_out/**")
                exclude("**/plugins/mux/**")
                exclude("**/plugins/services_discovery/**")
                exclude("**/plugins/stream_extractor/**")
                exclude("**/plugins/keystore/**")
                exclude("**/plugins/logger/**")
                exclude("**/plugins/lua/**")

                // 3. Capture devices, optical media, TV tuners & exotic network protocols
                exclude("**/plugins/access/*srt*")
                exclude("**/plugins/access/*vnc*")
                exclude("**/plugins/access/*dcp*")
                exclude("**/plugins/access/*bluray*")
                exclude("**/plugins/access/*dshow*")
                exclude("**/plugins/access/*dtv*")
                exclude("**/plugins/access/*sftp*")
                exclude("**/plugins/access/*cdda*")
                exclude("**/plugins/access/*live555*")
                exclude("**/plugins/access/*rtp*")
                exclude("**/plugins/access/*nfs*")
                exclude("**/plugins/access/*dvd*")
                exclude("**/plugins/access/*vcd*")
                exclude("**/plugins/access/*vdr*")
                exclude("**/plugins/access/*ftp*")
                exclude("**/plugins/access/*realrtsp*")
                exclude("**/plugins/access/*rist*")
                exclude("**/plugins/access/*mms*")
                exclude("**/plugins/access/*satip*")
                exclude("**/plugins/access/*smb*")
                exclude("**/plugins/access/*screen*")
                exclude("**/plugins/access/*shm*")
                exclude("**/plugins/access/*udp*")
                exclude("**/plugins/access/*sdp*")

                // 4. Video codecs & video hardware accelerators
                exclude("**/plugins/codec/*x264*")
                exclude("**/plugins/codec/*x265*")
                exclude("**/plugins/codec/*vpx*")
                exclude("**/plugins/codec/*aom*")
                exclude("**/plugins/codec/*dav1d*")
                exclude("**/plugins/codec/*schroedinger*")
                exclude("**/plugins/codec/*theora*")
                exclude("**/plugins/codec/*libmpeg2*")
                exclude("**/plugins/codec/*rawvideo*")
                exclude("**/plugins/codec/*rtpvideo*")
                exclude("**/plugins/codec/*jpeg*")
                exclude("**/plugins/codec/*png*")
                exclude("**/plugins/codec/*sdl_image*")
                exclude("**/plugins/codec/*crystalhd*")
                exclude("**/plugins/codec/*dxva2*")
                exclude("**/plugins/codec/*d3d11va*")
                exclude("**/plugins/codec/*qsv*")
                exclude("**/plugins/codec/*mft*")
                exclude("**/plugins/codec/*dmo*")

                // 5. Subtitles & closed captions
                exclude("**/plugins/codec/*libass*")
                exclude("**/plugins/codec/*zvbi*")
                exclude("**/plugins/codec/*kate*")
                exclude("**/plugins/codec/*subsdec*")
                exclude("**/plugins/codec/*subsusf*")
                exclude("**/plugins/codec/*substx3g*")
                exclude("**/plugins/codec/*spudec*")
                exclude("**/plugins/codec/*stl*")
                exclude("**/plugins/codec/*cvdsub*")
                exclude("**/plugins/codec/*cdg*")
                exclude("**/plugins/codec/*svcdsub*")
                exclude("**/plugins/codec/*textst*")
                exclude("**/plugins/codec/*t140*")
                exclude("**/plugins/codec/*ttml*")
                exclude("**/plugins/codec/*webvtt*")
                exclude("**/plugins/codec/*aribsub*")
                exclude("**/plugins/codec/*dvbsub*")
                exclude("**/plugins/codec/*scte*")
                exclude("**/plugins/codec/*cc*")
                exclude("**/plugins/codec/*oggspots*")
                exclude("**/plugins/codec/*sub*")

                // 6. Pure video & broadcast container demuxers
                exclude("**/plugins/demux/*avi*")
                exclude("**/plugins/demux/*mjpeg*")
                exclude("**/plugins/demux/*rawvid*")
                exclude("**/plugins/demux/*rawdv*")
                exclude("**/plugins/demux/*h26x*")
                exclude("**/plugins/demux/*diracsys*")
                exclude("**/plugins/demux/*vc1*")
                exclude("**/plugins/demux/*mpgv*")
                exclude("**/plugins/demux/*ts*")
                exclude("**/plugins/demux/*ps*")
                exclude("**/plugins/demux/*pva*")
                exclude("**/plugins/demux/*ty*")
                exclude("**/plugins/demux/*nuv*")
                exclude("**/plugins/demux/*nsv*")
                exclude("**/plugins/demux/*real*")
                exclude("**/plugins/demux/*nsc*")
                exclude("**/plugins/demux/*image*")
                exclude("**/plugins/demux/*chromecast*")
                exclude("**/plugins/demux/*subtitle*")
                exclude("**/plugins/demux/*stl*")
                exclude("**/plugins/demux/*vobsub*")
                exclude("**/plugins/demux/*cdg*")

                // 7. Video packetizers
                exclude("**/plugins/packetizer/*h264*")
                exclude("**/plugins/packetizer/*hevc*")
                exclude("**/plugins/packetizer/*av1*")
                exclude("**/plugins/packetizer/*mpegvideo*")
                exclude("**/plugins/packetizer/*mpeg4video*")
                exclude("**/plugins/packetizer/*vc1*")
                exclude("**/plugins/packetizer/*dirac*")

                // 8. Unneeded misc plugins
                exclude("**/plugins/misc/*audioscrobbler*")
                exclude("**/plugins/misc/*vod_rtsp*")
                exclude("**/plugins/misc/*fingerprinter*")
                exclude("**/plugins/misc/*addons*")
                exclude("**/plugins/misc/*stats*")
                eachFile {
                    path = path.substringAfter("vlc-3.0.21/")
                }
            }
            includeEmptyDirs = false
            into(targetDir)
        }
        println("LibVLC Windows bundled successfully (audio-only stripped).")
    }
}

tasks.register("setupVlcLinux") {
    group = "vlc"
    description = "Bundles stripped LibVLC for Linux from host system"
    val targetDir = layout.projectDirectory.dir("src/main/resources-packaged/linux/vlc").asFile
    outputs.dir(targetDir)
    onlyIf {
        !targetDir.resolve("libvlc.so").exists() && !targetDir.resolve("libvlc.so.5").exists()
    }

    doLast {
        val searchDirs = listOf(
            file("/usr/lib/x86_64-linux-gnu"),
            file("/usr/lib64"),
            file("/usr/lib")
        )
        val vlcLibDir = searchDirs.firstOrNull { dir ->
            dir.resolve("libvlc.so.5").exists() || dir.resolve("libvlc.so").exists()
        }
        val vlcPluginDir = searchDirs.map { it.resolve("vlc/plugins") }.firstOrNull { it.exists() }

        if (vlcLibDir == null) {
            println("WARNING: LibVLC not found on host Linux system. Install with: sudo apt install -y libvlc-dev vlc")
            return@doLast
        }

        println("Bundling LibVLC from $vlcLibDir to $targetDir...")
        targetDir.mkdirs()

        copy {
            from(vlcLibDir) {
                include("libvlc.so*")
                include("libvlccore.so*")
            }
            into(targetDir)
        }

        val libvlc5 = targetDir.resolve("libvlc.so.5")
        val libvlcSo = targetDir.resolve("libvlc.so")
        if (libvlc5.exists() && !libvlcSo.exists()) {
            libvlc5.copyTo(libvlcSo)
        }
        val libvlccore9 = targetDir.resolve("libvlccore.so.9")
        val libvlccoreSo = targetDir.resolve("libvlccore.so")
        if (libvlccore9.exists() && !libvlccoreSo.exists()) {
            libvlccore9.copyTo(libvlccoreSo)
        }

        if (vlcPluginDir != null && vlcPluginDir.exists()) {
            println("Bundling and stripping VLC plugins from $vlcPluginDir...")
            copy {
                from(vlcPluginDir) {
                    exclude("plugins.dat")
                    exclude("gui/**")
                    exclude("video_output/**")
                    exclude("video_filter/**")
                    exclude("video_chroma/**")
                    exclude("spu/**")
                    exclude("text_renderer/**")
                    exclude("visualization/**")
                    exclude("control/**")
                }
                includeEmptyDirs = false
                into(targetDir.resolve("plugins"))
            }
        }
        println("LibVLC Linux bundled successfully (video modules stripped).")
    }
}

tasks.matching {
    it.name.contains("Resource", ignoreCase = true) ||
    it.name.startsWith("package") ||
    it.name == "createDistributable"
}.configureEach {
    if (name != "setupVlcWindows" && name != "setupVlcLinux") {
        val os = org.gradle.internal.os.OperatingSystem.current()
        if (os.isWindows) {
            dependsOn("setupVlcWindows")
        } else if (os.isLinux) {
            dependsOn("setupVlcLinux")
        }
    }
}

