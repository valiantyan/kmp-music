package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.core.theme.scaledDp
import com.yanhao.kmpmusic.core.theme.scaledSp
import com.yanhao.kmpmusic.domain.model.LocalMusicDiscoveryPreferences
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceSummary
import com.yanhao.kmpmusic.feature.components.MobileSecondaryPage

private val scanPageBackground = Color(0xFFF8FAFB)
private val scanPageInk = Color(0xFF191C1D)
private val scanPageMuted = Color(0xFF3D4947)
private val scanPageAccent = Color(0xFF26A69A)

/**
 * 独立音频扫描页，承接首页空态入口和平台扫描动作。
 */
@Composable
fun AudioScanScreen(
    playableSongCount: Int,
    sources: List<LocalMusicSourceSummary>,
    scanState: LocalMusicScanState,
    discoveryPreferences: LocalMusicDiscoveryPreferences,
    discoveryPlatform: LocalMusicDiscoveryPlatform = LocalMusicDiscoveryPlatform.Android,
    onBack: () -> Unit,
    onScan: () -> Unit,
    onAutoScanOnLaunchChange: (Boolean) -> Unit,
    onShortAudioIgnoredChange: (Boolean) -> Unit,
    onSystemFoldersExcludedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val summaryDisplayModel: LocalMusicScanSummaryDisplayModel = buildLocalMusicScanSummaryDisplayModel(
        playableSongCount = playableSongCount,
        scanState = scanState,
    )
    MobileSecondaryPage(
        title = "扫描音频文件",
        onBack = onBack,
        backgroundColor = scanPageBackground,
        modifier = modifier,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(weight = 1f),
            contentPadding = PaddingValues(
                top = scaledDp(28.dp),
                bottom = contentPadding.calculateBottomPadding() + scaledDp(24.dp),
            ),
            verticalArrangement = Arrangement.spacedBy(scaledDp(28.dp)),
        ) {
        item(key = "scan-action") {
            ScanActionSection(
                scanState = scanState,
                discoveryPlatform = discoveryPlatform,
                onScan = onScan,
            )
        }
        item(key = "scan-statistics") {
            ScanStatisticsSection(summaryDisplayModel = summaryDisplayModel)
        }
        item(key = "scan-filters") {
            ScanFiltersSection(
                preferences = discoveryPreferences,
                discoveryPlatform = discoveryPlatform,
                onAutoScanOnLaunchChange = onAutoScanOnLaunchChange,
                onShortAudioIgnoredChange = onShortAudioIgnoredChange,
                onSystemFoldersExcludedChange = onSystemFoldersExcludedChange,
            )
        }
        item(key = "scan-folders-title") {
            SectionHeader(
                title = scanDirectoryTitle(discoveryPlatform = discoveryPlatform),
                action = "管理全部",
            )
        }
        if (sources.isEmpty()) {
            item(key = "scan-folders-empty") {
                ScanDirectoriesEmptyState(
                    discoveryPlatform = discoveryPlatform,
                    onScan = onScan,
                )
            }
        } else {
            item(key = "scan-folders") {
                ScanDirectoriesCard(
                    sources = sources,
                    discoveryPlatform = discoveryPlatform,
                    onScan = onScan,
                )
            }
        }
        }
    }
}

