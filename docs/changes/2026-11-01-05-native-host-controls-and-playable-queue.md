# 2026-11-01 — 将协调端控制与共享队列接入原生播放作用域

- 修改日期：2026-11-01（Asia/Shanghai）
- 类型：多人播放控制与队列修复；版本 0.6.5 / 39，未升版
- 影响平台：共享状态/驱动/UI、Android Media3、Windows PCM
- 关联：[基础控制接线](2026-10-31-31-route-room-playback-controls-and-queue.md)、[贡献音频](2026-10-31-32-play-contributed-room-audio.md)

## 需求或问题

房间队列的删除/移动此前仅改变协调器快照，没有更新原生播放队列；贡献曲完成后又依赖 Compose 动作收集器入队。页面收集器不存在时，基础控制也无法执行。刷新本地曲库会剔除临时贡献曲，显式空队列还会被重新解析为完整曲库。

## 原因与依据

`handleMemberWire.action` 修改 `TogetherGroupCoordinator`，仅对基础控制发送 `approvedActions`，且该流由 `App` 收集；Android Media3 时间线和 Windows 结束后换曲未与房间队列统一。`publishQueue` 把主机曲目固定放到贡献曲前面，也可能撤销跨来源排序。

## 具体修改

| 文件 | 关键符号 | 修改 |
| --- | --- | --- |
| [ListenTogetherService.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/ListenTogetherService.kt) | `TogetherHostQueue`、`hostQueue`、`hostPlayback` | 仅在本机传递的房间/修订号、可播放队列及原生状态；源路径不进入网络快照 |
| [TogetherHostPlaybackDriver.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/playback/TogetherHostPlaybackDriver.kt) | 新驱动 | 原生生命周期收集控制与队列，互斥执行并检查房间；Windows 结束后换曲遵循随机/循环设置 |
| [TogetherGroupCoordinator.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/TogetherGroupCoordinator.kt) | `syncPlayableQueue` | 保留贡献身份并同步混合可播放队列顺序，未完成上传的条目单独保留 |
| [JvmListenTogetherService.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/JvmListenTogetherService.kt) | 队列发布、`applyQueueAction`、贡献接收 | 修改及贡献完整校验后发布可恢复快照；投影确认防旧界面立即覆盖；可播放曲目才允许点歌；队列请求 UUID/递增序号去重；最多 50 首时保留当前候选曲 |
| [AndroidTogetherRuntime.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/together/AndroidTogetherRuntime.kt) | 原生绑定、`reconcileHostQueue` | 增删/移动 MediaItem 调整时间线，尽量保留现有曲目、位置、播放意图，仅删除当前曲时选替代曲 |
| [DesktopPlatformServices.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopPlatformServices.kt)、[DesktopPcmPlayer.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/audio/DesktopPcmPlayer.kt) | 原生驱动、结束回调、`playWhenReady` | 后台控制与自动换曲；纯队列排序不重启当前解码；删除当前曲保留播放意图 |
| [App.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | 队列/原生状态收集、删除/移动、空队列 | 原生管理时不重复消费控制/结束事件；界面读取可恢复投影；曲库刷新保留活动房间临时曲；空队列区别于未指定队列 |
| [TogetherScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TogetherScreen.kt) | 共享队列列表 | 所有新协议成员均可上移/下移/移除；传输中不能点歌 |

## 修改前后行为

网络命令序号按已验证连接去重，再分配协调器内部单调序号，避免对方进程重启计数从 1 开始后永久被旧设备水位拒绝；旧连接不会被当新连接处理。Windows 结束回调也捕获结束曲目，若执行前已换歌则不再推进。

投影同步时剔除已移出房间队列的目录项，避免旧贡献占满 50 项内存目录后，新接收歌曲虽已写完却无法加入可播放映射。其已登记缓存仍受原有 512 MiB 账目及离房清理管理，不据此宣称已即时回收磁盘。

源码设计中，房间删除/移动同时改变协调器、原生队列与界面，基础控制不再要求 Compose 页面存在。Android 由 Media3 时间线自动换曲，Windows 由原生驱动推进；UI 不重复推进。未下载贡献曲可见但不能选中，完整校验后加入可播放投影。

## 平台与数据影响

不修改曲库、原始音频、许可证或版本；清理仍限服务自有缓存。新增投影仅在内存，不写恢复快照或网络。队列项增加默认值 `playable` 字段，新实验群协议统一为 `groupVersion=2`，旧单对端兼容单独保留但未验证。Media3 保持既有 1.10.1，保留音频焦点、becoming-noisy、MediaSessionService 和睡眠定时。

## 验证

仅阅读共享接口、两端实现、界面收集器和既有播放/同步测试入口；用户要求不构建、不运行测试，没有编译或设备通过结论。

对涉及的已跟踪源码和现行文档执行 `git diff --check -- <对应路径>`，退出码 0，未报告空白错误；仅有 Git 行尾转换提示。此检查不验证 Kotlin 编译、未跟踪文件或运行行为。

## 限制与后续

不提供协调设备迁移，它离开后原房间结束；后台控制仍需进程及原生服务存活。队列投影反馈与本地同时换歌、慢传输、音量任务竞争、删除当前曲、睡眠交互及 50 首截取需测试。缺少可解析时长的歌曲仍不共享。源代码中缓存和发送公平性须继续审查，不把已编写当作运行通过。

## 同步更新的文档

文档索引、开发说明、继续检查点和 Antigravity 验收交接。
