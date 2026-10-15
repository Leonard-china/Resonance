# 2026-10-15 — 改进补全匹配与持久化并移除缺失计数横幅

- 修改日期：2026-10-15（Asia/Shanghai）
- 类型：Bug 修复 / 体验
- 版本：0.3.5 / 22，未升版
- 平台：Android / Windows（平台差异见下文）
- 关联：用户本轮深度优化需求；[验证与交接](2026-10-15-12-verification-and-handoff.md)

## 需求、原因与依据

补全常常无效且歌曲栏目常驻缺失数量不美观；旧逻辑只缺专辑时可能不触发AI，公共源首项可能匹配错歌。

## 具体修改

- 修正只缺专辑条件、forceRefresh选项；iTunes/酷狗须标题/已知歌手匹配，未知歌手可检索，拒绝随意取首项。
- 每曲变化立即保存，取消不丢已完成；封面有大小/签名限制，完成重载和清位图缓存。
- 移除歌曲栏目常驻“补全缺失信息”条；结果仅出现在显式补全流程。
- 自动歌词不隐式付费，显式AI使用当前模型/预算；AI来源保存，未找到不当歌词，不伪造时间轴。

| 文件 | 关键修改 |
| --- | --- |
| [enrich/MetadataEnricher.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/enrich/MetadataEnricher.kt) | enrichTrack/batchEnrich/candidateMatches |
| [ai/DeepSeekClient.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/ai/DeepSeekClient.kt) | 显式兼容调用/歌词校验 |
| [lyrics/LrclibLyricsRepository.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/lyrics/LrclibLyricsRepository.kt) | 来源持久化 |
| [ui/LibraryShell.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt) | 移除常驻缺失条 |

## 修改前后

以前缺项持续显示且可能误补/未触发；现在显式补全、可靠匹配、逐首保存，没有常驻缺失消息。

## 平台与数据影响

公共来源/选中AI会联网；Windows原有编辑可能写标签，AI元数据工具是曲库覆盖。新歌词缓存保留真实source。

## 验证

MetadataEnricherTest、歌词缓存/文本解析测试；两端全套回归。

实际命令、结果与设备/网络限制统一记录在 [本轮验证](2026-10-15-12-verification-and-handoff.md)，不把模拟服务测试当作付费API或B站真实下载通过。

## 限制与后续

公共接口不保证全覆盖，AI结果不能当事实核验；本轮未用真实付费Key验补全。

## 同步文档

[开发说明](../DEVELOPMENT.md)、[索引](../README.md)、[项目说明](../../README.md)；外部请求/凭据见 [第三方说明](../THIRD_PARTY.md)。
