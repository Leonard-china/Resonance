import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.api.tasks.Copy
import java.util.Properties
import java.io.File
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask

val releaseKeystoreProperties = Properties().apply {
    val propertiesFile = rootProject.file("keystore.properties")
    if (propertiesFile.isFile) propertiesFile.inputStream().use(::load)
}

val versionProperties = Properties().apply {
    val propertiesFile = rootProject.file("version.properties")
    if (propertiesFile.isFile) propertiesFile.inputStream().use(::load)
}
val appVersionName: String = versionProperties.getProperty("versionName", "0.2.1")
val appVersionCode: Int = versionProperties.getProperty("versionCode", "9").toIntOrNull() ?: 9

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.application)
}

val generatedVersionDirectory = layout.buildDirectory.dir("generated/version/commonMain")
val generateAppVersion by tasks.registering {
    inputs.file(rootProject.file("version.properties"))
    inputs.files(rootProject.fileTree("docs") { include("RELEASE_NOTES-$appVersionName.md") })
    outputs.dir(generatedVersionDirectory)
    doLast {
        val target = generatedVersionDirectory.get().file("com/resonance/player/model/BuildVersion.kt").asFile
        target.parentFile.mkdirs()
        val notesFile = rootProject.file("docs/RELEASE_NOTES-$appVersionName.md")
        val lines = if (notesFile.isFile) notesFile.readLines(Charsets.UTF_8) else emptyList()
        val date = Regex("""^# (\d{4}-\d{2}-\d{2})""").find(lines.firstOrNull().orEmpty())?.groupValues?.get(1).orEmpty()
        val highlights = lines.dropWhile { it.trim() != "## 主要更新" }.drop(1)
            .takeWhile { !it.trimStart().startsWith("## ") }
            .filter { it.trimStart().startsWith("- ") }.map { it.trim().removePrefix("- ") }
        fun literal(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("$", "\\$").replace("\r", "\\r").replace("\n", "\\n") + "\""
        target.writeText(buildString {
            append("package com.resonance.player.model\n\n")
            append("const val APP_VERSION = " + literal(appVersionName) + "\n")
            append("const val APP_RELEASE_DATE = " + literal(date) + "\n")
            append("val APP_RELEASE_HIGHLIGHTS: List<String> = listOf(" + highlights.joinToString(", ", transform = ::literal) + ")\n")
        })
    }
}
tasks.matching { it.name.startsWith("compile") && it.name.contains("Kotlin") }.configureEach {
    dependsOn(generateAppVersion)
}

kotlin {
    androidTarget()
    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            kotlin.srcDir(generatedVersionDirectory)
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.ui)
                implementation(libs.compose.material3)
                implementation(libs.compose.icons)
                implementation(libs.compose.resources)
                implementation(libs.compose.preview)
                implementation(libs.kotlinx.coroutines.core)
                // This release matches the project's Compose 1.11.0 / Kotlin 2.3.20 ABI.
                implementation("dev.chrisbanes.haze:haze:2.0.0-alpha02")
                implementation("dev.chrisbanes.haze:haze-blur:2.0.0-alpha02")
            }
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
        }
        val androidInstrumentedTest by getting {
            dependencies {
                implementation("androidx.test:runner:1.6.2")
                implementation("androidx.test.ext:junit:1.2.1")
                implementation("androidx.compose.ui:ui-test-junit4:1.11.1")
            }
        }
        val jvmMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation("org.json:json:20250517")
                // Certificate construction only; transport uses the platform TLS implementation.
                implementation("com.squareup.okhttp3:okhttp-tls:4.12.0")
                implementation("com.squareup.okhttp3:okhttp:4.12.0")
                implementation("io.github.dokar3:quickjs-kt:1.0.10")
                implementation("com.google.zxing:core:3.5.3")
                compileOnly("com.microsoft.onnxruntime:onnxruntime:1.20.0")
            }
        }
        val desktopMain by getting {
            dependsOn(jvmMain)
            resources.srcDir("src/ocrModels")
            resources.srcDir("src/jvmMain/resources")
            dependencies {
                implementation("com.microsoft.onnxruntime:onnxruntime:1.20.0")
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.jaudiotagger)
                implementation("org.xerial:sqlite-jdbc:3.47.1.0")
                implementation("net.java.dev.jna:jna-platform:5.17.0")
            }
        }
        val androidMain by getting {
            dependsOn(jvmMain)
            dependencies {
                implementation("com.microsoft.onnxruntime:onnxruntime-android:1.23.2")
                implementation("androidx.activity:activity-compose:1.10.1")
                implementation("androidx.documentfile:documentfile:1.1.0")
                implementation("androidx.work:work-runtime-ktx:2.10.1")
                implementation("androidx.media3:media3-exoplayer:1.10.1")
                implementation("androidx.media3:media3-session:1.10.1")
                implementation("dev.ffmpegkit-maintained:ffmpeg-kit-audio:8.1.7")
                // ffmpeg-kit-maintained 8.1.7 publishes a dependency-free POM even
                // though its Java facade still calls Smart Exception at runtime.
                implementation("com.arthenica:smart-exception-java:0.2.1")
            }
        }
    }
}

dependencies {
    add("debugImplementation", "androidx.compose.ui:ui-test-manifest:1.11.1")
}

