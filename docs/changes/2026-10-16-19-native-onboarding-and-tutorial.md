# 2026-10-16 — 新增首次启动双模式教程、真实界面操作教学与学习进度

- 修改日期：2026-10-16（Asia/Shanghai）
- 类型：功能 / 文档 / 发布版构建
- 适用版本：0.4.4 / 27；用户要求完成后构建发布版，本次不升版、不上传 GitHub Release。
- 影响平台：共享、Android、Windows；不涉及独立官网。
- 关联需求：首次启动提供快速图文与实际操作教学，完成或跳过后始终可从设置进入。

## 需求或问题

应用已有导入、播放、AI、推荐和歌词等入口，但新用户首次打开时没有使用教程，已有关于页和音量帮助也不能替代完整上手流程。用户确认同时提供适合快速浏览的图文和适合仔细学习的操作教学。

## 原因与依据

`App` 启动只恢复曲库、播放与设置，没有首次教程状态；`SettingsScreen` 没有统一教程目录。详细教学需要接入已有实际控件与状态，而不能把点击“下一步”当作已经导入歌曲或调用成功。收费、删除等功能只讲解必要行为，不由教程自动执行。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| [TutorialCatalog.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/tutorial/TutorialCatalog.kt) | `TutorialCatalog`、`TutorialProgress`、`ChapterProgress`、`record` | 单一内容源：四页快速介绍和七章详细教程。持久记录首次提示、快速阅读位置、逐章步骤与完成/跳过集合；区分实际操作和阅读确认。 |
| [TutorialController.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TutorialController.kt) | `restore/offerWelcome/start/event/advance/pause/locate` | 管理首次选择、章节定位、事件匹配、进度与继续；导航本身不执行音频、账号或付费操作。缺少当前歌曲时先回曲库。 |
| [TutorialScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TutorialScreens.kt) | `TutorialDialogs`、`TutorialScreen`、`QuickTutorial`、`TutorialCoach`、`tutorialTarget` | 欢迎选择、离线目录/图文/常见问题、主题自适应原生示意、实际控件高亮与滚动定位。窗口坐标兼容系统 inset、导航栏和播放器图层，入场结束后再次读取实际坐标；遮罩不吞掉真实点击。说明可滚动/收起，暂停按钮固定在卡片顶部。 |
| [App.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | 教程装配、启动加载、状态检查 | 按实际曲库、当前曲目、收藏、歌单和配置判断完成条件；沿用 `PersistenceQueue.submitLatest` 顺序写入本地进度。 |
| [LibraryShell.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt)、[NowPlayingOverlay.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt)、[PlaylistGrid.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/PlaylistGrid.kt) | `LibraryDestination.Tutorial`、实际控件锚点及播放/歌词/队列事件 | 按章进入实际页面和栏目，实际展开/切换后继续；暂停教学时收起教学打开的播放器。普通浏览不改变原有列表位置策略。 |
| [ToolsScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/ToolsScreens.kt)、[MusicFeatureScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureScreens.kt) | `SettingsScreen`、`SettingRow`、模型/导入等入口 | 设置顶部固定“使用教程”；定位导入、模型、推荐、翻译、语音、悬浮歌词和歌单文件入口。敏感操作仍需用户自己触发。 |
| [PlatformServices.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/PlatformServices.kt)、[AndroidPlatformServices.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt)、[DesktopPlatformServices.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopPlatformServices.kt) | `loadTutorialProgress/saveTutorialProgress` | 两端实现新进度接口。Android 使用现有私有偏好并检查 commit；Windows 使用既有同步、原子 UI 存储及备份恢复。 |
| [TutorialProgressCodec.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/TutorialProgressCodec.kt) | `KEY/encode/decode` | `tutorial.progress.v1` JSON 编解码，独立于应用版本。限制数据大小、步骤范围，过滤未知 ID；旧配置无此键时初始化教程。 |
| [TutorialProgressTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/TutorialProgressTest.kt)、[TutorialNavigationTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/TutorialNavigationTest.kt) | 进度与实际 Compose 回归 | 检查首次提示、跨启动恢复、章节隔离、错误事件、跳过语义、原子偏好、实际高亮点击、深层设置定位及 1.5 倍字体/320 dp 高窗口；测试使用隔离夹具。 |
| [LibraryNavigationTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/LibraryNavigationTest.kt) | `show` 中的平台替身 | 补齐 `supportsWebLogin`/`supportsKgg` 明确的 false 返回值，修正旧动态代理返回 null 导致设置页用例布尔拆箱失败；不改变应用实现。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 首次打开 | 直接进入曲库，无统一教程 | 首次提示选择“快速了解”“跟着操作”或跳过。 |
| 以后查阅 | 关于页/音量帮助分散 | 设置固定教程入口，可读每章说明、练习、继续或重新学习；教程页隐藏迷你播放器，手机快速浏览隐藏底栏以保留阅读空间。 |
| 实际操作学习 | 没有 | 高亮真实控件，匹配实际状态/事件后继续；用户可跳过、收起说明或暂停。 |
| 教程退出、重启 | 无学习记录 | 阅读位置与逐章进度保存在本机；已选择或跳过欢迎提示后普通重启/升级不重复弹出。 |
| 推荐清理、歌词识别 | 新用户难以了解语义 | 教程说明真实删除、保留规则、音频上传及收费；无需练习删除或发起付费识别。 |

## 平台与数据影响

- 共享 Compose UI 在 Android 与 Windows 使用相同内容和进度模型；控件定位依据实际布局，兼容底栏、rail、侧栏。
- 新键 `tutorial.progress.v1` 位于 Android `resonance_ui_state` 与 Windows `ui-state.properties`；不包含 Key、Cookie、歌曲路径或音频。不会加入 `.resonance` 歌单包。
- 旧安装升级后没有教程键，因此会在引入该功能后的第一次启动提示一次；已选择学习方式、完成或跳过后均可从设置继续，不按版本号强制重弹。
- 不增加权限、第三方依赖或外部请求。教程文本与示意离线可用；只有用户主动使用实际功能才可能请求权限、联网、写入歌单或付费。
- 沿用已有播放竞争保护、配置保存顺序、原子文件、音频删除与导入校验。官网出现的其他并行改动不属于本任务，未修改或验证。
- 发布版继续使用现有版本号，教程构建独立交付，不替换历史 `release/0.4.4/` 已发布附件和对应校验说明。

## 验证

环境：Windows、JDK 17、SDK / Build Tools 36、API 32 模拟器。测试使用生成的曲目夹具和隔离存储；Windows 启动检查同时隔离数据与音乐目录。没有使用真实音频执行删除，没有调用用户付费模型或语音接口。

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `./gradlew.bat :composeApp:compileKotlinDesktop :composeApp:compileDebugKotlinAndroid --no-configuration-cache --console=plain` | Windows / JDK 17 | **通过**，首轮两端编译，`build/tutorial-compile.log`。 |
| `./gradlew.bat :composeApp:allTests :composeApp:lintRelease :composeApp:assembleDebug :composeApp:assembleDebugAndroidTest --no-configuration-cache --console=plain` | Windows / SDK 36 | **通过**，首轮完整检查，10 分 39 秒；`build/tutorial-verification.log`。 |
| `./gradlew.bat :composeApp:assembleDebug :composeApp:assembleDebugAndroidTest :composeApp:allTests :composeApp:lintRelease :composeApp:assembleRelease :composeApp:packageReleaseExe --no-configuration-cache --console=plain` | Windows / SDK 36 / FFmpeg / WiX | **通过**，最终生产源码两端发布构建，9 分 20 秒，`build/tutorial-release-build.log`。桌面/共享 XML **121 项，0 失败、0 错误、0 跳过**；包含新增 6 项进度回归。Android Debug/Release 单元任务通过；Release Lint **0 错误、92 警告**，不声称零警告。 |
| 安装本次 Debug/测试 APK，逐项执行 `adb shell am instrument -w -r -e class <类#方法> com.resonance.player.debug.test/androidx.test.runner.AndroidJUnitRunner` | API 32，隔离 Compose 夹具 | **最终 11 项通过**：6 项 `TutorialNavigationTest`，5 项 `LibraryNavigationTest`。覆盖首次跳过、四页阅读、真实播放器高亮穿透点击、暂停续学且不自动开启推荐、滚动定位、1.5 倍字体/320 dp 高窗口，以及原有栏目/推荐/播放器导航；结果在 `build/tutorial-device-final/results-verified.json`。 |
| `./gradlew.bat :composeApp:assembleDebugAndroidTest --no-configuration-cache --console=plain`，补跑 `LibraryNavigationTest#disabledRecommendationCardAppearsBeforeUserPlaylistsAndOpensSettings` | 旧测试替身修正后 | **通过**，测试 APK 重建 31 秒，单项补跑 `OK (1 test)`，2.279 秒。其余 10 项已经通过，未将最初失败计为通过。 |
| Windows 完整发布目录启动，`RESONANCE_DATA` / `RESONANCE_LIBRARY` 指向独立测试目录；运行内置 `ffmpeg -version` | 本次完整发布运行目录 | **通过**：定位启动器实际子进程，`MainWindowTitle=Resonance`、`Responding=true`；内置 FFmpeg 8.1.1 可运行。证据 `build/tutorial-windows-smoke.json`；测试创建的进程已关闭。 |
| `apksigner verify --verbose --print-certs`、`zipalign -c -P 16 4`、`aapt2 dump badging` | 本次 Release APK | **通过**：v2 签名有效、16 KiB 对齐，包名 `com.resonance.player`，版本 `0.4.4 / 27`，minSdk 26 / targetSdk 36；证书 SHA-256 为 `b2b4ad35b8c7136569d219103448eaee75d4bbd30f3872c601373df43b303fe5`，与既有正式证书一致。v1 为 false，不宣称 v1 验证通过。 |
| 本地 Markdown 链接检查、`git diff --check` | 本次文档与改动 | **通过**，源码链接存在；未修改版本文件、历史发布说明和历史 `release/0.4.4/` 附件。 |

实际验证中出现的问题：首次连续启动三个教程测试时，第三个测试 Activity 出现空白窗口并等待，被主动停止，日志为 `build/tutorial-device-initial.log`，该次中断不计通过。改为每个用例单独启动相同 runner 后，新教程 6 项均通过。旧导航测试首次 4 项通过、1 项因动态代理返回 null 失败；修正替身后单独补跑通过，保留失败与补跑日志。Windows 首次 15 秒探测未取到主窗，延长到 45 秒上限后按实际子进程成功确认；不将首次探测写成通过。

界面实图已人工核对：`build/tutorial-quick-final.png` 与 `build/tutorial-coach-final.png`，仅包含夹具歌曲和本地示意。教程提示可阅读，高亮位于真实队列按钮，暂停按钮固定可见；这些本机 QA 图片没有加入仓库资源。

### 本次实际产物

独立交付目录：`release/0.4.4-tutorial-2026-10-16/`。保留本次安装包、`SHA256SUMS.txt` 和 `manifest.json`，不覆盖已有正式版归档。

| 文件 | 实际字节数 | 本次 SHA-256 |
| --- | --- | --- |
| `Resonance-0.4.4-release.apk` | 64,109,577 | `e3d82b2ad394bedb04a579d68197416fde045b4cc6f7bc809b5bfc31ce32425b` |
| `Resonance-0.4.4.exe` | 204,931,072 | `f06ac519ce41abe65ee9d85b201b1e43ea8edd2fbaff0dc2c325fc4dabac67a8` |

用户仅要求构建发布版，未自动升版，也未创建或更新 GitHub Release。没有执行会覆盖同版历史发布校验的 `release-manifest.ps1`；本次独立归档的校验来自实际新产物。

## 限制与后续

- 系统文件选择器、B 站登录页和第三方服务弹窗由原平台处理，教程在返回应用后继续，不在这些窗口中模拟点击。
- 详细教学包含真实操作步骤及明确标注的阅读步骤；导出/导入、付费请求、删除等无需实际执行即可读完。
- 真实服务商登录/付费请求、全部国产 ROM 与 Windows 多显示器交互不因本地编译或夹具回归而宣称通过。
- Windows 完成完整发布目录启动和内置 FFmpeg 检查，未逐章执行桌面鼠标/键盘教学回归；共享交互由 Android 实际 Compose 夹具验证。Android 单项 runner 可重复执行，连续测试 Activity 的稳定性限制如上记录。
- 本次范围内功能和构建已完成。后续正式对外发布时需要按用户指定版本单独升版、准备新版发布说明并重新构建/计算校验；当前产物版本保持 0.4.4 / 27。

## 同步更新的文档

- `docs/README.md`：新功能索引与当前开发构建说明。
- `docs/DEVELOPMENT.md`：教程模块、进度存储、启动规则与测试定位。
- `README.md`：公开使用说明增加首次教程和后续入口。
