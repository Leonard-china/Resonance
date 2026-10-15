# 2026-10-15 — 新增双平台后台悬浮歌词和任意目标语言翻译

- 修改日期：2026-10-15（Asia/Shanghai）
- 类型：功能
- 版本：0.3.5 / 22，未升版
- 平台：Android / Windows（平台差异见下文）
- 关联：用户本轮深度优化需求；[验证与交接](2026-10-15-12-verification-and-handoff.md)

## 需求、原因与依据

用户要求 Windows 与 Android 后台悬浮歌词，支持中文译英文等方向而非仅英译中。

## 具体修改

- 共享 FloatingLyricsHost/选行、原文/译文/双语设置，拖动、字号和锁定点击穿透。
- Windows透明置顶窗口与JNA；Android由PlaybackService持有WindowManager，后台换曲独立取本地/LRCLIB/已有译文。
- 歌词按50行请求等长JSON，核对行数后沿用原时间轴，原文hash+语言持久缓存，重译显式触发。
- Android系统悬浮权限单独申请，位置持久；后台不自动调用收费翻译，纯文本不伪造同步。
- Android 直接按原生毫秒位置选行，避免后台换曲时元数据时长未准备好而卡在第一行；拖动限制在屏幕内。Windows 歌词窗随字号调整高度，避免大字号被固定高度裁切。

| 文件 | 关键修改 |
| --- | --- |
| [platform/FloatingLyricsHost.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/FloatingLyricsHost.kt) | floatingLyricLines/expect |
| [ui/DesktopFloatingLyrics.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/ui/DesktopFloatingLyrics.kt) | 置顶/拖动/样式穿透 |
| [features/AndroidLyricsOverlay.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/features/AndroidLyricsOverlay.kt) | 服务持有与原生进度 |
| [features/JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | translate/cachedTranslation |

## 修改前后

以前只有未装配的桌面组件；现在两端主流程可开悬浮歌词，并支持自定义目标与缓存翻译。

## 平台与数据影响

不覆盖源歌词/时间戳；Key和译文按统一状态存储。Android需悬浮权限，进程/播放服务必须活着。

## 验证

FloatingLyricsTest 验时间轴/模式/缺译文；MusicAssistantTest 验双向目标/缓存/错行拒绝。两端编译及界面冒烟见验证记录。

实际命令、结果与设备/网络限制统一记录在 [本轮验证](2026-10-15-12-verification-and-handoff.md)，不把模拟服务测试当作付费API或B站真实下载通过。

## 限制与后续

无时间轴无准确同步；系统强停后需重新播放；各OEM后台/overlay策略和Windows不同显示缩放仍需实际使用确认。

## 同步文档

[开发说明](../DEVELOPMENT.md)、[索引](../README.md)、[项目说明](../../README.md)；外部请求/凭据见 [第三方说明](../THIRD_PARTY.md)。