// 主扫描卡片只负责表达当前扫描状态和触发扫描动作。
@Composable
private fun ScanActionSection(
    scanState: LocalMusicScanState,
    discoveryPlatform: LocalMusicDiscoveryPlatform,
    onScan: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = scaledDp(20.dp))
            .fillMaxWidth()
            .shadow(elevation = scaledDp(14.dp), shape = RoundedCornerShape(size = scaledDp(24.dp))),
        shape = RoundedCornerShape(size = scaledDp(24.dp)),
        color = Color.White,
    ) {
        Column(
            modifier = Modifier.padding(all = scaledDp(24.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ScanPulseIcon()
            Text(
                text = scanStatusTitle(scanState = scanState),
                color = scanPageInk,
                fontSize = scaledSp(24.sp),
                lineHeight = scaledSp(32.sp),
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = scanStatusSubtitle(scanState = scanState),
                modifier = Modifier.padding(top = scaledDp(8.dp), bottom = scaledDp(24.dp)),
                color = scanPageMuted,
                fontSize = scaledSp(14.sp),
                lineHeight = scaledSp(20.sp),
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
            Surface(
                modifier = Modifier.width(scaledDp(200.dp)),
                shape = RoundedCornerShape(size = scaledDp(999.dp)),
                color = scanPageAccent,
                onClick = onScan,
            ) {
                Text(
                    text = localMusicScanActionLabel(
                        scanState = scanState,
                        platform = discoveryPlatform,
                    ),
                    modifier = Modifier.padding(vertical = scaledDp(12.dp)),
                    color = Color(0xFF003430),
                    fontSize = scaledSp(16.sp),
                    lineHeight = scaledSp(24.sp),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// 多层圆环保留扫描页的主要视觉识别，不依赖外部图片资源。
@Composable
private fun ScanPulseIcon() {
    Box(
        modifier = Modifier
            .size(scaledDp(184.dp))
            .padding(bottom = scaledDp(24.dp)),
        contentAlignment = Alignment.Center,
    ) {
        ScanRing(size = 160.dp, alpha = 0.10f)
        ScanRing(size = 128.dp, alpha = 0.20f)
        ScanRing(size = 96.dp, alpha = 0.30f)
        Surface(
            modifier = Modifier.size(scaledDp(64.dp)),
            shape = CircleShape,
            color = scanPageAccent,
            shadowElevation = scaledDp(6.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.LibraryMusic,
                    contentDescription = null,
                    tint = Color(0xFF003430),
                    modifier = Modifier.size(scaledDp(28.dp)),
                )
            }
        }
    }
}

// 单个扫描圆环。
@Composable
private fun ScanRing(size: androidx.compose.ui.unit.Dp, alpha: Float) {
    Box(
        modifier = Modifier
            .size(scaledDp(size))
            .border(
                width = scaledDp(2.dp),
                color = scanPageAccent.copy(alpha = alpha),
                shape = CircleShape,
            ),
    )
}

// 曲库统计只暴露总数和最后扫描时间，不泄露增删改内部计数。
@Composable
private fun ScanStatisticsSection(summaryDisplayModel: LocalMusicScanSummaryDisplayModel) {
    Column(
        modifier = Modifier.padding(horizontal = scaledDp(20.dp)),
        verticalArrangement = Arrangement.spacedBy(scaledDp(12.dp)),
    ) {
        SectionHeader(title = "库统计")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(size = scaledDp(24.dp)),
            color = scanPageAccent.copy(alpha = 0.10f),
            border = BorderStroke(width = scaledDp(1.dp), color = scanPageAccent.copy(alpha = 0.20f)),
        ) {
            Row(
                modifier = Modifier.padding(all = scaledDp(25.dp)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatisticColumn(
                    value = summaryDisplayModel.playableSongTotalText.substringBefore(delimiter = " "),
                    label = "已发现曲目",
                    alignEnd = false,
                )
                Box(
                    modifier = Modifier
                        .height(scaledDp(40.dp))
                        .width(scaledDp(1.dp))
                        .background(scanPageAccent.copy(alpha = 0.20f)),
                )
                StatisticColumn(
                    value = summaryDisplayModel.lastScanTimeText.removePrefix(prefix = "最近扫描："),
                    label = "上次扫描",
                    alignEnd = true,
                )
            }
        }
    }
}

// 统计列在小屏上保持固定层级。
@Composable
private fun StatisticColumn(
    value: String,
    label: String,
    alignEnd: Boolean,
) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(
            text = value,
            color = if (alignEnd) scanPageInk else scanPageAccent,
            fontSize = scaledSp(if (alignEnd) 16.sp else 32.sp),
            lineHeight = scaledSp(if (alignEnd) 24.sp else 48.sp),
            fontWeight = if (alignEnd) FontWeight.Medium else FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = label,
            color = scanPageMuted,
            fontSize = scaledSp(12.sp),
            lineHeight = scaledSp(16.sp),
            fontWeight = FontWeight.Medium,
        )
    }
}

// 过滤器读取全局偏好，避免扫描中重组或离开页面后丢失用户选择。
@Composable
private fun ScanFiltersSection(
    preferences: LocalMusicDiscoveryPreferences,
    discoveryPlatform: LocalMusicDiscoveryPlatform,
    onAutoScanOnLaunchChange: (Boolean) -> Unit,
    onShortAudioIgnoredChange: (Boolean) -> Unit,
    onSystemFoldersExcludedChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = scaledDp(20.dp)),
        verticalArrangement = Arrangement.spacedBy(scaledDp(12.dp)),
    ) {
        SectionHeader(title = "扫描过滤器")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(size = scaledDp(24.dp)),
            color = Color.White,
            shadowElevation = scaledDp(4.dp),
        ) {
            Column(modifier = Modifier.padding(all = scaledDp(12.dp))) {
                FilterRow(
                    title = "启动时自动扫描",
                    subtitle = "每次打开应用时检查更新",
                    checked = preferences.isAutoScanOnLaunchEnabled,
                    onCheckedChange = onAutoScanOnLaunchChange,
                )
                FilterRow(
                    title = "忽略短音频 (<30s)",
                    subtitle = "过滤通知音和语音消息",
                    checked = preferences.shouldIgnoreShortAudio,
                    onCheckedChange = onShortAudioIgnoredChange,
                )
                FilterRow(
                    title = excludeSystemFoldersTitle(discoveryPlatform = discoveryPlatform),
                    subtitle = excludeSystemFoldersSubtitle(discoveryPlatform = discoveryPlatform),
                    checked = preferences.shouldExcludeSystemFolders,
                    onCheckedChange = onSystemFoldersExcludedChange,
                )
            }
        }
    }
}

// 过滤器行保持可点控件和说明文案分离。
@Composable
private fun FilterRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = scaledDp(12.dp), vertical = scaledDp(12.dp)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(weight = 1f)) {
            Text(
                text = title,
                color = scanPageInk,
                fontSize = scaledSp(16.sp),
                lineHeight = scaledSp(24.sp),
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                color = scanPageMuted,
                fontSize = scaledSp(12.sp),
                lineHeight = scaledSp(16.sp),
                fontWeight = FontWeight.Medium,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// 扫描目录列表直接来自当前来源摘要。
@Composable
private fun ScanDirectoriesCard(
    sources: List<LocalMusicSourceSummary>,
    discoveryPlatform: LocalMusicDiscoveryPlatform,
    onScan: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = scaledDp(20.dp))
            .fillMaxWidth(),
        shape = RoundedCornerShape(size = scaledDp(24.dp)),
        color = Color.White,
        shadowElevation = scaledDp(4.dp),
    ) {
        Column(
            modifier = Modifier.padding(all = scaledDp(12.dp)),
            verticalArrangement = Arrangement.spacedBy(scaledDp(8.dp)),
        ) {
            sources.forEach { source: LocalMusicSourceSummary ->
                ScanDirectoryRow(source = source)
            }
            AddFolderButton(
                discoveryPlatform = discoveryPlatform,
                onScan = onScan,
            )
        }
    }
}

