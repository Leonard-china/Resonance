# 2026-11-05 — 批量预览并重新定位失效音频路径

- 修改日期：2026-11-05（Asia/Shanghai）
- 类型：功能／Bug修复／性能与验证
- 适用版本：`0.6.6 / 40`，不修改`version.properties`，不升版
- 影响平台：Android／Windows／Linux，共享原生Kotlin／Compose
- 关联需求：用户要求全部实施五项优化，并修复歌词校准被遮挡和无法滚动；前置[一起听移除](2026-11-05-33-remove-listen-together.md)。

## 需求或问题

移动音乐文件夹、换硬盘后，以新目录批量恢复来源，同时保留收藏、歌单、统计和歌曲身份。

## 原因与依据

原平台仅有单曲chooseReplacementAudio；批量场景缺少目录扫描、歧义匹配、预览与提交前复核。

## 具体修改

| 文件 | 关键符号 | 实际修改 |
| --- | --- | --- |
| [AudioRelinkModels.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/AudioRelinkModels.kt) | `matchAudioRelinks` | 只建议唯一文件名匹配，支持Windows盘符／反斜杠及content URI编码文件名；一对多／多对一／版本不同不自动选择。 |
| [AudioRelinkSession.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/application/AudioRelinkSession.kt) | `missing / apply` | 只处理不可读条目；确认后重新加载当前元数据、复核原来源没变化和新来源可读，再保留ID更新sourceUri；逐首报告失败。 |
| [LibraryServices.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/LibraryServices.kt) | `chooseRelinkDirectory` | 新增可取消目录选择能力，两端分别实现。 |
| [AudioRelinkDialog.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/AudioRelinkDialog.kt) | `AudioRelinkEntry` | 智能管家增加入口；预览歌曲和相对路径、可逐项取消，确认后才保存，显示真实成功数及失败原因。 |

AndroidPlatformServices与DesktopPlatformServices实现目录选择与有界遍历，仍沿用各自saveReplacementAudio更新原索引。

## 修改前后行为

桌面使用文件夹选择和最多32层／50000项只读遍历，不跟随符号链接；Android使用SAF目录与持久读取权限，保留content URI，不转成本机路径。扫描不会导入、转换、移动、删除或写标签；未匹配记录原样保留。

## 平台与数据影响

保持曲库、收藏、歌单、统计ID、播放任务代次、顺序偏好写入及`.resonance`包格式。原音频不删除、不改标签；旧数据保留。本项不迁移数据格式。两端平台源码均检查并完整编译，Linux本机验证不代表Windows系统验收。无新服务器依赖，无新增付费请求，不读取真实Key或签名私钥。

## 验证

OfflineLibraryImprovementsTest验证唯一文件、同名歧义、共用候选和版本不匹配；AudioRelinkSessionTest验证最新元数据／收藏／ID保留，过期预览拒绝覆盖新来源；LibraryPersistenceTest继续验证重启和歌单关系。真实Android目录选择和Windows物理外置盘未验证；旧MediaStore来源若只剩数字ID、没有可推断文件名，会保留原条目，继续使用既有单曲重新定位，不猜测歌曲。

本次实际命令、最终计数、首轮失败与修正、资源限制和未验证范围集中见[本轮验收](../audit/2026-11-05/player-improvements/VALIDATION.md)。验证入口为本轮忽略目录`build/player-improvements-2026-11-05/final_validation.py`、`test_desktop.py`、`visual_tests.py`与`soak.py`，不是历史测试或旧安装包结果。

## 限制与后续

本轮只交付当前源码与本地验证，未替换用户已安装应用或公开发布；未升版。手机ADB未连接，Android测试仅编译，物理蓝牙／来电／锁屏／省电及Windows设备验收未完成。实际结果以统一验收记录为准。

## 同步更新的文档

`docs/README.md`索引、`docs/DEVELOPMENT.md`相应能力／UI／数据／测试章节、根README；歌词布局同时更新DESIGN.md。历史记录保留原验收事实。
