# 2026-11-05 — 修复 Linux 桌面歌词拖动与锁定后的解锁入口

- 修改日期：2026-11-05（Asia/Shanghai）
- 类型：Bug 修复；版本 0.6.6 / 40，不升版
- 平台：Linux；Windows 保留原生拖动／穿透
- 需求：用户反馈无法拖动桌面歌词；前置见[窗口隔离记录](2026-11-05-06-desktop-lyrics-window-and-stability-boundaries.md)。

## 需求或问题

Linux 歌词无法拖动，锁定后控制行隐藏，窗口内没有解锁入口。

## 原因与依据

旧实现只用 WindowDraggableArea 且初始位置保持底部居中。已确认 Linux 锁定时隐藏控制却没有 Windows 点击穿透；拖动采用显式指针位移与绝对窗口位置，避免依赖平台拖动桥接；原先解锁状态的拖动并非在所有环境都失效。锁定后的解锁入口缺失是已确认问题。语义节点检查不足以认定真实拖动通过。

## 具体修改

| 文件 | 关键符号 | 修改 |
| --- | --- | --- |
| [DesktopFloatingLyrics.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/ui/DesktopFloatingLyrics.kt) | `DesktopFloatingLyricsWindow` | Linux 手势移动窗口并同步绝对位置，保留不透明回退；锁定仍显示解锁／关闭。Windows 沿用原生拖动和锁定穿透。 |
| [DesktopWindowLifecycleTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/DesktopWindowLifecycleTest.kt) | `linuxLyricsCanBeDraggedUnlockedAndStayInPlaceDuringUpdates` | Robot 拖动、刷新／字体尺寸变化不归位、锁定不动、解锁后再拖动。 |

## 修改前后行为

解锁后可拖动，歌词更新与字号调整不重置位置；Linux 锁定仍显示“解锁位置”，解锁后可再次拖动。隔离原生输入回归已通过。

## 平台与数据影响

不改歌词时间／文本／高亮、播放任务、音频文件、持久化格式或 Android；位置只维护于当前窗口会话。Windows 真实交互未验收。

## 验证

- 桌面完整 200 源编译通过；不改 Windows 分支原生拖动与穿透逻辑，但没有 Windows 实机验收。
- 当前 Wayland 会话的 Robot 首轮没有向测试窗口送达输入，视为自动化环境不确定；没有据此宣称旧拖动实现故障。
- 隔离 X11 会话的实际 AWT Robot 鼠标输入通过：拖动移动、歌词／字号更新不归位、锁定不动、解锁再拖动。与助手／歌词高亮合计 69 项全通过。
- 两倍显示缩放单独三项全通过。上一安装实现对照三项中，新增拖动／解锁用例超时一项；另两项通过；新实现三项均通过。
- 补测使用 512 MiB 进程树上限、384 MiB 软限额、Swap0，峰值 285.2 MiB；没有故意制造宿主机内存压力。

## 限制与后续

用户要求保护内存后，单独桌面打包因启动余量不足被拦下，未启动 Gradle。当前安装仍为上一轮歌词修复版，本记录改动尚未覆盖。实际 Wayland 人工拖动与 Windows 实机仍未验收，不能将隔离 X11 测试当成全部桌面环境验收。打包前须遵守[新余量门槛](2026-11-05-14-reserve-host-memory-before-builds.md)。

## 同步更新的文档

`docs/README.md` 索引；开发说明中的窗口行为已同步。
