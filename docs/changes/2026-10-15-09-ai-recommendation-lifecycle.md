# 2026-10-15 — 新增定时推荐、自动下载和临时歌曲清理

- 修改日期：2026-10-15（Asia/Shanghai）
- 类型：功能
- 版本：0.3.5 / 22，未升版
- 平台：Android / Windows（平台差异见下文）
- 关联：用户本轮深度优化需求；[验证与交接](2026-10-15-12-verification-and-handoff.md)

## 需求、原因与依据

用户要求可设每隔多少小时推荐多少首，可关闭/选前台或后台；直接下载，未收藏/未入自建歌单的旧推荐下次清理，拒绝即删音频和所有关系。

## 具体修改

- 推荐默认关闭，间隔1–168小时、数量1–30首；Windows托盘轮询，Android WorkManager/网络存储约束和前台下载通知。
- 按当前库/歌手统计/收藏/拒绝原因找真实候选，过滤已有曲和拒绝曲，偏好原版而非现场/翻唱。
- 至少下载1首才提交新批；失败保留旧批。每首写 pending，中断后下一成功批统一清理。
- 旧曲按当前收藏/自建歌单成员保留，播放中暂缓；不喜欢无条件取消收藏、物理删除、移出所有歌单，原因可跳过。

| 文件 | 关键修改 |
| --- | --- |
| [features/JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | recommendInternal/dislike/recordDislikeReason |
| [features/RecommendationWorker.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/features/RecommendationWorker.kt) | 系统周期与 dataSync 前台通知 |
| [platform/DesktopPlatformServices.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopPlatformServices.kt) | 后台任务/托盘存活 |
| [ui/MusicFeatureScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureScreens.kt) | 推荐设置/播放/点赞/拒绝 |

## 修改前后

以前无自动推荐；现在到期生成下载，保存有明确生命周期，拒绝会实际删除。

## 平台与数据影响

删除会影响实际音频；Android自有下载可后台处理，应用外文件仍受系统权限。批次/reasons/dislikes/pending 写 JSON；进程实际播放状态保护当前曲。

## 验证

MusicAssistantTest 验保存保护、全失败保留、当前播放延期、拒绝已收藏/入单曲仍删除、下一批换新。

实际命令、结果与设备/网络限制统一记录在 [本轮验证](2026-10-15-12-verification-and-handoff.md)，不把模拟服务测试当作付费API或B站真实下载通过。

## 限制与后续

Android系统不保证精确定时；Windows完全退出后不运行。真实模型/B站限制可能使新批失败并保留旧批。

## 同步文档

[开发说明](../DEVELOPMENT.md)、[索引](../README.md)、[项目说明](../../README.md)；外部请求/凭据见 [第三方说明](../THIRD_PARTY.md)。
