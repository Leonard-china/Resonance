# 2026-10-15 — 新增多模型音乐 Agent 与长截图歌单整理

- 修改日期：2026-10-15（Asia/Shanghai）
- 类型：功能
- 版本：0.3.5 / 22，未升版
- 平台：Android / Windows（平台差异见下文）
- 关联：用户本轮深度优化需求；[验证与交接](2026-10-15-12-verification-and-handoff.md)

## 需求、原因与依据

用户希望 AI 真正管理音乐、支持其他国产 API、理解其他软件歌单长截图，并减少重复读库与接口费用。

## 具体修改

- 新增统一 MusicAiClient 与可注入传输：千问、DeepSeek、豆包、自定义 HTTPS，文本/视觉 provider 分开选择。
- 实现真实音乐工具：曲库/歌单检索、创建/改名/删除歌单、成员/收藏/元数据、找源下载、明确请求的音频删除。
- 不开放任意电脑/shell；无效ID/未知工具报错，截图和工具内容不授权删除，否定删除及只删歌单保护。
- 两端长图区域解码/重叠分片，视觉 OCR JSON 后本地优先匹配、自动下载入歌单，逐曲持久化/错误明细。
- 固定排序上下文前缀、短历史、本地 OCR/译文缓存、月 Token 预算/输入缓存输出计数；新凭据 DPAPI/AndroidKeyStore 加密。

| 文件 | 关键修改 |
| --- | --- |
| [features/JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | chat/execute/tools/importScreenshots/ask |
| [ai/MusicAiClient.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/ai/MusicAiClient.kt) | Chat Completions/图片/tools/用量 |
| [features/MusicJsonStore.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/MusicJsonStore.kt) | 原子状态/缓存 |
| [ui/MusicFeatureScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureScreens.kt) | AiConnectionEditor/AssistantScreen |

## 修改前后

以前主要 DeepSeek 文本兜底；现在聊天可实际执行音乐任务，视觉服务识别长截图并直接导入。

## 平台与数据影响

截图/歌词/曲库摘要发选中服务，音频不整库上传；新密钥加密，旧明文兼容保留。状态/缓存两端持久。

## 验证

MusicAssistantTest 验 OCR缓存、下载去重、权限、预算、稳定前缀和歌单工具；MusicJsonStoreTest 验备份恢复；Android 真分片/密钥回归。

实际命令、结果与设备/网络限制统一记录在 [本轮验证](2026-10-15-12-verification-and-handoff.md)，不把模拟服务测试当作付费API或B站真实下载通过。

## 限制与后续

未提供真实 Key，不做付费服务实测；供应商缓存时效/账单不由本地保证；自定义模型须支持对应视觉/工具消息。

## 同步文档

[开发说明](../DEVELOPMENT.md)、[索引](../README.md)、[项目说明](../../README.md)；外部请求/凭据见 [第三方说明](../THIRD_PARTY.md)。
