# 2026-10-28 — 完成回弹版真机回归并区分首次与再次切页耗时

- 修改日期：2026-10-28（Asia/Shanghai）
- 类型：测试 / 调试 / 文档
- 适用版本：`0.6.3 / 37`，未升版
- 影响平台：Android 测试；共享产品代码未改动
- 关联需求：[封面氛围与回弹版](2026-10-28-05-album-atmosphere-and-spring-motion-refinement.md) 的手机重连后安装、交互复测和窗口帧耗时诊断。

## 需求或问题

上一记录交付时 ADB 已断开，最新包未装机。用户重新连接手机，要求继续测量。第一轮窗口耗时显示两版切页均有尖峰，需要区别首次进入与同进程再次进入页面，不能凭离屏预览判断手机性能。

## 原因与依据

使用真实 Window FrameMetrics，不使用 Compose 人工测试时钟；同设备、同旋转、同一对场景比较前后包。该夹具为 Debug、模拟播放状态及小型测试库，仅作诊断。首次页面组成、运行时编译、渲染管线和调度均可能影响结果，本次没有调用栈/Release 证据将尖峰归因于其中某一项。

## 具体修改

| 文件 | 关键符号 | 修改与目的 |
| --- | --- | --- |
| [MusicStageFrameMetricsTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/MusicStageFrameMetricsTest.kt) | `navigateJourney`、`metricsWarmup`、`warmNavigation` | 提取同一八步导航路径，新增可选 `-e metricsWarmup true`：采集前走完一次路径并等待 1 秒，随后再采集相同路径约 8 秒；JSON 标记是否预热。原有默认场景不变。 |
| [文档入口](../README.md)、[开发说明](../DEVELOPMENT.md) | 当前设备验证状态 | 更新最新包已安装、37 次交互回归及六次诊断采集的事实；保留记录 05 当时设备断开的历史。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 手机上体验最新回弹版 | 尚未安装 | 安装并验证主包哈希，最终保留新版且启动真实 MainActivity |
| 导航耗时采样 | 仅有首次进入场景 | 可分别采集首次路径与同进程再次进入路径，输出标记区分 |
| UI 回归 | 新取色/草稿断言只编译 | 新版包真机执行取色、草稿不发送、连续导航、顶边覆盖、播放器、教程及音频夹具回归 |

## 平台与数据影响

只安装 `com.resonance.player.debug` / `.debug.test`，未安装或修改正式包。前后两包短暂替换用于比较，最终恢复新版。没有修改音频引擎、用户文件、账号、网络调用、持久化或同步格式。测试仅使用夹具及测试缓存音频，不发送助手请求。横屏检查后恢复旋转状态 `0/0`，系统字号保持 `1.0`；150% 字号在测试组合内部提供。

## 验证

设备为 VKJ-AN00，Android 16 / API 36，1264×2728 / 560dpi。测量场景报告刷新率约 120Hz；采样期间电池温度读数约 35～36℃，未锁定 CPU/GPU 频率。日志和产物在忽略的 `build/stage-refinement-2026-10-28/`。

