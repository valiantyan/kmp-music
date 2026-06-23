package com.yanhao.kmpmusic

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Android 音频读取权限请求器，把 ActivityResult 流程包装成 scanner 可等待的挂起调用。
 */
internal class AndroidAudioPermissionRequester(
    private val activity: ComponentActivity,
) {
    // 当前等待系统权限结果的协程，避免重复弹窗造成状态错乱。
    private var pendingContinuation: CancellableContinuation<AndroidAudioPermissionResult>? = null

    // 当前请求的权限名，用来在回调阶段判断是否需要转系统设置。
    private var pendingPermission: String? = null

    // 记录用户已经经历过系统权限请求，避免把首次请求误判为永久拒绝。
    private val permissionPreferences: SharedPreferences = activity.getSharedPreferences(
        AUDIO_PERMISSION_PREFERENCES,
        Context.MODE_PRIVATE,
    )

    // 系统权限启动器，只在 Activity 生命周期内使用。
    private val permissionLauncher: ActivityResultLauncher<String> = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        resumePendingRequest(isGranted = isGranted)
    }

    /**
     * 检查或请求当前 Android 版本所需的音频读取权限。
     *
     * @return 返回权限结果；永久拒绝时只报告需要设置，由 UI 再确认是否跳转。
     */
    suspend fun requestAudioPermission(): AndroidAudioPermissionResult {
        val permission: String = getRequiredAudioPermission()
        if (activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            return AndroidAudioPermissionResult.Granted
        }
        if (shouldOpenSettingsInsteadOfRequest(permission = permission)) {
            return AndroidAudioPermissionResult.NeedsSettings
        }
        return suspendCancellableCoroutine { continuation: CancellableContinuation<AndroidAudioPermissionResult> ->
            if (pendingContinuation != null) {
                continuation.resume(AndroidAudioPermissionResult.Denied)
                return@suspendCancellableCoroutine
            }
            pendingContinuation = continuation
            pendingPermission = permission
            continuation.invokeOnCancellation {
                if (pendingContinuation == continuation) {
                    pendingContinuation = null
                    pendingPermission = null
                }
            }
            launchPermissionRequest(
                permission = permission,
                continuation = continuation,
            )
        }
    }

    // 发起系统权限弹窗，启动失败时恢复协程并进入权限拒绝路径。
    private fun launchPermissionRequest(
        permission: String,
        continuation: CancellableContinuation<AndroidAudioPermissionResult>,
    ) {
        try {
            markPermissionRequested(permission = permission)
            Log.d(TAG, "发起 Android 音频权限请求: $permission")
            permissionLauncher.launch(permission)
        } catch (illegalStateException: IllegalStateException) {
            Log.e(TAG, "Android 音频权限请求启动失败", illegalStateException)
            if (pendingContinuation == continuation) {
                pendingContinuation = null
                pendingPermission = null
            }
            continuation.resume(AndroidAudioPermissionResult.Denied)
        }
    }

    // 收到系统权限结果后恢复对应协程。
    private fun resumePendingRequest(isGranted: Boolean) {
        val continuation: CancellableContinuation<AndroidAudioPermissionResult> = pendingContinuation ?: return
        val permission: String? = pendingPermission
        pendingContinuation = null
        pendingPermission = null
        val result: AndroidAudioPermissionResult = when {
            isGranted -> AndroidAudioPermissionResult.Granted
            permission != null && shouldOpenSettingsAfterDenied(permission = permission) -> AndroidAudioPermissionResult.NeedsSettings
            else -> AndroidAudioPermissionResult.Denied
        }
        continuation.resume(result)
    }

    // 已请求过且系统不再建议展示弹窗时，后续由 UI 提示进入系统设置。
    private fun shouldOpenSettingsInsteadOfRequest(permission: String): Boolean {
        if (!hasRequestedPermission(permission = permission)) {
            return false
        }
        return !activity.shouldShowRequestPermissionRationale(permission)
    }

    // 拒绝回调后再次判断，覆盖“本次拒绝后变成不再询问”的场景。
    private fun shouldOpenSettingsAfterDenied(permission: String): Boolean {
        return !activity.shouldShowRequestPermissionRationale(permission)
    }

    // 持久化权限请求历史，进程重启后仍能识别系统不再弹窗的状态。
    private fun hasRequestedPermission(permission: String): Boolean {
        return permissionPreferences.getBoolean(permission, false)
    }

    // 写入权限请求历史，按权限名隔离 Android 版本差异。
    private fun markPermissionRequested(permission: String) {
        permissionPreferences.edit().putBoolean(permission, true).apply()
    }

    /**
     * 用户确认后打开 App 详情页，让用户从系统设置恢复权限。
     */
    fun openAudioPermissionSettings() {
        val packageUri: Uri = Uri.fromParts("package", activity.packageName, null)
        val intent: Intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
        try {
            Log.d(TAG, "打开 App 系统权限设置页")
            activity.startActivity(intent)
        } catch (activityNotFoundException: ActivityNotFoundException) {
            Log.e(TAG, "打开 App 系统权限设置页失败", activityNotFoundException)
        }
    }
}

/**
 * Android 音频权限请求结果，区分普通拒绝和需要系统设置恢复的状态。
 */
internal enum class AndroidAudioPermissionResult {
    Granted,
    Denied,
    NeedsSettings,
}

// Android 13 及以上使用 READ_MEDIA_AUDIO，旧系统继续使用 READ_EXTERNAL_STORAGE。
private fun getRequiredAudioPermission(): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        return Manifest.permission.READ_MEDIA_AUDIO
    }
    return Manifest.permission.READ_EXTERNAL_STORAGE
}

// Android 音频权限请求器日志标签。
private const val TAG = "AndroidAudioPermissionRequester"

// 权限请求历史的 SharedPreferences 文件名。
private const val AUDIO_PERMISSION_PREFERENCES = "audio_permission_preferences"
