# 2026-11-13 — 隔离智能补全弹窗的跨窗口毛玻璃状态，排查开始补全卡死

- 修改日期：2026-11-13（Asia/Shanghai）
- 类型：Bug 修复
- 适用版本：0.6.11 / 45，保持 `version.properties` 不变
- 影响平台：共享 Compose UI，Android、Windows、Linux
- 关联需求：用户反馈多选四首歌曲，在“智能补全与 AI 检索”弹窗点击“开始补全”后应用卡死。
- 关联记录：[导出弹窗真实 Haze ANR](2026-11-04-17-export-dialog-anr-and-lazy-scopes.md)、[播放设置窗口隔离](2026-11-04-10-audio-effect-dialog-window-isolation.md)

## 需求或问题

用户提供的 Android 截图显示选中四首、缺封面零首、缺歌手/专辑零首、歌词库检索四首；封面、歌词、名称补全和 AI 兜底勾选，强制覆盖关闭。用户报告点击开始后应用卡死。截图不能确定当时的曲目、网络响应或主线程栈；当前 `adb devices -l` 没有连接设备，因此未在原手机复现本次 ANR。

## 原因与依据

已确认的代码缺陷是原生弹窗跨窗口使用了宿主的 Haze 状态：

1. `App.startBatchEnrich` 在 Compose 主线程上设置 `batchEnrichRunning`，`BatchEnrichDialog` 从配置切换为进度；后续进度和报告继续触发重组。
2. `BatchEnrichDialog` 使用独立原生 `Dialog`，其外层、配置统计区和完成结果区使用 `resonanceGlass`，此前没有隔离 `LocalGlassState`。这些效果会读取主窗口的 Haze source。
3. 新增测试在修改前的当前源码上检查真实窗口的 modifier 树，两项测试都因弹窗存在 `HazeEffectNodeElement` 失败。修复后要求所有弹窗阶段均无该节点，同时宿主仍存在 Haze 效果；没有通过关闭整个宿主效果绕过断言。

仓库此前在导出弹窗中抓到过 `SnapshotStateObserver → HazeSourceNode → SnapshotStateSet` 的主线程 ANR。这是本次优先消除同类跨窗口依赖的依据，**不是本次手机已经抓到相同线程栈的证据**。本次不能宣布原机卡死的唯一根因已确认或真机问题已验收解决。

按 `anr-audit-and-fix` 技能核对的排除项：Android / 桌面的 `batchEnrichTracks` 和 `MetadataEnricher.enrichTrack/batchEnrich` 均在 `withContext(Dispatchers.IO)` 中执行网络、缓存和逐曲落盘，未发现这些工作同步阻塞点击的主线程；协程挂起不等于阻塞。未发现这条链路中可据以认定同步屏障泄漏的 attached View 跨线程调用。未新增或扩大 ANR 归因。

## 具体修改

| 文件 | 关键类 / 函数 | 修改与目的 |
| --- | --- | --- |
| [BatchEnrichDialog.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/BatchEnrichDialog.kt) | `BatchEnrichDialog` | 在原生 Dialog 外提供 `LocalGlassState = null`，覆盖配置、进度、结果以及内部小面板；让现有 `resonanceGlass` 使用静态渐变后备，避免效果在独立窗口中订阅宿主 source。宿主状态保持原值。 |
| [BatchEnrichDialogTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/BatchEnrichDialogTest.kt) | `configProgressAndCompletionKeepGlassInsideTheHostWindow`、`startProgressStopCompleteAndReopenLeaveHostResponsive` | 在真实 Compose 原生弹窗和 Haze 宿主环境中检查窗口 modifier 树、浅深主题、四首进度更新、三轮开始/停止、完成、重开/取消和宿主点击；同时核对默认选项与 AI 未配置时不启用兜底。 |
| [BatchEnrichDialogDeviceTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/BatchEnrichDialogDeviceTest.kt) | `startStopCompleteAndReopenKeepHostResponsive` | 新增独立 Android 原生窗口回归。浅深主题各反复开始/停止、逐首更新进度、完成、重开/取消并操作宿主。使用虚构歌曲和回调，不访问真实音频、网络或模型。 |

没有改动补全算法、平台接口、任务保存和取消实现；此次修复集中在弹窗窗口边界。保留文件原有行尾，未格式化其他函数。

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 打开配置和点击开始 | 原生弹窗表面订阅主窗口 Haze，状态切换改变这些订阅 | 弹窗所有阶段使用独立静态后备表面 |
| 连续更新四首任务进度、停止后再开始 | 缺少针对此入口的窗口依赖与连续切换回归 | 新增回归验证多次开始/停止、进度和按钮继续响应 |
| 完成报告出现、关闭后操作宿主 | 结果小面板仍可能读取宿主 Haze | 结果小面板也被隔离；主窗口原有 Haze 和操作继续保留 |

