# 2026-10-28 — Android 16 真机调试音乐舞台并修正手机封面与助手标题布局

- 修改日期：2026-10-28（Asia/Shanghai）
- 类型：真机验证 / UI Bug 修复 / 回归测试
- 适用版本：`0.6.3 / 37`，保持版本号
- 影响平台：共享 UI / Android 真机；Windows 编译与离屏 UI 回归
- 关联需求：用户连接手机并要求立即调试；前序 [原生音乐舞台 UI](2026-10-28-03-mineradio-inspired-native-music-stage.md)。

## 需求或问题

此前只完成共享 Compose 离屏渲染与编译，尚无设备。用户连接 VKJ-AN00 后，需在实际 Android 16 / API 36 窗口验证音乐舞台、导航、横竖屏、150% 字号、软键盘及播放服务回归。

真机观察到两处布局问题：正常字号竖屏的常驻音量/速度面板挤小封面；助手标题和四个操作在同一行，361dp 左右的窗口中标题被压成逐字竖排。

## 原因与依据

- `NowPlayingOverlay` 的封面区域使用剩余高度；竖屏同时放置元信息、多行歌词、进度、播放键、音量设置与标签后，真实 Android 字体和系统安全区域进一步压缩封面。
- `AssistantScreen` 的标题列使用 `weight(1f)`，与四个固有宽度按钮竞争同一行；窄窗口没有单独的操作行，标题允许换行。
- 初轮 12 项既有设备 UI 测试中有 2 项失败：推荐卡测试依赖已替换的旧文案；关于页测试未先滚动到被迷你播放器下方遮住的入口。语义树诊断确认后者仍停在工具页，未伪报关于页不存在。
- 扩展教程回归发现 `TutorialNavigationTest` 的动态代理未实现 `getSupportsKeyboardShortcuts`，拆箱 null 导致夹具异常；补齐 false 返回值。封面尺寸检查改为查找未合并语义树；截图改为只抓取测试自身的 Compose 树，避免前台窗口切换造成截图失败或抓取其他应用。
- 真机截图位于忽略的 `build/device-ui-2026-10-28/`。测试使用自有夹具、原创歌词文本和 Debug 包；正式版未覆盖，用户音频和 API 配置未读取或修改。

## 具体修改

