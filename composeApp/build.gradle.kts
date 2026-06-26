import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
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
    }

    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            binaryOption("bundleId", "com.yanhao.kmpmusic.composeapp")
        }
    }

    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
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
            implementation("androidx.activity:activity-compose:1.12.2")
            implementation(libs.androidx.core)
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.session)
            implementation(libs.androidx.media3.ui)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.vlcj)
        }
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room3.compiler)
    add("kspIosX64", libs.androidx.room3.compiler)
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

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
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
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "KMP Music"
            packageVersion = "1.0.0"
        }
    }
}

val macosLibVlcDownloadDir = layout.buildDirectory.dir("macos-libvlc/download")
val macosLibVlcRuntimeDir = layout.buildDirectory.dir("macos-libvlc/runtime/LibVLC")
val releaseAppName = "KMP Music.app"
val releaseAppDir = layout.buildDirectory.dir("compose/binaries/main-release/app/$releaseAppName")

tasks.register<Exec>("downloadMacosArm64LibVlc") {
    workingDir = projectDir
    commandLine(
        "bash",
        "$projectDir/src/desktopMain/packaging/macos-libvlc/download-macos-arm64-libvlc.sh",
        macosLibVlcDownloadDir.get().asFile.absolutePath,
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

// The release signing/notarization pipeline must sign nested LibVLC code after this task and
// sign the outer app last. Running packageReleaseDmg before nested signing invalidates release acceptance.
tasks.register<Copy>("stageMacosArm64LibVlcIntoReleaseApp") {
    dependsOn("extractMacosArm64LibVlc", "createReleaseDistributable")
    from(macosLibVlcRuntimeDir)
    into(releaseAppDir.map { directory -> directory.dir("Contents/Frameworks/LibVLC") })
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
