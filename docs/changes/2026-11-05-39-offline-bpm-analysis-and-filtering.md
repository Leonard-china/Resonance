# 2026-11-05 — 本地BPM分析、来源校验缓存与真实节拍筛选

- 修改日期：2026-11-05（Asia/Shanghai）
- 类型：功能／Bug修复／性能与验证
- 适用版本：`0.6.6 / 40`，不修改`version.properties`，不升版
- 影响平台：Android／Windows／Linux，共享原生Kotlin／Compose
- 关联需求：用户要求全部实施五项优化，并修复歌词校准被遮挡和无法滚动；前置[一起听移除](2026-11-05-33-remove-listen-together.md)。

## 需求或问题

把已有节拍分析算法接入三端生产音频与搜索，支持120—140 BPM等本地范围查询，无需房间服务器或付费模型。

## 原因与依据

原AudioFeatureAnalyzer和AudioFeatureStore没有生产解码／搜索调用点；意图只解析标签，不能按节拍筛选，短音频还会返回虚构的默认120 BPM。

## 具体修改

| 文件 | 关键符号 | 实际修改 |
| --- | --- | --- |
| [SemanticSearchIntentParser.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/SemanticSearchIntentParser.kt) | `parse` | 识别连字符／破折号／至／到等显式BPM区间，保留单值和场景推断。 |
| [SemanticSearchModels.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/SemanticSearchModels.kt) | `matchLocalSemanticTracks` | BPM条件实际排除未分析／不明确／区间外曲目；文字场景等仍按元数据匹配，不声称模型理解音频情绪。 |
| [MusicAssistantServices.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/MusicAssistantServices.kt) | `analyzeAudioFeatures / refreshAudioFeatures` | 共享分析与缓存校验入口，JVM运行时记录任务、进度、取消与逐首错误。 |
| [SemanticSongSearch.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/SemanticSongSearch.kt) | `本地分析入口／进度与取消` | 点击后才逐首分析前90秒；展示本地估算与未知项边界，任务可取消；意图标签改FlowRow避免溢出。 |

jvmMain/audio/AudioFeatureAnalyzer.kt、AudioFeatureStore.kt，features/JvmMusicAssistant.kt、MusicJsonStore.kt、MusicLibraryAccess.kt；两端features/AndroidMusicAccess.kt、DesktopMusicAccess.kt提供revision与prepareAnalysisAudio；LibraryShell将“AI语义”改为“意图找歌”。

## 修改前后行为

Android FFmpegKit和Windows／Linux FFmpeg输出16kHz单声道PCM，单首最多90秒／2.88MB，单线程解码，可取消并清理临时文件；不上传音频，不调用模型。缓存audio-features.json新增revision，按来源／大小／修改时间校验；旧无revision缓存重新分析。MusicJsonStore增加固定白名单文件名，复用锁、原子替换、有效备份恢复，写失败向上传播；静音或过短输入BPM为0不纳入结果。

## 平台与数据影响

保持曲库、收藏、歌单、统计ID、播放任务代次、顺序偏好写入及`.resonance`包格式。原音频不删除、不改标签；旧数据保留。新增的草稿或分析缓存字段兼容缺省旧记录。两端平台源码均检查并完整编译，Linux本机验证不代表Windows系统验收。无新服务器依赖，无新增付费请求，不读取真实Key或签名私钥。

## 验证

OfflineLibraryImprovementsTest验证真实区间及未知／区间外排除；OfflineRecoveryTest验证缓存复用、修改后重算、无伪造静音节拍、临时文件清理和损坏主文件的备份恢复。DesktopAnalysisDecodeTest使用91秒原创合成120BPM WAV，真实FFmpeg输出严格90秒且算法检测115—125BPM，原文件保留。Android解码只完整编译，设备未连接；估算可能存在半拍／倍拍及前90秒不代表整曲的误差。

本次实际命令、最终计数、首轮失败与修正、资源限制和未验证范围集中见[本轮验收](../audit/2026-11-05/player-improvements/VALIDATION.md)。验证入口为本轮忽略目录`build/player-improvements-2026-11-05/final_validation.py`、`test_desktop.py`、`visual_tests.py`与`soak.py`，不是历史测试或旧安装包结果。

## 限制与后续

本轮只交付当前源码与本地验证，未替换用户已安装应用或公开发布；未升版。手机ADB未连接，Android测试仅编译，物理蓝牙／来电／锁屏／省电及Windows设备验收未完成。实际结果以统一验收记录为准。

## 同步更新的文档

`docs/README.md`索引、`docs/DEVELOPMENT.md`相应能力／UI／数据／测试章节、根README；歌词布局同时更新DESIGN.md。历史记录保留原验收事实。
