# 2026-10-16 — 官网论坛公告更新为播放器详细介绍并于首页增设跳转指引

- 修改日期：2026-10-16（Asia/Shanghai）
- 类型：功能 / 文档
- 适用版本：0.4.5（版本号未改变）
- 影响平台：官网服务（跨平台响应式 Web）
- 关联记录或需求：用户需求“把公告改成对这个软件的详细介绍，并在首页显示‘详细介绍在“论坛公告”（点击就能跳转）’”；关联 [2026-10-16-24-about-links-sponsor-and-release-0.4.5.md](2026-10-16-24-about-links-sponsor-and-release-0.4.5.md)

## 需求或问题

1. 官网论坛顶部公告原为初始测试文本“欢迎来到 Resonance 论坛 / 软件问题和功能建议可以发在这里。”，未能向访问官网的访客和新用户清晰展示 Resonance 播放器的核心理念、主要功能与特色。
2. 官网首页英雄区仅有简短标语与主要功能卡片，缺少直接导向播放器全貌详细介绍的醒目链接；用户希望在首页能直接看到“详细介绍在'论坛公告'（点击就能跳转）”并一键直达对应公告区域。

## 原因与依据

1. 论坛公告支持作者通过后台管理或数据库 `settings` 表动态更新；系统初始化默认值由 [`website/db.py`](../../website/db.py) 中的 `initialize()` 写入。为确保新部署和既有站点均具备完整介绍，需同步更新默认值常量及生产环境数据库。
2. 论坛模板 [`website/templates/forum.html`](../../website/templates/forum.html) 中的公告区块 `<section class="announcement">` 之前未设置 `id="announcement"`，导致锚点跳转无法精准滚动至公告可视区域。
3. 首页 [`website/templates/home.html`](../../website/templates/home.html) 在英雄副标题与四大核心按钮之间适合放置柔和、贴合设计语言的通知提示胶囊，包含放大镜/喇叭图标与跳转箭头，点击整条提示均可直达 `/forum#announcement`。
4. 原 `.announcement p` 限制了 `max-height: 112px`，在承载结构化多段落详细介绍时可视窗口过小，需优化排版行高与最大展开高度，并增加 `:target` 聚焦高亮反馈。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| [`website/db.py`](../../website/db.py) | `DEFAULT_ANNOUNCEMENT_TITLE`, `DEFAULT_ANNOUNCEMENT_BODY`, `initialize()` | 新增结构化详细软件介绍常量（涵盖本地优先理念、格式曲库、EBU R128 音量平衡、双语歌词、B 站导入、多模型 AI 音乐助手、双模式新手教程等），并在 `initialize()` 中作为默认公告写入。 |
| [`website/templates/forum.html`](../../website/templates/forum.html) | `<section id="announcement">` | 为公告容器添加 `id="announcement"`，使站内锚点与跨页导航均能精准命中公告区域。 |
| [`website/templates/home.html`](../../website/templates/home.html) | `.home-announcement-callout`, `.announcement-banner-link` | 在首页英雄区版本号下方添加公告胶囊链接，展示“详细介绍在“论坛公告”（点击就能跳转）”，支持一键跳转 `/forum#announcement`。 |
| [`website/static/site.css`](../../website/static/site.css) | `.home-announcement-callout`, `.announcement-banner-link`, `.announcement:target`, `.announcement p` | 添加胶囊按钮响应式样式、悬停过渡效果；为 `:target` 状态添加绿光轮廓与微弱外发光阴影；优化公告段落行高（1.8）、字号（13px/12px）与高度上限（480px/400px）。 |
| [`website/tests/test_site.py`](../../website/tests/test_site.py) | `test_home_and_forum_announcement_link_and_content` | 新增自动化测试用例，断言首页包含 `/forum#announcement` 链接与指引文字，以及论坛页包含 `id="announcement"` 与详细介绍文本。 |
| 生产部署脚本 | `deploy_announcement.py` | 向 `leonard.asia` 生产容器同步更新模板、CSS 与 Python 模块，执行 SQLite `settings` 更新并平滑重启 Gunicorn。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 访问首页 `/` | 仅展示标语与四个并列入口按钮，无关于播放器完整定位的直接阅读指引。 | 版本号下方显示醒目的绿色胶囊“详细介绍在“论坛公告”（点击就能跳转）”，视觉协调且点击直达。 |
| 点击首页公告提示 | 无此链接。 | 平滑跳转至 `/forum#announcement`，浏览器精准滚动并高亮显示论坛公告区域。 |
| 访问论坛页 `/forum` | 顶部公告仅有两句简短测试文字，高度限制 112px。 | 顶部公告展示“Resonance 软件详细介绍与核心特色”及 6 大模块详细要点，排版清晰美观，支持舒适滚动浏览。 |

## 平台与数据影响

- **跨平台播放器**：不修改 Kotlin 代码、原生资源与播放器二进制；不影响 Android/Windows 本地逻辑。
- **官网数据库**：更新 `site.sqlite3` 中 `settings` 表的 `announcement_title` 与 `announcement_body` 记录；不改变其他帖子、回复或版本资产数据。
- **网络与缓存**：静态资源由指纹路由管理，模板与动态页面不缓存，线上更新立即对所有访问者生效。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `.\website\.venv\Scripts\python.exe -m unittest discover -s website/tests -v` | Windows 本机 Python 3.12 虚拟环境 | 通过。34 项自动化测试全部通过（包含新增的首页链接与论坛公告断言）。 |
| `python -m compileall -q app.py db.py releases.py mailing.py manage.py` | Windows 本机 Python 3.12 | 通过。无语法错误。 |
| 本地 SQLite 验证 | `website/data/site.sqlite3` | 通过。`settings` 表已成功更新最新详细介绍。 |
| 生产环境部署 | 翼龙容器 + Gunicorn (`leonard.asia`) | 通过。文件传输、远端 SQLite 更新与容器重启顺利完成。 |
| 生产站点在线断言验证 | `https://leonard.asia` & `https://leonard.asia/forum` | 通过。实测首页返回 200 且包含 `/forum#announcement` 与提示文字；论坛页返回 200 且包含 `id="announcement"` 与详细介绍内容。 |

## 限制与后续

本次范围内无已知未完成项。作者后续可通过管理后台继续增改公告内容，或直接通过数据库进行修改。

## 同步更新的文档

- [`docs/README.md`](../README.md)：新增本次变更记录条目索引。
