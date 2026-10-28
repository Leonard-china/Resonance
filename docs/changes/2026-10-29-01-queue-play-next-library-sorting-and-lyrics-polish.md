# 2026-10-29 — 播放队列深度管理、下一首播放插队、曲库多维排序筛选与歌词舞台交互打磨

- 修改日期：2026-10-29（Asia/Shanghai）
- 类型：功能 / 交互打磨 / 质量加固
- 适用版本：`0.6.5 / 39`（由 `0.6.4 / 38` 升级）
- 影响平台：共享（commonMain / jvmMain）、Android、Windows
- 关联记录或需求：参考业界成熟播放器（椒盐音乐、Poweramp、Auxio）实践，响应全量体验深度优化目标

## 需求或问题

1. 播放队列管理薄弱：用户无法直观了解当前队列歌曲总数，无法快速定位并滚动至正在播放的歌曲，无法一键清空其他待播歌曲，也无法灵活移除指定队列项或上下微调顺序。
2. 缺乏「下一首播放」功能：用户在曲库浏览或搜索时，若想优先听某一首歌，必须直接切歌（打断当前播放）或手动将其加入自建歌单，操作链路长且体验割裂。
3. 曲库单曲列表缺少即时过滤与排序：歌曲较多时缺少单曲列表内即时过滤功能，且底层定义的 `TrackSortOption` 与 `TrackFilterOption` 未在单曲界面渲染呈现。
4. 歌词舞台滚动脱离后缺少复位：用户在歌词界面上下滑动浏览其他歌词后，歌词视图脱离当前播放进度，没有快速「回到当前播放」的便捷触控入口。
5. Android 真机与模拟器自动化测试适配：部分测试用例中硬编码调用了 Android API 34+ 引入的 `Path.of`，导致在 Android 12 (API 31/32) 模拟器或低版本系统上发生 `NoSuchMethodError`。

## 原因与依据

