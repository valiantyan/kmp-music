package com.yanhao.kmpmusic.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import com.yanhao.kmpmusic.domain.model.PlaybackSpeed
import kotlinx.coroutines.flow.first

/**
 * 使用 Preferences DataStore 保存全局播放倍速，避免为单个轻量偏好依赖 Room。
 *
 * @param dataStore 用户偏好 [DataStore] 实例，调用方需保证同一文件单例复用。
 */
internal class PlaybackSpeedPreferencesStore(
    private val dataStore: DataStore<Preferences>,
) {
    /**
     * 从 DataStore 读取播放倍速，非法或缺失值统一回退到 [PlaybackSpeed.resolveDefault]。
     *
     * @return 产品支持的离散播放倍速。
     */
    suspend fun readPlaybackSpeed(): PlaybackSpeed {
        val preferences: Preferences = dataStore.data.first()
        return PlaybackSpeed.resolveStoredMultiplier(value = preferences[KEY_PLAYBACK_SPEED])
    }

    /**
     * 保存产品支持的播放倍速，接口使用 [PlaybackSpeed] 阻止非法 Float 写入。
     *
     * @param playbackSpeed 用户选择的离散播放倍速。
     */
    suspend fun savePlaybackSpeed(playbackSpeed: PlaybackSpeed) {
        dataStore.edit { preferences: MutablePreferences ->
            preferences[KEY_PLAYBACK_SPEED] = playbackSpeed.multiplier
        }
    }

    companion object {
        /** 播放倍速偏好键，对应 DataStore 文件中的 `playback_speed`。 */
        private val KEY_PLAYBACK_SPEED: Preferences.Key<Float> = floatPreferencesKey(name = "playback_speed")
    }
}
