package com.yanhao.kmpmusic.playback

import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.pathString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MacosLibVlcRuntimeTest {
    @Test
    fun resolvesBundleRuntimeBeforeDevelopmentFallback(): Unit {
        val tempDir = Files.createTempDirectory("kmp-music-libvlc-test")
        val appDir = tempDir.resolve("KMP Music.app")
        val bundled = appDir.resolve("Contents/Resources/LibVLC")
        val lib = bundled.resolve("lib")
        val plugins = bundled.resolve("plugins")
        lib.createDirectories()
        plugins.createDirectories()
        lib.resolve("libvlc.dylib").createFile()
        val runtime = MacosLibVlcRuntime.resolve(
            appDirectory = appDir,
            developmentVlcApp = tempDir.resolve("VLC.app"),
            allowDevelopmentFallback = true,
        )
        assertNotNull(actual = runtime)
        assertEquals(expected = lib.pathString, actual = runtime.libraryDirectory)
        assertEquals(expected = plugins.pathString, actual = runtime.pluginDirectory)
        assertTrue(actual = runtime.isBundled)
    }

    @Test
    fun resolvesDevelopmentFallbackWhenBundleIsMissing(): Unit {
        val tempDir = Files.createTempDirectory("kmp-music-vlc-fallback-test")
        val vlcApp = tempDir.resolve("VLC.app")
        val lib = vlcApp.resolve("Contents/MacOS/lib")
        val plugins = vlcApp.resolve("Contents/MacOS/plugins")
        lib.createDirectories()
        plugins.createDirectories()
        lib.resolve("libvlc.dylib").createFile()
        val runtime = MacosLibVlcRuntime.resolve(
            appDirectory = tempDir.resolve("KMP Music.app"),
            developmentRuntimeDirectory = tempDir.resolve("missing-project-runtime"),
            developmentVlcApp = vlcApp,
            allowDevelopmentFallback = true,
        )
        assertNotNull(actual = runtime)
        assertEquals(expected = lib.pathString, actual = runtime.libraryDirectory)
        assertEquals(expected = plugins.pathString, actual = runtime.pluginDirectory)
        assertEquals(expected = false, actual = runtime.isBundled)
    }

    @Test
    fun resolvesProjectDevelopmentRuntimeBeforeSystemFallback(): Unit {
        val tempDir = Files.createTempDirectory("kmp-music-project-runtime-test")
        val projectRuntime = tempDir.resolve("composeApp/build/macos-libvlc/runtime/LibVLC")
        val projectLib = projectRuntime.resolve("lib")
        val projectPlugins = projectRuntime.resolve("plugins")
        val vlcApp = tempDir.resolve("VLC.app")
        val systemLib = vlcApp.resolve("Contents/MacOS/lib")
        val systemPlugins = vlcApp.resolve("Contents/MacOS/plugins")
        projectLib.createDirectories()
        projectPlugins.createDirectories()
        systemLib.createDirectories()
        systemPlugins.createDirectories()
        projectLib.resolve("libvlc.dylib").createFile()
        systemLib.resolve("libvlc.dylib").createFile()
        val runtime = MacosLibVlcRuntime.resolve(
            appDirectory = tempDir.resolve("KMP Music.app"),
            developmentRuntimeDirectory = projectRuntime,
            developmentVlcApp = vlcApp,
            allowDevelopmentFallback = true,
        )
        assertNotNull(actual = runtime)
        assertEquals(expected = projectLib.pathString, actual = runtime.libraryDirectory)
        assertEquals(expected = projectPlugins.pathString, actual = runtime.pluginDirectory)
        assertEquals(expected = false, actual = runtime.isBundled)
    }

    @Test
    fun returnsNullWhenReleaseBundleRuntimeIsMissing(): Unit {
        val tempDir = Files.createTempDirectory("kmp-music-vlc-missing-test")
        val runtime = MacosLibVlcRuntime.resolve(
            appDirectory = tempDir.resolve("KMP Music.app"),
            developmentVlcApp = tempDir.resolve("VLC.app"),
            allowDevelopmentFallback = false,
        )
        assertNull(actual = runtime)
    }
}
