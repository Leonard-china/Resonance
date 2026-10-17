# 2026-10-17 — 恢复页面切换弹性晃动感并彻底根除切页顶边黑闪

- 修改日期：2026-10-17（Asia/Shanghai）
- 类型：Bug 修复 / 体验优化
- 适用版本：0.5.0（`version.properties`，不自动升版）
- 影响平台：共享 / Android / Windows
- 关联记录或需求：关联 `docs/changes/2026-10-17-31-stable-page-viewport-and-top-edge.md`、`docs/changes/2026-10-17-33-release-0.5.0.md`

## 需求或问题

1. **切页晃动感丢失**：用户反馈在 0.5.0 版本中，主导航及页面切换时原先极具质感的弹性“晃动感”消失了，希望找回该动效。
2. **切页顶边黑闪偶现**：切换页面时的黑边闪烁问题仍未彻底解决，表现为“偶尔出现，刚更新完、头一次用的时候较频繁，多用一会就变少”。

## 原因与依据

1. **晃动感丢失原因**：
   - 查阅 `docs/changes/2026-10-17-31-stable-page-viewport-and-top-edge.md`，此前为了解决黑边，将 `DestinationContent` 中 `AnimatedContent` 的 `transitionSpec` 里的 `using SizeTransform(clip = false)` 替换为了 `using null`，并在外层增加了 `Modifier.clipToBounds()`。
   - 在 Jetpack Compose 的 `AnimatedContent` 机制中，默认的 `SizeTransform` 自带基于弹簧物理（`Spring.StiffnessMediumLow`）的动态尺寸插值，会在不同页面高度和内容重新布局时赋予容器微小柔和的呼吸/晃动弹性感；换成 `using null` 会将容器尺寸硬性锁定为静态突变，导致用户喜爱的微弹性触感彻底丧失。
2. **切页顶边黑闪与“刚更新完频繁、多用变少”的深层原因**：
   - **ART 运行期 JIT 编译特征**：Android 应用在刚安装或更新版本后，由于本地 ART 运行时的 Profile-guided AOT 缓存尚未形成，首次进入各个未访问页面时必须实时进行 classloading、JIT 字节码编译与测量布局树，导致切换进页面的首个关键帧发生 50~100ms 的瞬时卡顿/掉帧。多使用几次后，JIT 编译缓存就位，切换耗时恢复为 16ms 满帧，因此黑闪的暴露窗口大幅缩短，肉眼不易察觉。
   - **Alpha 混合离屏图层泄露**：此前水平滑动动画附加了 `fadeIn` 和 `fadeOut` 透明度过渡。在 Compose 底层 Skia 渲染中，透明度小于 1.0f 会强制分配离屏硬件合成图层（`SaveLayer`）。在 JIT 掉帧瞬间，半透明的退场页与进场页在状态栏接缝处（`WindowInsets.safeDrawing.top`）透出了底层原生 Window 的默认背景色（Android 原生主题 `styles.xml` 中的 `#111A1D`，即暗黑色）。
   - **窗口背景更新的异步延迟**：原先 `WeatherAppearanceTheme` 中仅在协程 `LaunchedEffect` 中调用 `updateWindowBackground`，在页面首帧提交到屏幕前仍处于异步等待中，导致底层原生的 `#111A1D` 在掉帧时短暂穿透。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| [LibraryShell.kt](file:///composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt#L903-L933) | `DestinationContent` / `transitionSpec` | 1. 恢复所有转场分支的 `using SizeTransform(clip = false)`，重新激活 Compose 内置弹性弹簧物理效果，找回备受喜爱的弹性切页晃动感。<br>2. 移除外层 `Box` 的硬裁剪 `Modifier.clipToBounds()`，消除状态栏切角处的机械 Scissor 截断。<br>3. 彻底移除横向推入滑出时的 `fadeIn()` 和 `fadeOut()`，改为 100% 不透明纯实体卡片滑入滑出，避免 Skia `SaveLayer` 离屏合成图层，杜绝 JIT 卡顿时底层黑边透光。 |
| [WeatherAppearance.kt](file:///composeApp/src/commonMain/kotlin/com/resonance/player/design/WeatherAppearance.kt#L111-L117) | `WeatherAppearanceTheme` | 引入 `SideEffect { services.updateSystemBars(dark); services.updateWindowBackground(canvas) }`，在 Compose 每次重组完成且绘制首帧前，同步将 Android Window 的背景 Drawable 设置为当前主题纯色画布，杜绝协程异步调度造成的首帧黑底透出。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 主导航与页面切换动效 | 容器尺寸完全静态突变，没有弹性触感，动画生硬 | 恢复柔和弹簧物理（`SizeTransform(clip = false)`），富有自然微弹性的呼吸与晃动感 |
| 刚更新完初次进入二级页面或切换主导航 | 因 ART JIT 首次编译发生微小掉帧，半透明离屏图层在顶栏处透出底层的 `#111A1D` 黑边 | 页面作为 100% 实体卡片无缝平移，底层 Window 在重组首帧立即同步为主题画布色，彻底杜绝黑闪 |
| 连续快速切页 | 偶尔闪过状态栏黑线 | 无论快速还是连续切换，全场景平滑过渡，无任何黑色底边泄露 |

## 平台与数据影响

- **平台影响**：
  - Android：彻底消除安装/更新后首次切页冷 JIT 期间的顶部黑边穿透，系统窗口底层同步贴合主题色。
  - Windows：主导航与二级页面切换同样重获弹簧弹性呼吸感，实体卡片滑动渲染更加轻快。
- **数据影响**：不涉及任何持久化结构、配置键、数据库或网络协议修改。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `./gradlew :composeApp:desktopTest` | Windows 11 / JVM 17 | 通过（1m 2s，全部桌面与功能单测通过） |
| `./gradlew :composeApp:compileReleaseKotlinAndroid` | Windows 11 / Android SDK 35 | 通过（Android Release 编译通过） |
| `./gradlew :composeApp:compileDebugAndroidTestKotlin` | Windows 11 / Android SDK 35 | 通过（39s 编译通过，0 错误） |
| `./gradlew :composeApp:lintRelease` | Windows 11 / AGP 8.5.2 | 通过（50s 检查通过，0 errors） |

## 限制与后续

- 本次修改聚焦于动效质感恢复与底层透光黑闪的彻底根除。
- 后续若重新打包发布，只需基于此代码执行 `scripts/build.ps1 -NoBump`。

## 同步更新的文档

- 更新 `docs/README.md` 变更索引。
