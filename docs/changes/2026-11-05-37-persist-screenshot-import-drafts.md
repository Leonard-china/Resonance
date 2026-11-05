# 2026-11-05 — 截图歌单草稿、编辑及勾选跨进程保留

- 修改日期：2026-11-05（Asia/Shanghai）
- 类型：功能／Bug修复／性能与验证
- 适用版本：`0.6.6 / 40`，不修改`version.properties`，不升版
- 影响平台：Android／Windows／Linux，共享原生Kotlin／Compose
- 关联需求：用户要求全部实施五项优化，并修复歌词校准被遮挡和无法滚动；前置[一起听移除](2026-11-05-33-remove-listen-together.md)。

## 需求或问题

截图识别后的核对工作在关闭面板或应用重启后可以继续，避免重扫和重做编辑。

## 原因与依据

原ScreenshotImportDraft仅保存在StateFlow；列表编辑和选择只在Dialog remember里，关闭即discard。

## 具体修改

| 文件 | 关键符号 | 实际修改 |
| --- | --- | --- |
| [MusicFeatureModels.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/MusicFeatureModels.kt) | `ScreenshotImportDraft.selectedTracks` | 可选选中项集合，兼容旧草稿默认全选，空集合明确表示全不选。 |
| [MusicAssistantServices.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/MusicAssistantServices.kt) | `saveScreenshotImportDraft` | 增加本机草稿保存能力，由JVM实现共享。 |
| [ScreenshotImportDialog.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/ScreenshotImportDialog.kt) | `草稿快照／稍后继续` | 编辑与选择300ms去抖保存；稍后继续立即保存后隐藏，任务页提供继续入口；丢弃草稿显式删除，底部动作可随大字体换行。 |
| [MusicFeatureController.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureController.kt) | `hiddenScreenshotDraftId` | 只隐藏指定草稿，新识别结果不会被旧的隐藏状态抑制。 |

jvmMain/features/ScreenshotDraftCodec.kt负责有界JSON编码／解码与选择校验；JvmMusicAssistant在识别后落盘、restoreState恢复、确认前消费；MusicTasksScreen提供继续入口。

## 修改前后行为

新增截图草稿存于music-assistant.json根节点screenshotDraft，沿用单路径锁、原子替换和备份。确认导入或丢弃时先删除磁盘草稿，防止重启重复消费；导入后已存在的逐步骤重试机制不变。草稿只存核对文字，不保存截图或密钥。

## 平台与数据影响

保持曲库、收藏、歌单、统计ID、播放任务代次、顺序偏好写入及`.resonance`包格式。原音频不删除、不改标签；旧数据保留。新增的草稿或分析缓存字段兼容缺省旧记录。两端平台源码均检查并完整编译，Linux本机验证不代表Windows系统验收。无新服务器依赖，无新增付费请求，不读取真实Key或签名私钥。

## 验证

OfflineRecoveryTest验证新Store／runtime恢复编辑和空选择、重命名保存、丢弃后不能复活、非法草稿拒绝；原MusicAssistantTest的截图消费、缓存、下载与付费边界回归继续通过。突然强杀可能损失最近300ms尚未提交的输入，显式稍后继续会等待本次保存完成。

本次实际命令、最终计数、首轮失败与修正、资源限制和未验证范围集中见[本轮验收](../audit/2026-11-05/player-improvements/VALIDATION.md)。验证入口为本轮忽略目录`build/player-improvements-2026-11-05/final_validation.py`、`test_desktop.py`、`visual_tests.py`与`soak.py`，不是历史测试或旧安装包结果。

## 限制与后续

本轮只交付当前源码与本地验证，未替换用户已安装应用或公开发布；未升版。手机ADB未连接，Android测试仅编译，物理蓝牙／来电／锁屏／省电及Windows设备验收未完成。实际结果以统一验收记录为准。

## 同步更新的文档

`docs/README.md`索引、`docs/DEVELOPMENT.md`相应能力／UI／数据／测试章节、根README；歌词布局同时更新DESIGN.md。历史记录保留原验收事实。
