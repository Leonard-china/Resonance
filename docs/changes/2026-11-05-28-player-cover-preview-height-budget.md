# 2026-11-05 — 播放页按可用高度分配封面与歌词预览

- 修改日期：2026-11-05（Asia/Shanghai）
- 类型：Bug 修复
- 适用版本：0.6.6 / 40（version.properties；不升版）
- 影响平台：Android / Windows / Linux（按下文平台边界）
- 关联需求：用户要求修复本次全面测试问题；[原始缺陷](../audit/2026-11-05/BUGS.md)、[原始手机结果](../audit/2026-11-05/ANDROID-DEVICE-REPORT.md)，编号D04。

## 需求或问题

长屏手机封面只有151.43dp，未满足180dp尺寸门槛；固定歌词预览优先占满中部空间。

## 原因与依据

封面在固定预览之后按剩余weight缩小。现在根据真实内容区高度约束预览，保留封面和控制区；小窗/大字仍保留歌词入口与可达操作。

## 具体修改

| 文件 | 关键符号 | 实际修改 |
| --- | --- | --- |
| [ui/NowPlayingOverlay.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | PlayerPane.Cover / HeroSyncedLyricsPreview | BoxWithConstraints从剩余空间计算maximumPreviewHeight，约束封面预览；歌词页本体不缩减。 |
| [ui/SyncedLyricsPreview.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/SyncedLyricsPreview.kt) | maximumHeight | 可选视口上限，连续滚动与当前行中心跟随保留。 |
| [ui/LyricsPlayerRegressionTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/LyricsPlayerRegressionTest.kt) | 受限窗口歌词断言 | 覆盖390×844、360×640大字、720×360横屏和1280×800；允许LazyColumn移除离屏旧行，仍验证当前居中/高亮与控件48dp。 |
| [MusicStageDeviceTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/MusicStageDeviceTest.kt) | 封面/歌词/队列/控制 | 保留180dp门槛与触控区检查，用当前内容描述切换页签。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| D04所述输入/操作 | 固定预览挤小正常长屏封面 | 正常长屏留出有用封面，预览按空间缩放；完整歌词页仍独立可打开 |

## 平台与数据影响

共享Android/Windows/Linux原生Compose，保留已确认灰/玫瑰UI、播放行为与歌词数据格式。

## 验证

本次相关用例：LyricsPlayerRegressionTest；MusicStageDeviceTest正常/1.5倍字体/设置/下一首；手机原生图像。执行命令、独立数据边界、逐用例结果、失败后的定点复测及计数统一见[本次修复验收](../audit/2026-11-05/FIX-VALIDATION.md)。该报告保留初次失败记录，历史修复前通过数不作为本次通过依据。

| 执行入口 | 本次结果 |
| --- | --- |
| JUnitCore或Android instrumentation：LyricsPlayerRegressionTest、MusicStageDeviceTest、LyricsPlayerRegressionDeviceTest | 6通过、0失败、0条件跳过；逐用例见验收报告JSON |
| 完整桌面/Android源码与测试编译 | 通过；202/195份主源码，100/34份测试源码，版本不变 |

## 限制与后续

180dp要求只针对足够高的正常竖屏；短横屏和大字优先保证播放控件与当前歌词可达。Windows实际窗口未复测。

## 同步更新的文档

- [开发说明](../DEVELOPMENT.md)：相应导入、持久化、AI、UI/测试或官网章节，核对日2026-11-05。
- [文档索引](../README.md)：独立记录与修复验收入口。
