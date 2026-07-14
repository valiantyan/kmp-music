import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.testing.Test
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
        val androidUnitTest by getting
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
        androidUnitTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation("org.robolectric:robolectric:4.11.1")
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

    testOptions {
        unitTests.isIncludeAndroidResources = false
    }
}

compose.desktop {
    application {
        mainClass = "com.yanhao.kmpmusic.DesktopMainKt"
        buildTypes.release.proguard {
            isEnabled.set(false)
        }
        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "KMP Music"
            packageVersion = "1.0.0"
        }
    }
}

val macosAvFoundationBridgeSource = layout.projectDirectory.file(
    "src/desktopMain/native/macos-avfoundation/KmpMacosAvFoundationBridge.mm",
)
val macosAvFoundationBridgeOutputDir = layout.buildDirectory.dir("macos-avfoundation-bridge/native")
val macosAvFoundationBridgeLibrary = macosAvFoundationBridgeOutputDir.map { directory ->
    directory.file("libkmp_music_macos_avfoundation_bridge.dylib")
}
val macosAvFoundationBridgeSmokeDir = layout.buildDirectory.dir("macos-avfoundation-bridge/smoke")
val isMacosHost: Boolean = System.getProperty("os.name").contains(other = "mac", ignoreCase = true)

tasks.register<Exec>("compileMacosAvFoundationBridge") {
    description = "Compiles the in-process macOS AVFoundation JNI bridge."
    group = "build"
    onlyIf { isMacosHost }
    inputs.file(macosAvFoundationBridgeSource)
    outputs.file(macosAvFoundationBridgeLibrary)
    doFirst {
        macosAvFoundationBridgeOutputDir.get().asFile.mkdirs()
    }
    val javaHome: String = System.getProperty("java.home")
    commandLine(
        "clang++",
        "-dynamiclib",
        "-std=c++17",
        "-fobjc-arc",
        "-mmacosx-version-min=12.0",
        "-framework",
        "Foundation",
        "-framework",
        "AVFoundation",
        "-framework",
        "CoreMedia",
        "-I",
        "$javaHome/include",
        "-I",
        "$javaHome/include/darwin",
        macosAvFoundationBridgeSource.asFile.absolutePath,
        "-o",
        macosAvFoundationBridgeLibrary.get().asFile.absolutePath,
    )
}

tasks.named<Test>("desktopTest") {
    dependsOn("compileMacosAvFoundationBridge")
    systemProperty(
        "kmp.music.macos.avfoundation.bridge.path",
        macosAvFoundationBridgeLibrary.get().asFile.absolutePath,
    )
}

tasks.register<JavaExec>("macosAvFoundationBridgeSmoke") {
    description = "Runs a real local M4A playback smoke through the macOS AVFoundation bridge."
    group = "verification"
    onlyIf { isMacosHost }
    dependsOn("compileMacosAvFoundationBridge", "desktopJar")
    mainClass.set("com.yanhao.kmpmusic.playback.MacosAvFoundationBridgeSmoke")
    classpath(
        tasks.named("desktopJar"),
        configurations.named("desktopRuntimeClasspath"),
    )
    systemProperty(
        "kmp.music.macos.avfoundation.bridge.path",
        macosAvFoundationBridgeLibrary.get().asFile.absolutePath,
    )
    systemProperty(
        "kmp.music.macos.avfoundation.smoke.dir",
        macosAvFoundationBridgeSmokeDir.get().asFile.absolutePath,
    )
}

tasks.register<JavaExec>("macosAvFoundationDefaultRuntimeSmoke") {
    description = "Runs a real local M4A playback smoke through the default macOS desktop runtime."
    group = "verification"
    onlyIf { isMacosHost }
    dependsOn("compileMacosAvFoundationBridge", "desktopJar")
    mainClass.set("com.yanhao.kmpmusic.MacosAvFoundationDefaultRuntimeSmoke")
    classpath(
        tasks.named("desktopJar"),
        configurations.named("desktopRuntimeClasspath"),
    )
    systemProperty(
        "kmp.music.macos.avfoundation.bridge.path",
        macosAvFoundationBridgeLibrary.get().asFile.absolutePath,
    )
    systemProperty(
        "kmp.music.macos.avfoundation.smoke.dir",
        macosAvFoundationBridgeSmokeDir.get().asFile.absolutePath,
    )
}