| 命令或操作 | 结果 |
| --- | --- |
| `adb install --no-streaming -r <APK>`、设备 `sha256sum` | 前一包 SHA-256 为 `50F05A143E6E7A5E2AA0438546CFABCFA3F41DD6EE494B6CF75FA97DB778930C`；本轮最新包为 `142331DAAB19C1BA1EBC9CBDA69EBC1FBA28D0F44249AC17544A1EC177C834DD`，176,111,928 字节。最终设备与本地新版哈希一致。 |
| `.\gradlew.bat :composeApp:assembleDebugAndroidTest --no-configuration-cache --console=plain` | 新增预热参数后通过，5s；日志 `warm-metrics-harness-build.log`。主包产品代码及 APK 未因本次测试调整而改变。 |
| `adb shell am instrument -w -r -e class 'com.resonance.player.UiInteractionContractTest,com.resonance.player.LibraryNavigationTest,com.resonance.player.TutorialNavigationTest,com.resonance.player.AudioSessionIntegrationTest' com.resonance.player.debug.test/androidx.test.runner.AndroidJUnitRunner` | 新版包 25 项全部通过，56.839s，日志 `verified-core.log`。PowerShell 调用脚本时 class 列表必须保持为一个带引号的参数；未加引号的初次调用由 am 拒绝，未执行测试，不计入结果。 |
| `adb shell am instrument -w -r -e class com.resonance.player.MusicStageDeviceTest -e stageTheme <dark/light> -e stageLabel refined-<portrait/landscape> ...` | 深浅主题 × 横竖屏各 3 项全部通过，共 12 次执行；分别 14.455s / 15.752s / 15.962s / 14.628s。每组包含正常/150% 字号、进度/歌词/队列/设置/播放状态。连同上行共 37 次回归执行（28 个独立方法）。 |
| `MusicStageFrameMetricsTest` 的 player/navigation 及 navigation + `-e metricsWarmup true` | 两包各三次采集均成功，共六次诊断执行。首次路径两包使用同一测试 APK，预热路径两包使用新增参数后的同一测试 APK。全部回调丢弃数为 0；不是性能通过断言。 |
| 自身 Compose 内容截图 | 28 张新版截图保存于 `device-stage/refined-*.png`；检查竖屏封面、横屏 150% 主控边界等，不截取其他应用。 |
| 设备最终状态 | 旋转恢复 `0/0`、字号 `1.0`，新版主包哈希复核一致；启动 `com.resonance.player.debug/com.resonance.player.MainActivity` 成功。 |

窗口诊断结果如下。p95、最大值为 TOTAL_DURATION，末列为超过各帧自身 DEADLINE 的样本数；不以统一的 16ms 门槛替代系统 deadline，也不将回调丢弃数视为掉帧。

| 包 / 场景 | 采集 ms | 样本 | p95 ms | 最大 ms | 超 deadline |
| --- | ---: | ---: | ---: | ---: | ---: |
| 前一包 / 播放器 | 8009 | 956 | 16.06 | 45.04 | 2 |
| 回弹版 / 播放器 | 8015 | 933 | 20.27 | 137.02 | 9 |
| 前一包 / 首次导航路径 | 8069 | 233 | 106.98 | 684.44 | 56 |
| 回弹版 / 首次导航路径 | 8019 | 216 | 121.93 | 691.78 | 56 |
| 前一包 / 预热后导航路径 | 8022 | 269 | 65.13 | 304.34 | 41 |
| 回弹版 / 预热后导航路径 | 8021 | 268 | 71.14 | 315.03 | 50 |

本次没有测出性能提升：回弹版 TOTAL_DURATION 分位数更高，两版预热后仍有尖峰。播放器 DRAW_DURATION p95 分别约 4.45 / 4.39ms，不能仅凭总耗时断言新增波浪导致绘制退化；该场景 DEADLINE 中位数也不同（约 22 / 30.33ms）。需 Release / 非 debuggable 构建及调用栈证据进一步定位，不能从这六段 Debug 短采样推断正式版稳定帧率或原因。原始 JSON/CSV 与 `metrics-summary.json` 保存在忽略的构建目录，不提交设备标识或本机工具路径。

## 限制与后续

- 完成了记录 05 因断线未完成的安装、设备回归和配对诊断；没有证明回弹版满帧或更快。切页耗时尖峰仍需非 Debug 性能调查。
- 用户随后反馈这版回弹仍偏僵硬，并新增直接加密一起听需求；后续更改应分别留新记录，不能将本记录的验证冒充后续包结果。
- 本次未运行全量 allTests、桌面测试、Release Lint、Release 性能基准、Perfetto、Windows 安装包或真实听音验收；前一记录的桌面结果仅属于其对应源码。

## 同步更新的文档

- [文档入口](../README.md) 与 [开发说明](../DEVELOPMENT.md)：更新本次设备结果、预热参数及性能限制。
