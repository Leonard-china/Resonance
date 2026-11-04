# 2026-11-03 — 为音乐助手回复加入可点播歌曲与追加队列

- 修改日期：2026-11-03（Asia/Shanghai）；2026-11-04 完成本地交付复核
- 类型：功能
- 适用版本：0.6.5 / 39，未升版
- 影响平台：Android、Windows、Linux
- 关联：用户确认的助手歌曲链接功能；[本轮布局](2026-11-03-11-macos-layout-and-navigation-cost.md)

## 需求或问题

助手过去仅展示文字歌名和管理操作，无法直接从回复中的歌曲条目点播或一次加入队列。用户看过设计稿后明确要求加入此功能。

## 原因与依据

工具已有真实歌曲 ID，但回复 UI 没有明确关联当前库条目的协议。按歌名模糊匹配会把同名歌曲或模型虚构名称误接到真实播放操作，因此采用显式 ID 链接，并只从当前曲库解析。

## 具体修改

| 文件 | 关键符号 | 修改与目的 |
| --- | --- | --- |
| [AssistantMusicLinks.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/AssistantMusicLinks.kt) | assistantTrackLink、resolveAssistantMusic、appendAssistantQueue | UTF-8 编码歌曲 ID；解析 Markdown `resonance://track/` 链接，按回复顺序去重，最多 20 首；外链和纯歌名不转为播放操作；追加仅有音源条目 |
| [JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | publicTrack、SYSTEM | 工具返回 playLink，仍剔除本地 source/artwork 路径；提示模型原样引用真实链接，不伪造未下载歌曲链接 |
| [AssistantMusicAttachment.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/AssistantMusicAttachment.kt) | AssistantMusicAttachment | 原生封面、歌名、歌手和点播按钮，底部加入队列；长歌名最多两行、歌手一行 |
| [MusicFeatureScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureScreens.kt) | AssistantScreen | 助手回复解析链接，以当前库元数据展示附件；点击经 playRecommendation 复用播放/恢复和防竞争入口 |
| [MusicFeatureController.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureController.kt)、[App.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | onAppendQueue | 保持现有队列顺序与当前播放，按 ID 去重追加；无当前播放时从空队列开始；一起听中提示通过房间队列添加 |
| [AssistantMusicLinksTest.kt](../../composeApp/src/commonTest/kotlin/com/resonance/player/AssistantMusicLinksTest.kt)、[MusicAssistantTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/MusicAssistantTest.kt)、[UiVisualContractTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/UiVisualContractTest.kt) | 解析/工具/原生交互回归 | 编码、未知和坏 ID、去重和上限、保序追加、真实工具结果与文件路径剔除、无点击不执行 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 模型引用已入库歌曲 | 文字 | 真实库歌曲附件，可点播/加入队列 |
| 回复中出现同名歌或外链 | 文字 | 仍为文字，不猜测 ID、不拉取任意网络音频 |
| 老消息的歌已删除 | 无附件 | 显示链接标签文本，不生成无效按钮 |
| 加入队列 | 无入口 | 追加而不自动开播，不重复已有 ID |

## 平台与数据影响

仍使用 AssistantMessage.content 原字符串，无数据库/JSON 格式迁移；历史含有效链接的消息可解析，其他历史照常展示。不注册系统 deep link、不接收外部 Intent、不增加权限、服务、外部请求或依赖。链接本身不授权下载或付费模型调用。用户明确点击后才执行本地播放/追加；曲库、音频文件和原有删除权限不变。

## 验证

`desktopTest --tests com.resonance.player.AssistantMusicLinksTest` 4项、`--tests com.resonance.player.features.MusicAssistantTest.libraryToolReturnsPlayableLinksWithoutExposingFileLocations` 1项、`--tests com.resonance.player.ui.UiVisualContractTest.assistantAttachmentsOnlyActOnExplicitClicks` 1项全部通过。与布局/歌词回归合计18项；11月4日复跑仍通过，完整命令见[布局记录](2026-11-03-11-macos-layout-and-navigation-cost.md)。

工具测试执行真实 JvmMusicAssistant 搜库→模型模拟回复→当前库解析链路，确认 playLink 可还原且不泄露文件位置；原生附件验证未点击不触发、点击指定歌曲和追加按钮传递正确对象。两主题附件截图使用可播放语义的合成音源URI（不实际读音频）。全部用临时库和模拟模型响应，不访问用户 API Key、不产生付费调用。

Android Release/Debug/测试APK构建通过，签名通过。最终包设备复测依用户11月4日要求延期；没有把已编译的仪器测试写成执行通过。

## 限制与后续

模型必须返回真实 playLink 才生成附件；不保证所有供应商都遵循格式。未入库歌曲不会被伪装为已可播放；只剩元数据的条目点击沿用现有恢复流程，缺少音源的条目不加入队列。本轮不自动发送用户真实聊天验证付费模型。

## 同步更新的文档

`docs/README.md`、`docs/DEVELOPMENT.md` 助手协议与测试定位、`DESIGN.md` 助手附件规则。
