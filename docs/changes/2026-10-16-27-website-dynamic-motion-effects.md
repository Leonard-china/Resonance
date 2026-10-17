# 2026-10-16 — 网站新增页面入场与交互动效

- 修改日期：2026-10-16（Asia/Shanghai）
- 类型：功能 / 前端优化
- 适用版本：0.4.5（version.properties 不变）
- 影响平台：共享（官网独立项目 `website/`，电脑端与手机端全适配）
- 关联记录或需求：用户需求“为网站显示时增加动态效果”；承接 [2026-10-16-26 作者后台增强](2026-10-16-26-owner-dashboard-enhancements.md) 与 [2026-10-16-21 官网渐变与静态缓存](2026-10-16-21-site-fades-and-static-cache.md)

## 需求或问题

用户提出为网站显示时增加动态效果。
修改前：
1. 网站各页面主要为静态排版，各模块、卡片与列表元素缺乏平滑的阶梯式入场（Staggered Entrance）动效。
2. 页面元素（如首页四大入口、安装包下载卡片、充电赞助卡片、论坛讨论列表行、公告跳转横幅等）在悬停或激活时缺少灵动的微交互反馈（Micro-interactions）。
3. 顶部固定导航栏在滚动时样式始终不变，缺乏随滚动深度的半透明毛玻璃与阴影浮动反馈。
4. 需要遵循现代 Web 动效最佳实践：仅对合成器属性（`opacity`、`transform`）进行动画处理以避免引起页面重排或掉帧，不引入庞大的第三方 JS 动效库，并严格遵守 `@media (prefers-reduced-motion: reduce)` 无障碍规范。

## 原因与依据

1. **渲染性能与现代规范**：依据现代前端动画指南，页面和卡片过渡必须严格限制在合成器属性（`transform` 和 `opacity`）上，避免触发 Layout/Reflow 重排；利用 CSS 变量 `--ease: cubic-bezier(.23,1,.32,1)` 实现顺滑自然的加减速曲线。
2. **渐进增强（Progressive Enhancement）**：
   - 支持跨文档原生过渡（Cross-document View Transitions）的现代浏览器通过 `@view-transition { navigation: auto; }` 与自定义 `::view-transition-old(root)` / `::view-transition-new(root)` 获得连贯的翻页动效。
   - 支持 CSS 滚动驱动动画（Scroll-driven Animations）的浏览器通过 `@supports (animation-timeline: view())` 实现基于视口滚动进度的渐进显现。
   - 对不支持的环境完全向下兼容，保持普通页面加载。
