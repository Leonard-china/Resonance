# 2026-10-15 — 移除设备同步和酷狗歌单链接，保留歌单文件

- 修改日期：2026-10-15（Asia/Shanghai）
- 类型：功能移除 / 兼容修复
- 版本：0.3.5 / 22，未升版
- 平台：Android / Windows（平台差异见下文）
- 关联：用户本轮深度优化需求；[验证与交接](2026-10-15-12-verification-and-handoff.md)

## 需求、原因与依据

按用户要求移除经常失败的酷狗分享链接和设备/LAN/二维码同步；保留完整音频歌单文件导入/导出。

## 具体修改

- 删除 KugouPlaylistImporter/PlaylistLinkDialog、DesktopLanShare、EncryptedSyncPackage、Android sync Intent/总线及相关 UI/依赖/测试。
- 目标页改 PlaylistArchive，仅导入/导出 .resonance ZIP；内部 Sync 命名/明文格式保留数据兼容。
- 修正原始 FLAC 等音频被统一标为 .mp3 的问题：清单、导出路径、目标扩展及 MIME 保留实际格式。
- 保留历史歌单/目录回匹配、酷狗元数据检索和 Windows KGG 密钥能力。

| 文件 | 关键修改 |
| --- | --- |
| [sync/PlainSyncPackage.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/sync/PlainSyncPackage.kt) | audioExtension/createManifest/import，原格式保留 |
| [ui/ToolsScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/ToolsScreens.kt) | 歌单导入/导出入口 |
| [AndroidManifest.xml](../../composeApp/src/androidMain/AndroidManifest.xml) | 移除 sync deeplink |
| [features/PlaylistArchiveFormatTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/PlaylistArchiveFormatTest.kt) | FLAC/hash/封面往返 |

## 修改前后

以前有分享链接、二维码、LAN 及文件包；现在只通过文件导入/导出歌单，已有库保留。

## 平台与数据影响

明文 ZIP 不包含 Key/Cookie；旧加密包明确拒绝，普通旧包兼容。保留音频 hash、staging、路径/容量验证；同名托管目标允许替换。

## 验证

PlainSyncPackageTest、PlaylistArchiveFormatTest、CrossDeviceSyncFixtureTest；两端编译。

实际命令、结果与设备/网络限制统一记录在 [本轮验证](2026-10-15-12-verification-and-handoff.md)，不把模拟服务测试当作付费API或B站真实下载通过。

## 限制与后续

Android 不能导入旧加密包，需要旧应用重新导出普通包；不提供设备自动同步。

## 同步文档

[开发说明](../DEVELOPMENT.md)、[索引](../README.md)、[项目说明](../../README.md)；外部请求/凭据见 [第三方说明](../THIRD_PARTY.md)。
