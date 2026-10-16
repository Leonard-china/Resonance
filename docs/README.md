# 2026-10-16 — Resonance 文档入口与开发变更索引

当前工作区：**0.4.1 / 24** 的 2026-10-16 正式发布构建。[本次发布说明](RELEASE_NOTES-0.4.1.md) · [本次发布记录](changes/2026-10-16-10-release-0.4.1.md)。GitHub 最新 Release 见 [v0.4.1 Release](https://github.com/Leonard-china/Resonance/releases/tag/v0.4.1)。功能回归见 [本轮交接](changes/2026-10-15-12-verification-and-handoff.md)。

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
| [0.4.1 发布说明](RELEASE_NOTES-0.4.1.md) | v0.4.1 正式版更新重点、安装包校验与升级指引 |
| [0.4.0 发布说明](RELEASE_NOTES-0.4.0.md) | 历史 0.4.0 Android 正式版记录 |

## 变更记录（最新在前）

| 修改日期 | 主要修改 | 详细记录 |
| --- | --- | --- |
| 2026-10-16 | 升版 0.4.1、构建签名 Android Release APK 与 Windows 安装包并交付 GitHub | [具体修改](changes/2026-10-16-10-release-0.4.1.md) |
| 2026-10-16 | AI助手多会话历史、模型溯源、桌面歌词权限引导与独立关于页面 | [具体修改](changes/2026-10-16-09-assistant-history-and-ui-polish.md) |
| 2026-10-16 | 本轮优化两端发布版构建与验证 | [具体修改](changes/2026-10-16-08-release-build-verification.md) |
| 2026-10-16 | 修复静止歌词并添加按音频时间戳生成同步歌词 | [具体修改](changes/2026-10-16-07-audio-synchronized-lyrics.md) |
| 2026-10-16 | 新增持久任务列表和按失败步骤重试 | [具体修改](changes/2026-10-16-06-durable-music-tasks.md) |
| 2026-10-16 | 为下载歌曲匹配真实专辑封面并缓存及保留视频兜底 | [具体修改](changes/2026-10-16-05-album-artwork.md) |
| 2026-10-16 | 在歌单页固定每日推荐并提供独立推荐页面 | [具体修改](changes/2026-10-16-04-pinned-recommendations.md) |
| 2026-10-16 | 音乐助手进入主导航并隐藏聊天页迷你播放器 | [具体修改](changes/2026-10-16-03-assistant-navigation.md) |
| 2026-10-16 | 合并完整播放器过渡并降低动态背景重组开销 | [具体修改](changes/2026-10-16-02-player-layer-transition.md) |
| 2026-10-16 | 恢复曲库切换动画并保留各栏目的浏览位置 | [具体修改](changes/2026-10-16-01-library-motion-and-scroll.md) |
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
