# 2026-10-17 — 增加 Windows 搜索播放切歌和返回快捷键

- 修改日期：2026-10-17（Asia/Shanghai）
- 类型：功能 / 体验修复
- 适用版本：0.4.9 / 32 工作区；本轮不升版、不发布
- 影响平台：共享 / Android / Windows（平台限定见下文）
- 关联需求：用户要求落地全部体验建议，并追加页面顶部黑边闪烁反馈；[完整交接与实际验证](../HANDOFF-2026-10-17-UX-ANTIGRAVITY.md)

## 需求或问题

Windows 日常搜索和播放主要依赖鼠标，Esc 没有接入现有返回层级。

## 原因与依据

Desktop ResonanceBackHandler 是空实现，App 没有全局键盘路由或搜索焦点请求。

## 具体修改

| 文件 | 关键符号 | 修改与目的 |
| --- | --- | --- |
| [App.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | `onPreviewKeyEvent / onKeyEvent` | Ctrl+F 打开并聚焦搜索；冒泡阶段处理空格、Ctrl+方向键与 Esc；按住空格不会连续切换。 |
| [KeyboardBackNavigation.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/KeyboardBackNavigation.kt) | `KeyboardBackNavigation / LocalBackHandlerActive` | 按最近有效层级返回，离开的动画页面不注册返回处理。 |
| [ResonanceBackHandler.desktop.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/ResonanceBackHandler.desktop.kt) | `ResonanceBackHandler` | 把现有播放器、详情和多选返回处理注册到键盘分发。 |
| [LibraryShell.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt) | `DiscoverScreen / DestinationContent` | 聚焦搜索框；键盘导航即时切换；Android 返回同样排除退出动画中的页面。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 本次使用流程 | 无统一快捷键，桌面返回处理为空。 | Ctrl+F 搜索，空格播放暂停，Ctrl+←／→ 切歌，Esc 关闭播放器／退出多选／返回；设置内可查说明。 |

## 平台与数据影响

仅 Windows 开启全局键盘快捷键；输入控件先处理文字、空格与光标操作，Ctrl+F 为专用搜索动作。无新增持久数据、权限或网络。返回处理随 Compose 生命周期注销，不拦截独立原生选择器或弹窗。

## 验证

双端 Kotlin 源码编译，Android 导航测试源码编译；现有导航和教程模型定点回归。没有把编译当作实际键盘操作验证。

| 本次检查 | 结果 |
| --- | --- |
| 定点回归与编译 | 本轮相关回归合计 75 项不同测试通过（失败 0、跳过 0）；双端源码及 Android 测试源码通过编译。完整命令与执行边界见交接文档。 |
| 真机、正式包与网站 | 本轮未执行；交 Antigravity 完成最后验收、正式构建及部署。 |

## 限制与后续

不同输入法、焦点位于按钮／输入框／弹窗时的快捷键，以及连续 Esc 的桌面 GUI 操作由 Antigravity 实测。

## 同步更新的文档

- [文档索引](../README.md)
- [开发说明第 18 节及对应原章节](../DEVELOPMENT.md#user-experience-update)
- [交接与构建部署提示词](../HANDOFF-2026-10-17-UX-ANTIGRAVITY.md)

