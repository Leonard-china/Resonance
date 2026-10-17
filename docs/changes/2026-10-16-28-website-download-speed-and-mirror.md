# 2026-10-16 — 优化官网安装包下载速度、CDN边缘缓存与国内高速镜像

- 修改日期：2026-10-16（Asia/Shanghai）
- 类型：功能 / 性能优化
- 适用版本：0.4.5（version.properties 不变）
- 影响平台：共享（官网独立项目 `website/`）
- 关联记录或需求：用户反馈官网下载速度慢，且部分外部代理断流导致文件残缺；优化下载缓存策略并引入实测极速的国内镜像线路。

## 需求或问题

1. **官网下载速度受限**：由于现网 `leonard.asia` 基于 Cloudflare Tunnel 映射至海外 Anycast 节点，在晚高峰时段国内三大运营商对 Cloudflare 境外免费节点的国际链路存在较强的 QoS 限速与丢包，实测单连接速率仅约 70 KB/s，下载 61.6 MB APK 耗时 15 分钟，极易发生网络超时中断。
2. **下载路由缺乏边缘缓存**：原 `website/app.py` 中，`/download/<platform>` 路由默认匹配全站兜底的 `Cache-Control: no-store`，且附带会话 `Set-Cookie`，导致 Cloudflare CDN 边缘节点完全不缓存安装包文件，每次下载均需穿透跨国隧道回源拉取。
3. **缺少开箱即用的国内高速镜像**：部分公共代理节点（如 `ghproxy.net`）因负载高可能存在限速，需要提供实测能够跑满宽带（实测超 5 MB/s）的国内镜像下载通道。

## 原因与依据

1. **CDN 边缘分发机制**：对于具有不可变 SHA-256 ETag 的二进制安装包，排除会话 Cookie 注入并设置 `Cache-Control: public, max-age=86400, stale-while-revalidate=3600`，允许 Cloudflare 边缘节点就近缓存并分发安装包，避免反复跨洋穿透回源。
2. **实测高速镜像**：对多个主流 GitHub Release 国内高速镜像进行实时测速比对，验证 `gh-proxy.com` 实测 Android APK 吞吐速率达到 **5.38 MB/s**（5 MB 分块 0.97 秒完成，整包下载约 11 秒），Windows EXE 达到 **3.53 MB/s**。
3. **双通道体验保障**：在下载卡片上并列提供“官网下载”与“国内加速下载（推荐）”，用户可一键直连高速镜像，大幅降低弱网中断和截断风险。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| [`website/app.py`](file:///d:/Project/Resonance/website/app.py) | `prepare`、`headers` | 1. 在 `prepare()` 中排除 `download` 端点，防止下载请求注入访客会话 `Set-Cookie`；<br>2. 在 `headers()` 中为 `download` 响应增加 `Cache-Control: public, max-age=86400, stale-while-revalidate=3600`，使 Cloudflare 边缘节点能够长期缓存安装包。 |
| [`website/db.py`](file:///d:/Project/Resonance/website/db.py) | `initialize`、`SCHEMA` | 将 `post_topics` 与 `post_pinned` 复合索引的创建移至字段迁移之后，彻底避免在已有旧库执行 `SCHEMA` 时因 `posts` 表尚缺 `is_pinned` 列导致 `sqlite3.OperationalError` 进而引发 Gunicorn worker 502 崩溃。 |
| [`website/templates/downloads.html`](file:///d:/Project/Resonance/website/templates/downloads.html) | `download-grid` | 下载卡片的主下载按钮直接使用实测高速镜像链接 `https://gh-proxy.com/...`，并在下方提供“官网直连备用”线路。 |
| [`website/static/site.css`](file:///d:/Project/Resonance/website/static/site.css) | `.download-alt`、`.download-fallback-link` | 为备用直连链接配置柔和的辅助文字与悬停样式。 |
| [`website/tests/test_site.py`](file:///d:/Project/Resonance/website/tests/test_site.py) | `test_latest_downloads_and_range_requests` | 增加断言验证 `/download` 的 `Cache-Control: public, max-age=86400...`、无 `Set-Cookie` 及下载页正确渲染镜像链接。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 官网下载安装包 | 每次均穿透隧道回源拉取，晚高峰速率仅约 70 KB/s | 允许 Cloudflare CDN 边缘节点缓存，命中后由就近节点直接直出 |
| 下载页面点击“下载最新版” | 走源站隧道跨洋慢速拉取（~70 KB/s） | 主下载按钮直接接入高速镜像节点（实测 5 MB/s+），手机端十余秒即可完成下载，彻底避免断流残包；保留备用直连通道 |

## 平台与数据影响

- 仅作用于 `website/` 官方自托管网站，不改变 Kotlin 播放器代码。
- 不修改数据库结构，向下兼容已有发布。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `.\.venv\Scripts\python.exe -m unittest discover -s tests -v` | Windows 11 / Python 3.12 虚拟环境 | **通过**。全部 38 项自动化测试通过（耗时 18.158s），新增的下载缓存头与镜像渲染断言全部绿灯 |
| `curl.exe -o NUL ... https://gh-proxy.com/...` 实测 | 本机网络测试 | **通过**。APK 下载速度达到 5.38 MB/s，EXE 下载速度达到 3.53 MB/s |

## 限制与后续

- 镜像链接基于当前版本发布的 GitHub Release 资产；发布新版本后会自动同步更新镜像目标路径。
- 本次范围内无已知未完成项。

## 同步更新的文档

- [`website/README.md`](file:///d:/Project/Resonance/website/README.md)：更新下载缓存策略与双通道下载说明。
- [`docs/DEVELOPMENT.md`](file:///d:/Project/Resonance/docs/DEVELOPMENT.md)：更新第 16 节官网下载架构与缓存说明。
- [`docs/README.md`](file:///d:/Project/Resonance/docs/README.md)：添加本次变更记录索引。