1. `NowPlayingOverlay.kt` 中的 `QueuePane` 原先仅作为静态列表展示当前播放歌曲，未暴露针对单个项的置顶下一首、移除、顺序调整及针对整个队列的清理回调。
2. `App.kt` 原有队列调度逻辑以当前正在播放的列表为基准，未提供将目标单曲插入当前索引下一位的纯函数队列构建器 `buildPlayNextQueue`。
3. `LibraryShell.kt` 的 `TrackListTab` 虽然接收了全量曲目，但未引入列表内局部搜索框以及排序下拉和筛选芯片组。
4. `TogetherConnectionDeviceTest.kt` 调用了 `java.nio.file.Path.of`，在 Android API 31/32 的运行时环境中该方法未向后移植，需使用通用的 `java.nio.file.Paths.get`。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| [LibraryModels.kt](file:///d:/Project/Resonance/composeApp/src/commonMain/kotlin/com/resonance/player/model/LibraryModels.kt) | `filterAndSortTracks`, `buildPlayNextQueue`, `buildRemoveFromQueue`, `buildReorderedQueue` | 增加队列纯函数变更构建器与多维排序筛选逻辑，供 UI 与单元测试复用 |
| [NowPlayingOverlay.kt](file:///d:/Project/Resonance/composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | `QueuePane`, `LyricsContent` | 队列面板增加总数统计、一键定位、一键清空其他、置顶下一首、移除与上下调序；歌词面板增加「回到当前播放」悬浮按钮与双语明暗层级 |
| [LibraryShell.kt](file:///d:/Project/Resonance/composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt) | `TrackListTab`, `TrackRow`, `LibraryDetailView`, `SearchResultList` | 单曲列表顶部内嵌即时搜索框、排序下拉菜单（歌名/歌手/专辑/时长）与类型筛选芯片（无损/高品质/缺封面/缺元数据）；单曲更多菜单中增加「下一首播放」操作 |
| [App.kt](file:///d:/Project/Resonance/composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | `playNext`, `removeFromQueue`, `clearQueue`, `moveTrackInQueue` | 实现队列变更动作，即时联动 `customQueue` 与播放调度，并给出轻量状态提示 |
| [PlaybackQueueAndFilterTest.kt](file:///d:/Project/Resonance/composeApp/src/desktopTest/kotlin/com/resonance/player/playback/PlaybackQueueAndFilterTest.kt) | `PlaybackQueueAndFilterTest` | 建立播放队列操作与曲库即时过滤、排序的完整桌面端单元测试套件 |
| [TogetherConnectionDeviceTest.kt](file:///d:/Project/Resonance/composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/TogetherConnectionDeviceTest.kt) | `TogetherConnectionDeviceTest` | 将 `Path.of` 替换为兼容 Android 12 的 `Paths.get`，确保模拟器自动化测试顺利通过 |
| [version.properties](file:///d:/Project/Resonance/version.properties) | `versionName`, `versionCode` | 升级至 `0.6.5` / `39` |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 曲库单曲更多操作 | 仅支持收藏、编辑、加入歌单、删除文件 | 新增「下一首播放」，点击后无缝插入当前播放项之后并给出提示 |
| 播放队列管理 | 仅列表高亮当前曲目 | 支持总数显示、定位正在播放、清空其他、单曲插队为下一首、移除和调序 |
| 曲库单曲检索与筛选 | 必须跳转全局搜索页面，无单曲维度排序 | 单曲列表内直接即时输入过滤，支持按歌名/歌手/专辑/时长正倒序及无损/缺失项过滤 |
| 浏览歌词脱离当前行 | 必须手动滚回当前高亮行 | 界面右下角自动浮现「回到当前播放」药丸按钮，点击即平滑回滚 |
| Android 12+ 模拟器端到端直连测试 | 触发 `NoSuchMethodError: Path.of` 失败 | 采用 `Paths.get` 兼容 API，测试顺利通过 |

## 平台与数据影响

- **共享逻辑**：纯 Kotlin 实现，完全跨平台支持 Android 与 Windows。
- **Android / Windows**：UI 与交互行为完全一致，动画遵循系统 `reducedMotion` 设定。
- **数据兼容性**：不改变数据库结构与歌单存档格式；`customQueue` 延续应用现有持久化机制自动写入快照。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `.\gradlew.bat :composeApp:desktopTest --tests "com.resonance.player.playback.PlaybackQueueAndFilterTest"` | Windows 11 / JDK 17 | 通过（覆盖插队、移出、调序、排序、过滤全部边界） |
| `.\gradlew.bat :composeApp:desktopTest` | Windows 11 / JDK 17 | 通过（全量桌面端测试 100% 通过） |
| `.\gradlew.bat :composeApp:assembleDebug` | Windows 11 / Android Gradle Plugin 8.11 | 通过（生成 167 MB Debug APK） |
| `.\gradlew.bat :composeApp:assembleDebugAndroidTest` | Windows 11 / Android Gradle Plugin 8.11 | 通过（生成 AndroidTest APK） |
| `adb -s 127.0.0.1:16384 install -r ...` | MuMu 12 模拟器 (Android 12, Vulkan 1.4) | 通过（Streamed Install Success，未唤醒真机） |
| `adb -s 127.0.0.1:16384 shell am instrument ... LibraryNavigationTest` | MuMu 12 模拟器 | 通过（`librarySwipesSelectTabsBeforeMainNavigationAndRetainListPosition` OK） |
| `adb -s 127.0.0.1:16384 shell am instrument ... TogetherConnectionDeviceTest` | MuMu 12 模拟器 | 通过（端到端加密双机直连证书生成与验证 OK） |
| `adb -s 127.0.0.1:16384 shell am instrument ... MusicFeaturesIntegrationTest` | MuMu 12 模拟器 | 通过（OK 2 tests） |
| `adb -s 127.0.0.1:16384 shell am instrument ... AudioSessionIntegrationTest` | MuMu 12 模拟器 | 通过（OK 4 tests） |
| `adb -s 127.0.0.1:16384 shell am instrument ... TogetherUiDeviceTest` | MuMu 12 模拟器 | 通过（OK 2 tests） |

## 限制与后续

- 本次范围内无已知未完成项。

## 同步更新的文档

- `docs/README.md`：增加 2026-10-29-01 变更条目与版本索引。
- `docs/DEVELOPMENT.md`：核对开发与测试说明，更新核对日期。
- `docs/RELEASE_NOTES-0.6.5.md`：建立 0.6.5 版本发行日志。
- `website/content/0.6.5.json`：建立官网版本元数据。
