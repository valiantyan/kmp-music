package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.LocalMusicScanError
import com.yanhao.kmpmusic.domain.model.LocalMusicScanErrorType
import com.yanhao.kmpmusic.domain.model.LocalMusicScanException
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerMode
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * iOS 文件夹选择器，负责把 UIKit delegate 回调桥接为挂起函数。
 */
internal class IosFolderPicker {
    /** 使用系统文件夹选择器获取用户授权目录。 */
    suspend fun chooseMusicFolder(): NSURL {
        return suspendCancellableCoroutine { continuation ->
            dispatch_async(queue = dispatch_get_main_queue()) {
                presentFolderPicker(continuation = continuation)
            }
        }
    }

    // 展示文档选择器，找不到宿主控制器时返回明确错误。
    private fun presentFolderPicker(continuation: CancellableContinuation<NSURL>) {
        val presentingViewController: UIViewController = findPresentingViewController()
            ?: run {
                continuation.resumeWithException(exception = missingPresenterException())
                return
            }
        val delegate: IosFolderPickerDelegate = IosFolderPickerDelegate(continuation = continuation)
        IosFolderPickerRetainer.delegate = delegate
        val picker: UIDocumentPickerViewController = UIDocumentPickerViewController(
            documentTypes = listOf("public.folder", "public.directory"),
            inMode = UIDocumentPickerMode.UIDocumentPickerModeOpen,
        )
        picker.allowsMultipleSelection = false
        picker.delegate = delegate
        presentingViewController.presentViewController(
            viewControllerToPresent = picker,
            animated = true,
            completion = null,
        )
    }

    // 寻找当前可展示文档选择器的顶层 [UIViewController]。
    private fun findPresentingViewController(): UIViewController? {
        var viewController: UIViewController? = UIApplication.sharedApplication.keyWindow?.rootViewController
        while (viewController?.presentedViewController != null) {
            viewController = viewController.presentedViewController
        }
        return viewController
    }

    // 构造找不到宿主控制器时的统一错误。
    private fun missingPresenterException(): LocalMusicScanException {
        return LocalMusicScanException(
            error = LocalMusicScanError(
                type = LocalMusicScanErrorType.Unknown,
                message = "无法打开 iOS 文件夹选择器",
                sourceKind = LocalMusicSourceKind.IosImportedFile,
            ),
        )
    }
}

// 保留文档选择器 delegate，避免系统回调前被 Kotlin/Native 释放。
private object IosFolderPickerRetainer {
    var delegate: IosFolderPickerDelegate? = null
}

/**
 * iOS 文档选择器回调代理，将系统选择结果恢复为挂起扫描流程。
 */
@OptIn(ExperimentalForeignApi::class)
private class IosFolderPickerDelegate(
    private val continuation: CancellableContinuation<NSURL>,
) : NSObject(), UIDocumentPickerDelegateProtocol {
    /** 处理用户选中的文件夹 URL。 */
    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        val folderUrl: NSURL? = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        IosFolderPickerRetainer.delegate = null
        if (!continuation.isActive) {
            return
        }
        if (folderUrl == null) {
            continuation.resumeWithException(exception = cancelledException())
            return
        }
        continuation.resume(value = folderUrl)
    }

    /** 处理用户取消选择的情况。 */
    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        IosFolderPickerRetainer.delegate = null
        if (!continuation.isActive) {
            return
        }
        continuation.resumeWithException(exception = cancelledException())
    }

    // 将取消选择映射为平台无关扫描错误。
    private fun cancelledException(): LocalMusicScanException {
        return LocalMusicScanException(
            error = LocalMusicScanError(
                type = LocalMusicScanErrorType.UserCancelled,
                message = "未选择音乐文件夹",
                sourceKind = LocalMusicSourceKind.IosImportedFile,
            ),
        )
    }
}
