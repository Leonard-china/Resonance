# 2026-10-16 — 音乐助手进入主导航并隐藏聊天页迷你播放器

- 修改日期：2026-10-16（Asia/Shanghai）
- 类型：功能 / Bug 修复
- 适用版本：0.4.0 / 23，本次按构建规则不自动升版
- 影响平台：共享、Android、Windows

## 需求、原因与依据

搜索在音乐库右上角已有入口；用户希望主导航放音乐助手，聊天时不要被当前歌曲挤占空间。

## 具体修改

| 文件 | 关键符号 | 具体修改与目的 |
| --- | --- | --- |
| [ui/LibraryShell.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt) | destinationItems / tabDestinationOf / LibraryShell | Android 底栏和 Windows rail/侧栏统一为音乐库、音乐助手、工具；Discover 搜索保留在音乐库右上角；助手系统返回与页面返回都回音乐库；三个响应式布局均在助手页隐藏 MiniPlayer。 |
| [ui/MusicFeatureScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureScreens.kt) | AssistantScreen | 移出每日推荐大列表；保留聊天、截图与模型用量，新增任务入口和 imePadding，键盘出现时保留输入区域。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 主导航第二项 | 搜索 | 音乐助手 |
| 聊天时有歌曲在播放 | 迷你播放器占据区域 | 隐藏此区域，音乐继续播放 |

## 平台与数据影响

共享布局；原有搜索功能、播放任务和音频内容不变。

## 验证

本次实际构建、测试、签名及产物结果统一见 [构建验证记录](2026-10-16-08-release-build-verification.md)。相关测试已随本次修改加入源码；不将旧版结果用作本次通过证据。

## 限制与后续

LibraryNavigationTest 检查底部不再有搜索、进入助手后迷你播放器不存在且发送入口可见。

## 同步更新的文档

已更新 [开发说明](../DEVELOPMENT.md)、[文档索引](../README.md)、[本次发布版构建说明](../RELEASE_NOTES-0.4.0-2026-10-16.md)。
