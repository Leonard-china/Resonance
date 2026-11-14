# 2026-11-14 — 合并 Android 同一音频的两种导入记录并修正导入提示

- 修改日期：2026-11-14（Asia/Shanghai）
- 类型：Bug 修复
- 适用版本：0.6.11 / 45
- 影响平台：Android 曲库、歌单、收藏和导入提示
- 关联：[正式包探索性测试](2026-11-14-03-android-exploratory-testing.md)

## 需求与原因

同一目录经系统媒体库扫描和系统文件夹授权扫描后，正式包把物理文件相同的三首 MP3 显示成六条。`MediaStore` 与文件夹授权的 `DocumentsProvider` 为同一文件返回不同 URI，两条路径各自生成 `Track.id`。旧逻辑仅按 `Track.id` 合并。文件夹入口旧文案只写 KGMA 转换，完成提示也把扫描到的普通 MP3 算成转换。

## 具体修改

| 文件 | 关键符号 | 修改 |
| --- | --- | --- |
| [AndroidLocalAudioIdentity.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidLocalAudioIdentity.kt) | `AndroidLocalAudioIdentity`、`AndroidLocalCatalog` | 使用 Android 认可的媒体条目身份。对主存储的文件夹授权文档，还可按精确路径匹配系统已索引条目。无法证实同一文件时保留各自 ID。解析旧目录记录和媒体记录的 ID 别名。 |
| [AndroidPlatformServices.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt) | `loadLibrary`、`importMusicTree`、`loadPlaylistsNow`、`resumePlayback` | 读取曲库时折叠旧重项；从文件夹新导入时跳过已在库内的文件，原子保存新增索引；按别名恢复歌单、收藏和暂停快照。显式执行用户原有删除操作时，用同一文件的 ID 集合清理曲库引用。 |
| [AndroidMusicAccess.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/features/AndroidMusicAccess.kt) | `catalogNow`、`playlistsNow`、`favorite` | 助手与后台从同一身份规则读取曲库和歌单，更新收藏时清理该曲目的旧别名。 |
| [ToolsScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/ToolsScreens.kt)、[App.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | `ImportScreen`、`importReportMessage` | Android 提供「选择文件夹」，说明可导入普通音频及转换 KGMA。结果分别列出新入库、实际转换和已在曲库。内存报告新增计数，桌面调用仍走原来的结果分支。 |

## 修改前后、平台与数据影响

同一文件能确认身份时从两条显示变成一条。同名的不同文件继续独立存在。已有别名在读取时解析，保留旧身份和元数据。有可访问的系统媒体条目时使用其 URI，避免旧目录权限失效后无法继续播放。无法匹配到系统媒体身份的第三方提供者继续按原 ID 处理。修复不删除或转换普通音频，也不改签名、同步包或托管索引格式。KGMA 解密、转码、取消和源文件保留规则仍走原有代码。

## 验证与限制

| 命令或操作 | 结果 |
| --- | --- |
| 构建当前源码 Debug APK 后通过 `adb -s emulator-5560 install -r` 覆盖调试包 | 通过。构建方法、内存和日志见[定时器验证记录](2026-11-14-04-isolate-sleep-timer-window.md)。最终包签名与旧调试包一致。 |
| 使用旧调试包与隔离合成数据验证升级。为同一首歌准备 MediaStore 和旧目录 ID、两个歌单成员、收藏及 50% 的暂停快照，目录 URI 未授权给调试包 | 旧包可见两条 `QA Alpha`，第二条提示文件不可用。早期修复包能映射到 16000 ms，但旧 URI 导致 `Source error`。最终包使用系统媒体 URI 后，小窗显示同一首歌，歌单从两个成员变为一个，收藏仍在，`dumpsys media_session` 报告 `PAUSED`、16000 ms、无错误。 |
| 最终包通过系统文件夹授权选择原来的三首已索引 MP3 | 通过。提示「3 首已在曲库中」，曲库仍为三首，不报告转换。 |
| 添加一份同歌名/歌手/专辑但内容不同的 11 秒 MP3，扫描系统媒体库并核对搜索结果 | 通过。共四首，两个 `QA Alpha` 分别显示 0:11 和 0:32。四份音频在设备上与宿主的 SHA-256 一致。 |

仅在 Android 16 x86_64 模拟器验证主存储和普通 MP3。KGMA 实际解密、其他存储提供者、删除文件、低版本 Android 与桌面端未在本轮执行。日志和截图保存在 `build/android-fixes-2026-11-14/`。

## 同步更新的文档

[文档索引](../README.md)和[开发说明](../DEVELOPMENT.md)。