3. **滚动状态被动监听**：通过原生的 `window.addEventListener('scroll', ..., { passive: true })` 监听轻量滚动位移（`> 8px`），为 `.site-header` 添加 `.scrolled` 类，配合 CSS `backdrop-filter: blur(10px)` 提供现代毛玻璃浮动体验。
4. **无障碍标准**：严格支持 `@media (prefers-reduced-motion: reduce)`，当用户在操作系统中开启减弱动态效果时，强制将所有 `animation`、`transition` 和 `scroll-behavior` 关闭（`none !important` / `auto !important`），确保晕动症或视障用户不受干扰。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| [`website/static/site.css`](file:///d:/Project/Resonance/website/static/site.css) | `@keyframes` / 动效规则集 | 1. 定义关键帧：`page-enter`、`page-exit`、`fade-slide-up`、`fade-scale-in`、`hero-float`；<br>2. 跨文档原生过渡配置：`navigation: auto`、定制根伪元素动画；<br>3. 页面模块入场阶梯动画：首页 Intro (0ms)、Hero 图标 (0ms + 浮动循环)、操作按钮组 (80ms)、界面预览 (140ms)、四大特性卡片 (120ms~240ms 错峰)、下载卡片 (90ms)、更新日志 (0ms)、论坛讨论列表 (80ms)、置顶公告 (0ms)、邮件订阅 (50ms)、赞助卡片 (0ms)、作者看板指标卡 (0ms)；<br>4. 滚动驱动增强：`@supports (animation-timeline: view())` 视口滚动入场；<br>5. 交互微动效：下载/赞助卡片悬停轻微浮起与阴影扩展（`translateY(-3px)` + `box-shadow`）、Hero 图标悬停暂停浮动并高亮、主按钮组悬停微上浮与按压缩放（`scale(0.98)`）、论坛讨论行悬停高亮与箭头右滑（`translateX(3px)`）、公告链接右移；<br>6. 顶部导航栏过渡与 `.scrolled` 毛玻璃阴影；<br>7. 严格的减弱动态无障碍覆盖。 |
| [`website/static/site.js`](file:///d:/Project/Resonance/website/static/site.js) | `updateHeaderScroll` | 添加 `{ passive: true }` 滚动事件监听器，随页面纵向滚动位移动态切换 `.site-header` 的 `scrolled` 类。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 访问或刷新网站首页、下载页、论坛页 | 页面各区块瞬间静态呈现，无层次感 | 标题、图标、入口按钮、卡片依次平滑淡入滑入（错峰延迟 50ms~240ms），Hero 图标带有优雅的缓慢浮动循环 |
| 鼠标悬停下载卡片或赞助卡片 | 仅有边框变化或静态无反馈 | 卡片平滑上浮 3px，同时投射柔和阴影，增强可点击感 |
| 鼠标悬停论坛讨论列表行 | 纯静态文字变色 | 整行背景变为微绿高亮，右侧指示箭头平滑右移 3px |
| 鼠标悬停主入口按钮 | 普通背景变色 | 按钮轻微上浮 2px 并附带阴影，按下时轻微缩放 0.98 提供触觉般按压反馈 |
| 页面向下滚动 | 顶部导航栏始终为普通实色边框 | 滚动超过 8px 时顶部栏平滑过渡为毛玻璃半透明背景（`backdrop-filter: blur(10px)`）并附带柔和阴影 |
| 操作系统开启“减弱动态效果” | 依赖浏览器默认行为 | 立即停用所有过渡、动画与平滑滚动，所有内容瞬时展示 |

## 平台与数据影响

- **平台影响**：仅作用于 `website/` 自托管官方网站；完全不改动 Kotlin/Compose Multiplatform 播放器代码。
- **数据影响**：不改变数据库表结构、会话验证或邮件队列。
- **资源缓存影响**：静态资源指纹自动依据文件内容 SHA-256 生成，`site.css` 与 `site.js` 内容更新后将自动获得新版本哈希（如 `?v=...`），各端浏览器立即获得最新样式，且享有 `Cache-Control: public, max-age=31536000, immutable` 长期缓存。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `.\.venv\Scripts\python.exe -m unittest discover -s tests -v` | Windows 11 / Python 3.12 虚拟环境 | **通过**。全部 38 项自动化测试通过（耗时 16.190s），包含静态资源指纹匹配与变动校验测试、CSRF 防护测试、作者管理测试等 |
| `.\.venv\Scripts\python.exe -m compileall -q app.py db.py releases.py mailing.py manage.py` | Windows 11 / Python 3.12 虚拟环境 | **通过**。Python 核心文件语法与编译检查无报错 |

## 限制与后续

- 动效全部采用纯 CSS 声明与原生浏览器滚动事件驱动，完全无需加载额外 npm/JS 动效包，零额外网络载荷。
- 跨文档视图过渡依赖 Chromium 126+ / Safari 18+ 原生 MPA 特性；非支持浏览器优雅回退到普通的入场关键帧动画。
- 本次范围内无已知未完成项。

## 同步更新的文档

- [`website/README.md`](file:///d:/Project/Resonance/website/README.md)：更新官网动效与交互说明。
- [`docs/DEVELOPMENT.md`](file:///d:/Project/Resonance/docs/DEVELOPMENT.md)：更新第 16 节独立官网功能描述与动效规范。
- [`docs/README.md`](file:///d:/Project/Resonance/docs/README.md)：添加本次变更文档索引。
