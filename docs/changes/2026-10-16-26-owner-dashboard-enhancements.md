# 2026-10-16 — 作者专属管理后台新增数据概览、话题直接管理、精准删帖与帖子置顶功能

- 修改日期：2026-10-16（Asia/Shanghai）
- 类型：功能
- 适用版本：0.4.5（不改变播放器版本号）
- 影响平台：独立官网与匿名论坛（`website/`）
- 关联记录或需求：用户需求（为作者专属界面添加更多功能，比如删除某个帖子，添加某些话题等）

## 需求或问题

此前自托管官网与论坛虽然具备基础的作者后台，但在日常内容管理中存在若干痛点：
1. **话题管理受限**：作者此前只能在后台被动审批访客提交的「话题申请」，无法直接主动创建新话题；已有话题也无法在后台一览其帖子规模，无法安全删除空话题。
2. **删帖定位困难**：此前「留言管理」仅折叠展示最新的 100 篇帖子和 100 条回复，不支持关键词检索；若某篇违规帖子不在最新 100 条内，或作者从用户反馈中拿到帖子 ID / 链接时，无法在后台直接输入 ID / 链接精准删除。
3. **缺乏置顶能力**：论坛无法将重要讨论帖或指引贴置顶，列表仅按最近更新时间排序。
4. **缺乏全局数据指标**：作者后台缺乏核心统计概览，无法一目了然掌握全站帖子量、回复量、话题数、待审申请与确认订阅规模。

## 原因与依据

