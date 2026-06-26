package com.yanhao.kmpmusic.feature.app

import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackErrorType
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackErrorMessageTest {
    @Test
    fun engineUnavailableDoesNotAskUserToInstallVlc(): Unit {
        val error = PlaybackError(
            type = PlaybackErrorType.EngineUnavailable,
            songId = "song-1",
            message = "libvlc missing",
        )

        assertEquals(
            expected = "《山海》播放器组件不可用，请重新安装应用或联系开发者。",
            actual = error.userMessage(songTitle = "山海"),
        )
    }

    @Test
    fun unknownSongUsesCurrentSongFallback(): Unit {
        val error = PlaybackError(
            type = PlaybackErrorType.Unknown,
            songId = null,
            message = "native error",
        )

        assertEquals(
            expected = "当前歌曲播放失败，已尝试播放下一首。",
            actual = error.userMessage(songTitle = null),
        )
    }
}
