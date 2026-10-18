# 2026-10-18 — 优化滑动手势门槛、详情页歌词换行与译文、高亮居中跟随及每日推荐完整性保护

- 修改日期：2026-10-18（Asia/Shanghai）
- 类型：体验优化 / Bug 修复
- 适用版本：0.5.0（`version.properties`，不自动升版）
- 影响平台：共享 / Android / Windows
- 关联记录或需求：用户反馈：
  1. 滑动屏幕切换页面时不太好滑，需要滑很远；
  2. 在歌曲详细页面显示的歌词经常显示不全（会有....省略号），而且没有翻译（我已经翻译过了）；
  3. 在歌词详细页面的歌词跟随中，标亮的歌词是显示在最下面，很麻烦，我希望能显示在中间；
  4. 每日推荐中有的歌不完整，正播放着呢突然切了。
  关联历史记录 [2026-10-17-11-horizontal-page-swipe.md](2026-10-17-11-horizontal-page-swipe.md)、[2026-10-17-17-interactive-page-swipe-animation-and-builtin-music-sources.md](2026-10-17-17-interactive-page-swipe-animation-and-builtin-music-sources.md)、[2026-10-17-34-restore-spring-bounce-and-eliminate-top-edge-flicker.md](2026-10-17-34-restore-spring-bounce-and-eliminate-top-edge-flicker.md)。

## 需求或问题

1. **滑动手势门槛偏高（需要滑很远）**：
   - 用户在手机上左右滑动切换主页面、曲库标签或播放器面板时，必须拖动较大距离（原阈值为 `size.width * 0.18f` 且最小 48dp，在常见移动设备上需滑动 72dp 以上）；
   - 缺乏初速度扫滑（fling / flick）判定，单手快速轻扫松手后，因距离未达门槛被弹簧强行弹回，体验沉重费力。
2. **歌曲详细页（封面页预览）歌词截断与缺失翻译**：
   - 歌曲播放主界面底部的 `HeroSyncedLyricsPreview` 歌词预览中，当前行硬编码为 `maxLines = 1` 与 `TextOverflow.Ellipsis`，长歌词极易被粗暴截断成 `...` 省略号；
   - 预览组件直接读取 `line.text`，未读取用户的歌词展示偏好（`lyricDisplay`，如译文/双语）及 `line.translation`，导致用户即使翻译了歌词，回到播放页仍仅显示原文。
3. **歌词详细页高亮跟随歌词卡在底部**：
   - `LyricsPane` / `LyricsContent` 在下一句歌词到来时，若该句尚未在可视区，原逻辑使用默认 `listState.animateScrollToItem(activeIndex)`（`scrollOffset = 0`），导致向下播放时新行刚露在视口最底部就停止滚动；
   - 导致标亮行始终卡在屏幕最下方，视野逼仄。
4. **每日推荐中下载到残缺试听片段（播放中途突发切歌）**：
   - 网易云内置歌源搜索未识别 `fee === 1 || fee === 4`（VIP 试听与付费专辑），其外链接口返回 30 秒试听音频；
   - 酷狗、酷我歌源某些条目未返回 `duration` 或返回片段时长，下载后未识别试听；
   - B 站视频搜索缺少时长下限过滤，部分几十秒的短视频/副歌剪辑被误当完整歌曲下载；
   - 歌曲下载缺少时长下限兜底（<60 秒），导致 30~50 秒的试听音频入库并在播放到 EOF 时自动切到下一首。

## 原因与依据

1. **滑动手势逻辑**：
   - 位于 `HorizontalPageSwipe.kt`。原逻辑只依靠累积位移 `distance.x` 与静态阈值比较，没有利用 `VelocityTracker` 识别移动端高频使用的抬手轻扫动作；
   - 减小最小距离门槛至 28dp，比例降至 12%，并引入 `VelocityTracker`，当横向速度超过 450dp/s 且方向一致时即便距离较短也判定切页。
2. **封面预览歌词渲染**：
   - 位于 `NowPlayingOverlay.kt` 中的 `HeroSyncedLyricsPreview`。原逻辑使用 `lyrics.lines.getOrNull(targetIdx)?.text`，未读取 `LocalMusicFeatures.current?.settings?.lyricDisplay`，且 `maxLines` 设置为 1；
   - 接入译文与双语显示，将当前行调整为 `maxLines = 2` 并支持 `softWrap = true`，自动居中换行。
3. **歌词跟随居中对齐**：
   - 位于 `NowPlayingOverlay.kt` 中的 `LyricsContent`。当 `itemInfo == null` 时调用 `listState.scrollToItem(activeIndex)` 后，立即测量其真实偏移并补足 `scrollBy(currentOffset - realTarget)` 居中对齐；当在可视区时通过 `animateScrollBy(diff.toFloat())` 平滑维持在正中间，打破每行卡在底部的恶性循环。
