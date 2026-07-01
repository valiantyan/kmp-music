package com.yanhao.kmpmusic.feature.app

import com.yanhao.kmpmusic.domain.model.PlaybackStatus

/**
 * 当前状态是否应在界面上显示暂停入口。
 *
 * [PlaybackStatus.Loading] 和 [PlaybackStatus.Buffering] 代表用户已经发起播放，
 * 但底层还没有进入真实 [PlaybackStatus.Playing]，因此只影响 UI 控件，不改变播放状态语义。
 */
internal val PlaybackStatus.shouldShowPauseControl: Boolean
    get() = this == PlaybackStatus.Playing ||
        this == PlaybackStatus.Loading ||
        this == PlaybackStatus.Buffering
