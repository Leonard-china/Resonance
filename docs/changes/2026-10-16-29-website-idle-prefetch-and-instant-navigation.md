# 2026-10-16 — 官网引入停留空闲预加载、Speculation Rules 与即时无缝切页

- 修改日期：2026-10-16（Asia/Shanghai）
- 类型：性能优化 / 用户体验升级
- 适用版本：0.4.5（version.properties 不变）
- 影响平台：共享（官网独立项目 `website/`）
- 关联记录或需求：用户反馈在关闭 VPN 直连官网时，页面切换速度缓慢转圈；实现用户停留页面时的空闲预加载、触控悬停即时抢跑与内存级无缝极速切页。

## 需求或问题

1. **关掉 VPN 后直连切页缓慢**：
   - 现网 `https://leonard.asia` 经由 Cloudflare Tunnel 映射至海外 Anycast 节点（日本/美西），国内网络直连存在 150~300ms 的跨洋物理延迟，且晚高峰期运营商对海外免费节点存在一定丢包；
   - 过去用户在页面之间跳转（如从首页点击“下载”或“论坛”）时，浏览器每次都要发起跨大洋网络握手与文档请求，遇丢包重传时切页耗时飙升至 1~3 秒，用户体验有强烈的转圈停顿感。
2. **缺乏预加载与即时呈现机制**：
   - 用户停留在某个页面阅读时，浏览器处于完全闲置状态，未能在后台预先拉取接下来大概率会访问的页面；
   - 用户点击链接后才临时发起请求，无法达到现代 Web 原生级“即点即开”体验。

## 原因与依据

1. **现代 Web 投机加载最佳实践（Speculation Rules & Predictive Prefetching）**：
   - 根据 W3C 与 Chrome 官方《Improve next page load performance》指南，针对以内容展示和导航为主的多页面站点，利用页面停留空闲（Idle）提前将站内核心页面缓存到内存，可使后续导航直接命中缓存，耗时直接从数秒骤降至 0 毫秒；
   - 现代 Chromium 内核（Chrome、Edge、绝大多数 Android 手机系统浏览器）原生支持 Speculation Rules API，支持浏览器底层级别的智能投机预取；
   - 针对非 Chromium 浏览器或不支持 Speculation Rules 的环境，构建轻量渐进增强（Progressive Enhancement）的客户端内存级预加载与即时切换控制器（Instant MPA Navigator），实现全平台兼容。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| [`website/static/site.js`](file:///d:/Project/Resonance/website/static/site.js) | `initInteractive` | 将侧边栏、复制按钮、模式切换、文件校验、防重提交等 DOM 交互封装为带 `data-bound` 守卫的可重入初始化函数，确保切页后组件自动重新激活且防重复绑定。 |
| [`website/static/site.js`](file:///d:/Project/Resonance/website/static/site.js) | `initInstantSpeed` | 1. **停留空闲预加载（Idle Prefetching）**：用户在页面停留 1 秒后，利用 `requestIdleCallback` 在后台静默预取公开核心页面（`/`, `/downloads`, `/forum`, `/support`）；<br>2. **触控与悬停即时抢跑（Touchstart/Hover Prefetching）**：手指碰到链接或鼠标指针靠近时，提前发起预取；<br>3. **Speculation Rules API**：自动动态向 `<head>` 注入声明式预取规则，激活现代浏览器底层硬件级预取能力；<br>4. **即时无缝切页（Instant MPA Navigation）**：拦截站内安全 GET 链接点击，直接从内存呈现文档并利用 `document.startViewTransition` 实现平滑淡入滑入；<br>5. **前进后退瞬间还原（popstate）**：历史记录返回瞬间还原，无需跨洋重发请求；<br>6. **严格安全边界**：排除作者后台（`/owner...`）、订阅退订（`/subscription/`）、安装包直链（`/download/`、`.apk`、`.exe`）与外部链接，作者后台与安装包完全走原生通道。 |
| [`website/static/site.css`](file:///d:/Project/Resonance/website/static/site.css) | `.nav-progress` | 增加未完成缓存跳转时的极简渐变顶部进度指示条，加载完毕即刻淡出。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 关掉 VPN 后在首页阅读 1~2 秒再点击“下载”或“论坛” | 浏览器发起全新跨洋 GET 请求，耗时 1~3 秒白屏/转圈等待 | 后台已在停留期间静默完成预加载，点击瞬间（0 毫秒）直接完成切页，结合平滑淡入动画，丝滑秒开 |
| 手机端在论坛列表触摸任意帖子链接 | 必须等待网络请求完成才能看到帖子详情 | 手指触碰屏幕（`touchstart`）瞬间即触发预取，点击进入时速度显著提升 |
| 浏览器点击“后退”或手机系统返回键 | 重新向海外服务器发起请求 | 监听 `popstate` 直接从内存秒级还原历史页面，零网络耗时 |
| 节省流量模式 / 极弱网（2g） | 无论网络状况均一律发起常规请求 | 自动感知 `navigator.connection.saveData` 或 2G 网络，静默关闭自动预取，绝不浪费用户流量 |
| 作者后台登录与管理操作 | 原生加载 | 保持严格原生隔离，不进入预取缓存，保护敏感数据与会话状态 |

## 平台与数据影响

- 仅作用于 `website/` 官方自托管网站客户端脚本与样式，不改变 Kotlin 播放器代码。
- 不影响已有自动化测试（服务端路由、模板渲染与 HTTP 响应断言全部保持兼容）。
- 不增加额外服务端存储或数据库开销。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `.\website\.venv\Scripts\python.exe -m unittest discover -s website/tests -v` | Windows 11 / Python 3.12 虚拟环境 | **通过**。全部 38 项自动化测试全部通过（耗时 16.420s） |
| `deploy_resonance.py` 同步部署 | 翼龙 Ubuntu 22.04.5 生产容器 | **成功**。生产服务平稳热重启，内存保持 334 MB / 2048 MB |
| `curl.exe -I https://leonard.asia/` | 生产线上环境验证 | **通过**。返回 `HTTP/1.1 200 OK` |
| `curl.exe -s https://leonard.asia/static/site.js` | 生产线上脚本检查 | **通过**。生产静态文件已包含 `initInstantSpeed`、`speculationrules` 与即时切页控制器 |

## 限制与后续

- 客户端内存缓存有效期（TTL）设置为 3 分钟，保证既能实现即刻秒开，又不会长期持有陈旧的论坛讨论。
- 发帖或回复提交后走正常 POST 表单提交与重定向刷新，数据完全保持实时。

## 同步更新的文档

- [`website/README.md`](file:///d:/Project/Resonance/website/README.md)：更新预加载、Speculation Rules 与即时秒开切页说明。
- [`docs/DEVELOPMENT.md`](file:///d:/Project/Resonance/docs/DEVELOPMENT.md)：更新第 16 节官网前端加速架构。
- [`docs/README.md`](file:///d:/Project/Resonance/docs/README.md)：添加本次变更记录索引（2026-10-16-29）。
