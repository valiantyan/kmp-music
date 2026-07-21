package com.yanhao.kmpmusic.feature.app

import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackErrorType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackErrorMessageTest {
    @Test
    fun playbackErrorsProvideDistinctRecoveryActions() {
        val messages: Map<PlaybackErrorType, String> =
            PlaybackErrorType.entries.associateWith { type: PlaybackErrorType ->
                PlaybackError(
                    type = type,
                    songId = "song-1",
                    message = "native diagnostic",
                ).userMessage(songTitle = "山海")
            }

        assertTrue(actual = messages.getValue(key = PlaybackErrorType.MissingFile).contains(other = "重新扫描"))
        assertTrue(actual = messages.getValue(key = PlaybackErrorType.PermissionDenied).contains(other = "重新授权"))
        assertTrue(actual = messages.getValue(key = PlaybackErrorType.UnsupportedFormat).contains(other = "无保护"))
        assertTrue(actual = messages.getValue(key = PlaybackErrorType.UnsupportedFormat).contains(other = "已验证格式"))
        assertTrue(actual = messages.getValue(key = PlaybackErrorType.EngineUnavailable).contains(other = "Apple 播放组件"))
        assertTrue(actual = messages.getValue(key = PlaybackErrorType.Unknown).contains(other = "稍后重试"))
        assertFalse(
            actual =
                messages
                    .getValue(key = PlaybackErrorType.MissingFile)
                    .contains(other = "重新授权"),
        )
        assertFalse(
            actual =
                messages
                    .getValue(key = PlaybackErrorType.PermissionDenied)
                    .contains(other = "重新扫描"),
        )
    }

    @Test
    fun userMessagesDoNotMentionLegacyVlcRuntime() {
        val forbiddenWords: List<String> =
            listOf(
                "V" + "LC",
                "Lib" + "V" + "LC",
                "v" + "lcj",
                "插件" + "路径",
                "安装 " + "V" + "LC",
            )

        PlaybackErrorType.entries.forEach { type: PlaybackErrorType ->
            val message: String =
                PlaybackError(
                    type = type,
                    songId = "song-1",
                    message = "native bridge diagnostic",
                ).userMessage(songTitle = "山海")
            forbiddenWords.forEach { forbiddenWord: String ->
                assertFalse(actual = message.contains(other = forbiddenWord))
            }
        }
    }

    @Test
    fun unknownSongUsesCurrentSongFallback() {
        val error =
            PlaybackError(
                type = PlaybackErrorType.Unknown,
                songId = null,
                message = "native error",
            )

        assertEquals(
            expected = "当前歌曲播放失败，已尝试播放下一首；请稍后重试，若持续失败请重新扫描或换用已验证格式的音频。",
            actual = error.userMessage(songTitle = null),
        )
    }
}
