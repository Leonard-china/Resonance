# 2026-10-17 — 生产环境配置 QQ 邮箱 SMTP 服务并开放网站邮件订阅

- 修改日期：2026-10-17（Asia/Shanghai）
- 类型：功能 / 生产环境配置与运维
- 适用版本：0.4.9（version.properties 不变）
- 影响平台：共享（官网项目 `website/`，生产环境 `https://leonard.asia`）
- 关联记录或需求：用户反馈官网论坛订阅区域显示“邮件订阅暂未开放”，输入框与按钮禁用。用户提供了发件邮箱（QQ 邮箱）及 SMTP 授权码。按系统安全设计，配置生产容器 `website/.env`，重启守护进程使 `mailing.mail_ready(app)` 生效，并验证端到端可用性。真实授权码与敏感信息绝不提交至代码仓库，符合 `AGENTS.md` 凭据隔离规则。

## 需求或问题

1. **邮件订阅未开放现象**：
   - 访客在 `https://leonard.asia/forum` 看到订阅卡片显示文字“邮件订阅暂未开放。”；
   - 邮箱输入框 `<input id="subscribe-email">` 与提交按钮 `<button class="button primary">` 均带有 `disabled` 属性，无法点击与输入；
   - 接口 `/subscribe` 在服务未就绪时直接拒绝（HTTP 503）。
2. **原因定位**：
   - 网站采用双重确认订阅（Double Opt-In）机制，访客输入邮箱后必须由系统向其投递带有 48 小时加密验证链接的确认邮件；
   - 后端函数 `mailing.mail_ready(app)` 强依赖 `app.config['SMTP_HOST']` 与 `app.config['MAIL_FROM']`；
   - 生产环境此前未配置 SMTP 发信参数，各项环境变量均为空，系统因此主动关闭订阅入口以防访客输入后无法收到确认信。

## 原因与依据

1. **SMTP 发信测试通过**：
   - 使用 Python 真实连接 `smtp.qq.com:465`（SSL/TLS，`ssl.create_default_context()`），经由发件邮箱认证通过，并成功投递测试邮件（`SEND SUCCESS`）。
2. **安全凭据隔离**：
   - 代码仓库中仅保留公开模板 [`website/.env.example`](file:///d:/Project/Resonance/website/.env.example)；
   - 真实 SMTP 配置（主机、端口、账号、授权码）直接写入远端容器 `/home/container/website/.env`；
   - 遵照 `AGENTS.md` 规则，文档与代码均对授权码做脱敏处理，不向公共仓库泄露凭据。
3. **守护进程重载**：
   - 容器由 Java 启动器守护 Gunicorn 与 `manage.py worker`；控制台发送 `restart` 后，Web 服务与后台邮件队列消费者同步重载最新的 `.env` 环境变量。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| 远端容器 `/home/container/website/.env` | `SMTP_*` / `MAIL_*` | 配置 `SMTP_HOST=smtp.qq.com`、`SMTP_PORT=465`、`SMTP_SECURITY=tls`、`SMTP_USER`、`SMTP_PASSWORD`、`MAIL_FROM` 与 `MAIL_TEST_TO`。 |
| [`website/OPERATIONS.md`](file:///d:/Project/Resonance/website/OPERATIONS.md) | 现网运维说明 | 更新邮件服务配置状态与验证记录，说明 QQ 邮箱 465 端口 SSL 发信与 double opt-in 队列工作模式。 |
| [`docs/DEVELOPMENT.md`](file:///d:/Project/Resonance/docs/DEVELOPMENT.md) | 官网服务与核对记录 | 更新 SMTP 邮件生产服务已接通且自动化测试 42 项全通的核对状态。 |
| [`docs/README.md`](file:///d:/Project/Resonance/docs/README.md) | 变更记录索引 | 登记本次变更记录（2026-10-17-22）。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 访问 `https://leonard.asia/forum` 订阅区 | 提示“邮件订阅暂未开放。”，输入框和按钮 `disabled` | 提示“提交后需要到邮箱确认。邮箱不会显示在论坛。”，输入框与按钮处于激活可输入状态 |
| 访客提交订阅表单（POST `/subscribe`） | 返回 503 错误（邮件订阅暂未开放） | 生成安全 nonce 与加密确认链接，写入 `outbox` 待发队列，返回“请查看邮箱”确认页面 |
| 后台邮件队列守护进程（`manage.py worker`） | 检测到 `mail_ready=False`，跳过邮件出队 | 轮询提取待发邮件并通过 QQ 邮箱 `smtp.qq.com:465` 自动送达收件人 |
| 作者后台邮件管理 | 发送与预览按钮不可用 | 支持向作者测试邮箱（`MAIL_TEST_TO`）发送测试样件及向已确认用户群发新版本更新通知 |

## 平台与数据影响

- **Kotlin 客户端**：无影响，不改动 Android / Windows 播放器端代码与配置。
- **官网数据库**：现有论坛帖子、话题、用户及 `subscribers` / `outbox` 表结构无缝兼容，不修改 SQLite 库结构。
- **环境凭据**：生产环境已保存完整 SMTP 环境变量；本地仓库代码与 Git 记录均无敏感密码。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `python` 脚本直接握手并发送测试邮件 | 本地 Windows 11 / `smtp.qq.com:465` (SSL) | **通过**。成功认证并完成测试邮件发送（`SEND SUCCESS`） |
| `PterodactylClient.write_file` 写入远端 `.env` 并发送 `restart` | 远端 Pterodactyl 容器（Ubuntu 22.04） | **通过**。文件写入成功，控制台重启完成，容器状态 `running`（CPU 0.42%，RAM 524MB） |
| `curl.exe -s -A 'Mozilla/5.0' https://leonard.asia/forum` | 生产线上公网环境 | **通过**。响应中 `<input id="subscribe-email">` 与 `<button class="button primary">` 均无 `disabled`，状态提示为“提交后需要到邮箱确认。邮箱不会显示在论坛。” |
| `.\website\.venv\Scripts\python.exe -m unittest discover -s website/tests -v` | 本地测试环境 | **通过**。全部 42 项测试通过（耗时 15.44s），覆盖订阅流程、double opt-in 验证、退订与邮件队列重试 |

## 限制与后续

- QQ 邮箱个人免费账户受腾讯官方发信频率与每日配额限制（通常个人账号每日上限数百封），若后续订阅者规模增长或有批量高频群发需求，建议迁移至专业商业邮件推送服务（如阿里云邮件推送 DirectMail 等）。
- 本次范围内无已知未完成项。

## 同步更新的文档

- [`website/OPERATIONS.md`](file:///d:/Project/Resonance/website/OPERATIONS.md)
- [`docs/DEVELOPMENT.md`](file:///d:/Project/Resonance/docs/DEVELOPMENT.md)
- [`docs/README.md`](file:///d:/Project/Resonance/docs/README.md)
