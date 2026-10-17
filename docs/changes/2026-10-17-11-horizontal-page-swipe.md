# 2026-10-17 — 原生主导航、曲库标签与播放器支持左右滑动切换

- 修改日期：2026-10-17（Asia/Shanghai）
- 类型：功能
- 适用版本：0.4.6 / 29 工作区，未升版、未发布
- 影响平台：共享 / Android / Windows
- 关联需求：用户希望切换页面时也能左右滑动；[原栏目状态/动画记录](2026-10-16-01-library-motion-and-scroll.md)。

## 需求或问题

主导航、曲库四标签与播放器封面/歌词/队列已有按钮及过渡，但没有统一的横向页面手势。用户希望在内容区滑动切换，同时维持列表滚动位置。

## 原因与依据

LibraryShell 的 DestinationContent / LibraryScreen、NowPlayingOverlay.PaneContent 只由点击改变状态。已有 LibraryTabTransition 保持单棵 Lazy 树，不能为滑动简单同时挂载两个完整曲库页面。

## 具体修改

| 文件 | 关键符号 | 具体修改与目的 |
| --- | --- | --- |
| [HorizontalPageSwipe.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/HorizontalPageSwipe.kt) | horizontalPageSwipe / swipePageTarget | 横向优势方向锁定、抬手距离阈值、单次一页；子控件先消费，竖滚/多指/边缘不抢占 |
| [LibraryShell.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt) | DestinationContent / LibraryScreen | 主页面切换音乐库/助手/工具；曲库内容区优先歌曲/歌单/专辑/歌手，栏目边界交父级主导航处理 |
| [NowPlayingOverlay.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | PaneContent / onPaneChange | 紧凑与宽屏播放器内容区均可滑动切换封面/歌词/队列，仍使用现有按钮状态 |
| [HorizontalPageSwipeTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/HorizontalPageSwipeTest.kt)、[LibraryNavigationTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/LibraryNavigationTest.kt) | 手势/页面回归 | 覆盖每次一页、不循环、竖滚/滑块冲突、曲库优先及栏目位置保留、边界进助手 |

采用原生 Compose 指针事件，复用已有页面/栏目过渡及减少动态效果设置，无新增动效依赖。起点距左右边缘24dp以内留给系统/边缘操作；横移超过touchSlop且大于纵移1.25倍才认领，抬手达到max(56dp, min(内容宽20%,112dp))才切换。边界不循环。正在教程时禁用页面滑动；曲库多选时禁用栏目滑动。

## 修改前后行为

| 场景 | 修改前 | 修改后（待运行验证） |
| --- | --- | --- |
| 曲库内容区左右滑动 | 不切栏目 | 优先四标签切换，保留提升在栏目外的列表/网格状态 |
| 主页面/播放器内容滑动 | 仅按钮能切换 | 抬手达到阈值切相邻主栏目或播放器面板 |
| 操作滑块或竖向列表 | 已有操作 | 子控件已消费/明显竖向时不触发页面切换 |

## 平台与数据影响

共享代码，两端使用相同手势；Windows支持指针拖动，触控板横向滚轮不等同本次拖动手势。仅改变原有UI状态，不改音频、播放命令、歌单和存储格式。手势完成后沿用现有过渡，不是跟随手指的双页Pager，保持曲库单棵Lazy树。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| git -c core.safecrlf=false diff --check -- composeApp docs | Windows | 通过 |
| 新增/扩充Android手势及原生导航用例 | instrumentedTest源码 | 已编写，未运行 |
| Gradle编译、设备测试、实机手势/屏幕边缘/歌词拖动 | Android / Windows | 未执行，用户此前要求本轮不构建发布，交Antigravity |

## 限制与后续

需真机验证方向锁定、快速反向、横向控件和多层手势冲突、系统返回及不同密度阈值。不为工具详情任意建立线性顺序。没有循环切换或触控板滚轮翻页。工作区原有Antigravity与官网修改已保留；全工作区diff检查曾发现既有website/OPERATIONS.md末尾空行，未改动该无关文件。后续入口见 [交接](../HANDOFF-2026-10-17.md)。

## 同步更新的文档

docs/README.md、docs/DEVELOPMENT.md、docs/HANDOFF-2026-10-17.md。
