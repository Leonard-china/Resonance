# 2026-10-17 — 截图歌单先核对勾选和编辑后再导入

- 修改日期：2026-10-17（Asia/Shanghai）
- 类型：功能 / 体验修复
- 适用版本：0.4.9 / 32 工作区；本轮不升版、不发布
- 影响平台：共享 / Android / Windows（平台限定见下文）
- 关联需求：用户要求落地全部体验建议，并追加页面顶部黑边闪烁反馈；[完整交接与实际验证](../HANDOFF-2026-10-17-UX-ANTIGRAVITY.md)

## 需求或问题

截图识别成功后立即创建歌单并逐首匹配、下载，错误信息只能事后处理；用户不能在下载前删去识别错的条目。

## 原因与依据

importScreenshots 在 OCR 或云端识图返回后直接执行 obtain 和 addToPlaylist，识别与有副作用的入库没有用户可见的分界。

## 具体修改

| 文件 | 关键符号 | 修改与目的 |
| --- | --- | --- |
| [MusicFeatureModels.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/MusicFeatureModels.kt) | `ScreenshotImportDraft / ScreenshotPlaylistDraft` | 增加仅内存持有的待核对草稿，包含识别来源与歌单、歌曲信息。 |
| [JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | `importScreenshots / confirmScreenshotImport / discardScreenshotImport` | 识别阶段只生成草稿；确认校验草稿 ID、字段和数量，再按选中信息导入；消费一次防重复提交。 |
| [ScreenshotImportDialog.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/ScreenshotImportDialog.kt) | `ScreenshotImportDialog` | 提供全选／全不选、歌单名、歌名、歌手和专辑编辑，提示本地记录／需要下载／截断名称。 |
| [MusicFeatureScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureScreens.kt) | `MusicFeatureDialogs` | 全局显示待核对结果；教程与 AssistantHelp 同步说明本地 OCR 和确认步骤。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 本次使用流程 | 发送截图后直接创建歌单、匹配或下载。 | 发送截图先生成可编辑预览；取消不创建歌单、不下载，确认后只处理选中条目。 |

## 平台与数据影响

OCR 缓存与云端计费保持原有语义；本地 OCR 不上传图片，也不会自动回退付费 API。草稿不落盘、不进入同步包，重启后需重新识别，但 OCR 缓存可复用。确认后的逐首操作进入持久任务，可对失败歌曲重试；已完成歌曲保留。数量仍限制 20 个歌单、每单 300 首。

## 验证

MusicAssistantTest 更新缓存／本地 OCR 测试，并新增未确认不下载、编辑与取消、过期 ID、重复提交的回归；双端 UI 编译通过。

| 本次检查 | 结果 |
| --- | --- |
| 定点回归与编译 | 本轮相关回归合计 75 项不同测试通过（失败 0、跳过 0）；双端源码及 Android 测试源码通过编译。完整命令与执行边界见交接文档。 |
| 真机、正式包与网站 | 本轮未执行；交 Antigravity 完成最后验收、正式构建及部署。 |

## 限制与后续

本地有记录的提示不等于音频一定可读，导入时进一步检查真实文件。OCR 本身不保证识别正确，复杂排版仍需用户核对。

## 同步更新的文档

- [文档索引](../README.md)
- [开发说明第 18 节及对应原章节](../DEVELOPMENT.md#user-experience-update)
- [交接与构建部署提示词](../HANDOFF-2026-10-17-UX-ANTIGRAVITY.md)

