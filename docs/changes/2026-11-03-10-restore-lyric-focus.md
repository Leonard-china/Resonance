# 2026-11-03 — 恢复滚动歌词当前行的亮色文字与淡底高亮

- 修改日期：2026-11-03（Asia/Shanghai）
- 类型：Bug 修复 / UI 可读性
- 适用版本：0.6.5 / 39，不升版
- 影响平台：共享 Android／Windows／Linux
- 关联需求：用户反馈歌词滚动没有高亮提示，并选择“亮色文字＋轻微底色”
- 关联记录：[连续滚动](2026-11-03-01-continuous-lyrics-motion.md)、[本次全应用材质与 APK](2026-11-03-09-app-wide-frosted-material.md)

## 需求或问题

滚动仍会定位到当前行，但普通行和当前行颜色接近，尤其在浅色磨砂背景上难以辨认哪句正在播放。需要恢复清楚的焦点，同时保留连续滚动和稳定换行。

## 原因与依据

`LyricRow` 和 `PreviewLyricRow` 使用 TextPrimary 与 Muted 区分当前／普通行，这组颜色优先正文可读性，彼此对比不足；当前行没有底色标记。此次调整以用户指定的亮色文字与轻微底色为准，不回退到切换字号／字重导致行高变化的旧方案。

## 具体修改

| 文件 | 关键符号 | 修改与目的 |
| --- | --- | --- |
| [LyricFocus.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/LyricFocus.kt) | `Modifier.lyricFocus` | 当前行使用 Notice 的浅色 10%／深色 14% 底色、24% 细边，160ms 颜色过渡，减少动效时即时完成；只影响绘制 |
| [NowPlayingOverlay.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | `LyricRow`、返回当前播放按钮 | 当前行用 Notice 文字与共用焦点带，原文／译文同时突出；返回按钮使用 onPrimary，修复深色主题上的白字低对比 |
| [SyncedLyricsPreview.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/SyncedLyricsPreview.kt) | `PreviewLyricRow` | 封面预览同步采用文字与底色焦点，保留稳定列表身份、连续滚动与既有绘制缩放 |
| [LyricsMotionTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/LyricsMotionTest.kt) | `focusBandAndSelectedLineMoveTogetherInBothThemes` | 实际渲染像素确认新行出现底色、旧行取消底色，并确认 selected 语义同步；保留原长句行高、换句中间位置、快速重定向与减少动效测试 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 正常跟随 | 当前行与邻句主要靠微小颜色差 | 明显强调色文字与淡底焦点带 |
| 长句高亮／离开高亮 | 字号稳定但提示不清楚 | 字号／字重／行高继续稳定，焦点仅改变绘制 |
| 双语歌词 | 当前译文仍进一步降低不透明度 | 当前原文／译文共同用强调色 |
| 用户手动浏览 | 按既有规则暂停跟随 | 仍可浏览和返回，当前行状态保留 |
| 减少动态效果 | 取消滚动过渡 | 同样立即切换焦点，不增加常驻动画 |

## 平台与数据影响

仅共享 UI 绘制；不改变时间戳、歌词偏移、翻译缓存、seek、播放后端、音频文件、同步格式或任何权限。纯文本歌词没有同步时间轴时不伪造当前行。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `:composeApp:desktopTest --tests 'com.resonance.player.ui.LyricsMotionTest'`（与 UI 定向回归一并执行） | Linux / JDK 17 / 原生 Compose 离屏 | 4 项全部通过：新增双主题焦点像素与 selected 语义同步，既有长句行高与中间帧、预览列表身份、减少动效与翻译到达检查均通过 |
| `UiVisualContractTest` 7 项定向回归 | 同上 | 全部通过：大字号短窗口、横屏、宽屏、主题可读性、歌曲信息入口、14 个主壳页面与双主题错误弹窗 |
| 人工检查浅／深主题 `lyrics-*-after.png` | 本次生成的原生截图 | 通过：当前长句文字与焦点带明确、邻句保持可读；像素断言确认高亮随当前行变化，旧行恢复普通状态 |
| `:composeApp:compileDebugKotlinAndroid :composeApp:assembleRelease` | Android SDK 36 | 通过；APK v2 签名校验通过，版本不变。构建排除了 lintVital 任务 |
| 真机触摸／实际播放听感／性能 | 未执行 | 无连接 Android 设备；不将离屏结果等同于真机验收 |

完整实际命令、首次中断／夹具失败及修正后的结果、APK 路径和校验值见[全应用材质记录](2026-11-03-09-app-wide-frosted-material.md#验证)。最终 11 项回归报告位于 `composeApp/build/test-results/desktopTest/`，日志位于 `build/glass-coverage-2026-11-03/render-verification.log`。本次歌词截图保存在 `composeApp/build/glass-coverage/lyrics-{Light,Dark}-{before,plain,after,old}.png`，全部使用合成歌词，不包含用户歌曲或数据。

## 限制与后续

原生离屏逐帧与像素断言不能替代 Android 真机触摸、帧率和听感验收；本次无连接设备。APK 与全应用材质一并构建，结果见关联记录。

## 同步更新的文档

`docs/README.md`、`docs/DEVELOPMENT.md`、`DESIGN.md`、`PRODUCT.md`。
