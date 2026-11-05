# 2026-11-05 — 同步原生界面测试契约并按真实变化采集窗口帧

- 修改日期：2026-11-05（Asia/Shanghai）
- 类型：Bug 修复 / 测试
- 适用版本：0.6.6 / 40（version.properties；不升版）
- 影响平台：Android / Windows / Linux（按下文平台边界）
- 关联需求：用户要求修复本次全面测试问题；[原始缺陷](../audit/2026-11-05/BUGS.md)、[原始手机结果](../audit/2026-11-05/ANDROID-DEVICE-REPORT.md)，编号D01–D03、D05–D10及桌面图集超时。

## 需求或问题

旧测试要求已取消的opaqueCanvas/旧文案/教程首屏位置，固定顶栏却执行滚动；静止UI等待60帧造成无效性能结论；默认导出大量PNG过慢。

## 原因与依据

按当前已批准透明连续转场和导航结构核对断言；保留剪裁/混合/最终画面与真实创建/加入/关闭动作，不添加无意义动画凑帧。

## 具体修改

| 文件 | 关键符号 | 实际修改 |
| --- | --- | --- |
| [LibraryDetailTransitionTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/LibraryDetailTransitionTest.kt) | 转场像素契约 | 检查两页透明混合、父背景、裁剪及反向/减弱动态终态。 |
| [TogetherUiDeviceTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/TogetherUiDeviceTest.kt) | 房间交互 | 更新实际标签，固定页头直接点击，仍检查加入前无连接、审批和关闭。 |
| [TutorialNavigationTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/TutorialNavigationTest.kt) | 设置教程 | 先滚动到真实入口，完整浏览四页；结束后按目标路由和曲库实际节点检查，不再要求已移除的“你的歌单”旧标题。 |
| [LibraryNavigationTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/LibraryNavigationTest.kt) | 推荐设置 | 禁用推荐卡隐藏，仍从设置到达开关。 |
| [MusicStageFrameMetricsTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/MusicStageFrameMetricsTest.kt) | playingStageReportsActualWindowFrameDurations | 80次100ms间隔的实际进度状态变化及切歌，再采FrameMetrics。 |
| [ui/UiVisualContractTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/UiVisualContractTest.kt) | 图集/动效导出 | 70场景仍渲染并校验尺寸；PNG与360帧媒体导出仅RESONANCE_UI_ARTIFACTS=1启用。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| D01–D03、D05–D10及桌面图集超时所述输入/操作 | 定位假设失败；静止帧数量不足；默认图集编码耗时 | 按当前契约检查真实动作；明确刺激产生实际帧，报告p95/超截止数而非闲置FPS |

## 平台与数据影响

测试基础设施改动，无生产网络、用户文件、无限动画或版本变化；Debug诊断不能外推Release性能；本次采样通过不等于帧预算达标，实际窗口统计见验收报告。

## 验证

本次相关用例：对应Android测试类；UiVisualContractTest与桌面歌词回归；完整结果见本轮修复验收报告。执行命令、独立数据边界、逐用例结果、失败后的定点复测及计数统一见[本次修复验收](../audit/2026-11-05/FIX-VALIDATION.md)。该报告保留初次失败记录，历史修复前通过数不作为本次通过依据。

| 执行入口 | 本次结果 |
| --- | --- |
| JUnitCore或Android instrumentation：UiVisualContractTest、LibraryDetailTransitionTest、LibraryNavigationTest、TogetherUiDeviceTest、TutorialNavigationTest、MusicStageFrameMetricsTest | 49通过、0失败、0条件跳过；逐用例见验收报告JSON |
| 完整桌面/Android源码与测试编译 | 通过；202/195份主源码，100/34份测试源码，版本不变 |

## 限制与后续

不以“改文案定位”证明跨网、真实AI或Release性能。360帧宣传导出属于条件媒体任务，未执行。

## 同步更新的文档

- [开发说明](../DEVELOPMENT.md)：相应导入、持久化、AI、UI/测试或官网章节，核对日2026-11-05。
- [文档索引](../README.md)：独立记录与修复验收入口。
