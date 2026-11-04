# 2026-11-04 — 重做浅色 HTML 原型的连续切页交接

- 修改日期：2026-11-04（Asia/Shanghai）
- 类型：Bug 修复 / 交互原型
- 适用版本：`version.properties` 为 0.6.5 / 39；未升版
- 影响平台：桌面与手机浏览器原型；不修改原生 Android / Windows / Linux
- 关联：[原型方案](2026-11-04-02-light-ui-proposal-and-html-prototype.md)、[上一版单页入场](2026-11-04-04-prototype-page-transitions.md)

## 需求或问题

用户反馈“这动画一点不丝滑”，要求继续处理正在试用的 HTML 原型。上一版在导航时立即删除旧内容，再对新内容执行 220ms / 18px 入场；快速往返会重置内容，无法呈现新旧页面的连续交接。

## 原因与依据

源码定位到 `render` 和 `transitionPage`：前者每次替换 `#content.innerHTML`、顶栏、侧栏及播放条，后者只对已替换的内容执行入场。旧画面先消失，控件节点及音量滑块也被重建。全局 `lucide.createIcons()` 会再次扫描并替换已有图标。以上为代码确认的问题，不将其等同于已经取得真实设备掉帧轨迹。

## 具体修改

| 文件 | 关键符号 | 具体修改与目的 |
| --- | --- | --- |
| [app.js](../../design-system/resonance/proposals/2026-11-04-light-glass/app.js) | `transitionPage`、`activatePage`、`stopPageTransition` | 新旧内容短暂共存；由 CSS transition 接续当前透明度和位移。返回仍在退场的页面时复用节点，保留输入、搜索结果和滚动位置 |
| 同上 | `pageTransitionVersion`、`parkedIds` | 取消过期收尾；离场节点 inert、aria-hidden，暂存并移除其 ID，避免重复 ID 和误交互；透明层及时回收，可见层退出后再回收 |
| 同上 | `render`、`updateNavigation`、`iconMarkup` | 导航只更新选中状态与面包屑，保留侧栏、顶栏、播放条及 SVG；图标 SVG 标记按名称缓存，移除全局重复扫描 |
| [style.css](../../design-system/resonance/proposals/2026-11-04-light-glass/style.css) | `.page-viewport`、`.page-moving` | 独立内容视口，仅动画 transform / opacity，12px / 280ms，复用 `--ease-drawer`；不逐帧运行 JS、动画布局或模糊半径 |
| [index.html](../../design-system/resonance/proposals/2026-11-04-light-glass/index.html) | 页面视口、资源版本 | 容纳并行内容层；更新资源版本，刷新后取得新脚本 |
| [verify-motion.cjs](../../design-system/resonance/proposals/2026-11-04-light-glass/verify-motion.cjs) | 连续转场验证 | 检查画面交接、反向连续、控件节点保留、输入/滚动、连续快切、焦点隔离、减少动态和全部页面图标 |
| [design-tokens.json](../../design-system/resonance/proposals/2026-11-04-light-glass/design-tokens.json)、[PLAN.md](../../design-system/resonance/proposals/2026-11-04-light-glass/PLAN.md) | motion、动效方案 | 同步当前参数、实现边界和验证结果 |

使用 CSS transition 的当前呈现值作为新目标的起点；不等待旧页退出才响应新目的地。连续点击第三、第四个目的地时，不强行删除仍可见的旧层，以免再次出现闪跳。每个离场层的透明度结束后独立回收，最终只剩当前页；同帧生成但尚未显示的层直接回收。仅在转场窗口内保留页面，不增加跨页面持久状态缓存。

初始加载、键盘操作、手动/系统减少动态及截图模式即时展示。转场中打开播放器也立即清理旧层，交由已有播放器动画处理。全页面检查发现原有 `chart-radar` 在本地 Lucide 包中不存在，改为已有的 `radar` 图标。

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 普通切页 | 旧内容立即消失，新内容单独入场 | 旧页退出与新页进入同时进行 |
| 快速反向 | 重建目标内容并重启动画 | 从当前画面继续，复用尚未退场的目标页 |
| 切页时调整过的音量 | 播放条重建，滑块回到模板值 | 播放条及滑块节点保留 |
| 连续多个目的地 | 新内容反复替换 | 可见层完成退出，过期收尾不会删除最新页 |
| 减少动态、键盘、展开播放器 | 取消单个入场动画 | 立即结束内容转场并回收全部旧页 |

## 平台与数据影响

仅候选 HTML 原型、静态效果图、离线包与说明变动。原生 Kotlin/Compose、官网、播放器数据、音频文件、元数据写回、同步包、权限和凭据不涉及。原型继续使用虚构歌曲，无实际音频或联网业务。预览仍为原有 `127.0.0.1:8765` 用户服务，不改变服务暴露范围。

## 验证

在本机 Linux / Playwright Chromium 执行；`PLAYWRIGHT_MODULE` 指向已有工具运行时，不将本机依赖路径写入项目脚本。

| 命令或操作 | 结果 |
| --- | --- |
| `node --check app.js` | 通过 |
| `node verify-motion.cjs` | 10组通过，页面脚本错误0；1440×960与390×844，含反向前后位移/透明度连续、固定控件节点保留及全部78页图标 |
| Chromium 4倍CPU降速、390px视口、每45ms切换 | 通过可见旧层不中途删除和最终清理检查；同步 render 耗时16.4–22.6ms，最多5个临时层，最终1层。数据位于 `motion-report.json`，不是帧率测量 |
| `node verify.cjs` | 最终脚本重跑10组通过，脚本错误0；含全部78页360px横向溢出检查 |
| `node capture.cjs` | 重生成156张桌面/手机截图，脚本错误0、缺图0、横向溢出0；同步刷新概览与8张分组总览 |
| 应用内浏览器 | 桌面切到专辑、切换手机预览并进入歌单，终态显示正常；更新后的预览供用户继续体验 |
| 离线 ZIP | 重新打包，并检查压缩包内关键代码与当前文件一致 |
| Kotlin/Gradle、Android真机、Windows/Linux原生、高刷屏帧率 | 未执行；本次只修改HTML设计演示 |

首轮新增的全部页面图标检查在 `taste` 失败，定位到不存在的 `chart-radar`；修正为 `radar` 后最终10组全部通过。不得把浏览器行为检查或CPU降速视为真实手机流畅度验收。

## 限制与后续

动画只移动内容区，不是共享封面变形，也未实现触摸拖拽翻页。固定毛玻璃播放条仍可能在低端设备增加合成成本，需要在用户实际设备上继续体验。此次修复已完成原型范围内的交接与回收检查，但不能保证所有设备达到60Hz或120Hz。

## 同步更新的文档

更新 `docs/README.md` 变更索引、`docs/DEVELOPMENT.md` 第11节、方案目录 `PLAN.md` / `README.md` / `design-tokens.json` 与验证报告。历史04号记录保留，当前行为以上述05号记录为准。
