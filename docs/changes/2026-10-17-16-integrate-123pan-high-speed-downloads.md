# 2026-10-17 — 官网下载页集成 123 云盘备用下载通道与高速镜像分流优化

- 修改日期：2026-10-17（Asia/Shanghai）
- 类型：功能 / 用户体验与性能优化
- 适用版本：0.4.6（version.properties 不变）
- 影响平台：共享（官网项目 `website/`，生产站 `https://leonard.asia`）
- 关联记录或需求：v0.4.6 内置本地 OCR 模型后安装包体积增至 163MB（APK）与 317MB（EXE），国内直连境外 GitHub 镜像受运营商国际出口 QoS 限速仅数百 KB/s，且单机国际出口占满导致同时浏览官网卡死；调研并集成 123 云盘大文件分享通道。针对 123 云盘对超过 100MB 文件普通用户需支付 1 元流量费的限制，明确体验分级：主按钮保持 100% 免费的高速镜像下载，123 云盘作为辅助备用链接并排在最后。

## 需求或问题

1. **安装包体积增大后的下载限速与卡顿**：
   - 0.4.6 双端内置本地 OCR 模型后，APK 体积增至 162.9MB，Windows EXE 增至 305.9MB；
   - 原绿色主下载按钮走 GitHub 镜像分发（`gh-proxy.com`），由于中转节点在境外，国内部分运营商对家庭宽带境外流量施加单机 QoS 队列限速（通常封顶数百 KB/s）；
   - 在下载进行的数分钟内，由于国际出口总配额被单一长连接占满，发向 Cloudflare 境外节点的官网网页请求可能在运营商网关排队丢包，导致用户“只要一点下载，再点开网页就特别卡，下载完后恢复正常”。

2. **123 云盘流量限制与用户决策**：
   - 尝试将 123 云盘作为主通道时，实测发现 123 云盘对 100MB 以上大文件在未登录或非 VIP 用户点击时强制提示“需支付 1 元流量费”；
   - 商业网盘强制收费严重损害访客体验，绝不能作为默认主推荐；用户明确要求将其从主按钮撤下，作为备选下载项排在辅助行末尾。

## 原因与依据

1. **主下载通道纯净、免登录、零门槛**：
   - **主通道（绿色高亮按钮）**：保持为【下载最新版】，采用 `https://gh-proxy.com/...` 高速镜像直链，完全免登录、零费用、即点即下；
   - 保证绝大多数用户第一眼看到并点击的主按钮无任何阻碍与付费门槛。

2. **多线路备用与容灾**：
   - **辅助链接行（`download-alt`）**：并列提供【官网直连备用】与【123云盘备用】；
   - 123 云盘排在最后，作为国内 BGP 节点极速备用（适合拥有 123 云盘会员或愿意免翻墙极速下载的用户）；
   - 官网直连走源站 Gunicorn 直出，增加 `target="_blank"` 属性避免中断当前页面的无缝浏览。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| [`website/templates/downloads.html`](file:///d:/Project/Resonance/website/templates/downloads.html) | `download-grid` | 1. 恢复主按钮 `button primary` 为高速免费镜像直链 `mirror_url`；<br>2. 增加 `pan_urls` 字典映射 Android 与 Windows 的 123 云盘分享；<br>3. 辅助行并列渲染【官网直连备用】与【123云盘备用】，123 云盘排在最后；<br>4. 所有下载链接均添加 `target="_blank"` 和安全属性。 |
| [`website/OPERATIONS.md`](file:///d:/Project/Resonance/website/OPERATIONS.md) | 运维说明 | 记录下载页 123 云盘链接维护、Cloudflare 边缘缓存刷新流程与容器部署验证。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 点击主按钮“下载最新版” | 尝试 123 云盘时弹出 1 元付费提示 | 恢复为 100% 免费、免登录的 GitHub 镜像直链（`gh-proxy.com`），即点即下 |
| 用户处于国内极慢网络且有 123 云盘账号/需求 | 无国内网盘备选 | 可点击卡片下方的“123云盘备用”直接在网盘极速转存或高速下载 |
| 点击官网直连备用 | 在当前标签页直接加载，可能挂起当前页面导航 | 在新标签页打开，不影响当前页面的无缝切页与浏览 |

## 平台与数据影响

- 仅修改 `website/templates/downloads.html` 前端模板，不改变数据库与后端 API，不影响 Kotlin 播放器。
- 向下兼容已有的版本发布流程与自动化测试。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `.\website\.venv\Scripts\python.exe -m unittest discover -s website/tests -v` | Windows 11 / Python 3.12 虚拟环境 | **通过**。全部 42 项自动化测试通过（耗时 24.56s），断言主按钮 mirror_url 匹配 |
| `python D:\Project\mcServer\deploy_resonance.py` | 生产服务器容器同步脚本 | **成功**。远端自动上传模板并热重启 Gunicorn 容器，容器运行稳定，内存占用 438.8 MB / 2048 MB |
| Cloudflare 控制台 Cache Purge | Edge 浏览器 `dash.cloudflare.com` | **成功**。点击 Purge Everything 清理全站边缘节点缓存 |
| `curl.exe -s "https://leonard.asia/downloads"` | 生产环境公网实际探测 | **通过**。页面已更新，主按钮为 `gh-proxy.com` 镜像下载，辅助行为 `官网直连备用 · 123云盘备用` |

## 限制与后续

- 123 云盘分享链接针对 >100MB 资源对免费访客收取 1 元流量费，因此仅作为辅助行末尾备选，不作默认推荐。
- 官网主按钮将持续保持免登录、免费高速镜像。

## 同步更新的文档

- [`website/OPERATIONS.md`](file:///d:/Project/Resonance/website/OPERATIONS.md)：更新下载页 123 云盘链接维护说明。
- [`docs/DEVELOPMENT.md`](file:///d:/Project/Resonance/docs/DEVELOPMENT.md)：在第 16 节官网下载说明中增加 123 云盘作为辅助备用链接。
- [`docs/README.md`](file:///d:/Project/Resonance/docs/README.md)：更新变更记录索引（2026-10-17-16）。

