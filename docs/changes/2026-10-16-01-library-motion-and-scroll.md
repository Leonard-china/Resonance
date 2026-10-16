# 2026-10-16 — 恢复曲库切换动画并保留各栏目的浏览位置

- 修改日期：2026-10-16（Asia/Shanghai）
- 类型：功能 / Bug 修复
- 适用版本：0.4.0 / 23，本次按构建规则不自动升版
- 影响平台：共享、Android、Windows

## 需求、原因与依据

从歌单、专辑或歌手切到歌曲时会瞬间换页并快速长距离滚动。此前为避免双 Lazy 树重叠绘制，直接移除了标签动画；TrackListTab 在每次挂载时又会 animateScrollToItem 到当前播放曲目。

## 具体修改

| 文件 | 关键符号 | 具体修改与目的 |
| --- | --- | --- |
| [ui/LibraryTabTransition.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryTabTransition.kt) | LibraryTabTransition | 仅挂载目标栏目一棵 Lazy 树；160 ms、24 dp 的短距离入场与 0.65→1 透明度，减少动态效果时只渐显；动画值只在 graphicsLayer 读取。 |
| [ui/LibraryShell.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt) | LibraryScreen / TrackListTab / DestinationContent | 将歌曲、专辑、歌手 LazyListState 与歌单 LazyGridState 提升到栏目切换外；移除歌曲页及歌单详情进入时自动定位播放曲目的副作用；SaveableStateHolder 保存切到助手或搜索前的栏目/列表状态。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 进入歌曲页 | 重新挂载后滚向当前播放歌曲 | 初次从顶部浏览，返回保留上次浏览位置 |
| 切换四个栏目 | 没有动画 | 轻量目标页入场，快速切换取消旧入场动画 |

## 平台与数据影响

共享 Compose 改动，两端生效；不改变曲库排序、音频或持久歌单。状态保存属于 UI saveable 状态。

## 验证

本次实际构建、测试、签名及产物结果统一见 [构建验证记录](2026-10-16-08-release-build-verification.md)。相关测试已随本次修改加入源码；不将旧版结果用作本次通过证据。

## 限制与后续

LibraryNavigationTest 使用 300 首歌曲、正在播放末尾曲目，检查入场不跳动及第 31 行返回位置；真实大曲库与不同 GPU 的帧率仍需真机体验。

## 同步更新的文档

已更新 [开发说明](../DEVELOPMENT.md)、[文档索引](../README.md)、[本次发布版构建说明](../RELEASE_NOTES-0.4.0-2026-10-16.md)。