| 文件 | 关键符号 | 具体修改与目的 |
| --- | --- | --- |
| [NowPlayingOverlay.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | `NowPlayingOverlay`、`ArtworkPane` | 手机与宽屏统一从现有「播放选项 → 播放设置 · 音量与速度」打开次要控制，移除竖屏常驻面板；封面获得剩余空间；增加无视觉影响的封面测试标签。 |
| [MusicFeatureScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureScreens.kt) | `AssistantScreen` | 紧凑窗口将新对话、历史、任务和设置放入可换行的第二行；标题限定单行，模型信息仍可省略；回调与聊天输入不变。 |
| [LibraryNavigationTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/LibraryNavigationTest.kt) | 推荐/关于/助手标题检查 | 推荐入口使用语义描述；关于入口先滚动再点击；新增标题单行及操作可见性断言。 |
| [MusicStageDeviceTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/MusicStageDeviceTest.kt) | 三项真实设备窗口测试 | 覆盖封面尺寸、控件 48dp 目标/视口、歌词/队列/进度/收起、播放/暂停/下一首回调、150% 字号和设置菜单。只保存自身原生 Compose 内容截图，对话框单独抓取；等待 Android 窗口动画完成。 |
| [TutorialNavigationTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/TutorialNavigationTest.kt) | `show` 的平台测试代理 | 补齐键盘快捷键能力布尔值，避免测试夹具在设置页拆箱 null。 |
| [UiVisualContractTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/UiVisualContractTest.kt) | `compactAssistantKeepsTitleOnOneLineWithLargeText` | 360dp、100%/150% 字号下检查标题实际排版行数及顶部操作边界，回归共享代码在 Windows 的行为。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 标准字号手机播放封面 | 常驻音量区压缩封面高度 | 次要控制统一放进播放设置，封面更大，音量/倍速/音效仍可达 |
| 手机助手标题 | 被同排四个操作挤成逐字竖排 | 标题保持单行，操作行可按宽度换行 |
| 关于页设备回归 | 未滚动直接点屏幕外入口，未真正进入页面 | 滚动入口进入可视区域后点击并检查实际版本说明 |

## 平台与数据影响

共享布局同步影响 Android 与 Windows；没有修改音频引擎、播放竞争保护、音量算法、持久化格式、同步包、权限、账号、网络请求或音频标签。所有设备测试安装在 `com.resonance.player.debug`，与正式包 `com.resonance.player` 并存。

测试前保存旋转状态，横屏测试后恢复；系统字号不修改，150% 使用测试组合内的 `LocalDensity`。助手键盘检查仅输入并清空测试草稿，不发送请求。播放服务回归仅使用测试缓存音频，清理夹具并恢复相关 Debug 偏好。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `.\gradlew.bat :composeApp:assembleDebug :composeApp:assembleDebugAndroidTest :composeApp:desktopTest --tests 'com.resonance.player.ui.UiVisualContractTest' --no-configuration-cache --console=plain` | Windows / JDK 17 / SDK 36 | 最终共享布局构建通过，1m45s；10 项桌面原生 UI 回归，0 失败/错误/跳过，重建 50 张离屏截图。日志 `build/device-ui-2026-10-28/final-ui-build.log`。 |
| `.\gradlew.bat :composeApp:assembleDebugAndroidTest --no-configuration-cache --console=plain` | Windows / JDK 17 | 最终截图/夹具修正后通过，3s；安装本次生成的测试 APK，未使用 10 月 17 日的旧测试产物。日志 `final-harness-build.log`。 |
| `adb install --no-streaming -r <Debug APK>` 及测试 APK | VKJ-AN00 / Android 16 / API 36，1264×2728，560dpi | 两包安装成功；手机主 APK 的 SHA-256 与本次构建文件一致。此前流式安装拒绝/等待未完成，用户允许重试后采用非流式安装；中断轮次不计入最终结果。 |
| `adb shell am instrument -w -r -e class com.resonance.player.UiInteractionContractTest,com.resonance.player.LibraryNavigationTest,com.resonance.player.TutorialNavigationTest,com.resonance.player.AudioSessionIntegrationTest com.resonance.player.debug.test/androidx.test.runner.AndroidJUnitRunner` | 同一 API 36 真机，最新 Debug APK | 23 项全部通过，38.323s：滑块/标签、导航/位置保留/顶边像素/关于/助手标题、教程流程、Media3 会话/后台换曲音量、PCM/FFmpeg 响度回归。日志 `verified-core.log`。 |
| `adb shell am instrument -w -r -e class com.resonance.player.MusicStageDeviceTest -e stageTheme <dark/light> -e stageLabel <portrait/landscape> com.resonance.player.debug.test/androidx.test.runner.AndroidJUnitRunner` | 真机四种组合：竖屏深色、竖屏浅色、横屏深色、横屏浅色 | 每组 3 项通过，共 12 次用例执行；组内分别检查正常字号和 150% 字号。耗时 10.891s / 10.494s / 15.351s / 11.245s；日志 `verified-stage-*.log`。连同上行共 35 次设备用例执行通过，独立测试方法为 26 个。 |
| 真机截图与实际应用启动/键盘检查 | 设备测试夹具 + 调试版真实 MainActivity | 28 张最终设备内容截图位于 `build/device-ui-2026-10-28/screenshots-verified/`；目视检查封面放大、横屏大字号及设置弹窗。实际应用的助手标题为单行；输入草稿、弹出键盘后输入框/发送按钮仍可见，清空草稿后收起键盘。没有发送聊天请求。实际界面树记录为 `verified-main.xml`、`verified-assistant.xml`、`verified-ime.xml`。 |
| 设备状态与运行日志 | 同一设备 | 旋转锁定、旋转方向恢复为原先 `0/0`，系统字号保持 `1.0`。当前真实调试进程最近 300 行 AndroidRuntime 错误日志未发现 `FATAL EXCEPTION`；不据此宣称长期无崩溃。 |
| Markdown 文件链接检查、`git diff --check` | Windows / PowerShell | 索引、开发说明与本记录共 177 个本地文件链接存在，diff 检查通过。 |

最终 Debug APK 为 176,101,018 字节，SHA-256：`50F05A143E6E7A5E2AA0438546CFABCFA3F41DD6EE494B6CF75FA97DB778930C`。完整日志及图片仅在忽略的 `build/device-ui-2026-10-28/`，不提交设备标识、真实音频或本机工具路径。

## 限制与后续

- 一台 API 36 真机不能代表所有 Android 版本、厂商系统或屏幕。
- Compose 播放器交互夹具不连接真实歌曲；播放服务测试也不等于真人听音、蓝牙、锁屏长时间播放验收。
- 未进行 Perfetto、专项帧率、能耗或长时间压力测试；不宣称性能优化已获实测证明。
- 本轮未执行全量 `allTests`、Release Lint 或新的 Windows 安装包验证；前序记录的 Lint 结果不冒充本次结果。
- 本次不发布新版本、不覆盖正式版、不发布 GitHub/官网安装包。

## 同步更新的文档

- [文档入口](../README.md)：增加本次独立真机调试索引。
- [开发说明](../DEVELOPMENT.md)：更新手机播放设置、助手窄屏标题及真机验证边界。
- 前序 UI 记录保留当时无设备的事实，当前结果以本记录为准。
