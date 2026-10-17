# 2026-10-17 — 生产站开启 Cloudflare 边缘页面缓存加速国内直连访问

- 修改日期：2026-10-17（Asia/Shanghai）
- 类型：生产配置 / 网络性能优化
- 适用版本：0.4.6（version.properties 不变）
- 影响平台：共享（官网生产环境 `https://leonard.asia`）
- 关联记录或需求：国内网络未开启 VPN 时直连官网页面加载与冷启动极慢，穿透海外隧道频繁丢包超时；在 Cloudflare 边缘网络配置公开展示页面的边缘强缓存，跳过跨洋隧道回源。

## 需求或问题

1. **国内直连首屏冷启动慢**：
   - 现网生产站点部署于海外 Ubuntu 容器，通过 Cloudflare Tunnel 穿透映射至域名 `https://leonard.asia`；
   - 原先由于 `website/app.py` 动态页面响应统一由兜底逻辑附加 `Cache-Control: no-store`，导致 Cloudflare 边缘节点对包括首页在内的所有 HTML 均不予缓存；
   - 国内三大运营商网络在直连境外 Cloudflare 免费 Anycast 节点时，常被解析到美西高延迟节点（物理往返延时 200~350ms），晚高峰期由于跨洋国际出口丢包严重，TCP/TLS 握手及穿越隧道回源耗时长达 2~4 秒甚至频繁超时。

2. **动态与静态页面需求分化**：
   - 论坛（`/forum`）需要实时发帖、实时展示作者回复及携带 CSRF 会话 Cookie，必须严格保持 `no-store`；
   - 首页（`/`）、下载页（`/downloads`）与关于赞助页（`/support`）为高频访问的纯展示型公开页面，除版本发布外极少变更，极其适合利用 CDN 边缘就近分发。

## 原因与依据

1. **Cloudflare Cache Rules 边缘覆盖**：
   - 在 Cloudflare 控制台新建针对公开路径 `http.request.uri.path in {"/" "/downloads" "/support"}` 的缓存规则；
   - 将缓存状态设为 `Eligible for cache`，并设置 `Edge TTL` 为 `Ignore cache-control header and use this TTL: 2 hours`（或 4 小时）；
   - 这样不仅绕过源站的 `no-store` 限制，而且让 Cloudflare 边缘全球节点（含国内访客命中的亚太 Anycast 节点）在首次拉取后直接将 HTML 驻留边缘内存，后续任意国内访客访问直接秒级吐出（`cf-cache-status: HIT`）。

2. **安全与隔离边界保障**：
   - 规则仅精确匹配 `/`、`/downloads` 与 `/support`；
   - 论坛（`/forum`、`/forum/topic/*`、`/forum/new`）、作者后台（`/owner*`）以及邮件订阅接口完全排除在该规则之外，维持原生动态处理与会话隔离。

## 具体修改

| 配置位置 / 文件 | 关键规则 / 属性 | 具体设置与目的 |
| --- | --- | --- |
| Cloudflare 控制台（`leonard.asia`） | **Caching → Cache Rules** | 新增规则 `Cache Public Pages`：<br>1. 匹配表达式：`http.request.uri.path in {"/" "/downloads" "/support"}`；<br>2. 缓存资格：`Eligible for cache`；<br>3. Edge TTL：`Ignore cache-control header and use this TTL` 设置为 `2 hours`（Cloudflare 响应 `Cache-Control: max-age=14400`）。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 国内网络不开 VPN 首次打开官网首页（`https://leonard.asia/`） | 请求穿透跨洋隧道回源容器，晚高峰白屏转圈 2~4 秒甚至超时 | 命中 Cloudflare 边缘节点缓存（`cf-cache-status: HIT`），直接由就近 Anycast 节点吐出，首屏时间降至数百毫秒内 |
| 访问下载页（`https://leonard.asia/downloads`） | 穿透隧道回源拉取 HTML 页面（~2s） | 命中边缘缓存（`cf-cache-status: HIT`），配合页面内高速镜像一键极速下载 |
| 访问论坛（`https://leonard.asia/forum`） | 原生动态回源响应，`Cache-Control: no-store` | 保持不变，严格排除在缓存规则外（`cf-cache-status: DYNAMIC`），保证发帖和回复实时性 |

## 平台与数据影响

- 仅作用于 Cloudflare CDN 边缘网络规则，无需重启服务器或修改数据库，向下兼容所有客户端。
- 不影响论坛数据、作者后台管理与邮件订阅功能。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| Cloudflare 控制台检查 | Edge 浏览器窗口 | **通过**。Cache Rules 列表中 `Cache Public Pages` 规则已启用（状态为绿色 `Active`） |
| `curl.exe -I https://leonard.asia/`（连续2次） | 本机 Windows 终端直连公网 | **通过**。第 1 次返回 `cf-cache-status: MISS`，第 2 次返回 `cf-cache-status: HIT`，`Age: 6`，`Cache-Control: max-age=14400` |
| `curl.exe -I https://leonard.asia/downloads`（连续2次） | 本机 Windows 终端直连公网 | **通过**。第 2 次返回 `cf-cache-status: HIT`，`Age: 5` |
| `curl.exe -I https://leonard.asia/forum` | 本机 Windows 终端直连公网 | **通过**。返回 `cf-cache-status: DYNAMIC`，`Cache-Control: no-store`，动态接口完全正常隔离 |

## 限制与后续

- 当发布新版本播放器或修改了首页内容后，若需立即让全网访客看到最新页面，可前往 Cloudflare 控制台的 **Caching → Configuration → Purge Everything（清除所有缓存）** 或按 URL 单独清除。
- 边缘缓存默认 TTL 为 2~4 小时，过期后 Cloudflare 会在下一次请求时自动异步回源拉取最新页面。

## 同步更新的文档

- [`website/OPERATIONS.md`](file:///d:/Project/Resonance/website/OPERATIONS.md)：补充 Cloudflare 边缘页面缓存配置与发布后缓存清除指引。
- [`docs/DEVELOPMENT.md`](file:///d:/Project/Resonance/docs/DEVELOPMENT.md)：在第 16 节官网架构中补充边缘页面缓存规则。
- [`docs/README.md`](file:///d:/Project/Resonance/docs/README.md)：增加本次变更记录索引（2026-10-17-10）。
