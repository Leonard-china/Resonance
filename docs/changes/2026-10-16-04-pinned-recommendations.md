# 2026-10-16 — 在歌单页固定每日推荐并提供独立推荐页面

- 修改日期：2026-10-16（Asia/Shanghai）
- 类型：功能 / Bug 修复
- 适用版本：0.4.0 / 23，本次按构建规则不自动升版
- 影响平台：共享、Android、Windows

## 需求、原因与依据

每日推荐原来混在助手聊天记录前，且关闭时歌单页没有稳定入口。

## 具体修改

| 文件 | 关键符号 | 具体修改与目的 |
| --- | --- | --- |
| [ui/PlaylistGrid.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/PlaylistGrid.kt) | PlaylistListTab | 无论是否开启推荐，都将每日推荐卡放在第一个网格项；过滤持久歌单中相同 DAILY_PLAYLIST_ID 避免重复。关闭时显示未开启，点击进入设置；开启时进入推荐页面。空曲库也保留这张卡。 |
| [ui/MusicTasksScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicTasksScreen.kt) | RecommendationsScreen | 独立展示推荐曲目、理由、播放、点赞收藏、不喜欢删除与可选原因；更新按钮沿用原推荐生命周期。 |
| [model/MusicFeatureModels.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/MusicFeatureModels.kt) | DAILY_PLAYLIST_ID | 共享固定歌单 ID，兼容原有推荐歌单。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 推荐关闭 | 歌单页无稳定入口 | 第一张卡提示未开启并进入设置 |
| 推荐开启 | 在助手中显示曲目 | 第一张卡进入专用歌单页面 |

## 平台与数据影响

不复制推荐音频，不改变收藏/自建歌单保护及不喜欢立即物理删除的既定语义；后台调度方式不变。

## 验证

本次实际构建、测试、签名及产物结果统一见 [构建验证记录](2026-10-16-08-release-build-verification.md)。相关测试已随本次修改加入源码；不将旧版结果用作本次通过证据。

## 限制与后续

设备测试覆盖关闭/开启两种入口与置顶顺序；推荐生命周期继续运行 MusicAssistantTest。

## 同步更新的文档

已更新 [开发说明](../DEVELOPMENT.md)、[文档索引](../README.md)、[本次发布版构建说明](../RELEASE_NOTES-0.4.0-2026-10-16.md)。
