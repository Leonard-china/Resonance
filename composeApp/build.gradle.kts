import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.api.tasks.Copy
import java.util.Properties

val releaseKeystoreProperties = Properties().apply {
    val propertiesFile = rootProject.file("keystore.properties")
    if (propertiesFile.isFile) propertiesFile.inputStream().use(::load)
}

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.application)
}

kotlin {
    androidTarget()
    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.kotlinx.coroutines.core)
            }
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        val jvmMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation("org.json:json:20250517")
            }
        }
        val desktopMain by getting {
            dependsOn(jvmMain)
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.jaudiotagger)
                implementation("com.google.zxing:javase:3.5.4")
                implementation("org.openjfx:javafx-base:21.0.8:win")
                implementation("org.openjfx:javafx-graphics:21.0.8:win")
                implementation("org.openjfx:javafx-media:21.0.8:win")
            }
        }
        val androidMain by getting {
            dependsOn(jvmMain)
            dependencies {
                implementation("androidx.activity:activity-compose:1.10.1")
                implementation("androidx.documentfile:documentfile:1.1.0")
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

android {
    namespace = "com.resonance.player"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.resonance.player"
        minSdk = 26
        targetSdk = 36
        versionCode = 6
        versionName = "0.1.5"
    }

    signingConfigs {
        if (releaseKeystoreProperties.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(releaseKeystoreProperties.getProperty("storeFile"))
                storePassword = releaseKeystoreProperties.getProperty("storePassword")
                keyAlias = releaseKeystoreProperties.getProperty("keyAlias")
                keyPassword = releaseKeystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

compose.desktop {
    application {
        mainClass = "com.resonance.player.MainKt"
        buildTypes.release.proguard {
            // The JavaFX toolkit and media backends are discovered reflectively.
            // Shrinking them makes the packaged launcher fail before the UI opens.
            configurationFiles.from(project.file("compose-desktop.pro"))
        }
        nativeDistributions {
            appResourcesRootDir.set(layout.buildDirectory.dir("desktopAppResources"))
            modules("jdk.httpserver")
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "Resonance"
            packageVersion = "0.1.5"
            description = "Local music library, player, converter, and device sync"
            vendor = "Resonance"
            windows {
                iconFile.set(project.file("src/desktopMain/resources/resonance.ico"))
            }
        }
    }
}

val ffmpegSource = providers.environmentVariable("RESONANCE_FFMPEG").orElse(
    "D:/Software/Github/FlyMouseFormat/release/exe/FlyingMouse Format/resources/ffmpeg/ffmpeg.exe",
)
val prepareDesktopAppResources by tasks.registering(Copy::class) {
    from(ffmpegSource)
    into(layout.buildDirectory.dir("desktopAppResources/windows"))
    rename { "ffmpeg.exe" }
    doFirst {
        require(file(ffmpegSource.get()).isFile) {
            "找不到 FFmpeg。请用 RESONANCE_FFMPEG 指向 ffmpeg.exe（建议放在 D: 盘）。"
        }
    }
}

tasks.matching {
    it.name in setOf(
        "prepareAppResources",
        "createDistributable",
        "createReleaseDistributable",
        "packageExe",
        "packageMsi",
        "packageReleaseExe",
        "packageReleaseMsi",
        "packageDistributionForCurrentOS",
    )
}.configureEach {
    dependsOn(prepareDesktopAppResources)
}

tasks.register<JavaExec>("lanShareProbe") {
    group = "verification"
    dependsOn("desktopTestClasses")
    val testCompilation = kotlin.targets.getByName("desktop").compilations.getByName("test")
    classpath = files(testCompilation.runtimeDependencyFiles, testCompilation.output.allOutputs)
    mainClass.set("com.resonance.player.sync.LanShareProbe")
}
