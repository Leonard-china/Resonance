# 2026-10-17 — 进一步覆盖页面切换顶部背景并减少系统栏重设

- 修改日期：2026-10-17（Asia/Shanghai）
- 类型：功能 / 体验修复
- 适用版本：0.4.9 / 32 工作区；本轮不升版、不发布
- 影响平台：共享 / Android / Windows（平台限定见下文）
- 关联需求：用户要求落地全部体验建议，并追加页面顶部黑边闪烁反馈；[完整交接与实际验证](../HANDOFF-2026-10-17-UX-ANTIGRAVITY.md)

## 需求或问题

用户在 0.4.9 后仍报告偶发页面切换顶部黑边，说明历史 20 号记录的 clip=false 和透明系统栏不足以确认问题已完全解决。

## 原因与依据

静态检查发现 DestinationContent 仍使用尺寸变换，移动和淡出的页面没有稳定的不透明视口覆盖；窗口底色固定深色，WeatherAppearance 在每次重组时重复更新系统栏。这些是可暴露底层或增加边缘刷新机会的路径，尚未在设备上复现并确认唯一根因。

## 具体修改

| 文件 | 关键符号 | 修改与目的 |
| --- | --- | --- |
| [LibraryShell.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt) | `DestinationContent` | 在动画和横向拖动外维持固定尺寸、裁剪且绘满主题 Canvas 的视口；每页独立背景，using null 禁止尺寸插值，保留水平切换。 |
| [App.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | `根 Surface` | safeDrawing 外的完整窗口也使用同一 Canvas。 |
| [WeatherAppearance.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/WeatherAppearance.kt) | `LaunchedEffect(dark, canvas)` | 仅主题或底色改变时更新窗口和系统栏，不随播放重组重复设置。 |
| [MainActivity.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/MainActivity.kt) | `onCreate` | Android 10+ 关闭状态栏／导航栏额外对比度遮罩。 |
| [AndroidPlatformServices.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt) | `updateWindowBackground` | 窗口 drawable 跟随当前主题，避免浅色页间隙露出固定深色底。 |
| [LibraryNavigationTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/LibraryNavigationTest.kt) | `rapidPageChangesKeepTopEdgeCoveredInLightMode / DarkMode` | 对真实 LibraryShell 的快速切页逐帧采样顶部像素，覆盖浅／深色。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 本次使用流程 | 仅关闭裁剪并设置固定窗口底色，页面仍可能在尺寸／透明帧暴露背景。 | 固定视口与每页持续覆盖顶边，窗口底色跟随主题；减少重复系统栏更新。 |

## 平台与数据影响

共享 Compose 与 Android 窗口绘制调整，无歌曲、设置格式、权限、网络或存储变化。原有手势、滚动位置与减少动态偏好保留。退出动画页面不再响应返回。

## 验证

新增像素采样测试的 Android Instrumentation 源码已编译通过；双端源码编译通过。首次编译存在声明顺序和初始化类型推断错误，已修正并复验。

| 本次检查 | 结果 |
| --- | --- |
| 定点回归与编译 | 本轮相关回归合计 75 项不同测试通过（失败 0、跳过 0）；双端源码及 Android 测试源码通过编译。完整命令与执行边界见交接文档。 |
| 真机、正式包与网站 | 本轮未执行；交 Antigravity 完成最后验收、正式构建及部署。 |

## 限制与后续

未执行真机像素测试，不能宣称所有 ROM 的偶发闪烁已消失。需在实际出现问题的设备上连续切主导航、设置及返回，并检查浅／深色和系统手势。

## 同步更新的文档

- [文档索引](../README.md)
- [开发说明第 18 节及对应原章节](../DEVELOPMENT.md#user-experience-update)
- [交接与构建部署提示词](../HANDOFF-2026-10-17-UX-ANTIGRAVITY.md)

