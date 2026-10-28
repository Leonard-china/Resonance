# 2026-10-26 — 回退全屏播放页波浪进度条与UI重构改动并恢复至v0.6.0基线

- 修改日期：2026-10-26（Asia/Shanghai）
- 类型：重构 / 回退（Rollback）
- 适用版本：`0.6.0 / 34`（回退至上一稳定版本）
- 影响平台：共享（Compose Multiplatform）/ Android / Windows
- 关联记录或需求：[全屏播放页温润重构记录](2026-10-26-03-wavy-progress-and-now-playing-refactor.md)、[v0.6.0 发布说明](../RELEASE_NOTES-0.6.0.md)

## 需求或问题

用户在实际体验 v0.6.1 的全屏播放页与波浪进度条后反馈视觉与交互效果未达预期（“讲实话，效果并不好，退回上一版”），要求完整回退本轮对全屏播放页（Now Playing Overlay）及相关组件的修改，恢复到上一稳定版本（v0.6.0 / 34）。

## 原因与依据

1. 尊重用户对产品视觉设计与交互体验的明确决策与反馈。
2. 彻底清理实验性波浪进度条及配套的触觉控件修改，确保工作区代码、版本配置与单元测试完全恢复至 v0.6.0 稳定基线状态。
3. 清理未被采纳的 `0.6.1` 发布说明与产物包，重新对齐 `release/0.6.0/` 官方产物。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| [`NowPlayingOverlay.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | `NowPlayingOverlay` | 回退至 git HEAD，恢复原有的全屏播放页布局、背景模糊、排版与按键调用。 |
| [`SoftNeumorphicButtons.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/common/SoftNeumorphicButtons.kt) | `SoftHeroPlayButton`, `SoftRefinedPlaybackScrubber` 等 | 回退至 git HEAD，恢复原有的播放控件实现并移除波浪进度条绘制逻辑。 |
| [`HorizontalPageSwipeUnitTest.kt`](../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/HorizontalPageSwipeUnitTest.kt) | `appVersionAndHighlightsGeneratedFromReleaseNotes` | 恢复版本断言为 `0.6.0` 及对应 6 项主要更新要点。 |
| [`version.properties`](../../version.properties) | `versionName`, `versionCode` | 恢复版本为 `0.6.0` 与 `34`。 |
| `WavyProgressScrubberTest.kt` | 移除 | 删除波浪进度条专用的未提交单测文件。 |
| `RELEASE_NOTES-0.6.1.md` / `release/0.6.1` | 移除 | 删除 0.6.1 发布说明及本地生成的 0.6.1 产物包目录。 |
| [`docs/README.md`](../README.md) | 索引与版本 | 当前工作区版本重置为 `0.6.0 / 34`，索引追加本次回退记录。 |
| [`docs/DEVELOPMENT.md`](../DEVELOPMENT.md) | 版本信息 | 核对日期恢复为 2026-10-26，工作区对齐 `0.6.0 / 34`。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 全屏播放页（Now Playing） | 实验性鼠尾草绿波浪进度条、64dp 扁平按键与调整后的版面 | 完整恢复为 v0.6.0 既有样式、经典线性进度条与成熟控件交互 |
| 工作区版本与产物 | `0.6.1 / 35` 产物包 | `0.6.0 / 34` 官方稳定基线与 `release/0.6.0` 产物包 |

## 平台与数据影响

- **平台与数据**：无任何破坏性或数据迁移影响；本地曲库、自建歌单、AI 语义模型、品味画像与伴奏模式缓存均完整保留不受影响。
- **Skills 技能目录**：保留用户已安装在 `.agents/skills/` 的 `android` 原生开发技能集与 `frontend-design` 技能，供未来其他模块优化使用。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `git checkout HEAD -- composeApp/... version.properties` | Windows 11, Git | 通过（源码与版本已无损还原至 HEAD） |
| `./gradlew.bat :composeApp:compileKotlinDesktop` | Windows 11, JDK 17 | 通过（UP-TO-DATE，无任何错误） |
| `./gradlew.bat :composeApp:desktopTest` | Windows 11, JDK 17 | 通过（耗时 45s，185 个单元测试全部通过，0 失败） |

## 限制与后续

- 后续若需再次重构播放页或进行 UI 探索，建议提前准备多套视觉原型或小步切片由用户确认后再行实施。

## 同步更新的文档

- `docs/README.md`：更新当前工作区版本为 `0.6.0 / 34`，并追加本次回退变更索引。
- `docs/DEVELOPMENT.md`：保持核对至 2026-10-26 及 `0.6.0 / 34` 状态。
