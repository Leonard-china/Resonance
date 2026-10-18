# 2026-10-18 — 官网下线 123 云盘备用下载通道并同步更新最新公告

- 修改日期：2026-10-18（Asia/Shanghai）
- 类型：功能 / 体验优化 / 运维配置
- 适用版本：0.5.0（`version.properties`，不自动升版）
- 影响平台：官网（Web）/ 部署工具
- 关联记录或需求：用户明确要求：
  1. 取消官网的 123 云盘下载方式；
  2. 推到 GitHub 和官网（更新公告和下载页面）；
  关联今日前序改动 [2026-10-18-01-page-swipe-lyrics-display-and-recommendation-integrity.md](2026-10-18-01-page-swipe-lyrics-display-and-recommendation-integrity.md) 及历史记录 [2026-10-17-16-integrate-123pan-high-speed-downloads.md](2026-10-17-16-integrate-123pan-high-speed-downloads.md)。

## 需求或问题

1. **下线 123 云盘备用通道**：
   - 官网下载页面原包含 123 云盘备用链接，因维护与分发调整需求，需要将 123 云盘下载入口彻底移除，仅保留 GitHub 高速镜像主通道与官网直连备用通道；
2. **官网公告内容同步**：
   - 针对今日完成的手势门槛降低（支持 fling 轻扫）、歌词两行折行与译文、高亮居中跟随及每日推荐完整性保护优化，需要将官网更新公告同步至最新版本与说明内容；
3. **部署同步与代码开源推送**：
   - 将所有播放器代码改动与官网模板及文档推送到 GitHub 远程仓库（`origin/main`）；
   - 通过生产部署脚本同步至现网容器并更新线上公告与下载页。

## 原因与依据

1. **下载页面模板**：
   - 位于 `website/templates/downloads.html`。移除模板内部定义的 `pan_urls` 字典，并在辅助链接行（`download-alt`）中移除指向 123 云盘的超链接及其前置分隔点，使备用链接仅呈现【官网直连备用】。
2. **公告配置与内容**：
   - 位于 `website/content/0.5.0.json`。增加对最新交互手势（轻扫）、歌词折行居中与译文、歌词居中平滑跟随、每日推荐时长下限与试听音频拦截的说明条目，保持标题与内容在合法字符数边界内。
3. **部署脚本加固**：
   - 位于 `D:\Project\mcServer\deploy_resonance.py`。同步列表纳入 `announcements.py` 与 `content/` 目录，并在远端解压后自动触发 `.venv/bin/python manage.py publish-announcement content/0.5.0.json`，在单一事务内无缝更新现网 SQLite 数据库中的 `announcement_title` 与 `announcement_body`。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| [`website/templates/downloads.html`](file:///d:/Project/Resonance/website/templates/downloads.html) | `download-grid` / `download-alt` | 移除 `pan_urls` 映射与 123 云盘链接，仅保留高速镜像主下载按钮与官网直连备用 |
| [`website/content/0.5.0.json`](file:///d:/Project/Resonance/website/content/0.5.0.json) | `version` / `title` / `body` | 更新公告正文，纳入今日交付的手势轻扫、歌词折行、译文居中跟随及每日推荐完整性等打磨亮点 |
| [`website/OPERATIONS.md`](file:///d:/Project/Resonance/website/OPERATIONS.md) | 运维说明 | 更新现网运维文档，明确 123 云盘下载通道已于 2026-10-18 下线移除 |
| `D:\Project\mcServer\deploy_resonance.py` | `deploy` | 补充 `announcements.py` 与 `content/` 同步项，解压后自动调用 `manage.py publish-announcement` 刷新数据库公告 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 访问官网下载页（`/downloads`） | 辅助行显示 `官网直连备用 · 123云盘备用` | 辅助行仅显示 `官网直连备用`，123 云盘入口彻底移除 |
| 访问官网论坛公告（`/forum#announcement`） | 显示旧版 0.5.0 重磅更新概览 | 显示最新的 2026-10-18 体验优化更新内容（手势、歌词折行译文、居中跟随、推荐完整性） |

## 平台与数据影响

- **平台影响**：
  - 纯官网 Web 页面与远端运维脚本，不改动 Android/Windows 原生客户端代码与二进制打包流程。
- **数据影响**：
  - 更新现网 SQLite 数据库 `settings` 表中的 `announcement_title` 与 `announcement_body`；
  - 不修改任何用户数据、论坛帖子与邮件订阅队列。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `.\website\.venv\Scripts\python.exe -m unittest discover -s website/tests` | Windows 11 / Python 3.12 | **通过**（42 个测试全部通过，17.56s） |
| `python D:\Project\mcServer\deploy_resonance.py` | Windows 11 / Pterodactyl API | **通过**（代码成功打包上传、解压更新、更新公告并重启 Gunicorn 容器） |
| `python -c "from mc_api import PterodactylClient; ..."` | 生产容器文件直接读取 | **通过**（远端 `website/templates/downloads.html` 确认无 123pan） |
| `curl.exe -s "https://leonard.asia/forum"` | 公网生产环境 | **通过**（论坛实时公告已更新为 2026-10-18 最新体验优化内容） |
| `curl.exe -s "https://leonard.asia/downloads?ts=20261018"` | 公网生产环境（透传回源） | **通过**（下载页面辅助链接仅为“官网直连备用”，123 云盘已移除） |

## 限制与后续

- 现网在 Cloudflare 上配置了公开页面 2 小时 Edge 强缓存，已有未带 query 的 `/downloads` 缓存将在 TTL 到期后自动刷新为无 123 云盘的最新页面，透传回源请求已证实完全移除。

## 同步更新的文档

- [docs/README.md](../README.md)（变更索引）。
- [website/OPERATIONS.md](../../website/OPERATIONS.md)（现网运维记录）。
