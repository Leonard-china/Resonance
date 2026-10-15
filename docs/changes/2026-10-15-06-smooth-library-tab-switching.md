# 2026-10-15 — 消除歌单与歌曲标签重叠绘制造成的撕裂

- 修改日期：2026-10-15（Asia/Shanghai）
- 类型：性能 / Bug 修复
- 版本：0.3.5 / 22，未升版
- 平台：Android / Windows（平台差异见下文）
- 关联：用户本轮深度优化需求；[验证与交接](2026-10-15-12-verification-and-handoff.md)

## 需求、原因与依据

高频标签 AnimatedContent 同时组合两个重 Lazy 树并滑动/混合，切换期间放大封面解码与列表布局负担。

## 具体修改

- 曲库标签改为单一裁剪容器内直接选择当前内容，不同时绘制两棵列表。
- 详情页独立保留 LibraryDetailTransition 的裁剪/过渡底层和减少动态效果行为。
- 保留稳定条目 ID，封面请求由共享缓存合并。

| 文件 | 关键修改 |
| --- | --- |
| [ui/LibraryShell.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt) | Library 标签内容切换 |
| [ui/LibraryDetailTransition.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryDetailTransition.kt) | 详情转场边界保持 |
| [LibraryDetailTransitionTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/LibraryDetailTransitionTest.kt) | 中途像素/快速返回/深浅色 |

## 修改前后

标签切换以前可能叠影；现在立即显示目标列表，不做双列表透明滑动。

## 平台与数据影响

不改变库/歌单数据；两端共享 UI，减少动效模式仍有效。

## 验证

LibraryDetailTransitionTest 的4项设备像素回归；两端构建。

实际命令、结果与设备/网络限制统一记录在 [本轮验证](2026-10-15-12-verification-and-handoff.md)，不把模拟服务测试当作付费API或B站真实下载通过。

## 限制与后续

没有声称已测得所有真实大库的帧率；进一步性能工作需设备帧追踪。

## 同步文档

[开发说明](../DEVELOPMENT.md)、[索引](../README.md)、[项目说明](../../README.md)；外部请求/凭据见 [第三方说明](../THIRD_PARTY.md)。
