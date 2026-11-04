# 2026-11-03 — 修复每日推荐点击后的播放错位与切歌队列

- 修改日期：2026-11-03（Asia/Shanghai）
- 类型：Bug 修复
- 适用版本：`version.properties` 中的 0.6.5 / 39，本次不升版
- 影响平台：共享 / Android / Windows / Linux
- 关联需求：用户反馈 Android 每日推荐单次点击后，声音或当前歌曲显示不对
- 关联记录：[加载与缓冲状态](2026-11-02-07-loading-and-buffering-feedback.md)、[推荐独立页面](2026-10-16-04-pinned-recommendations.md)

## 需求或问题

每日推荐点击歌曲后，界面显示的曲目可能与实际播放不一致；后续上一首、下一首也可能播到本批推荐以外的曲目。

## 原因与依据

1. `App.playAvailableTrack` 先暂停旧歌，再经 `PlaybackStateStore.activate` 异步读取音量和保存快照。Android 的旧歌 `onIsPlayingChanged(false)` 随后进入 `PlaybackStateStore.setPlaying`，原实现将它当成新的播放意图，取消新歌的 `onReady`。此时 UI 已选中新歌，原生播放器仍保留旧歌；恢复播放可能听到旧歌。这条链路由可控延迟的 JVM 回归复现；尚未在本次 Android 设备上复现。
2. `RecommendationsScreen` 的播放按钮原来只传单曲，`App` 将 `importedTracks` 整库作为队列，丢失当前推荐的范围和顺序。
3. 恢复下载后，`onReload` 修改 App 状态不等于 Compose 已把新曲库传入旧回调。目标可能不在旧队列里，或同 ID 仍指向旧 URI。Android `play` 在找不到目标 ID 时将索引钳为 0，可能实际播放队首，而 UI 已显示目标。
4. 音频文件不可读后的恢复弹窗只保存单曲，恢复后没有保留原队列和随机模式。

## 具体修改

