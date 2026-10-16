# 2026-10-16 — 新增适配手机的官网、匿名论坛、最新版下载与私有作者管理

- 修改日期：2026-10-16（Asia/Shanghai）
- 类型：功能 / 部署配置 / 文档
- 适用版本：播放器 `0.4.1 / 24`，未修改 `version.properties`，未重建或升版播放器
- 影响平台：独立官网（桌面浏览器 / 手机浏览器）；目标部署 Ubuntu
- 关联需求：用户确认浅灰绿、紧凑首页后要求开工，并明确手机适配、匿名论坛、服务器仅保留最新版和可选推广订阅

## 需求或问题

此前仓库只有原生 Android / Windows 播放器，无官网和论坛。用户要求首页保留左侧 Resonance、右侧当前图标、下方下载/论坛/GitHub/充电四个入口；文案直接，不使用宣传口号。官网需要最新版下载与更新说明、旧版 GitHub 入口、无需注册的帖子/回复、分类话题与审批、公告、可选推广邮件，以及公共页面不暴露入口的密码作者页面。

用户服务器约 1 核 / 2 GB / 10 GB，系统 Ubuntu，有域名但尚未提供实际域名与服务器连接信息；微信/支付宝收款码和 SMTP 配置也尚未提供。

## 原因与依据

- 原生播放器代码不能直接承担浏览器页面和论坛/订阅持久化，因此新增隔离的 `website/`，不改 Kotlin 播放、曲库或存储链路。
- 首页设计迭代后确定浅灰绿底、墨绿文字/按钮、收紧首屏空白与真实软件截图；应用当前图标来自桌面资源中的蓝绿音符，品牌素材目录的旧声波图不再使用。
- 最新版必须两端一致，上传不能覆盖到一半就失效；采用独立暂存目录和数据库事务切换，再清理旧文件。邮件需要确认和持久队列，不能把无 SMTP 的请求伪装为已投递。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| [website/app.py](../../website/app.py) | `create_app`、公共路由、`owner_only`、`save_image` | 公共页面/匿名发帖回复/话题申请；随机私有作者入口、密码散列登录、CSRF、限速、8 小时作者会话；截图校验/压缩/去 EXIF，收款码无损保存；公告、审核、删除、发布和邮件管理。 |
| [website/db.py](../../website/db.py) | `initialize`、`transaction`、`rate_limit` | SQLite WAL、外键、连接关闭、串行写事务；保存话题、帖子/回复、审批、发布记录、订阅偏好和邮件队列，限速键使用 IP HMAC。 |
| [website/releases.py](../../website/releases.py) | `publish`、`cleanup`、`safe_remove` | 两端安装包流式写入；校验格式、版本和磁盘余量，实际计算 SHA-256；完整提交新版本后清理旧包，失败保留旧版，拒绝降版；清理边界仅允许受控 UUID 目录，遗留暂存超过 1 小时再清理。 |
| [website/mailing.py](../../website/mailing.py) | `queue_confirmation`、`confirm`、`queue_campaign`、`process_one` | 48 小时确认、邮件偏好绑定、推广主动选择、退订、TLS/STARTTLS、独立投递、发送前复查偏好、持久重试；成功/取消任务清空正文以节省存储。 |
| [website/manage.py](../../website/manage.py) | `setup`、`owner-url`、`import-release`、`worker`、`backup` | 本地设置作者密码/随机入口；从应用唯一版本来源和真实产物导入；队列/清理 worker；SQLite 在线备份。 |
| [website/templates](../../website/templates) | 公共页面与作者模板 | 首页、下载、论坛、帖子详情、发帖、话题申请、订阅/退订、充电和作者管理；没有默认密码、假收款码或伪造论坛帖子。 |
| [website/static/site.css](../../website/static/site.css)、[site.js](../../website/static/site.js) | 响应式布局、预览切换、话题展开 | 小屏入口 2×2、默认展示手机截图、下载单列且 Android 优先、话题列表可展开、管理表单单列；键盘焦点、减弱动态效果、小屏表单 16px 防输入缩放。 |
| [website/static/assets](../../website/static/assets) | 图标和截图 | 复制当前音符图标与已有 UI 测试截图；不读取或复制用户歌曲。截图仅为测试样例，未宣称是本次新拍的最新版。 |
| [website/deploy](../../website/deploy)、[requirements.txt](../../website/requirements.txt) | Nginx、Gunicorn 26.2.0、systemd | 1 worker / 4 threads、网站 512 MB / 邮件 192 MB 限额、仅回环监听、外部 HTTPS 代理、运行数据隔离；不是压力测试结论。 |
| [website/package.py](../../website/package.py)、[README.md](../../website/README.md) | 白名单部署包、Ubuntu 操作说明 | 源码 ZIP 只包括网站源码/模板/静态资源/部署配置/测试；明确排除运行数据库、密钥、真实 `.env` 和安装包；ZIP 文件统一 Unix 0644 权限。 |
| [website/tests/test_site.py](../../website/tests/test_site.py) | `SiteTest` | 28 项功能、持久化与边界回归，使用临时数据库、微型安装包、模拟 SMTP。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 官网访问 | 无官网 | 首页及下载/论坛/充电页面，电脑和手机均可使用 |
| 下载更新 | 只有 GitHub Release | 官网提供一套当前最新版；更新成功后清理旧包，历史下载仅指向 GitHub |
| 用户交流 | 无站内入口 | 无注册匿名帖子/回复，截图、话题申请；审批前申请不公开 |
| 作者管理 | 无 | 服务器终端生成私有入口、设置密码后使用作者管理，公共页面无链接 |
| 邮件 | 无 | 确认后按用户选择发送；推广默认不勾选；每封带退订；未配置 SMTP 时明确关闭 |
| 充电 | 无 | 两个收款码位置及作者上传能力；当前未提供真实图片，显示待添加 |