android {
    namespace = "com.resonance.player"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    sourceSets.getByName("main").assets.srcDir("src/ocrModels")
    sourceSets.getByName("main").resources.srcDir("src/jvmMain/resources")
    sourceSets.getByName("debug").manifest.srcFile("src/androidDebug/AndroidManifest.xml")

    defaultConfig {
        applicationId = "com.resonance.player"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseKeystoreProperties.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(releaseKeystoreProperties.getProperty("storeFile"))
                storePassword = releaseKeystoreProperties.getProperty("storePassword")
                keyAlias = releaseKeystoreProperties.getProperty("keyAlias")
                keyPassword = releaseKeystoreProperties.getProperty("keyPassword")
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
        getByName("release") {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // 构建产物固定输出到 build/outputs/apk/<variant>/ 并带版本号命名（如 Resonance-0.1.5-release.apk）
    applicationVariants.all {
        outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                "Resonance-${versionName}-${name}.apk"
        }
    }
}

val desktopHostIsWindows = System.getProperty("os.name").startsWith("Windows", true)

compose.desktop {
    application {
        mainClass = "com.resonance.player.MainKt"
        buildTypes.release.proguard {
            // Preserve dependencies used through reflection by desktop integrations.
            configurationFiles.from(project.file("compose-desktop.pro"))
        }
        nativeDistributions {
            if (desktopHostIsWindows) appResourcesRootDir.set(layout.buildDirectory.dir("desktopAppResources"))
            targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Deb, TargetFormat.Rpm)
            // java.sql, TLS providers and reflection-based integrations must work in the bundled JVM.
            if (!desktopHostIsWindows) {
                includeAllModules = true
                licenseFile.set(rootProject.file("LICENSE"))
            }
            packageName = "Resonance"
            packageVersion = appVersionName
            description = "Local music library, player, converter, and playlist archives"
            vendor = "Resonance"
            windows {
                iconFile.set(project.file("src/desktopMain/resources/resonance.ico"))
            }
            linux {
                iconFile.set(project.file("src/desktopMain/resources/resonance-icon.png"))
                packageName = "resonance"
                appCategory = "AudioVideo"
                menuGroup = "AudioVideo"
                shortcut = true
                // Use the distribution's maintained FFmpeg instead of bundling a dynamically linked binary.
            }
        }
    }
}

// Windows bundles FFmpeg. Linux declares it as a system dependency.
val ffmpegSource = providers.environmentVariable("RESONANCE_FFMPEG").orElse(provider {
    System.getenv("PATH").orEmpty().split(File.pathSeparator)
        .map { File(it, "ffmpeg.exe") }.firstOrNull { it.isFile }?.absolutePath.orEmpty()
})
val prepareDesktopAppResources by tasks.registering(Copy::class) {
    onlyIf { desktopHostIsWindows }
    from(ffmpegSource)
    into(layout.buildDirectory.dir("desktopAppResources/windows"))
    rename { "ffmpeg.exe" }
    doFirst {
        require(file(ffmpegSource.get()).isFile) {
            "Cannot find FFmpeg. Set RESONANCE_FFMPEG to an existing ffmpeg.exe."
        }
    }
}

tasks.matching {
    it.name in setOf(
        "prepareAppResources", "createDistributable", "createReleaseDistributable",
        "packageExe", "packageMsi", "packageReleaseExe", "packageReleaseMsi",
        "packageDistributionForCurrentOS", "packageReleaseDistributionForCurrentOS",
    )
}.configureEach {
    if (desktopHostIsWindows) dependsOn(prepareDesktopAppResources)
}

tasks.withType<AbstractJPackageTask>().configureEach {
    if (targetFormat == TargetFormat.Deb || targetFormat == TargetFormat.Rpm) {
        freeArgs.addAll("--linux-package-deps", "ffmpeg")
    }
}

tasks.register<Tar>("packageLinuxPortable") {
    group = "distribution"
    description = "Archive the complete Linux release app image, including its Java runtime"
    dependsOn("createReleaseDistributable")
    onlyIf { System.getProperty("os.name").startsWith("Linux", true) }
    compression = Compression.GZIP
    val architecture = if (System.getProperty("os.arch") == "amd64") "x86_64" else System.getProperty("os.arch")
    archiveFileName.set("Resonance-$appVersionName-linux-$architecture.tar.gz")
    destinationDirectory.set(layout.buildDirectory.dir("compose/binaries/main-release/tar"))
    from(layout.buildDirectory.dir("compose/binaries/main-release/app")) { include("Resonance/**") }
}

tasks.register("bumpVersion") {
    group = "versioning"
    description = "Explicitly bumps version.properties for a release; normal builds keep the same version"
    doLast {
        val file = rootProject.file("version.properties")
        val props = Properties().apply { if (file.isFile) file.inputStream().use(::load) }
        val currentName = props.getProperty("versionName", "0.2.0")
        val currentCode = props.getProperty("versionCode", "8").toIntOrNull() ?: 8

        val parts = currentName.split('.').mapNotNull { it.toIntOrNull() }
        val nextName = if (parts.size >= 3) {
            "${parts[0]}.${parts[1]}.${parts[2] + 1}"
        } else {
            "$currentName.1"
        }
        val nextCode = currentCode + 1

        props.setProperty("versionName", nextName)
        props.setProperty("versionCode", nextCode.toString())
        file.outputStream().use { props.store(it, "Resonance build version") }

        println("Resonance version bumped: $currentName ($currentCode) -> $nextName ($nextCode)")
    }
}
