package com.yanhao.kmpmusic.data

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import java.io.File
import com.yanhao.kmpmusic.AndroidAudioPermissionResult
import com.yanhao.kmpmusic.domain.model.LocalMusicScanError
import com.yanhao.kmpmusic.domain.model.LocalMusicScanErrorType
import com.yanhao.kmpmusic.domain.model.LocalMusicScanException
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceSummary
import com.yanhao.kmpmusic.domain.model.MusicFileMetadata
import com.yanhao.kmpmusic.domain.repository.LocalMusicScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android 平台本地音乐扫描器，从 [MediaStore.Audio.Media] 查询用户授权的系统音频索引。
 */
internal class AndroidMediaStoreScanner(
    context: Context,
    private val requestAudioPermission: suspend () -> AndroidAudioPermissionResult,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) : LocalMusicScanner {
    // 使用 application context 的 resolver 查询 MediaStore，避免持有 Activity。
    private val contentResolver: ContentResolver = context.contentResolver

    // MediaStore 游标到 common 元数据的映射器。
    private val metadataReader: AndroidMediaStoreMetadataReader = AndroidMediaStoreMetadataReader(
        artworkExtractor = AndroidEmbeddedArtworkExtractor(
            context = context,
            cacheDirectory = context.cacheDir,
        ),
    )

    /** 执行 Android MediaStore 扫描，并把权限或查询失败映射为 common 错误。 */
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult {
        if (request is LocalMusicScanRequest.Source && request.sourceKind != LocalMusicSourceKind.AndroidMediaStore) {
            return LocalMusicScanResult(
                discovered = emptyList(),
                completedAt = nowMillis(),
            )
        }
        when (requestAudioPermission()) {
            AndroidAudioPermissionResult.Granted -> Unit
            AndroidAudioPermissionResult.Denied -> throw LocalMusicScanException(error = buildPermissionDeniedError())
            AndroidAudioPermissionResult.NeedsSettings -> throw LocalMusicScanException(error = buildPermissionSettingsError())
        }
        return withContext(context = Dispatchers.IO) {
            queryMediaStore()
        }
    }

    // 查询系统音频媒体库；失败会转成平台无关错误，避免 UI 依赖 Android 异常。
    private fun queryMediaStore(): LocalMusicScanResult {
        try {
            Log.d(TAG, "开始查询 Android MediaStore 音频")
            val collectionUri: Uri = getAudioCollectionUri()
            val cursor: Cursor = contentResolver.query(
                collectionUri,
                AUDIO_PROJECTION,
                AUDIO_SELECTION,
                null,
                "${MediaStore.Audio.Media.DATE_MODIFIED} DESC",
            ) ?: throw LocalMusicScanException(error = buildUnknownError(message = "系统媒体库没有返回查询结果"))
            cursor.use { mediaCursor: Cursor ->
                val discovered: List<MusicFileMetadata> = metadataReader.readMetadata(
                    cursor = mediaCursor,
                    collectionUri = collectionUri,
                )
                val completedAt: Long = nowMillis()
                Log.d(TAG, "Android MediaStore 音频查询完成，发现 ${discovered.size} 首歌曲")
                return LocalMusicScanResult(
                    discovered = discovered,
                    sourceSummaries = listOf(
                        LocalMusicSourceSummary(
                            sourceKind = LocalMusicSourceKind.AndroidMediaStore,
                            displayName = LocalMusicSourceKind.AndroidMediaStore.displayName,
                            songCount = discovered.size,
                            problemCount = 0,
                            lastScannedAt = completedAt,
                        ),
                    ),
                    completedAt = completedAt,
                )
            }
        } catch (securityException: SecurityException) {
            Log.e(TAG, "Android MediaStore 查询缺少权限", securityException)
            throw LocalMusicScanException(
                error = buildPermissionDeniedError(),
                cause = securityException,
            )
        } catch (illegalArgumentException: IllegalArgumentException) {
            Log.e(TAG, "Android MediaStore 查询参数不可用", illegalArgumentException)
            throw LocalMusicScanException(
                error = buildUnknownError(message = "系统媒体库查询失败"),
                cause = illegalArgumentException,
            )
        }
    }

    // Android 10 以后使用卷名 API，旧系统保持兼容 URI。
    private fun getAudioCollectionUri(): Uri {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        }
        return MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }

    // 构造权限拒绝错误，供控制器进入统一错误态。
    private fun buildPermissionDeniedError(): LocalMusicScanError {
        return LocalMusicScanError(
            type = LocalMusicScanErrorType.PermissionDenied,
            message = "需要音频权限后才能扫描本机歌曲",
            sourceKind = LocalMusicSourceKind.AndroidMediaStore,
        )
    }

    // 构造永久拒绝错误，让 common UI 提示用户回到系统权限设置。
    private fun buildPermissionSettingsError(): LocalMusicScanError {
        return LocalMusicScanError(
            type = LocalMusicScanErrorType.PermissionPermanentlyDenied,
            message = "请到系统设置开启音频权限",
            sourceKind = LocalMusicSourceKind.AndroidMediaStore,
        )
    }

    // 构造未知扫描错误，保留平台边界内的异常上下文。
    private fun buildUnknownError(message: String): LocalMusicScanError {
        return LocalMusicScanError(
            type = LocalMusicScanErrorType.Unknown,
            message = message,
            sourceKind = LocalMusicSourceKind.AndroidMediaStore,
        )
    }
}

// Android scanner 日志标签。
private const val TAG = "AndroidMediaStoreScanner"

// MediaStore 只读取构建曲库快照所需字段，避免把 UI 绑定到平台游标。
private val AUDIO_PROJECTION: Array<String> = arrayOf(
    MediaStore.Audio.Media._ID,
    MediaStore.Audio.Media.DISPLAY_NAME,
    MediaStore.Audio.Media.TITLE,
    MediaStore.Audio.Media.ARTIST,
    MediaStore.Audio.Media.ALBUM,
    MediaStore.Audio.Media.DURATION,
    MediaStore.Audio.Media.MIME_TYPE,
    MediaStore.Audio.Media.SIZE,
    MediaStore.Audio.Media.DATE_MODIFIED,
)

// P0 使用系统音乐标记过滤铃声、通知音等非歌曲条目。
private const val AUDIO_SELECTION = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