## 平台与数据影响

- Android / Windows 播放器源代码、曲库、播放快照、同步包与音频文件均未改动；应用版本保持 `0.4.1 / 24`。
- 网站数据只进入独立运行目录，`.env`、密钥/密码散列、真实作者地址、数据库、上传图片与安装包均被忽略并排除源码 ZIP。官网没有访问播放器本机音乐库的接口。
- 最新版导入只复制现有发布产物，不移动或删除原工程 `release/`。实际从 GitHub API 核对当前 `v0.4.1`，并导入现有 EXE / APK；没有声称这些播放器产物是本次官网任务重新构建的。
- 作者删除帖子会连同回复及附图删除；只允许已验证作者执行。匿名访客可发布内容，不能调用作者管理。
- 对外服务仅用户点击的 GitHub 链接和将来作者配置的 SMTP；没有统计追踪、CDN 字体或额外云端账号。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `python -m unittest discover -s tests -v`（在 `website`） | Windows / Python 3.12.14 / Flask 3.1.3 / Pillow 12.3.0 | 通过：28 项；包含大于公共 4 MB 限额的作者上传、失败/低空间保留旧版、降版拒绝、原子切换/清理、范围下载、CSRF/XSS、作者权限、审批、确认/推广筛选/退订、队列重试。SMTP 模拟，不真实投递。 |
| `python -m compileall -q app.py db.py releases.py mailing.py manage.py package.py wsgi.py deploy/gunicorn.conf.py` | Windows / Python | 通过；语法检查不等同于 Gunicorn/Nginx 在 Ubuntu 启动通过。 |
| `python manage.py import-release` | 本机 `version.properties`、`release/0.4.1` 实际产物 | 通过：从唯一版本源读取 0.4.1，导入两端包并重新计算哈希；原产物保留。 |
| 实际 HTTP 下载两端完整包并计算 SHA-256、请求 `Range: bytes=0-15` | 本地 HTTP 服务器 | 通过：APK 63,831,045 字节、EXE 204,185,088 字节，响应哈希与已导入实际文件一致；两端 Range 均为 206。结果在忽略的 `build/website/qa/http-download-checks.json`。 |
| 320 / 390 / 768 / 1440px × 首页、下载、论坛、发帖、话题申请、充电 | Codex 真实浏览器，24 个组合 | 通过：无横向溢出、无损坏图片；另验证手机预览切换、话题展开/选中；结果与截图位于忽略的 `build/website/qa/`。 |
| 手机浏览器匿名发帖/回复、作者登录与公告保存 | 独立 8861 测试实例/临时数据，实际页面操作 | 通过：保存后跳转帖子、显示 1 条匿名回复；作者管理在 390px 无横向溢出，公告保存后提示成功；未在交付预览库制造假讨论。 |
| `python website/package.py` | 本机 | 已生成 `build/website/Resonance-website.zip`；源文件白名单打包，运行数据另行保留。 |
| Ubuntu systemd/Gunicorn/Nginx、域名 HTTPS、真实 SMTP/压力测试 | 尚无服务器连接、实际域名和邮件配置 | 未执行；只提供配置与操作说明，不宣称上线或真实投递已通过。 |
| Kotlin / Android / Windows 应用构建和设备测试 | 本次不改原生应用 | 未执行；本轮网站验证不替代原生回归。 |

## 限制与后续

1. 需要用户提供实际域名/服务器连接方式才能远程上线；作者密码在服务器自行通过 `manage.py setup` 设置。当前没有默认作者密码。
2. 需要配置 SMTP 和自己的测试邮箱并实际验证投递；推广只能发给主动选择的已确认用户。SMTP 已接受后进程中断可能导致少量重试重复，不承诺恰好一次送达。
3. 微信、支付宝收款码尚未提供，可通过作者页面添加；当前占位不会导向任何收款账户。
4. 首页为已有测试截图，后续可替换成用户认可的当前版本真实素材。无真实手机设备触摸测试或 Ubuntu 压力测试，浏览器尺寸测试不等同于所有设备验证。
5. 最新安装包更新期间暂存会额外占空间；可用余量检查和失败保留已实现，不能在满盘时强行删除当前可用版本腾空间。后台 worker 需持续运行以重试占用旧包及清理中断暂存。

## 同步更新的文档

- [文档索引](../README.md)：新增官网记录与部署入口。
- [开发说明](../DEVELOPMENT.md)：播放器/官网边界、工程与任务定位表、新增第 16 节。
- [AI 协作流程](../AI-WORKFLOW.md)、[AGENTS.md](../../AGENTS.md)：官网隔离、敏感运行数据与独立验证边界。
- [第三方说明](../THIRD_PARTY.md)：网站依赖、SMTP、数据和素材来源。
- [官网说明](../../website/README.md)：运行、Ubuntu 部署、版本替换、订阅、备份与验证命令。
