# 2026-10-17 — 自动推荐支持不计费网络限制与下次到期提示

- 修改日期：2026-10-17（Asia/Shanghai）
- 类型：功能 / 体验修复
- 适用版本：0.4.9 / 32 工作区；本轮不升版、不发布
- 影响平台：共享 / Android / Windows（平台限定见下文）
- 关联需求：用户要求落地全部体验建议，并追加页面顶部黑边闪烁反馈；[完整交接与实际验证](../HANDOFF-2026-10-17-UX-ANTIGRAVITY.md)

## 需求或问题

用户可设后台推荐，但难以控制自动下载的流量，也不容易知道何时再次检查。

## 原因与依据

设置没有网络限制字段，Android WorkManager 一直使用 CONNECTED，桌面和前台每分钟轮询没有计费状态限制。

## 具体修改

| 文件 | 关键符号 | 修改与目的 |
| --- | --- | --- |
| [MusicFeatureModels.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/MusicFeatureModels.kt) | `recommendationsUnmeteredOnly` | 增加兼容旧数据的 false 默认值。 |
| [MusicFeatureCodec.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/MusicFeatureCodec.kt) | `settings / encode` | 读写新设置；不改已有用户是否开启推荐。 |
| [RecommendationWorker.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/features/RecommendationWorker.kt) | `configure` | 开启限制后使用 UNMETERED，平台重配计划把该字段纳入变更键。 |
| [AutomaticDownloadNetwork.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/AutomaticDownloadNetwork.kt) | `allowed / checkCost` | 使用 Windows 系统连接计费状态，30 秒缓存、3 秒超时，无法判断时等待，不擅自允许。 |
| [JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | `recommend / checkAutomaticDownloadNetwork` | 自动任务开始与下载前检查条件；明确手动更新仍可立即联网。 |
| [RecommendationScheduleStatus.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/RecommendationScheduleStatus.kt) | `RecommendationScheduleStatus` | 显示预计到期、等待网络和系统调度边界；教程帮助同步。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 本次使用流程 | 自动下载只要求联网，设置只展示间隔。 | 可选择仅在不计费网络自动下载，并看到预计到期或等待状态；手动更新明确说明立即联网。 |

## 平台与数据影响

设置默认 false 保持旧行为；不开启推荐不会运行。Android 按系统网络计费状态判断，Windows 调用只读连接成本查询，不读取凭据。前台、后台与共享下载均检查，仍受系统调度限制，不承诺精确时刻。无新权限，沿用已有 ACCESS_NETWORK_STATE。

## 验证

MusicAssistantTest 验证旧配置兼容、新字段往返、自动等待与手动继续；本机 Windows 只读查询实际返回 Unrestricted；Android Worker 与平台代码通过编译。

| 本次检查 | 结果 |
| --- | --- |
| 定点回归与编译 | 本轮相关回归合计 75 项不同测试通过（失败 0、跳过 0）；双端源码及 Android 测试源码通过编译。完整命令与执行边界见交接文档。 |
| 真机、正式包与网站 | 本轮未执行；交 Antigravity 完成最后验收、正式构建及部署。 |

## 限制与后续

计费网络切换、Wi-Fi 被标记计费、国产 ROM 调度与 Windows 受限执行策略待设备验证。Windows 查询失败时自动任务等待，手动操作仍可用。

## 同步更新的文档

- [文档索引](../README.md)
- [开发说明第 18 节及对应原章节](../DEVELOPMENT.md#user-experience-update)
- [交接与构建部署提示词](../HANDOFF-2026-10-17-UX-ANTIGRAVITY.md)