| 文件 | 关键符号 | 具体修改与目的 |
| --- | --- | --- |
| [PlaybackStateStore.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/playback/PlaybackStateStore.kt) | `StartAction`、`setPlaying`、`refreshAudio` | 显式切歌尚未交给原生播放器时，忽略旧媒体的播放状态回调；用户主动暂停仍由 `setUserPlayIntent` 生效；刷新音量时保留待启动请求的属性 |
| [MusicFeatureController.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureController.kt) | `onPlay`、`playRecommendation` | 回调显式携带队列；恢复下载后用返回的新 Track 替换批次旧项；取消旧恢复请求并检查协程与请求代次，防止迟到结果抢播 |
| [MusicTasksScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicTasksScreen.kt) | `RecommendationsScreen` | 播放按钮传入页面按推荐 ID 顺序构建的本批队列 |
| [RecommendationHistoryScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/RecommendationHistoryScreen.kt) | 播放／恢复下载回调 | 适配显式队列参数，历史页面继续使用曲库队列 |
| [App.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | `playAvailableTrack`、`resolveTrackPlaybackQueue`、`PendingTrackRestore` | 原生队列和快照共用同一份有效队列；目标同 ID 项使用最新 Track，目标缺失时补入，过滤空音频并去重；恢复弹窗保留原队列与随机模式 |
| [PlaybackStateStoreTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/playback/PlaybackStateStoreTest.kt) | 延迟原生暂停／播放回归 | 验证单次选歌不被旧暂停取消、主动暂停不被旧播放事件撤销，以及准备完成后的正常播放状态 |
| [PlaybackQueueResolutionTest.kt](../../composeApp/src/commonTest/kotlin/com/resonance/player/PlaybackQueueResolutionTest.kt) | 目标缺失／旧 URI／批次顺序回归 | 验证恢复曲目总能在实际队列中找到正确音频，并保留推荐顺序 |
| [RecommendationPlaybackTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/RecommendationPlaybackTest.kt) | 推荐播放与恢复回归 | 验证整库不混入推荐队列、未重组时的恢复下载、旧请求迟到、下载失败与重试 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| Android 播放旧歌时单次点选新歌，旧暂停回调迟到 | 可能不启动新音频，界面先显示新歌 | 待启动请求不接受旧媒体状态改变播放意图 |
| 准备中用户主动暂停，旧播放回调迟到 | 可能撤销暂停并启动新歌 | 保留主动暂停 |
| 点击本批推荐，再切上一首／下一首 | 使用整库顺序，可能跳出推荐 | 使用本批推荐的有效音频与原顺序 |
| 恢复下载后立即播放 | 旧队列可能缺失目标或保留旧 URI | 以恢复返回的目标 Track 构建原生队列与快照 |
| 下载未完成又点另一首推荐 | 旧下载完成可能重新抢播 | 旧请求取消或迟到时不再发出播放动作 |
| 文件不可读，选择重新定位或重新下载 | 可能丢失队列上下文 | 保留请求的队列和随机模式 |

## 平台与数据影响

- 共享修复影响 Android、Windows、Linux；Android 使用原生队列索引，桌面仍使用既有 FFmpeg／Java Sound 播放链路。
- 平台接口、偏好键、快照结构、歌单与同步包格式不变；旧数据继续兼容。只改变保存到现有快照的正确队列内容。
- 不改变音频转换、文件删除、推荐清理、标签写回、收藏保留、原子文件和备份恢复规则。不增加权限、外部服务或凭据。
- `PlaybackStateStore` 原有准备任务取消、代次检查和用户音量／增益优先级保留。

## 验证

本机 Linux / JDK 17 / Android SDK 36；使用仓库已有的本机临时工具，不将工具路径提交为资源。

最终执行命令（省略本机 JDK／SDK 环境变量赋值）：

```bash
"$JAVA_HOME/bin/java" -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain \
  :composeApp:desktopTest \
  --tests 'com.resonance.player.ui.RecommendationPlaybackTest' \
  --tests 'com.resonance.player.PlaybackQueueResolutionTest' \
  --tests 'com.resonance.player.playback.PlaybackStateStoreTest' \
  --tests 'com.resonance.player.playback.PlaybackQueueAndFilterTest' \
  :composeApp:compileDebugKotlinAndroid --no-configuration-cache --console=plain --continue
```

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| 初次运行上述 4 个桌面测试类（当时尚未新增原生状态回调回归） | Linux / JVM 17 | 通过：33 项，1m14s；只证明推荐队列／恢复修复与既有状态测试通过 |
| 原状态实现运行 `desktopTest --tests 'com.resonance.player.playback.PlaybackStateStoreTest.previousNativePauseCannotCancelASingleTrackSelection' --tests 'com.resonance.player.playback.PlaybackStateStoreTest.previousNativePlayingCannotUndoAnExplicitPauseDuringPreparation'` | Linux / JVM 17 | 失败：单次切歌用例观察到比较断言失败。另一项初版断言过早退出，遗留夹具待决协程，该次执行主动终止；随后把断言移到任务排空后，最终测试见下一行 |
| 上述最终联合命令 | Linux / JVM 17 / Android SDK 36 | 通过：36 项测试、0 失败、0 错误、0 跳过；Android Debug Kotlin 编译通过，联合任务 46s。存在既有弃用／可空性编译警告 |
| `git diff --check -- <本次涉及的已有文件>` | 当前混合 CRLF 工作区 | 默认设置将既有 CRLF 作为行尾空白报告；未据此转换整份用户文件 |
| `git -c core.whitespace=blank-at-eol,blank-at-eof,space-before-tab,cr-at-eol diff --check -- <本次涉及的已有文件>` | 同上 | 通过 |
| 新记录相对链接与索引脚本核对 | Python 3 | 12 个相对链接均存在，索引条目存在 |
| `adb devices -l` | 本机 Android platform-tools | 无已连接设备，设备播放验收受环境限制未执行 |

最终 36 项包括：`PlaybackStateStoreTest` 15 项、`PlaybackQueueResolutionTest` 7 项、`RecommendationPlaybackTest` 4 项、`PlaybackQueueAndFilterTest` 10 项。此次新增 10 项。原始日志、最终测试 XML 和汇总位于忽略目录 `build/daily-recommendation-2026-11-03/`；不是引用历史测试产物。

## 限制与后续

- 本次 `adb devices -l` 未发现连接设备，Android 真机／模拟器的单次点击、锁屏切歌、音频焦点中断和真实音频听感尚未验收；JVM 回归不能替代设备验收。
- Windows 原生窗口与音频设备未在本次运行。未打包、安装、发布或改动已安装版本。
- 如果新构建仍出现声音与显示不同，继续从 Android MediaController 的媒体 ID、URI 和回调顺序追踪，不将本次替身测试视为真实歌源内容正确性验证。

## 同步更新的文档

- [文档入口](../README.md)：新增本记录索引。
- [开发说明](../DEVELOPMENT.md)：第 7 节播放并发边界及第 13 节推荐播放链路，核对日期为 2026-11-03。