// 单个来源行用目录图标和删除图标对应 Figma 结构。
@Composable
private fun ScanDirectoryRow(source: LocalMusicSourceSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = scanPageBackground, shape = RoundedCornerShape(size = scaledDp(12.dp)))
            .border(
                width = scaledDp(1.dp),
                color = MusicColors.Line.copy(alpha = 0.30f),
                shape = RoundedCornerShape(size = scaledDp(12.dp)),
            )
            .padding(all = scaledDp(13.dp)),
        horizontalArrangement = Arrangement.spacedBy(scaledDp(16.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(scaledDp(40.dp)),
            shape = CircleShape,
            color = scanPageAccent.copy(alpha = 0.10f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Folder,
                    contentDescription = null,
                    tint = scanPageAccent,
                    modifier = Modifier.size(scaledDp(22.dp)),
                )
            }
        }
        Column(modifier = Modifier.weight(weight = 1f)) {
            Text(
                text = source.displayName,
                color = scanPageInk,
                fontSize = scaledSp(14.sp),
                lineHeight = scaledSp(20.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "包含 ${source.songCount} 个文件",
                color = scanPageMuted,
                fontSize = scaledSp(10.sp),
                lineHeight = scaledSp(15.sp),
                fontWeight = FontWeight.Medium,
            )
        }
        Icon(
            imageVector = Icons.Rounded.Delete,
            contentDescription = null,
            tint = scanPageMuted,
            modifier = Modifier.size(scaledDp(18.dp)),
        )
    }
}

// 没有来源时保持 Figma 卡片区域，但不伪造目录。
@Composable
private fun ScanDirectoriesEmptyState(
    discoveryPlatform: LocalMusicDiscoveryPlatform,
    onScan: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = scaledDp(20.dp))
            .fillMaxWidth(),
        shape = RoundedCornerShape(size = scaledDp(24.dp)),
        color = Color.White,
        shadowElevation = scaledDp(4.dp),
    ) {
        Column(
            modifier = Modifier.padding(all = scaledDp(12.dp)),
            verticalArrangement = Arrangement.spacedBy(scaledDp(12.dp)),
        ) {
            Text(
                text = "还没有扫描目录",
                modifier = Modifier.padding(horizontal = scaledDp(12.dp), vertical = scaledDp(8.dp)),
                color = scanPageMuted,
                fontSize = scaledSp(14.sp),
                lineHeight = scaledSp(20.sp),
            )
            AddFolderButton(
                discoveryPlatform = discoveryPlatform,
                onScan = onScan,
            )
        }
    }
}

