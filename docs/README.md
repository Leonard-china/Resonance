# 2026-10-15 — Resonance 文档入口与开发变更索引

当前开发基线：版本 **0.4.0 / 23**，发布标记 `v0.4.0`，功能改造基于 `cdf7ee3`。Android 正式 APK 见 [v0.4.0 Release](https://github.com/Leonard-china/Resonance/releases/tag/v0.4.0)。功能回归见 [本轮交接](changes/2026-10-15-12-verification-and-handoff.md)，本次发布验证见 [发布记录](changes/2026-10-15-13-release-0.4.0-android.md)。

## 新 AI 从这里开始

1. 读根目录 [AGENTS.md](../AGENTS.md) 与 [AI 协作流程](AI-WORKFLOW.md)。
2. 根据 [开发说明的任务定位表](DEVELOPMENT.md#gaps) 找相关章节、实现、测试。
3. 阅读影响本任务的下表记录；文档不足时才扩大源码范围。
4. 完成每个独立功能/修复后新增带日期/具体修改的记录，并更新本索引和开发说明。

| 文档 | 用途 |
| --- | --- |
| [详细开发说明](DEVELOPMENT.md) | 当前模块/状态/存储、两端功能、AI工具、截图、推荐清理、翻译/悬浮、构建/排查 |
| [AI 协作流程](AI-WORKFLOW.md) | 最小上下文、实施顺序、文档/验证/交接规则 |
| [变更模板](CHANGE-TEMPLATE.md) | 日期、原因、具体修改、前后/数据、真实验证、限制 |
| [组件/外部服务/凭据](THIRD_PARTY.md) | 新模型、B站、WorkManager/JNA、请求内容与加密/兼容 |
| [项目使用说明](../README.md) | 用户入口与设置/推荐删除语义 |
| [0.3.5 发布说明入口](RELEASE_NOTES-0.3.5.md) | 发布脚本约定与产物核验，开发记录不等同发布 |
| [0.4.0 发布说明](RELEASE_NOTES-0.4.0.md) | Android 正式版的新功能、兼容影响、签名与实际校验值 |

## 变更记录（最新在前）

| 修改日期 | 主要修改 | 详细记录 |
| --- | --- | --- |
| 2026-10-15 | 升版 0.4.0、构建签名 Release APK 并交付 GitHub | [具体修改](changes/2026-10-15-13-release-0.4.0-android.md) |
| 2026-10-15 | 深度优化的构建/设备回归、文档和交接 | [具体修改](changes/2026-10-15-12-verification-and-handoff.md) |
| 2026-10-15 | 改进补全匹配与持久化并移除缺失计数横幅 | [具体修改](changes/2026-10-15-11-improve-metadata-enrichment.md) |
| 2026-10-15 | 新增双平台后台悬浮歌词和任意目标语言翻译 | [具体修改](changes/2026-10-15-10-floating-lyrics-and-translation.md) |
| 2026-10-15 | 新增定时推荐、自动下载和临时歌曲清理 | [具体修改](changes/2026-10-15-09-ai-recommendation-lifecycle.md) |
| 2026-10-15 | 新增多模型音乐 Agent 与长截图歌单整理 | [具体修改](changes/2026-10-15-08-multi-model-music-agent.md) |
| 2026-10-15 | 新增 B 站音频导入及自定义歌曲信息 | [具体修改](changes/2026-10-15-07-bilibili-audio-import.md) |
| 2026-10-15 | 消除歌单与歌曲标签重叠绘制造成的撕裂 | [具体修改](changes/2026-10-15-06-smooth-library-tab-switching.md) |
| 2026-10-15 | 修复列表封面回退并限制大图内存 | [具体修改](changes/2026-10-15-05-fix-artwork-fallback.md) |
| 2026-10-15 | 修复目录转换无进度、取消与长时间等待 | [具体修改](changes/2026-10-15-04-conversion-progress-and-cancel.md) |
| 2026-10-15 | 移除设备同步和酷狗歌单链接，保留歌单文件 | [具体修改](changes/2026-10-15-03-remove-device-sync-and-kugou-links.md) |
| 2026-10-15 | 简化自动音量平衡并独立提供使用说明 | [具体修改](changes/2026-10-15-02-simplify-volume-balance.md) |
| 2026-10-15 | 重建开发说明与 AI 协作规则 | [具体修改](changes/2026-10-15-01-rebuild-development-docs.md) |

同一天编号递增，每个独立任务单独留记录；不要覆盖历史文档或只在 README 追加一句“已优化”。

## 旧文档处理

用户要求重建 docs，原有发布日志、提案、报告、截图已移出；可恢复本机备份为忽略目录 build/docs-before-rewrite-2026-10-15/，已提交历史还可查 Git。它们不是当前开发依据。当前能力以开发说明及本次记录为准，特别是已移除的 LAN/二维码/酷狗分享链接和新增 AI/推荐/悬浮歌词。