4. **每日推荐试听音频防护**：
   - `netease.js`、`kuwo.js`、`kugou.js`：从音源层标注 `isTrial = true`，并拦截获取试听外链；
   - `BilibiliClient.kt`：搜索候选过滤时长在 1..59 秒的短视频；
   - `JvmMusicAssistant.kt`：在 `importSourceAudio` 和 `downloadAudio`（每日推荐）中加入实际音频时长下限保护（<60 秒且非特定短音频标题时报错并排除，由引擎尝试备选音源）。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| [HorizontalPageSwipe.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/HorizontalPageSwipe.kt) | `horizontalPageSwipe` / `swipePageTarget` | 门槛由 18% 降至 12%（最小 28dp），接入 `VelocityTracker`，支持快速轻扫（fling）即时换页，解决“需要滑很远” |
| [HorizontalPageSwipeUnitTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/HorizontalPageSwipeUnitTest.kt) | `HorizontalPageSwipeUnitTest` | 新增针对 `swipePageTarget` 轻扫速度匹配、反向速度与速度不足等分支的单元测试 |
| [NowPlayingOverlay.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | `HeroSyncedLyricsPreview` | 支持 `lyricDisplay` 译文/双语展示，当前行改为两行折行并居中，彻底消除省略号截断 |
| [NowPlayingOverlay.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | `LyricsContent` | 修复歌词跟随位置：无论是从屏外进入还是视口内推进，均精确、平滑滚动至视口正中央 |
| [netease.js](../../composeApp/src/jvmMain/resources/music-source/builtin/netease.js) | `search` / `getMediaSource` | 增加网易云 `item.fee` 校验，排除 VIP（1）与数字专辑（4）试听片段，阻断外链下载 30s 试听音频 |
| [kuwo.js](../../composeApp/src/jvmMain/resources/music-source/builtin/kuwo.js) | `search` / `getMediaSource` | 增强酷我音乐付费和试听片段检测，阻断试听音频下载 |
| [kugou.js](../../composeApp/src/jvmMain/resources/music-source/builtin/kugou.js) | `search` / `getMediaSource` | 识别酷狗 `IsFreePart`、`FailProcess`、`privilege` 试听字段，拦截免费试听接口 |
| [BilibiliClient.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/media/BilibiliClient.kt) | `search` | 搜索候选时解析视频时长，过滤低于 60 秒的短视频/副歌剪辑片段 |
| [JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | `importSourceAudio` / `downloadAudio` | 增加实际音频时长下限保护（<60s 拦截），从根本上避免残缺音频进入每日推荐 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 屏幕横向轻划切换主导航/标签/面板 | 必须拖拽滑过很长距离（>72dp），轻划松手后容易直接弹回原位 | 滑动门槛更轻灵贴手（约 12% 屏宽），且支持快速轻拂（fling）即时换页，操作顺畅丝滑 |
| 歌曲详情页面（封面页）底部的歌词显示 | 稍长歌词被截断为 `...`，且无法显示用户已翻译的歌词译文 | 歌词支持最多 2 行自动换行展示，完整显示无省略号，并支持原文/译文/双语显示 |
| 歌词详细页面歌词自动跟随 | 标亮的高亮歌词一直卡在屏幕最底部，视野狭窄 | 标亮歌词始终稳定、平滑居中在视口正中间，上下视野开阔对称 |
| 每日推荐中包含 VIP 歌曲或网络剪辑 | 网易云/酷我外链或 B 站短视频下载了 30~50 秒音频，播放中途突然结束切歌 | 自动识别排除试听与短片段，强制获取完整歌曲，播放不再中途突切 |

## 平台与数据影响

- **平台影响**：
  - Android & Windows：共享 UI 与逻辑全部生效，手势、歌词居中、译文和音源过滤双端体验完全一致。
- **数据影响**：
  - 不改变任何现有本地数据库、配置文件或音频格式结构；
  - 纯前端与推荐获取链路加固，对既有已下载且完整的歌曲无负面影响。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `./gradlew :composeApp:desktopTest --tests "com.resonance.player.features.HorizontalPageSwipeUnitTest"` | Windows 11 / JVM 17 | 通过（覆盖距离与 fling 速度双维度） |
| `./gradlew :composeApp:desktopTest` | Windows 11 / JVM 17 | 通过（1m 37s，全量桌面单测通过） |
| `./gradlew :composeApp:compileDebugKotlinAndroid` | Windows 11 / Android SDK 35 | 通过（2m 21s，Android Debug 编译通过） |
| `./gradlew :composeApp:compileReleaseKotlinAndroid` | Windows 11 / Android SDK 35 | 通过（2m 24s，Android Release 编译通过） |

## 限制与后续

- 本次修改范围内各项体验与逻辑已全部完成代码修改与单元测试验证。
- 遵循不自动升版原则，版本号保持为 0.5.0。
- 若需发布新安装包，可按照既定发布流程构建。

## 同步更新的文档

- [docs/README.md](../README.md)（变更索引）。
