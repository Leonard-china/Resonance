# 2026-11-13 — 修复播放页窄屏歌词字号过大和长句裁切

- 修改日期：2026-11-13（Asia/Shanghai）
- 类型：Bug 修复
- 适用版本：`0.6.9 / 43`，版本号保持原值
- 影响平台：Android、Windows、Linux 的共享原生播放页
- 关联需求：用户反馈手机播放页歌词过大、显示不完整。前置记录见[沉浸歌词焦点恢复](2026-11-05-08-restore-player-lyrics-and-synchronized-priority.md)和[歌词工具独立滚动](2026-11-05-34-lyrics-tools-scroll-and-layering.md)。

## 需求或问题

手机播放页的完整歌词分栏里，一句英文折成五行，当前句高亮占满了滚动视口。文字更长或同时显示译文时，同一条歌词容易在视口底部被裁切。

## 原因与依据

`LyricRow` 在沉浸播放页固定使用 34sp 和 44sp 行高，双语译文固定使用 17sp 和 26sp 行高。`LyricsContent` 的列表分配播放器控件占用后的剩余高度。长句受歌词栏宽度限制而换行，叠加系统字体缩放后，一条歌词可能比视口更高。现有滚动逻辑会让超高条目的开头对齐视口，用户需要滑动才能看到剩余部分。

## 具体修改

| 文件 | 关键符号 | 实际修改 |
| --- | --- | --- |
| [NowPlayingOverlay.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | `LyricsContent`、`LyricRow` | 按列表实际宽高选择字阶。视口高不足 240dp 时，主歌词为 18sp、22sp 行高，双语译文为 12sp、16sp 行高。其余宽不足 480dp 或高不足 300dp 的视口分别使用 22sp、28sp 行高和 14sp、20sp 行高。使用 `sp` 保留系统字体缩放。 |
| [LyricsPlayerRegressionTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/LyricsPlayerRegressionTest.kt) | `longBilingualCurrentLyricFitsThePhoneViewport` | 用完整播放器渲染长英文歌词和中文译文，检查当前条目完整落在歌词视口内并居中。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 手机窄屏或较矮的窗口 | 沉浸歌词使用 34sp，换行后容易占满视口 | 使用 22sp。歌词区较矮时使用 18sp。长句自然换行，并按换行后的真实高度定位 |
| 宽高充足的歌词区 | 主歌词 34sp，双语译文 17sp | 主歌词 34sp，双语译文 17sp |

## 平台与数据影响

三端使用同一套视口规则。此修复只涉及播放页完整歌词的文字样式，无存储格式、权限或网络请求变化。歌词时间轴、点击跳转和高亮继续走原有路径。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `:composeApp:desktopTest --tests '*LyricsPlayerRegressionTest' --tests '*LyricScrollTest'` | Linux，隔离源码副本，2 GiB 进程树限额 | 通过。5 项通过，0 失败，0 跳过。长英文和中文译文在 390×820 默认字号、360×640 的 1.5 倍字号下均完整显示并居中。 |
| 检查本轮离屏截图 | Linux Compose，手机尺寸与 1280×800 | 通过。手机两种尺寸均可见完整测试句和译文，桌面宽屏显示当前句高亮。 |
| `:composeApp:compileDebugKotlin` | Linux，隔离源码副本，2 GiB 进程树限额 | 通过。Android Debug Kotlin 编译成功。 |
| `adb devices -l` | Linux | 无已连接设备，Android 真机未执行 |
| `git diff --check` | Linux | 通过。本次修改的已跟踪文件及新增变更记录均通过空白检查。 |

## 限制与后续

第一轮测试代码误用 `ImageComposeScene.use`，改为 `try/finally` 调用 `close()` 后通过测试编译。首轮渲染显示 22sp 在 360×640、1.5 倍字号下仍会裁切，新增矮视口字阶后复测通过。

特别长的句子在系统大字号或极短窗口中仍需纵向滑动。手机和 Windows 实机验收待执行，本轮未安装或发布。

## 同步更新的文档

[文档索引](../README.md)、[开发说明第 8 节](../DEVELOPMENT.md#lyrics)和[设计规范](../../DESIGN.md)。
