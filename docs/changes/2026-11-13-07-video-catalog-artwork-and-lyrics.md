# 2026-11-13 — 为 B 站歌曲升级专辑封面并统一歌词查询

- 修改日期：2026-11-13（Asia/Shanghai）
- 类型：功能与缺陷修复
- 适用版本：0.6.11 / 45，版本号保持原值
- 影响平台：Android、Windows、Linux
- 关联：[智能补全弹窗隔离](2026-11-13-05-isolate-batch-enrich-dialog.md)

## 需求与原因

带有视频缩略图的曲目被算成已有封面，普通批量补全跳过它们。下载后的自动查词与批量查词只走本地或 LRCLIB，而播放器还会查询启用的歌源。用户报告下载后经常遇到视频截图充当专辑封面和缺歌词的情况。

## 修改与行为

| 文件或符号 | 修改 |
| --- | --- |
| [MusicSourceModels.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/MusicSourceModels.kt)、`BatchEnrichDialog` | 根据来源识别待匹配封面。视频图可直接补全，正式目录图可复用，自选封面在批量操作中受保护。 |
| [AlbumCoverRepository.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/enrich/AlbumCoverRepository.kt) | 保留版本标记，去掉质量标签，核对歌名和歌手。QQ 目录优先精确专辑，其次优先非精选合辑。图片获取失败继续尝试下一目录。新查询缓存使用 `cover-v2`，无候选缓存 15 分钟，图片故障可继续重试。 |
| [JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt)、[MetadataEnricher.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/enrich/MetadataEnricher.kt) | 复用封面匹配步骤和失败记录，旧 B 站下载通过来源索引识别。更新封面仅写曲库和封面溯源。下载入库、批量和播放器通过 `resolveLyrics` 查询本地、歌词库及可用歌源，优先有效同步歌词。下载阶段查词整体上限 75 秒，封面或歌词失败可以单独重试。 |
| [LrclibLyricsRepository.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/lyrics/LrclibLyricsRepository.kt)、[MusicSourceRegistry.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/sources/MusicSourceRegistry.kt) | 线上查词使用保留版本标记的规范化歌名。本地文件和缓存继续按原身份索引。视频明确标注现场或翻唱而曲库歌名缺少版本时，保留本地歌词入口并提示核对版本，避免套用录音室结果。 |
| [App.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt)、Android 和桌面平台适配 | 批量开始前等待已提交的存储操作，防止重复启动。停止后等待任务退出再重载，已逐首写入的结果可保留。移除批次结束时对已处理曲目的重复写入。 |
| [MusicCompletionTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/MusicCompletionTest.kt) | 覆盖旧视频来源、自选封面、目录图失败回退、歌词旧未命中恢复、下载查词、版本保护和取消。 |

## 平台与数据

三端共享匹配策略，Android 和桌面沿用各自曲库持久化。旧来源索引、歌词缓存、歌单关系和同步包兼容。本轮补全操作不删除音频、不修改原文件标签、不上传音频。仅原有显式 AI 兜底选项可调用已配置模型。

## 验证

本轮在隔离的 Git 交付分支验证，构建进程树限制为 2 GiB，单 worker 和进程内 Kotlin。Gradle `:composeApp:desktopTest` 已编译桌面生产和测试源码，渲染执行在限额内耗时过长后停止。复用同批编译产物与 Gradle 依赖，在 Xvfb 中直接运行 JUnit，九个定点测试类 84 项通过、0 失败、2 项显式开启的公网歌词测试跳过。覆盖来源与版本判定、目录回退、歌词缓存及单项重试、补全窗口停止与完成。日志为忽略目录 `build/music-completion-2026-11-13/delivery-validation/junit.log`。

Android `:composeApp:assembleDebug :composeApp:assembleDebugAndroidTest` 构建成功。Debug APK 使用 `com.resonance.player.debug`，`apksigner verify` 和 `sha256sum -c SHA256SUMS.txt` 通过。 2026-11-13 至 11-14 的 Android 16 模拟器启动在 2 GiB 进程树限额下超时，含干净镜像和低内存配置复试。系统未完成启动，`BatchEnrichDialogDeviceTest` 未执行；原手机也未连接。

## 限制与后续

目录元数据是录音版本证据，不能证明视频音轨与专辑发行一致。多个相近发行版的候选供选及片头偏移辅助仍属后续范围。原手机未连接，真机响应、触摸与实际曲库操作待复测。

## 同步文档

更新 [文档索引](../README.md) 和 [开发说明](../DEVELOPMENT.md) 第 8、13、14 节。