## 平台与数据影响

三平台使用同一共享弹窗。没有改变 Android / Windows / Linux 平台接口、偏好字段、缓存或同步包格式；没有迁移或删除音频、写回文件标签、读取签名文件或实际 API Key。没有新增模型调用、付费测试或网络上传。本次版本保持 0.6.11 / 45；没有打包、安装或发布。

## 验证

日志和验证脚本在忽略目录 `build/batch-enrich-freeze-2026-11-13/`。Kotlin / 原生渲染分别串行运行于独立 systemd 用户服务，并取得共享 `build/resonance-heavy-validation.lock`。整个进程树限制 `MemoryMax=2G`、`MemoryHigh=1536M`、`MemorySwapMax=0`；运行前检查至少 3.5 GiB 可用，运行中低于 1536 MiB 余量停止自身验证。编译 JVM 使用 768 MiB 堆，测试 JVM 使用 512 MiB 堆；单编译进程，不启动 Gradle / Kotlin daemon。本次最终桌面服务峰值约 1.1 GiB，Android 服务峰值约 1.2 GiB，均正常结束；未调整总内存上限、swap 或关闭用户应用。

| 实际命令或操作 | 环境与结果 |
| --- | --- |
| `python3 build/batch-enrich-freeze-2026-11-13/verify_desktop.py baseline` | 当前完整共享/JVM/桌面 189 份生产源码编译通过。首次测试编译因当前 Compose `LayoutInfo` 没有 `childrenInfo` 失败；改用公共 `parentInfo` 从语义节点回溯，保存首轮日志。重新编译测试时校验生产源码哈希未变化并复用本轮新编译结果；随后两项新增回归均因跨窗口 Haze 断言而预期失败。 |
| `python3 build/batch-enrich-freeze-2026-11-13/verify_desktop.py final` | **通过**：189 份当前完整生产源码、3 份测试源码编译；隔离 Xvfb / 软件渲染下 8 项定向回归全部通过、0 失败、0 跳过。含新增弹窗 2 项、播放设置窗口 2 项、元数据补全 4 项；源码哈希守卫通过。桌面编译后只还原未改动函数原有的两处混合行尾，校验规范化文本完全相同，见 `line-ending-preservation.json`。 |
| `python3 build/batch-enrich-freeze-2026-11-13/verify_android.py` | **通过**：以最终源码完整编译 182 份共享/JVM/Android/Debug 应用源码，并编译新增设备测试 1 份；退出码均为 0，源码哈希守卫通过。设备用例仅编译、没有运行，未构建 APK。 |
| `adb devices -l` | **环境受阻**：没有连接设备；新设备回归未执行，未取得本次原机 ANR / logcat，不代表手机卡死已复验消失。 |
| `git -c core.whitespace=blank-at-eol,blank-at-eof,space-before-tab,cr-at-eol diff --check -- <本次跟踪文件>`；新测试空白及文档相对链接脚本核对 | **通过**。源文件既有 CRLF / 混合行尾按原样保留，命令显式识别 CR 行尾；两份新测试无行尾空格，新记录 5 个相对链接均存在。 |

上述是当前源码的直接 Kotlin / Compose 编译及实际 JVM 回归，**不是全工程 Gradle 测试、Lint、APK 构建或 Android 设备验收**。完整生产源码重新编译，没有用历史安装中的业务类替代本次源码；依赖复用既有解析结果。

## 限制与后续

- 原 Android 手机点击卡死尚需连接设备后复测。优先在独立验证包运行 `BatchEnrichDialogDeviceTest`，再在明确保留数据的安装流程中验证真实四首歌曲入口；必要时抓取当时主线程/渲染线程与 `lastanr`，不根据本次桌面测试推断用户手机的具体栈。
- Windows 原生窗口、真实模型调用和公网封面/歌词来源本次未执行；JVM 窗口回归不能替代这些验收。
- 前面讨论的专辑封面匹配质量、下载/批量查词接入歌源回退属于后续工作，本次没有将它们当成卡死根因或宣称已经完成。

## 同步更新的文档

`docs/README.md` 最新入口及变更索引；`docs/DEVELOPMENT.md` 第 8 节补全窗口边界、验证限制和第 14 节任务定位。