// 添加来源按钮沿用虚线边框，当前点击仍交给主扫描入口完成平台动作。
@Composable
private fun AddFolderButton(
    discoveryPlatform: LocalMusicDiscoveryPlatform,
    onScan: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onScan)
            .border(
                width = scaledDp(2.dp),
                color = scanPageAccent.copy(alpha = 0.50f),
                shape = RoundedCornerShape(size = scaledDp(12.dp)),
            )
            .padding(vertical = scaledDp(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(scaledDp(8.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.AddCircleOutline,
                contentDescription = null,
                tint = scanPageAccent,
                modifier = Modifier.size(scaledDp(20.dp)),
            )
            Text(
                text = addSourceLabel(discoveryPlatform = discoveryPlatform),
                color = scanPageAccent,
                fontSize = scaledSp(14.sp),
                lineHeight = scaledSp(16.sp),
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// 区块标题和可选操作文字。
@Composable
private fun SectionHeader(
    title: String,
    action: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = scaledDp(32.dp)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = scanPageMuted,
            fontSize = scaledSp(14.sp),
            lineHeight = scaledSp(16.sp),
            fontWeight = FontWeight.Medium,
        )
        if (action != null) {
            Text(
                text = action,
                color = scanPageAccent,
                fontSize = scaledSp(16.sp),
                lineHeight = scaledSp(24.sp),
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// 扫描状态标题映射到 Figma 主卡片。
private fun scanStatusTitle(scanState: LocalMusicScanState): String {
    return when (scanState) {
        LocalMusicScanState.Idle -> "准备就绪"
        LocalMusicScanState.WaitingForPermission -> "等待授权"
        is LocalMusicScanState.Importing,
        is LocalMusicScanState.Scanning,
        -> "正在扫描"
        is LocalMusicScanState.Done -> "扫描完成"
        is LocalMusicScanState.Cancelled -> "已取消"
        is LocalMusicScanState.Error -> "扫描失败"
    }
}

// 扫描状态副标题解释当前动作，不泄露内部增删改统计。
private fun scanStatusSubtitle(scanState: LocalMusicScanState): String {
    return when (scanState) {
        LocalMusicScanState.Idle -> "扫描您的设备以发现新音乐"
        LocalMusicScanState.WaitingForPermission -> "需要授权后才能发现本地音乐"
        is LocalMusicScanState.Importing -> "正在导入您选择的音频"
        is LocalMusicScanState.Scanning -> "正在发现本地音频文件"
        is LocalMusicScanState.Done -> "当前曲库已更新"
        is LocalMusicScanState.Cancelled -> "当前曲库已保留，可稍后继续扫描"
        is LocalMusicScanState.Error -> "请稍后重试或检查授权状态"
    }
}

// 平台扫描目录标题。
private fun scanDirectoryTitle(discoveryPlatform: LocalMusicDiscoveryPlatform): String {
    return when (discoveryPlatform) {
        LocalMusicDiscoveryPlatform.Android -> "扫描来源"
        LocalMusicDiscoveryPlatform.Desktop -> "扫描目录"
        LocalMusicDiscoveryPlatform.Ios -> "音频来源"
    }
}

// 排除项标题根据平台避免 Android 文案误导 iOS/Desktop。
private fun excludeSystemFoldersTitle(discoveryPlatform: LocalMusicDiscoveryPlatform): String {
    return when (discoveryPlatform) {
        LocalMusicDiscoveryPlatform.Android -> "排除系统文件夹"
        LocalMusicDiscoveryPlatform.Desktop -> "排除系统目录"
        LocalMusicDiscoveryPlatform.Ios -> "排除系统音频"
    }
}

// 排除项说明根据平台收敛。
private fun excludeSystemFoldersSubtitle(discoveryPlatform: LocalMusicDiscoveryPlatform): String {
    return when (discoveryPlatform) {
        LocalMusicDiscoveryPlatform.Android -> "跳过 Android 系统音频目录"
        LocalMusicDiscoveryPlatform.Desktop -> "跳过系统和缓存目录"
        LocalMusicDiscoveryPlatform.Ios -> "跳过系统提示音和不可访问来源"
    }
}

// 添加来源按钮文案复用平台真实授权模型。
private fun addSourceLabel(discoveryPlatform: LocalMusicDiscoveryPlatform): String {
    return when (discoveryPlatform) {
        LocalMusicDiscoveryPlatform.Android -> "添加来源"
        LocalMusicDiscoveryPlatform.Desktop -> "添加文件夹"
        LocalMusicDiscoveryPlatform.Ios -> "导入音频"
    }
}
