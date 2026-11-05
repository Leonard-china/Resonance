# 2026-11-05 — 减少暂停及悬浮歌词后台开销并索引歌词时间戳

- 修改日期：2026-11-05（Asia/Shanghai）
- 类型：功能／Bug修复／性能与验证
- 适用版本：`0.6.6 / 40`，不修改`version.properties`，不升版
- 影响平台：Android／Windows／Linux，共享原生Kotlin／Compose
- 关联需求：用户要求全部实施五项优化，并修复歌词校准被遮挡和无法滚动；前置[一起听移除](2026-11-05-33-remove-listen-together.md)。

## 需求或问题

落实流畅度、耗电与内存优化，保留播放快照恢复和歌词跟随行为。

## 原因与依据

App 随 player 变化不断取消／创建浮窗更新协程；Android 悬浮歌词关闭后仍每250ms轮询。App 与 Android PlaybackService 重复保存播放快照；桌面暂停每50ms唤醒。歌词当前行每次进度更新均线性扫描。

## 具体修改

| 文件 | 关键符号 | 实际修改 |
| --- | --- | --- |
| [App.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | `浮窗 SideEffect / 快照 snapshotFlow` | 浮窗仅启用时发布或在关闭时发布一次，避免逐帧协程重启；快照队列ID只在队列变化时生成，桌面仅状态变化时保存且合并2秒内更新，静止时不轮询。 |
| [PlaybackServices.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/PlaybackServices.kt) | `persistsNativePlaybackSnapshot` | Android明确由原生服务持久化快照，App不重复定时保存；初次切歌的顺序写入继续保留。 |
| [LyricTimeline.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/LyricTimeline.kt) | `activeIndex` | 歌词实例创建一次时间戳索引，单调时间轴二分定位；重复时间、空项和非单调输入保留兼容回退。 |
| [NowPlayingOverlay.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | `LyricsContent` | 使用记忆的时间戳索引，懒列表增加内容类型以复用歌词行。 |

AndroidPlatformServices.persistsNativePlaybackSnapshot、AndroidLyricsOverlay的collectLatest启停，以及DesktopPcmPlayer的通知等待。

## 修改前后行为

暂停的桌面解码线程等待命令通知；Android关闭悬浮歌词时不再维持250ms定时器；歌曲和进度仍由原生服务驱动。

## 平台与数据影响

保持曲库、收藏、歌单、统计ID、播放任务代次、顺序偏好写入及`.resonance`包格式。原音频不删除、不改标签；旧数据保留。本项不迁移数据格式。两端平台源码均检查并完整编译，Linux本机验证不代表Windows系统验收。无新服务器依赖，无新增付费请求，不读取真实Key或签名私钥。

## 验证

LyricTimelineBenchmarkTest对2000行／20000次查询作同进程5轮中位数对照并校验结果；原线性60.831ms，索引1.049ms，约58.0倍，指查询耗时，不是整页FPS。真实Release帧率、功耗和Android Perfetto因手机未连接未测；不能将本机离屏检查当成正式包性能验收。

本次实际命令、最终计数、首轮失败与修正、资源限制和未验证范围集中见[本轮验收](../audit/2026-11-05/player-improvements/VALIDATION.md)。验证入口为本轮忽略目录`build/player-improvements-2026-11-05/final_validation.py`、`test_desktop.py`、`visual_tests.py`与`soak.py`，不是历史测试或旧安装包结果。

## 限制与后续

本轮只交付当前源码与本地验证，未替换用户已安装应用或公开发布；未升版。手机ADB未连接，Android测试仅编译，物理蓝牙／来电／锁屏／省电及Windows设备验收未完成。实际结果以统一验收记录为准。

## 同步更新的文档

`docs/README.md`索引、`docs/DEVELOPMENT.md`相应能力／UI／数据／测试章节、根README；歌词布局同时更新DESIGN.md。历史记录保留原验收事实。