1. 数据库中 `topics` 表已具备 `slug`、`category`、`name`、`position` 等字段，但之前仅由 `owner_topic` 审核申请插入，缺少直接的新增、删除与列表查询接口；为了保证数据安全性，话题删除前必须检查 `post_count`，若话题下仍有帖子则禁止级联破坏，防止误删用户讨论。
2. 帖子置顶需要在 `posts` 表中增加 `is_pinned`（0 或 1），并在 SQLite 初始化中提供轻量迁移检查（`ALTER TABLE posts ADD COLUMN is_pinned`）；论坛列表查询改为 `ORDER BY p.is_pinned DESC, p.updated_at DESC, p.id DESC`。
3. 精准删帖需要支持解析多种输入格式（纯数字 ID、包含 `/forum/post/<id>` 的相对或绝对链接），并保持原子事务，级联删除帖子所属的所有回复与上传的 WebP 附图。
4. 根据系统安全设计基线（`website/README.md`），所有公共页面绝对不可泄露作者私有地址（`private`），因此作者所有管理操作集中在私有后台面板内完成，同时公共页面通过置顶徽章与排序即时联动生效。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| [`website/db.py`](file:///d:/Project/Resonance/website/db.py) | `SCHEMA`、`initialize` | 在 `posts` 表 schema 中增加 `is_pinned INTEGER NOT NULL DEFAULT 0` 及索引；在 `initialize` 函数中添加已有库的轻量自动列检查与 `ALTER TABLE` 迁移。 |
| [`website/app.py`](file:///d:/Project/Resonance/website/app.py) | `owner_dashboard` | 聚合全站数据统计（`stats`：帖子/回复/话题/待审/订阅/版本），统计话题下帖子数量，并支持按关键词或 ID 检索帖子（`q` 查询参数）。 |
| [`website/app.py`](file:///d:/Project/Resonance/website/app.py) | `owner_topic_create` | 新增作者直接创建话题接口，支持话题名称、分类、自定义/自动 Slug 以及排序权重，防重复名称校验。 |
| [`website/app.py`](file:///d:/Project/Resonance/website/app.py) | `owner_topic_delete` | 新增话题删除接口；检查话题下帖子数，当 `post_count > 0` 时拦截并提示保护，仅允许安全删除空话题。 |
| [`website/app.py`](file:///d:/Project/Resonance/website/app.py) | `owner_topic_edit` | 新增话题信息更新接口，支持修改名称、分类和排序。 |
| [`website/app.py`](file:///d:/Project/Resonance/website/app.py) | `owner_pin_post` | 新增作者置顶/取消置顶帖子接口，一键切换 `is_pinned` 状态并刷新显示。 |
| [`website/app.py`](file:///d:/Project/Resonance/website/app.py) | `owner_delete_post_by_query` | 新增按 ID 或链接直接精准删除帖子的接口，自动正则提取 ID，级联删除关联回复与附图。 |
| [`website/app.py`](file:///d:/Project/Resonance/website/app.py) | `owner_delete_reply_by_query` | 新增按回复 ID 直接精准删除单条回复的接口。 |
| [`website/app.py`](file:///d:/Project/Resonance/website/app.py) | `forum_data` | 论坛帖子列表排序加入 `p.is_pinned DESC`，使置顶贴优先排在顶部。 |
| [`website/templates/owner.html`](file:///d:/Project/Resonance/website/templates/owner.html) | `owner.html` | 重构作者专属管理界面：顶部新增数据概览卡片、话题直接新建/已有话题表格/空话题删除、快速精准删帖/删回复卡片、帖子搜索栏、帖子置顶与删除操作。 |
| [`website/templates/forum.html`](file:///d:/Project/Resonance/website/templates/forum.html) | `forum.html` | 帖子列表中为置顶帖显示醒目的「置顶」徽章。 |
| [`website/templates/post.html`](file:///d:/Project/Resonance/website/templates/post.html) | `post.html` | 帖子详情页标题旁为置顶帖展示「置顶」徽章。 |
| [`website/static/site.css`](file:///d:/Project/Resonance/website/static/site.css) | `.owner-stats-grid`、`.quick-delete-card`、`.management-table`、`.pinned-post-badge` 等 | 新增数据概览卡片、话题表格、精准删除卡片与移动端响应式样式（大屏 6 列，中屏 3 列，小屏 2 列）。 |
| [`website/tests/test_site.py`](file:///d:/Project/Resonance/website/tests/test_site.py) | `SiteTest` | 新增 4 项全链路测试：直接新建话题与空话题删除保护、精准 ID/链接删帖与删回复、帖子置顶与排序测试、后台数据看板与关键词搜索测试。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 作者想添加新话题 | 只能等待用户在 `/forum/request-topic` 提交申请后由作者审核批准 | 作者在后台「话题管理」中直接填写名称、分类与排序，一键创建新话题并向论坛开放 |
| 作者想删除话题 | 无删除接口 | 后台提供已有话题列表与帖子计数；若无帖子可安全删除，若有帖子则拦截并提示保护 |
| 删除不在前 100 条的违规帖 | 无法删除（后台仅显示最新 100 篇帖子折叠项，且无搜索） | 作者在「快速精准删除」输入帖子 ID 或链接直接删除；也可在搜索框输入关键词筛选后删除 |
| 重要讨论或公告帖子置顶 | 论坛帖子只能按更新时间自然排序，无法固定置顶 | 作者在后台帖子列表点击「置顶」，该帖即时置于论坛和话题最上方并带有置顶徽章 |
| 作者查看站点运营状况 | 只有邮件部分有简单订阅统计，看不到论坛帖子/回复总量 | 后台顶部常驻 6 项指标看板：论坛帖子、全部回复、开放话题、待审申请、确认订阅与当前版本 |

## 平台与数据影响

- **数据模型**：`posts` 表增加 `is_pinned INTEGER NOT NULL DEFAULT 0` 字段与对应索引。现有数据库通过 `initialize` 自动执行 `ALTER TABLE` 升级，平滑兼容旧数据。
- **公共安全**：公共前端页面依然严守安全规范，绝对不输出作者私有地址（`private`），所有管理操作必须通过带 CSRF 校验的作者会话在私有后台执行。
- **文件与附图**：精准删除帖子时严格复用 `remove_images` 事务清理机制，彻底清除帖子附图及关联的所有回复附图。
- **两端应用影响**：不涉及 Kotlin 跨平台播放器端，不改变应用版本号。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `.\.venv\Scripts\python.exe -m unittest discover -s tests -v` | Windows 11 (Python 3.12 虚拟环境) | **通过**：全部 38 项单元测试顺利通过（耗时 15.8 秒），覆盖新增的话题创建/删除保护、精准删帖/删回复、置顶排序以及搜索看板。 |
| `.\.venv\Scripts\python.exe -m compileall -q app.py db.py releases.py mailing.py manage.py` | Windows 11 (Python 3.12 虚拟环境) | **通过**：所有核心 Python 脚本编译无语法或导入错误。 |
| 移动端与桌面端 CSS 响应式核对 | CSS 样式定义检查 | **通过**：指标网格在不同屏幕下自适应断点（`>720px` 为 6 列，`<=720px` 为 3 列，`<=370px` 为 2 列），表格支持横向溢出滚动。 |

## 限制与后续

本次范围内无已知未完成项。若后续需要在作者登录态下进一步实现前台即时悬浮管理，可探索通过单独设置加密 session capability token 的方案，在不泄露作者私有真实路径的前提下进行交互。

## 同步更新的文档

- 更新 [`website/README.md`](file:///d:/Project/Resonance/website/README.md) 的特性清单与作者管理日常操作说明。
- 更新 [`docs/DEVELOPMENT.md`](file:///d:/Project/Resonance/docs/DEVELOPMENT.md) 第 16 节独立官网与论坛的作者后台功能描述与核对记录。
- 更新 [`docs/README.md`](file:///d:/Project/Resonance/docs/README.md) 的最新变更索引。
