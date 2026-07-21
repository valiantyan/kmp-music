package com.yanhao.kmpmusic.playback

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.Foundation.NSError

/**
 * iOS audio session 控制边界，确保播放开始前统一配置后台播放需要的 category。
 */
internal interface IosAudioSessionController {
    /**
     * 配置并激活 playback category。
     *
     * @return true 表示配置成功，false 表示 native 层拒绝配置。
     */
    fun configureForPlayback(): Boolean

    /** 释放或停用当前 audio session 配置。 */
    fun release()
}

/**
 * 基于 [AVAudioSession] 的真实 iOS audio session 控制器。
 */
internal class IosAvAudioSessionController(
    // 系统共享 audio session。
    private val audioSession: AVAudioSession = AVAudioSession.sharedInstance(),
) : IosAudioSessionController {
    /** 播放前按 Apple 媒体播放要求设置 playback category 并激活。 */
    @OptIn(ExperimentalForeignApi::class)
    override fun configureForPlayback(): Boolean {
        return memScoped {
            val categoryError = alloc<ObjCObjectVar<NSError?>>()
            val activeError = alloc<ObjCObjectVar<NSError?>>()
            val isCategorySet: Boolean =
                audioSession.setCategory(
                    category = AVAudioSessionCategoryPlayback,
                    error = categoryError.ptr,
                )
            if (!isCategorySet) {
                return@memScoped false
            }
            audioSession.setActive(
                active = true,
                error = activeError.ptr,
            )
        }
    }

    /** 会话关闭时停用 audio session，避免框架对象继续占用音频焦点。 */
    @OptIn(ExperimentalForeignApi::class)
    override fun release() {
        memScoped {
            val activeError = alloc<ObjCObjectVar<NSError?>>()
            audioSession.setActive(
                active = false,
                error = activeError.ptr,
            )
        }
    }
}
