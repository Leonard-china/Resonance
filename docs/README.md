# 2026-10-17 — Resonance 文档入口与开发变更索引

当前工作区：**0.4.8 / 31**，滑动换页跟手动画、动效流畅度优化与内置三大免配置歌源正式集成。[本次发布说明](RELEASE_NOTES-0.4.8.md) · [新版详细介绍](Resonance-v0.4.8-新版详细介绍.md) · [本次发布记录](changes/2026-10-17-18-release-0.4.8.md)。GitHub 发布见 [v0.4.8 Release](https://github.com/Leonard-china/Resonance/releases/tag/v0.4.8)。实际验证与限制见各单项记录。

## 新 AI 从这里开始

1. 读根目录 [AGENTS.md](../AGENTS.md) 与 [AI 协作流程](AI-WORKFLOW.md)。
2. 根据 [开发说明的任务定位表](DEVELOPMENT.md#gaps) 找相关章节、实现、测试。
3. 阅读影响本任务的下表记录；文档不足时才扩大源码范围。
4. 完成每个独立功能/修复后新增带日期/具体修改的记录，并更新本索引和开发说明。

| 文档 | 用途 |
| --- | --- |
| [详细开发说明](DEVELOPMENT.md) | 当前模块/状态/存储、两端功能、AI工具、截图、推荐清理、翻译/悬浮、构建/排查 |
| [AI 协作流程](AI-WORKFLOW.md) | 最小上下文、实施顺序、文档/验证/交接规则 |
| [变更模板](CHANGE-TEMPLATE.md) | 日期、原因、具体修改、前后/数据、真实验证、限制 |
| [组件/外部服务/凭据](THIRD_PARTY.md) | 新模型、B站、WorkManager/JNA、请求内容与加密/兼容 |
| [项目使用说明](../README.md) | 用户入口与设置/推荐删除语义 |
| [官网运行与 Ubuntu 部署](../website/README.md) | 响应式官网、匿名论坛、下载发布、邮箱订阅、作者后台及备份 |
| [0.4.9 发布说明](RELEASE_NOTES-0.4.9.md) | 歌源协议放开、多候选回退、试听防伪与二级页面黑边消除双端正式版 |
| [0.4.8 发布说明](RELEASE_NOTES-0.4.8.md) | 滑动换页跟手动画、动效流畅度与内置三大免配置歌源双端正式版 |
| [0.4.7 发布说明](RELEASE_NOTES-0.4.7.md) | 歌源生态、推荐反馈、空间管理与手势切页双端正式版 |
| [0.4.6 发布说明](RELEASE_NOTES-0.4.6.md) | 本地截图 OCR 与完整歌名导入的双端正式发布 |
| [0.4.5 发布说明](RELEASE_NOTES-0.4.5.md) | v0.4.5 正式版更新重点、安装包校验与升级指引 |
| [0.4.4 发布说明](RELEASE_NOTES-0.4.4.md) | 历史 0.4.4 Android/Windows 正式版记录 |
| [0.4.3 发布说明](RELEASE_NOTES-0.4.3.md) | 历史 0.4.3 Android/Windows 正式版记录 |
| [0.4.2 发布说明](RELEASE_NOTES-0.4.2.md) | 历史 0.4.2 Android/Windows 正式版记录 |
| [0.4.1 发布说明](RELEASE_NOTES-0.4.1.md) | 历史 0.4.1 Android/Windows 正式版记录 |
| [0.4.0 发布说明](RELEASE_NOTES-0.4.0.md) | 历史 0.4.0 Android 正式版记录 |

## 变更记录（最新在前）

| 修改日期 | 主要修改 | 详细记录 |
| 2026-10-17 | 生产环境配置 QQ 邮箱 SMTP 服务并开放网站邮件订阅 | [具体修改](changes/2026-10-17-22-configure-production-smtp-and-enable-email-subscription.md) |
| 2026-10-17 | 升版 0.4.9、构建并打包 Android Release APK 与 Windows 安装包 | [具体修改](changes/2026-10-17-21-release-0.4.9.md) |
| 2026-10-17 | 修复打开设置等工具二级页面时顶部黑边闪烁问题 | [具体修改](changes/2026-10-17-20-fix-settings-top-edge-flicker.md) |
| 2026-10-17 | 修复歌源网络公网协议限制、接口跳转与多候选智能回退机制 | [具体修改](changes/2026-10-17-19-fix-music-source-network-and-fallback.md) |
| 2026-10-17 | 升版 0.4.8、构建并打包 Android Release APK 与 Windows 安装包 | [具体修改](changes/2026-10-17-18-release-0.4.8.md) |
| 2026-10-17 | 滑动换页跟手动画、动效流畅度优化与内置三大主流歌源 | [具体修改](changes/2026-10-17-17-interactive-page-swipe-animation-and-builtin-music-sources.md) |
| 2026-10-17 | 官网下载页集成 123 云盘备用通道与高速镜像分流优化 | [具体修改](changes/2026-10-17-16-integrate-123pan-high-speed-downloads.md) |
| 2026-10-17 | 升版 0.4.7、构建并打包 Android Release APK 与 Windows 安装包 | [具体修改](changes/2026-10-17-15-release-0.4.7.md) |
| 2026-10-17 | 全链路功能验证、手势与反馈修复及双端Release构建 | [具体修改](changes/2026-10-17-14-verification-gestures-feedback-and-dual-build.md) |
| 2026-10-17 | 关于页内置当前版本发布日期与主要更新 | [具体修改](changes/2026-10-17-13-about-version-highlights.md) |
| 2026-10-17 | 推荐独立点赞、踩和本机评论参与后续选曲 | [具体修改](changes/2026-10-17-12-recommendation-votes-and-comments.md) |
| 2026-10-17 | 主导航、曲库栏目和播放器内容支持左右滑动 | [具体修改](changes/2026-10-17-11-horizontal-page-swipe.md) |
| 2026-10-17 | 生产站开启 Cloudflare 边缘页面缓存加速国内直连访问 | [具体修改](changes/2026-10-17-10-cloudflare-edge-cache-for-public-pages.md) |
| 2026-10-17 | 助手播放导航工具与内置教程帮助 | [具体修改](changes/2026-10-17-09-assistant-tools-local-help.md) |
| 2026-10-17 | 单曲歌词校准持久化与手机播放器/歌词布局 | [具体修改](changes/2026-10-17-08-lyric-offset-player-layout.md) |
| 2026-10-17 | 应用下载空间、清理预览与保留歌单释放音频 | [具体修改](changes/2026-10-17-07-managed-download-storage.md) |
| 2026-10-17 | 推荐歌曲去重、历史冷却与保存到歌单 | [具体修改](changes/2026-10-17-06-recommendation-history-playlists-reasons.md) |
| 2026-10-17 | MusicFree核心歌源、录音室候选及音频/封面溯源 | [具体修改](changes/2026-10-17-05-music-sources-studio-provenance.md) |
| 2026-10-17 | 官网发布对应版本更新公告与详细软件介绍 | [具体修改](changes/2026-10-17-04-release-announcement-and-introduction.md) |
| 2026-10-17 | 发布 v0.4.6 双端安装包并同步 GitHub 和官网 | [具体修改](changes/2026-10-17-03-release-0.4.6.md) |
| 2026-10-17 | 截断歌名核对完整信息后再下载及写入标签 | [具体修改](changes/2026-10-17-02-complete-truncated-song-titles.md) |
| 2026-10-17 | 双端内置本地歌单 OCR，保留云端 API 切换并按任务释放模型 | [具体修改](changes/2026-10-17-01-local-screenshot-ocr.md) |
| 2026-10-16 | 官网引入停留空闲预加载、Speculation Rules 与即时无缝切页 | [具体修改](changes/2026-10-16-29-website-idle-prefetch-and-instant-navigation.md) |
| 2026-10-16 | 优化官网安装包下载速度、CDN边缘缓存与国内高速镜像 | [具体修改](changes/2026-10-16-28-website-download-speed-and-mirror.md) |
| 2026-10-16 | 网站新增页面入场与交互动效、滚动驱动与微交互反馈 | [具体修改](changes/2026-10-16-27-website-dynamic-motion-effects.md) |
| 2026-10-16 | 作者后台新增全站数据看板、话题直接管理、精准删帖与帖子置顶 | [具体修改](changes/2026-10-16-26-owner-dashboard-enhancements.md) |
| 2026-10-16 | 官网论坛公告更新为播放器详细介绍并于首页增设跳转指引 | [具体修改](changes/2026-10-16-25-website-announcement-software-introduction-and-home-callout.md) |
| 2026-10-16 | 关于页新增官网/充电/爱发电/GitHub，发布 v0.4.5 并同步双平台 | [具体修改](changes/2026-10-16-24-about-links-sponsor-and-release-0.4.5.md) |
| 2026-10-16 | 官网下载同步最新双模式教程安装包并更新生产校验和 | [具体修改](changes/2026-10-16-23-website-download-tutorial-release.md) |
| 2026-10-16 | 作者登录后参与论坛，作者回复显示标签并置顶 | [具体修改](changes/2026-10-16-22-author-forum-pinned-replies.md) |
| 2026-10-16 | 官网增加页面淡入淡出、内容指纹缓存与轻量展示图 | [具体修改](changes/2026-10-16-21-site-fades-and-static-cache.md) |
| 2026-10-16 | 核对 leonard.asia 生产部署并记录容器运维方式 | [具体修改](changes/2026-10-16-20-production-site-handoff.md) |
| 2026-10-16 | 新增首次启动快速图文、真实操作教学、设置教程目录与学习进度，并构建两端发布版 | [具体修改](changes/2026-10-16-19-native-onboarding-and-tutorial.md) |
| 2026-10-16 | 升版 0.4.4、构建签名 Android Release APK 与 Windows 安装包并交付 | [具体修改](changes/2026-10-16-18-release-0.4.4.md) |
| 2026-10-16 | 支持 B 站扫码登录与 Android 网页快捷登录自动获取 Cookie | [具体修改](changes/2026-10-16-17-bilibili-qr-and-web-login.md) |
| 2026-10-16 | 升版 0.4.3、构建签名 Android Release APK 与 Windows 安装包并交付 | [具体修改](changes/2026-10-16-16-release-0.4.3.md) |
| 2026-10-16 | 修复 Android 导出歌单因部分音频无封面导致 No album art found 异常中断 | [具体修改](changes/2026-10-16-15-fix-export-playlist-missing-artwork.md) |
| 2026-10-16 | 官网下载同步 v0.4.2，补全作者操作说明与部署交接 | [具体修改](changes/2026-10-16-14-website-latest-release-and-handoff.md) |
| 2026-10-16 | 升版 0.4.2、构建签名 Android Release APK 与 Windows 安装包并交付 | [具体修改](changes/2026-10-16-13-release-0.4.2.md) |
| 2026-10-16 | 修复软键盘与聊天框大空隙并大幅提升歌曲下载速度 | [具体修改](changes/2026-10-16-12-fix-ime-gap-and-download-speed.md) |
| 2026-10-16 | 新增适配手机的独立官网、匿名论坛、最新版下载与私有作者管理 | [具体修改](changes/2026-10-16-11-responsive-website-and-community.md) |
| 2026-10-16 | 升版 0.4.1、构建签名 Android Release APK 与 Windows 安装包并交付 GitHub | [具体修改](changes/2026-10-16-10-release-0.4.1.md) |
| 2026-10-16 | AI助手多会话历史、模型溯源、桌面歌词权限引导与独立关于页面 | [具体修改](changes/2026-10-16-09-assistant-history-and-ui-polish.md) |
| 2026-10-16 | 本轮优化两端发布版构建与验证 | [具体修改](changes/2026-10-16-08-release-build-verification.md) |
| 2026-10-16 | 修复静止歌词并添加按音频时间戳生成同步歌词 | [具体修改](changes/2026-10-16-07-audio-synchronized-lyrics.md) |
| 2026-10-16 | 新增持久任务列表和按失败步骤重试 | [具体修改](changes/2026-10-16-06-durable-music-tasks.md) |
| 2026-10-16 | 为下载歌曲匹配真实专辑封面并缓存及保留视频兜底 | [具体修改](changes/2026-10-16-05-album-artwork.md) |
| 2026-10-16 | 在歌单页固定每日推荐并提供独立推荐页面 | [具体修改](changes/2026-10-16-04-pinned-recommendations.md) |
| 2026-10-16 | 音乐助手进入主导航并隐藏聊天页迷你播放器 | [具体修改](changes/2026-10-16-03-assistant-navigation.md) |
| 2026-10-16 | 合并完整播放器过渡并降低动态背景重组开销 | [具体修改](changes/2026-10-16-02-player-layer-transition.md) |
| 2026-10-16 | 恢复曲库切换动画并保留各栏目的浏览位置 | [具体修改](changes/2026-10-16-01-library-motion-and-scroll.md) |
| 2026-10-15 | 升版 0.4.0、构建签名 Release APK 并交付 GitHub | [具体修改](changes/2026-10-15-13-release-0.4.0-android.md) |
| 2026-10-15 | 深度优化的构建/设备回归、文档和交接 | [具体修改](changes/2026-10-15-12-verification-and-handoff.md) |
| 2026-10-15 | 改进补全匹配与持久化并移除缺失计数横幅 | [具体修改](changes/2026-10-15-11-improve-metadata-enrichment.md) |
| 2026-10-15 | 新增双平台后台悬浮歌词和任意目标语言翻译 | [具体修改](changes/2026-10-15-10-floating-lyrics-and-translation.md) |
| 2026-10-15 | 新增定时推荐、自动下载和临时歌曲清理 | [具体修改](changes/2026-10-15-09-ai-recommendation-lifecycle.md) |
| 2026-10-15 | 新增多模型音乐 Agent 与长截图歌单整理 | [具体修改](changes/2026-10-15-08-multi-model-music-agent.md) |
| 2026-10-15 | 新增 B 站音频导入及自定义歌曲信息 | [具体修改](changes/2026-10-15-07-bilibili-audio-import.md) |
| 2026-10-15 | 消除歌单与歌曲标签重叠绘制造成的撕裂 | [具体修改](changes/2026-10-15-06-smooth-library-tab-switching.md) |
| 2026-10-15 | 修复列表封面回退并限制大图内存 | [具体修改](changes/2026-10-15-05-fix-artwork-fallback.md) |
| 2026-10-15 | 修复目录转换无进度、取消与长时间等待 | [具体修改](changes/2026-10-15-04-conversion-progress-and-cancel.md) |
| 2026-10-15 | 移除设备同步和酷狗歌单链接，保留歌单文件 | [具体修改](changes/2026-10-15-03-remove-device-sync-and-kugou-links.md) |
| 2026-10-15 | 简化自动音量平衡并独立提供使用说明 | [具体修改](changes/2026-10-15-02-simplify-volume-balance.md) |
| 2026-10-15 | 重建开发说明与 AI 协作规则 | [具体修改](changes/2026-10-15-01-rebuild-development-docs.md) |

同一天编号递增，每个独立任务单独留记录；不要覆盖历史文档或只在 README 追加一句“已优化”。

## 旧文档处理

用户要求重建 docs，原有发布日志、提案、报告、截图已移出；可恢复本机备份为忽略目录 build/docs-before-rewrite-2026-10-15/，已提交历史还可查 Git。它们不是当前开发依据。当前能力以开发说明及本次记录为准，特别是已移除的 LAN/二维码/酷狗分享链接和新增 AI/推荐/悬浮歌词。
