# 2026-10-16 — 官网增加页面渐变并减轻重复资源加载

- 修改日期：2026-10-16（Asia/Shanghai）
- 类型：界面体验 / 性能
- 适用版本：播放器 `0.4.4 / 27`，未升版
- 影响平台：电脑、手机浏览器；独立官网
- 关联需求：用户反馈网页有卡顿感，希望进入和切换页面时有渐变效果

## 需求或问题

原网站页面直接切换，没有过渡。生产静态资源返回 `no-cache`，浏览器重复访问需重新验证；手机展示 PNG 为 621,880 字节。这些是额外传输与等待因素，不能仅靠动画声称所有网络卡顿均已解决。

## 原因与依据

Flask 静态资源默认需重验证；所有请求原本都会初始化或刷新访客会话，不能直接把带会话的响应设为公共长期缓存。网站为服务端多页面，使用浏览器原生跨文档 View Transitions，无需拦截链接、重写表单或引入 SPA。

## 具体修改

| 文件 | 关键符号 | 实际修改 |
| --- | --- | --- |
| [app.py](../../website/app.py) | `static_versions`、`static_version`、`prepare`、`headers` | 启动时计算静态内容指纹，自动加入静态 URL；只有匹配内容指纹的成功静态响应可公共缓存一年。静态请求不初始化/刷新会话，其他动态请求保持 no-store。 |
| [site.css](../../website/static/site.css) | `@view-transition`、`page-enter`、`page-exit` | 首次进入正文 200ms 淡入；支持的同源文档导航 150ms 退出、200ms 进入。只动画 opacity，使用既有 `--ease`，不人为延迟跳转。活动原生过渡时取消正文双重动画，减弱动态偏好保持即时显示。 |
| [home.html](../../website/templates/home.html)、[base.html](../../website/templates/base.html)、[owner_login.html](../../website/templates/owner_login.html) | 展示图、图标和 favicon | 使用离线生成的 WebP 与小 favicon；原 PNG 素材保留。桌面展示 21,436 字节、手机 22,900 字节、无损图标 24,894 字节、favicon 1,318 字节。 |
| [test_site.py](../../website/tests/test_site.py) | 静态缓存回归 | 检查内容指纹变化、缓存/304、已有和新访客均无静态 Set-Cookie、错误指纹不能长期缓存、动态页与作者页仍 no-store。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 站内导航 | 内容直接替换 | 支持的浏览器原生淡入淡出；不支持时正文淡入 |
| 重复加载静态资源 | 每次向服务器验证 | 内容指纹匹配时复用缓存；更新内容使用新 URL |
| 首页三张展示素材 | PNG 合计 785,178 字节 | WebP 合计 69,230 字节，约减少 91%；没有承诺整站速度提升同等比例 |

## 平台与数据影响

不改变帖子、邮箱或安装包数据结构；页面、作者入口和订阅响应不缓存。未添加导航拦截、定时跳转或客户端存储私有入口的逻辑。CSS 与展示图由生产相同静态路径提供，部署后需重载应用以更新指纹。

## 验证

| 操作 | 环境 | 结果 |
| --- | --- | --- |
| `python -m unittest discover -s tests -v` | Windows / Python 3.12 | 通过：33 项，含本需求与作者置顶相关回归。首次两处测试夹具问题已修正，最终完整运行通过。 |
| Pillow 离线转换并核对尺寸/字节数 | 本机 | 通过：手机 540×1200、桌面 1600×860、图标 288×288，原素材保留。 |
| 原生跨页导航、正文样式与图片 | Codex 浏览器 / 隔离 8861 实例 | 通过：pageswap/pagereveal 均捕获原生 ViewTransition，记录淡入 0.2s；首页图片无损坏。诊断脚本仅在忽略的 QA 服务，不部署生产。 |
| 生产源码更新 | 翼龙容器 / 现有面板客户端 | 通过：比对线上旧源码后白名单更新 12 文件，校验实际摘要；源码回滚包 `website-source-before-e401e82558de.tar.gz` 保留在容器根目录、权限 600；未覆盖配置、数据或启动器，随后发送 restart。 |
| 更新后 HTTPS 页面 / 资源 / Range | 生产 leonard.asia | 通过：首页、论坛、下载均 200 / no-store；7 个带指纹静态资源的实际 SHA、immutable 与无 Set-Cookie 均通过，两端 Range 206；安装包仍 v0.4.4。默认 urllib UA 首次获 403，带明确运维标识及真实浏览器重新检查通过，未修改安全设置。证据在忽略的 `build/website/qa/production-ui-update-checks.json`。 |

## 限制与后续

跨文档过渡依赖浏览器支持；不支持时保留普通导航和正文淡入。真实手机设备帧率、弱网压力测试及不同浏览器/系统减弱动态的实机测试尚未执行；减弱动态 CSS 已提供。原生能力依据 [Chrome 官方说明](https://developer.chrome.com/docs/web-platform/view-transitions/cross-document)。未声称所有网络等待已消除，源码备份未包含生产凭据或用户数据。

## 同步更新的文档

[文档索引](../README.md)、[开发说明官网章节](../DEVELOPMENT.md#website)、[官网说明](../../website/README.md)。
