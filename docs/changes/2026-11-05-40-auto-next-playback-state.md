# 2026-11-05 — 修复自动切歌后按钮与实际播放状态不同步

- 修改日期：2026-11-05（Asia/Shanghai）
- 类型：Bug 修复
- 适用版本：`0.6.6 / 40`，保持 `version.properties`，不升版
- 影响平台：共享、Android、Windows、Linux
- 关联需求：用户确认自动切到下一首时按钮显示播放三角形（看起来已暂停），实际音频仍继续；前置[播放后台与恢复](2026-11-05-36-resume-playback-after-device-failure.md)。

## 需求或问题

自动切歌后，按钮表达的播放状态与声音不一致。用户于 2026-11-05 补充确认：“显示播放三角形，音乐继续”。该现象与本次复现及回归的“界面判为已暂停、底层继续播放”一致；同时覆盖随后点击暂停的状态取反。此次补充仅澄清同一问题的现象，不改变实现或既有验证结果。

## 原因与依据

Android Media3 自行切到下一首时，通过 `activeTrackChanges` 通知 App。旧 App 调用通用 `PlaybackStateStore.activate`，它将 `isPlaying` 重置为 false；连续播放时 Media3 的实际 isPlaying 可能一直为 true，因而不会再产生一次 true 回调纠正界面。播放按钮依赖此字段选择图标和执行取反，随后一次点击可能再次请求播放。

本次使用修改前完整源码编译类、代理平台及非延迟协程复现旧 App 相同调用：`nativeIsPlaying=true; uiIsPlaying=false`，断言“自动下一首错误重置按钮”失败。该复现检查的是共享状态链路，不宣称已在手机复现声音及图标。

检查中还发现，普通点播／桌面下一首启动后，后续响度分析抛错也会被同一个 catch 设置为暂停，但不会暂停原生音频；本次一并修正同类错误。

## 具体修改

| 文件 | 关键符号 | 具体修改与目的 |
| --- | --- | --- |
| [NativePlaybackState.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/playback/NativePlaybackState.kt) | `NativePlaybackState` | 表达一次读取的原生曲目 ID、实际播放、播放意图、缓冲、进度和毫秒位置；不依靠下一条状态回调。 |
| [PlaybackServices.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/PlaybackServices.kt) | `currentPlaybackState` | 新增只读平台契约，缺少原生曲目时返回 null。 |
| [AndroidPlatformServices.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt) | `currentPlaybackState` | 从当前 MediaController 读取同一曲目的实际状态；保留 Media3 在 Android 源集。 |
| [DesktopPlatformServices.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopPlatformServices.kt) | `currentPlaybackState` | Windows／Linux 共用原生音频会话快照与 playWhenReady；桌面自动下一首仍沿用原有 PlaybackSession。 |
| [App.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | `activeTrackChanges` | 读取当前原生快照并交给附着入口，拒绝已过期且与快照曲目不符的事件。 |
| [PlaybackStateStore.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/playback/PlaybackStateStore.kt) | `adoptNativeTrack / activate / refreshAudio / acceptNativeTrack` | 区分显式启动和已发生的原生切歌；附着保留播放／缓冲／位置，设置加载完成不发播放／暂停。原生下一首可取消未完成的前曲增益加载；用户显式点播的旧回调保护仍生效。已经开播后的分析失败仅报告错误，保留实际播放状态。 |
| [PlaybackStateStoreTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/playback/PlaybackStateStoreTest.kt) | 十项新增回归 | 验证无额外 true 回调、旧暂停 UI、缓冲、暂停原生会话、用户点击暂停、连续原生切歌、音量刷新、设置读取错误、过期快照及开播后响度失败。 |

原生附着不重置媒体队列或重新 seek；异步增益仍受 generation、取消检查及用户音量／增益修订号保护。显式开始播放前的准备／持久化失败仍阻止启动。

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 原生连续播放 A → B，isPlaying 一直为 true | App 把 B 标为 false，等待不会再来的回调 | 从 B 原生快照立即显示正在播放 |
| B 正在缓冲，音量读取完成 | 不可闻的 false 可能被当成暂停意图并下发暂停 | 保留缓冲及位置，设置任务不改变播放 |
| 自动切歌后点击暂停 | 错误的 false 可能反向请求播放 | 根据当前真实播放状态请求暂停，音量加载结束不恢复 |
| B 增益读取中又切到 C | 未完成准备使 C 事件被拒绝 | C 接管并取消 B 任务，迟到增益不能覆盖 C |
| 播放已启动，响度分析失败 | 按钮变成已暂停，声音仍播放 | 报告分析错误，按钮保持已有原生播放状态 |

## 平台与数据影响

共享状态和两端平台源码变更；Windows／Linux 继续使用同一桌面原生实现。无新增权限、网络、服务器或付费请求，不读取用户真实歌曲、Key 或签名私钥；不修改原音频、标签、曲库、收藏、歌单、播放快照格式或同步包。保留原音频转换、原子写入与恢复、配置写入顺序及播放请求竞争保护。

## 验证

本轮使用 Kotlin／Compose 2.3.20 CLI 完整编译当前源码及测试，执行相关 JUnit 回归，运行类路径排除旧应用 JAR。构建串行持有共享锁，整个进程树限制 2 GiB／MemoryHigh 1536 MiB／Swap 0，另保留宿主机 1536 MiB；不修改共享 Gradle 配置，不关闭用户应用。

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `python3 build/auto-next-state-2026-11-05/reproduce.py` | 修改前完整共享／JVM／桌面编译类 | 通过复现检查：断言按预期失败，实际输出 native=true、UI=false；首个夹具的 MIME 参数错误已纠正，最终只按目标断言确认复现。 |
| `python3 build/auto-next-state-2026-11-05/final_validation.py` | 当前共享／JVM／桌面及 Android 源码、测试 | 通过：桌面 186／Android 178 份生产源码、桌面 92／Android 29 份测试源码完整编译；7 类 49 项播放回归全部通过，0 失败／跳过。其中状态仓库 25 项，新增 10 项；验证前后源码哈希一致。 |
| `git -c core.whitespace=cr-at-eol diff --check` | 当前工作区 | 通过；新变更文档、入口及开发说明的 506 个本地 Markdown 链接核对通过。 |
| `adb devices -l` | 当前 Linux | 设备列表为空；Android 测试仅编译，未执行设备测试。 |

本次[机器摘要](../audit/2026-11-05/auto-next-playback-state/verification.json)、[逐项测试结果](../audit/2026-11-05/auto-next-playback-state/test-results.json)与[源码摘要](../audit/2026-11-05/auto-next-playback-state/source-sha256.json)已保存；本地编译和测试日志保留在上述忽略的 build 目录。

初轮桌面生产编译通过后发现“开播后响度分析失败”的同类路径，主动停止尚未完成的 Android 编译并补充修复／回归；最终结果以重新全量编译及源码哈希一致性检查为准，不复用首轮通过。

## 限制与后续

本次交付源码与本地回归，未打包、安装或发布。未在 Android 设备或物理 Windows 上验证实际自动切歌、系统通知／锁屏或音频焦点；不能把 JVM 状态测试及 Android 编译当作手机播放验收。用户已确认现象与本次回归方向一致；新包的实际自动切歌图标和暂停点击仍待连接手机验收。

## 同步更新的文档

- `docs/README.md`：新增本记录索引。
- `docs/DEVELOPMENT.md`：第 7 节播放状态附着语义和第 14 节任务定位表，核对日期 2026-11-05。
