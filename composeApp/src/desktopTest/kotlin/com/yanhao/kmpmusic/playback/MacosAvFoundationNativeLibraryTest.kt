package com.yanhao.kmpmusic.playback

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * macOS AVFoundation native library 路径解析门禁。
 */
class MacosAvFoundationNativeLibraryTest {
    /** 验证打包 `.app` 可通过 Compose resources 属性找到内置 bridge。 */
    @Test
    fun resolvesBundledBridgePathFromComposeResources() {
        val resourcesDir: Path = Files.createTempDirectory("kmp-music-macos-resources")
        try {
            val bridgeDirectory: Path = resourcesDir.resolve(MACOS_AVFOUNDATION_BRIDGE_BUNDLED_RESOURCE_DIRECTORY)
            Files.createDirectories(bridgeDirectory)
            val bridgePath: Path = bridgeDirectory.resolve(MACOS_AVFOUNDATION_BRIDGE_LIBRARY_FILE_NAME)
            Files.writeString(bridgePath, "fake dylib")
            assertEquals(
                expected = bridgePath.toAbsolutePath().toString(),
                actual =
                    MacosAvFoundationNativeLibraryPathResolver.resolveBundledBridgePath(
                        resourcesDir = resourcesDir.toString(),
                    ),
            )
        } finally {
            resourcesDir.toFile().deleteRecursively()
        }
    }
}
