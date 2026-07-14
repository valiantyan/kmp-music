# Apple 平台格式支持矩阵

本矩阵是 iOS 与 macOS 首轮 Apple 原生播放迁移的 P0 格式边界。扫描入口只发布 Apple P0 格式；待验证格式不会进入可播放曲库，避免用户看到“扫描成功但播放失败且无法自救”。当前自动化格式样本证据来自 macOS AVFoundation，iOS 导入扫描复用同一 P0 allowlist，但 iOS 真实样本播放仍需真机或后续 gate 验证，不能把 macOS smoke 伪装成 iOS 播放证据。

| 格式 | 扫描入口 | macOS 自动化证据 | iOS 边界 | 说明 |
| --- | --- | --- | --- | --- |
| MP3 | 支持 | `macosAvFoundationBridgeSmoke` 用本机编码器生成 MP3 样本，并通过 AVFoundation `AVURLAsset.load(.isPlayable)` 检查。 | 导入扫描按 Apple P0 放行；真实样本播放仍需真机或后续 gate 验证。 | 作为 Apple P0 格式进入扫描结果。 |
| M4A/AAC | 支持 | `macosAvFoundationBridgeSmoke` 真实播放 M4A/AAC 样本，并对 M4A 与 AAC ADTS 样本执行 AVFoundation 可播放性检查。 | 导入扫描按 Apple P0 放行；真实样本播放仍需真机或后续 gate 验证。 | `.m4a` 与 `.aac` 进入扫描结果。 |
| WAV | 支持 | `macosAvFoundationBridgeSmoke` 生成 WAV 样本，并通过 AVFoundation 可播放性检查。 | 导入扫描按 Apple P0 放行；真实样本播放仍需真机或后续 gate 验证。 | PCM WAV 进入扫描结果。 |
| FLAC | 支持 | `macosAvFoundationBridgeSmoke` 生成 FLAC 样本，并通过 AVFoundation 可播放性检查。 | 导入扫描按 Apple P0 放行；真实样本播放仍需真机或后续 gate 验证。 | `.flac` 进入扫描结果。 |
| AIFF/ALAC | 支持 | `macosAvFoundationBridgeSmoke` 生成 AIFF 与 ALAC-in-M4A 样本，并通过 AVFoundation 可播放性检查。 | 导入扫描按 Apple P0 放行；真实样本播放仍需真机或后续 gate 验证。 | `.aif`、`.aiff` 进入扫描结果；ALAC 以 `.m4a` 容器进入扫描，不单独放行未验证的 `.alac` 扩展名。 |
| OGG/OPUS | 待验证 | 本票未取得可靠样本播放证据。 | 本票未取得可靠样本播放证据。 | `.ogg`、`.oga`、`.opus` 暂不进入扫描结果。 |
| AMR | 待验证 | 本票未取得可靠样本播放证据。 | 本票未取得可靠样本播放证据。 | `.amr`、`.awb` 暂不进入扫描结果。 |

播放失败时，用户文案按错误类型提供不同自救路径：缺文件提示恢复文件后重新扫描，权限拒绝提示重新授权或重新导入，格式不支持和受保护资源提示换用已验证格式的无保护本地文件，播放器组件不可用提示 Apple 播放组件不可用，未知错误提示稍后重试并重新扫描或更换已验证格式。
