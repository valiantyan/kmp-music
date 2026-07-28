package com.yanhao.kmpmusic.domain.model

/**
 * 全局播放倍速选项，只暴露产品确认支持的离散值。
 *
 * @property multiplier 下发到平台播放器的速度倍率。
 * @property label UI 展示使用的稳定文本。
 */
enum class PlaybackSpeed(
    val multiplier: Float,
    val label: String,
) {
    Half(
        multiplier = 0.5f,
        label = "0.5",
    ),
    ThreeQuarter(
        multiplier = 0.75f,
        label = "0.75",
    ),
    Normal(
        multiplier = 1.0f,
        label = "1.0",
    ),
    OneQuarter(
        multiplier = 1.25f,
        label = "1.25",
    ),
    OneHalf(
        multiplier = 1.5f,
        label = "1.5",
    ),
    Double(
        multiplier = 2.0f,
        label = "2.0",
    ),
    ;

    companion object {
        /** 返回产品默认倍速，供状态和偏好读取保持同一入口。 */
        fun resolveDefault(): PlaybackSpeed = Normal

        /** 从持久化倍率恢复倍速；旧值或非法值回退默认，避免阻塞冷启动。 */
        fun resolveStoredMultiplier(value: Float?): PlaybackSpeed = entries.firstOrNull { speed: PlaybackSpeed -> speed.multiplier == value } ?: resolveDefault()
    }
}
