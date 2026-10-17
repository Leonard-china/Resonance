# 2026-10-17 — 修复打开设置等工具二级页面时顶部黑边闪烁问题

- 修改日期：2026-10-17（Asia/Shanghai）
- 类型：Bug 修复
- 适用版本：0.4.8（不改变版本号）
- 影响平台：共享 / Android
- 关联记录或需求：无

## 需求或问题

在 Android 端主界面从“工具”点击打开“设置”或其它工具二级页面时，屏幕顶部（状态栏与页面边界处）会出现瞬间的黑边闪烁，影响转场过渡视觉体验。

## 原因与依据

1. **SizeTransform(clip = true) 裁剪中间帧暴露底层**：
   在 [`LibraryShell.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt) 的 `DestinationContent` 导航过渡规范中，`initialState == LibraryDestination.Profile && targetState in profileSubPages` 与返回分支配置了 `using SizeTransform(clip = true)`。在进入二级页面（如 `SettingsScreen`）的第一帧，由于二级页包含 Header、Divider 及带有 `weight(1f)` 的多组件垂直滚动区域，Compose 初始测量与尺寸动画插值使得 `AnimatedContent` 对容器执行了动态矩形边界裁剪，导致顶部未绘制区域暴露。
2. **Android 窗口底层背景与状态栏颜色默认偏黑**：
   在 [`styles.xml`](../../composeApp/src/androidMain/res/values/styles.xml) 中，系统主题未显式配置 `android:windowBackground`（默认继承纯黑），且 `statusBarColor` 设为 `#0D1117`。同时在 [`MainActivity.kt`](../../composeApp/src/androidMain/kotlin/com/resonance/player/MainActivity.kt) 中调用 `enableEdgeToEdge()` 时未显式传入全透明沉浸式系统栏样式，导致中间帧穿透到窗口黑底或系统遮罩。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| [`LibraryShell.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt) | `DestinationContent` 的 `transitionSpec` | 将进入与退出 `profileSubPages`（包括 `Settings`）以及 `reducedMotion` 分支的 `SizeTransform(clip = true)` 全部改为 `SizeTransform(clip = false)`，与同级主导航及二级页面间平移规则保持一致，禁止过渡期间对页面顶边界和边框进行尺寸截断裁切。 |
| [`styles.xml`](../../composeApp/src/androidMain/res/values/styles.xml) | `AppTheme` | 将 `android:statusBarColor` 与 `android:navigationBarColor` 修改为 `@android:color/transparent`，并将 `android:windowBackground` 设置为 `#111A1D`（与 Resonance 深色 Canvas 底色保持一致），避免测量间隙透出纯黑底色。 |
| [`MainActivity.kt`](../../composeApp/src/androidMain/kotlin/com/resonance/player/MainActivity.kt) | `MainActivity.onCreate` | 调用 `enableEdgeToEdge()` 时显式配置 `statusBarStyle` 和 `navigationBarStyle` 为透明背景自动适配模式（`SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)`），彻底消除系统栏默认蒙层及边缘闪烁。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 从“工具”进入“设置”页面 | 页面横向滑入瞬间，屏幕顶端出现一条黑色边框闪烁后恢复 | 页面满屏平滑推入，顶端状态栏与内容背景连贯过渡，无黑边闪烁与尺寸撕裂 |
| 从“设置”返回“工具”页面 | 返回视差退场瞬间顶部同样可能因尺寸裁剪产生微小跳变 | 平滑向右滑出，背景通透一致 |

## 平台与数据影响

- **共享逻辑**：`LibraryShell.kt` 修正了 `profileSubPages` 的 `SizeTransform` 配置，Android 与 Desktop 均受益于更平滑的页面推入动效。
- **Android**：沉浸式状态栏与导航栏在底层完全透明，窗口底色与主题背景无缝统一。
- **数据与权限**：不影响任何用户数据、偏好设置或存储结构。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `.\gradlew.bat desktopTest` | Windows 11 / JVM | 通过（BUILD SUCCESSFUL in 2m 15s） |
| `.\gradlew.bat assembleDebug` | Windows 11 / Android SDK | 通过（BUILD SUCCESSFUL in 1m 39s，APK 编译成功） |

## 限制与后续

- 针对特定真机系统定制状态栏（如部分特定定制 ROM 的浅色模式状态栏反色逻辑），后续可结合用户反馈进一步跟进。本次范围内无已知未完成项。

## 同步更新的文档

- `docs/README.md`：在变更记录索引中增加本次记录链接。
- `docs/DEVELOPMENT.md`：核对 UI 与动效规范。
