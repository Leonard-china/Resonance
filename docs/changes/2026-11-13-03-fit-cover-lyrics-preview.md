# 2026-11-13 — 修复封面预览截断双语歌词

- 修改日期：2026-11-13（Asia/Shanghai）
- 类型：Bug 修复
- 适用版本：0.6.11 / 45
- 影响平台：Android、Windows、Linux 的共享原生播放页
- 关联：[0.6.10 完整歌词页修复](2026-11-13-01-fit-long-player-lyrics.md)

## 需求或问题

手机封面页下方的一条英文歌词折成两行后占满预览框，译文被底部裁掉。用户要求在推送前完整测试。

## 原因与依据

封面预览和完整歌词页分别布局。封面给预览设置固定高度，原文、译文、行距和内边距的总高度可以超过它。旧布局另外限制了原文和译文的最大行数。预览禁止纵向滚动，因此被遮住的内容无法在这里看完。

## 具体修改

| 文件 | 关键符号 | 修改 |
| --- | --- | --- |
| [NowPlayingOverlay.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | 封面分栏 | 在控件测量后的空间内为双语歌词预留高度，并保留封面空间。 |
| [SyncedLyricsPreview.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/SyncedLyricsPreview.kt) | `SyncedLyricsPreview`、`PreviewLyricRow` | 用实际宽高测量原文和译文，含换行、行距和内边距。优先使用常规字阶，其次紧凑字阶。 |
| [Android 回归](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/LyricsPlayerRegressionDeviceTest.kt)、[桌面回归](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/LyricsPlayerRegressionTest.kt) | 真实播放器布局 | 检查实际文字布局无省略号、整条文字在视口内、点击打开完整歌词、超长句可滑动读完，并检查前后跳转。 |

## 修改前后行为

旧预览可能显示半行或省略号。新预览在可用空间内显示完整原文和译文，特别长的歌词提供「查看完整歌词」入口。点按预览打开完整歌词并定位当前句，长条目仍可滚动阅读。系统字号与当前句同步行为沿用原有设置。

## 平台与数据影响

三端共享显示修复，歌词文本、歌词偏移、翻译缓存、音乐文件及权限沿用原有数据格式。测试使用合成歌词及隔离设备数据。

## 验证

- 旧版为固定预览高度与两行上限，用户截图出现底部裁切。Android 基线新增测试失败，初次文字溢出断言同时发现字体度量取整差异，最终测试改为检查完整字符、行底边及视口边界。
- 修复版 Android 16 共 25 次测试通过。360×780 默认字号运行 22 项歌词与播放页仪器测试。360×640 及 1.5 倍字号、390×844 及 1.3 倍字号、780×360 横屏各运行双语用例。横屏使用隔离副本中的测试宿主，截图尺寸已核对。
- 本机 `:composeApp:desktopTest` 通过 443 项，跳过 10 项。双语截图覆盖五种尺寸，检查播放前后跳转。Android Debug 和 Release JVM 单测分别通过 79 和 79 项。
- 最终安装包、签名及官网验收见[发布记录](2026-11-13-04-publish-0.6.11-release.md)。

复核入口：本机在隔离源码副本、共享资源锁和 2 GiB 进程树限额内执行 `:composeApp:desktopTest :composeApp:testDebugUnitTest :composeApp:testReleaseUnitTest`。Android 仪器测试使用以下命令，22 项组覆盖六个测试类；其余三个窗口配置单独运行双语用例。

```bash
adb -s emulator-5562 shell am instrument -w -r -e class \
  com.resonance.player.LyricsPlayerRegressionDeviceTest,com.resonance.player.LyricsPreviewMotionDeviceTest,com.resonance.player.LyricsScrollDeviceTest,com.resonance.player.LyricsToolsDeviceTest,com.resonance.player.LyricsTranslationDeviceTest,com.resonance.player.MusicStageDeviceTest \
  com.resonance.player.debug.test/androidx.test.runner.AndroidJUnitRunner
```

## 限制与后续

Android UI 验证使用模拟器和合成歌词，未安装到用户真机。特别长的歌词在空间不足时由预览入口打开完整歌词查看。

## 同步更新的文档

[文档索引](../README.md)和[歌词开发说明](../DEVELOPMENT.md#lyrics)。
