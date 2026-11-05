# 2026-11-05 — 语义搜索明确BPM尚未参与筛选

- 修改日期：2026-11-05（Asia/Shanghai）
- 类型：Bug 修复
- 适用版本：0.6.6 / 40（version.properties；不升版）
- 影响平台：Android / Windows / Linux（按下文平台边界）
- 关联需求：用户要求修复本次全面测试问题；[原始缺陷](../audit/2026-11-05/BUGS.md)、[原始手机结果](../audit/2026-11-05/ANDROID-DEVICE-REPORT.md)，编号Q07。

## 需求或问题

140 BPM能显示意图标签，但本地搜索没有节拍数据管线，容易误认为已按真实节拍筛选。

## 原因与依据

解析bpmRange与真实音频特征检索是两个能力。按审计验收的“明确仅展示识别意图”选项修复误导，不添加虚构的节拍匹配。

## 具体修改

| 文件 | 关键符号 | 实际修改 |
| --- | --- | --- |
| [ui/SemanticSongSearch.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/SemanticSongSearch.kt) | 查询示例 / intent.bpmRange提示 | 移除BPM筛选示例；解析出范围时明确仅按歌名/歌手/专辑等文字匹配，尚不支持BPM筛选，并提示补文字关键词。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| Q07所述输入/操作 | 标签像是筛选已生效 | 范围旁明确说明不支持节拍筛选，不据此宣称音频能力 |

## 平台与数据影响

共享UI，无音频重新分析、数据迁移或网络费用。现有关键词、场景/情绪/曲风文字匹配保留。

## 验证

本次相关用例：SemanticSearchIntentParserTest与共享UI完整编译。执行命令、独立数据边界、逐用例结果、失败后的定点复测及计数统一见[本次修复验收](../audit/2026-11-05/FIX-VALIDATION.md)。该报告保留初次失败记录，历史修复前通过数不作为本次通过依据。

| 执行入口 | 本次结果 |
| --- | --- |
| JUnitCore或Android instrumentation：SemanticSearchIntentParserTest | 3通过、0失败、0条件跳过；逐用例见验收报告JSON |
| 完整桌面/Android源码与测试编译 | 通过；202/195份主源码，100/34份测试源码，版本不变 |

## 限制与后续

真实BPM分析/索引/筛选仍未实现，这是明确能力边界的修复。

## 同步更新的文档

- [开发说明](../DEVELOPMENT.md)：相应导入、持久化、AI、UI/测试或官网章节，核对日2026-11-05。
- [文档索引](../README.md)：独立记录与修复验收入口。
