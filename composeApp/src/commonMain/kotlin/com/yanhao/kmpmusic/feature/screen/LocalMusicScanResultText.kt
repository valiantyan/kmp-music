package com.yanhao.kmpmusic.feature.screen

import com.yanhao.kmpmusic.domain.model.LocalMusicScanState

private const val CANCELLED_SCAN_TITLE = "已取消"
private const val CANCELLED_SCAN_MESSAGE = "当前曲库已保留，部分结果可在下次扫描补齐。"

// 取消结果标题属于 UI 呈现层，domain 只保留取消事实和结果时间。
internal fun cancelledScanResultTitle(scanState: LocalMusicScanState.Cancelled): String = CANCELLED_SCAN_TITLE

// 取消结果说明保持单一来源，移动端和桌面来源页共同复用。
internal fun cancelledScanResultDetail(scanState: LocalMusicScanState.Cancelled): String = "$CANCELLED_SCAN_MESSAGE · 最近结果：${formatLocalMusicScanDate(timestampMillis = scanState.summary.completedAt)}"

// commonMain 中避免平台日期 API，用纯算法生成稳定日期文本。
internal fun formatLocalMusicScanDate(timestampMillis: Long): String {
    val epochDay: Long = timestampMillis.floorDiv(86_400_000L)
    val shiftedDay: Long = epochDay + 719_468L
    val eraOffset: Long = if (shiftedDay >= 0L) shiftedDay else shiftedDay - 146_096L
    val eraIndex: Long = eraOffset / 146_097L
    val dayOfEra: Long = shiftedDay - eraIndex * 146_097L
    val yearOfEra: Long =
        (
            dayOfEra - dayOfEra / 1_460L + dayOfEra / 36_524L - dayOfEra / 146_096L
        ) / 365L
    val yearBase: Long = yearOfEra + eraIndex * 400L
    val dayOfYear: Long = dayOfEra - (365L * yearOfEra + yearOfEra / 4L - yearOfEra / 100L)
    val monthPrime: Long = (5L * dayOfYear + 2L) / 153L
    val day: Int = (dayOfYear - (153L * monthPrime + 2L) / 5L + 1L).toInt()
    val month: Int = (monthPrime + if (monthPrime < 10L) 3L else -9L).toInt()
    val year: Int = (yearBase + if (month <= 2) 1L else 0L).toInt()
    return "${year.toString().padStart(length = 4, padChar = '0')}-${month.toString().padStart(length = 2, padChar = '0')}-${day.toString().padStart(length = 2, padChar = '0')}"
}
