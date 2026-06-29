import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.JavaExec
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidxRoom3)
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }

    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            binaryOption("bundleId", "com.yanhao.kmpmusic.composeapp")
        }
    }

    jvm("desktop") {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        mainRun {
            mainClass.set("com.yanhao.kmpmusic.DesktopMainKt")
        }
    }

    sourceSets {
        val desktopMain by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(libs.coil.compose)
            implementation(libs.coil.compose.core)
            implementation(libs.coil.network.ktor3)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.androidx.room3.runtime)
            implementation(libs.androidx.sqlite.bundled)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core)
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.session)
            implementation(libs.androidx.media3.ui)
            implementation(libs.ktor.client.android)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.ktor.client.java)
            implementation(libs.jaudiotagger)
            implementation(libs.vlcj)
        }
        iosArm64Main.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        iosSimulatorArm64Main.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room3.compiler)
    add("kspIosArm64", libs.androidx.room3.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room3.compiler)
    add("kspDesktop", libs.androidx.room3.compiler)
}

configurations.matching { configuration ->
    configuration.name == "kspPluginClasspath" ||
        configuration.name == "kspPluginClasspathNonEmbeddable"
}.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlinx" && requested.name.startsWith("kotlinx-serialization")) {
            useVersion(libs.versions.kotlinxSerialization.get())
            because("Room3 schema export requires the newer kotlinx.serialization ABI on the KSP runtime classpath.")
        }
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

android {
    namespace = "com.yanhao.kmpmusic"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.yanhao.kmpmusic"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

compose.desktop {
    application {
        mainClass = "com.yanhao.kmpmusic.DesktopMainKt"
        buildTypes.release.proguard {
            isEnabled.set(false)
        }
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "KMP Music"
            packageVersion = "1.0.0"
        }
    }
}

val macosLibVlcDownloadDir = layout.dir(
    providers
        .gradleProperty("kmp.music.libvlc.download.dir")
        .map { path: String -> file(path) }
        .orElse(
            providers.provider {
                gradle.gradleUserHomeDir.resolve("caches/kmp-music/macos-libvlc/download")
            },
        ),
)
val legacyMacosLibVlcDownloadDir = layout.buildDirectory.dir("macos-libvlc/download")
val macosLibVlcRuntimeDir = layout.buildDirectory.dir("macos-libvlc/runtime/LibVLC")
val releaseAppName = "KMP Music.app"
val releaseAppDir = layout.buildDirectory.dir("compose/binaries/main-release/app/$releaseAppName")
val macosLibVlcDownloadUrl: String = providers
    .gradleProperty("kmp.music.libvlc.download.url")
    .getOrElse("https://download.videolan.org/pub/videolan/vlc/last/macosx/vlc-3.0.23-arm64.dmg")

tasks.register<Exec>("downloadMacosArm64LibVlc") {
    workingDir = projectDir
    commandLine(
        "bash",
        "$projectDir/src/desktopMain/packaging/macos-libvlc/download-macos-arm64-libvlc.sh",
        macosLibVlcDownloadDir.get().asFile.absolutePath,
        legacyMacosLibVlcDownloadDir.get().asFile.absolutePath,
        macosLibVlcDownloadUrl,
    )
}

tasks.register<Exec>("extractMacosArm64LibVlc") {
    dependsOn("downloadMacosArm64LibVlc")
    workingDir = projectDir
    commandLine(
        "bash",
        "$projectDir/src/desktopMain/packaging/macos-libvlc/extract-macos-arm64-libvlc.sh",
        macosLibVlcDownloadDir.get().file("vlc-3.0.23-arm64.dmg").asFile.absolutePath,
        macosLibVlcRuntimeDir.get().asFile.absolutePath,
    )
}

tasks.register("prepareMacosArm64LibVlc") {
    dependsOn("extractMacosArm64LibVlc")
    description = "Downloads, verifies, and extracts the macOS arm64 LibVLC runtime for local playback."
    group = "distribution"
}

// Desktop 开发运行只复用已准备好的项目内 LibVLC；发布打包任务才强制准备并内置运行时。
fun JavaExec.configureDesktopDevelopmentRun(): Unit {
    systemProperty(
        "kmp.music.libvlc.runtime.dir",
        macosLibVlcRuntimeDir.get().asFile.absolutePath,
    )
}

tasks.matching { task -> task.name == "run" || task.name == "desktopRun" }.configureEach {
    (this as? JavaExec)?.configureDesktopDevelopmentRun()
}

// The release signing/notarization pipeline must sign nested LibVLC code after this task and
// sign the outer app last. Running packageReleaseDmg before nested signing invalidates release acceptance.
tasks.register<Copy>("stageMacosArm64LibVlcIntoReleaseApp") {
    dependsOn("extractMacosArm64LibVlc", "createReleaseDistributable")
    doFirst {
        delete(releaseAppDir.get().dir("Contents/Frameworks/LibVLC"))
        delete(releaseAppDir.get().dir("Contents/Resources/LibVLC"))
    }
    from(macosLibVlcRuntimeDir)
    into(releaseAppDir.map { directory -> directory.dir("Contents/Resources/LibVLC") })
}

tasks.register<Exec>("verifyMacosArm64ReleaseApp") {
    dependsOn("stageMacosArm64LibVlcIntoReleaseApp")
    workingDir = projectDir
    commandLine(
        "bash",
        "$projectDir/src/desktopMain/packaging/macos-libvlc/verify-macos-app-libvlc.sh",
        releaseAppDir.get().asFile.absolutePath,
    )
}

tasks.matching { task -> task.name == "packageReleaseDmg" }.configureEach {
    dependsOn("stageMacosArm64LibVlcIntoReleaseApp")
}
