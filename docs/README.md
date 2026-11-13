# 2026-11-13 — Resonance 文档入口与开发变更索引

2026-11-13 播放页歌词修复。窄屏或短窗口自动采用较紧凑的歌词字号，让长句和译文更容易完整显示。[变更记录](changes/2026-11-13-01-fit-long-player-lyrics.md)

0.6.9 三端正式版已于 2026-11-06 公开，包含曲库批量重新定位单次提交、截图导入草稿检查点恢复、暂停换曲后恢复选中曲目与语义搜索文字/BPM联合筛选。Android APK、Windows EXE、Linux 便携包和 DEB 的实际校验值见发布记录。[GitHub v0.6.9](https://github.com/Leonard-china/Resonance/releases/tag/v0.6.9) · [官网下载](https://leonard.asia/downloads) · [本轮发布记录](changes/2026-11-06-07-publish-0.6.9-release.md)

2026-11-06 审查修复：[截图导入检查点](changes/2026-11-06-03-screenshot-import-checkpoints.md)、[暂停后恢复选中曲目](changes/2026-11-06-04-resume-selected-track-after-preparation.md)、[文字与 BPM 联合筛选](changes/2026-11-06-05-semantic-search-text-and-bpm.md)、[批量音频路径一次提交](changes/2026-11-06-06-batch-audio-relink-commit.md)。源码、回归与验证边界见[本轮验证](audit/2026-11-06/review-fixes/VALIDATION.md)。

0.6.8 三端正式版已于 2026-11-06 公开，纳入音乐助手的下载指引。Android APK、Windows EXE、Linux 便携包和 DEB 的实际校验值见发布记录。[GitHub v0.6.8](https://github.com/Leonard-china/Resonance/releases/tag/v0.6.8) · [官网下载](https://leonard.asia/downloads) · [发布记录](changes/2026-11-06-02-publish-0.6.8-assistant-release.md)

最新助手能力：[移植 DeepSeek Harness 网页搜索／正文抓取并接入原生 Markdown 回答](changes/2026-11-05-42-harness-web-research-and-native-replies.md)。复用官方 Search／Fetch 路径，当前官方连接按能力开放；可点击来源、可选文字及真实检索步骤使用成熟 Compose 组件和现有任务记录。实际验证、计费及未安装限制见记录。

最新助手修复：[检索进展识别、受阻后的最终回答与错误去重](changes/2026-11-05-41-assistant-search-progress-and-final-answer.md)。新检索结果计入进展；连续空结果／重复失败改为结束工具阶段并只请求一次文字回答；普通播客推荐可直接回答，明确现有搜索能力。具体回归与未安装限制见记录。

最新播放修复：[自动切歌后按钮状态与真实播放同步](changes/2026-11-05-40-auto-next-playback-state.md)。用户已确认“显示播放三角形，音乐继续”；自动切歌读取原生当前曲目及播放快照，音量／响度加载不再重置按钮或误发暂停。本次验证与设备限制见记录。

本轮播放器优化：[歌词校准面板](changes/2026-11-05-34-lyrics-tools-scroll-and-layering.md)、[空闲开销与歌词索引](changes/2026-11-05-35-playback-idle-work-and-lyric-index.md)、[设备失败恢复／后台验证](changes/2026-11-05-36-resume-playback-after-device-failure.md)、[截图草稿恢复](changes/2026-11-05-37-persist-screenshot-import-drafts.md)、[批量音频重新定位](changes/2026-11-05-38-batch-relink-missing-audio.md)、[本地BPM筛选](changes/2026-11-05-39-offline-bpm-analysis-and-filtering.md)。当前源码与本次验证见[统一验收](audit/2026-11-05/player-improvements/VALIDATION.md)，版本保持0.6.6／40，未安装或发布。

最新功能调整：[移除一起听、房间网络与服务器部署依赖](changes/2026-11-05-33-remove-listen-together.md)。Android／Windows／Linux 源码移除房间、邀请、聊天通知及远端播放同步；主导航保留音乐库、音乐助手、工具。本轮验证和未发布限制见记录；下方一起听相关内容仅为历史。

上一版发布：[0.6.6三端正式发布](changes/2026-11-05-29-publish-three-platform-release.md)已完成GitHub公开与官网受控上线，含Android APK、Windows EXE、Linux便携包/DEB及本次实际SHA。[官网介绍/五张截图/下载页](changes/2026-11-05-30-refresh-website-release-pages.md)已更新；公开Range与源站校验通过。普通页面Cloudflare缓存按用户选择等待自然更新，可立即访问[新版首页](https://leonard.asia/?v=0.6.6)及[下载](https://leonard.asia/downloads?v=0.6.6)。最终CI、构建及平台限制见发布记录。

最新修复验收：[本次缺陷修复与重新验收](audit/2026-11-05/FIX-VALIDATION.md)。处理前轮12项问题及手机失败项，本次有效589项：575通过、0失败、14条件跳过；Q07明确BPM尚未接入筛选。完整双端编译及独立手机包验收通过，未升版。各独立原因、代码和限制见下方18—28号记录；[191项功能清单](audit/2026-11-05/FUNCTION-CHECKLIST.md)与[原手机结果](audit/2026-11-05/ANDROID-DEVICE-REPORT.md)保留修复前历史。

最新手机更新：[播放页动效与助手修复正式包覆盖安装](changes/2026-11-05-15-player-animation-phone-install.md)。已同签名保留数据覆盖，设备实际 APK 与本轮完整包逐字节一致；启动、展开、短拉回弹、长拉收起及再次开合实机通过。版本保持 0.6.6 / 40，电脑本轮未更新；验证范围见记录。

当前内存约束：[构建前预留桌面内存](changes/2026-11-05-14-reserve-host-memory-before-builds.md)。共享构建锁；可用内存须覆盖进程树上限并另留 1.5 GiB，运行中余量不足停止自身构建，不关闭用户应用。前轮内存不足时停止了打包；满足门槛后的手机构建/安装以上方 15 号记录为准。

本轮代码与回归：[助手重复请求与下载修复](changes/2026-11-05-12-assistant-repeat-cost-and-download-recovery.md)、[Linux 桌面歌词拖动与解锁](changes/2026-11-05-13-linux-desktop-lyrics-drag-and-unlock.md)。完整双端源码编译、69 项本地回归及两倍缩放三项通过；当轮打包因内存余量不足被拦下。手机后续已通过上方 15 号记录完成更新，电脑尚未覆盖。付费接口、真实公网下载及 Wayland 人工操作边界见记录。

播放页交互改进：[参考 Apple Music UI Demo 的原生展开与收起](changes/2026-11-05-11-apple-music-player-expansion.md)。调整播放条到全屏的衔接和跟手回弹，保留已修复的歌词显示与焦点；原源码验证范围见该记录，后续手机安装与实机开合以上方 15 号记录为准。

前轮手机安装：[重连后完成歌词修复正式包覆盖与启动验收](changes/2026-11-05-10-complete-phone-lyrics-fix-installation.md)。已保留数据覆盖原正式应用，设备 APK 与该轮最终包逐字节核对一致，同一进程连续运行 15 秒且主界面恢复；电脑前轮已更新。版本仍为 0.6.6 / 40，该轮验证范围见记录，当前手机安装以上方 15 号记录为准。

最新本机安装：[歌词修复正式包替换手机与 Linux 电脑](changes/2026-11-05-09-install-lyrics-fix-on-phone-and-linux.md)。Linux 已切换至完整歌词修复版并正常启动；手机安装及启动复核的最终状态以记录为准，保留原数据和回退副本。

最新歌词回归修复：[恢复封面预览、同步歌词优先与当前句高亮](changes/2026-11-05-08-restore-player-lyrics-and-synchronized-priority.md)。封面歌词重新可见，纯文本缓存不再阻断同步版本，沉浸歌词恢复淡底焦点；本次验证和手机安装范围见记录。

最新核心架构：[应用任务、播放会话、平台能力与桌面音频重构](changes/2026-11-05-07-application-core-and-desktop-audio-refactor.md)。已拆分导入/队列/歌词用例与平台职责，统一退出排空和统计快照，重写桌面音频会话边界；保持原生 Kotlin 和数据兼容。共享/桌面 388 通过、8 条件跳过，最终补充 55 项通过；Android 独立包三轮重建/关闭通过，Linux 完整包已安装并从原入口启动，旧安装保留。Windows/长时限制见记录。

前轮窗口修复：[桌面歌词窗口与应用会话分离](changes/2026-11-05-06-desktop-lyrics-window-and-stability-boundaries.md)。已采集本机黑框、窗口状态与音频线程证据；应用/窗口分离与 Linux 歌词不透明回退已通过 25 项定向回归及隔离实屏核对。整工程构建受内存约束、未更新正式安装，具体见记录。[架构与语言评估](ARCHITECTURE-STABILITY.md)区分已实施与后续工作，不将换语言视为稳定性保证。

最新歌单包导入：[修复 Linux `.resonance` 不可见与拖入忽略](changes/2026-11-05-04-linux-resonance-package-import.md)。普通文件选择、递归目录和整窗拖放均接入现有歌单包校验与恢复流程，导入后同步刷新歌单；本次验证及安装状态见记录。

最新桌面运行：[正常退出旧实例并启动最新版](changes/2026-11-05-03-desktop-latest-running-instance.md)。已逐文件核对最新安装、正常退出两个旧实例，并启动 `0.6.6-20261105-am-ui/` 的可见原生窗口；当前桌面/菜单入口与运行程序一致。

最新桌面导入：[多选文件/目录与整窗拖入](changes/2026-11-05-01-desktop-files-and-drop-import.md)。Linux/Windows 共用扫描、转换、取消和入库链路，新增文件管理器拖入提示、路径去重与错误隔离；本次实际验证和系统限制见记录。

最新三端界面：[AM++ 源码/截图参考的原生 UI](changes/2026-11-05-02-am-reference-native-ui.md)。宽屏左侧封面/播放、右侧歌词，手机双胶囊底栏，灰白/玫红主题与白色沉浸控制；保留 Resonance 功能及原生平台。本机入口已更新，后续已正常退出旧实例并打开新版，见上方桌面运行记录。最终桌面354通过/10跳过，Android/Linux构建通过；设备与鼠标拖放边界以记录为准，不宣称完整 Apple Music 宿主源码或逐像素相同着色器。

本机优先更新：[最终导出修复 Linux 电脑安装](changes/2026-11-04-21-install-final-export-fix-on-linux-desktop.md)。用户要求先安装到电脑，使用本次重新构建的完整程序，不复用今晚较早的同版本包；实际结果见记录。

前轮发布准备：[0.6.6 三端 GitHub 与官网发布](changes/2026-11-04-20-publish-0.6.6-three-platforms.md)。用户已授权将导出修复发布到 Android／Windows／Linux；本次重新构建及实际上线状态以记录为准，不复用旧 Windows／Linux 包。

最新导出命名与目录：[按歌单名保存并直接打开导出目录](changes/2026-11-04-19-playlist-names-and-direct-export-directory.md)。单歌单使用简洁歌单名，全库使用「整个音乐库」；重名加序号。Android 入口以明确目录 URI 打开系统文件浏览器，并避免复用其他浏览位置；20 项 JVM／最终 6 项设备回归及 Lint 通过。最终正式包已安装原手机，真实歌单命名、旧包保留和连续三次直达 `Download/Resonance` 实测通过；APK、证据和平台限制见记录。

前轮导出反馈：[导出完成显示保存位置、文件名和下载入口](changes/2026-11-04-18-export-location-feedback.md)。补齐导出中的状态与完成结果弹窗及复制位置；16 项 JVM／4 项原手机 Debug 回归及 Lint 通过。后续重连已安装并完成真实小歌单导出；用户新增直达目录和命名要求，当前行为与最终验证以上方记录 19 为准。

最新导出排查与修复：[Android 导出选项 Haze ANR 实机复现与歌单虚拟化](changes/2026-11-04-17-export-dialog-anr-and-lazy-scopes.md)。原手机较早 0.6.5 包打开选项即触发输入超时，主线程栈指向 Haze 状态通知；同机隔离包恢复响应。本次补齐有界懒加载，13 项 JVM／3 项原生弹窗回归及 Lint 通过；最终正式包已保留数据覆盖安装到原手机，真实曲库连续五轮打开／选择／取消通过，无新 ANR。未在手机实际打包用户音频；独立 APK 和验证范围见记录。

最新发布准备：[0.6.6 三端发布与本机桌面更新](changes/2026-11-04-16-release-0.6.6-three-platforms.md)。本次按用户发布要求显式设为 0.6.6 / 40，准备 Android、Linux 与原生 Windows 构建；实际安装、GitHub 推送和 Release 状态以该记录为准。

最新音量弹窗修复：[隔离播放设置弹窗的主窗口采样](changes/2026-11-04-15-volume-dialog-window-isolation.md)。通用 `FrostedAlertDialog` 的表面及内容不再继承主窗口 Haze，使用静态主题渐变；对应回归与 APK、本次实机验证限制见记录。

最新 UI 视觉与 Release：[纯正毛玻璃透光调优、剔除残留红色与原生平整播放按键安装包](changes/2026-11-04-14-frosted-glass-palette-and-native-controls.md)。全屏背景去饱和度限制解除（0.35f→0.65f）且遮罩下调（0.86f→0.58f），彻底剔除残留红色并对接天气冰蓝/矿物绿；大播放键告别 AI 概念图的伪 3D 玻璃球与光晕，收敛至 58dp 平整实心哑光圆钮与纯白矢量三角；全量 UI 31 项测试全通过；Release APK 已构建并完成 v2 签名校验。

最新 Android Release：[本轮歌词与歌源修复安装包](changes/2026-11-04-13-lyrics-and-sources-release-apk.md)，0.6.5 / 39，159.65 MiB；构建、v1／v2 签名、正式包信息、ZIP 对齐和五个内置歌源资源检查通过。仅本地构建，未安装手机或公开上传；独立 APK 与实际校验链接见记录。

最新实现：[歌词查找与空态布局](changes/2026-11-04-09-lyrics-source-recovery-and-compact-tools.md)、[音效弹窗隔离](changes/2026-11-04-10-audio-effect-dialog-window-isolation.md)、[收藏仅显示数量](changes/2026-11-04-11-favorites-count-only.md)、[增加 QQ／独立音乐歌源与 VPN DNS 兼容](changes/2026-11-04-12-more-builtin-sources-and-vpn-dns.md)。版本仍为 0.6.5 / 39；全量桌面 337 通过／9 跳过，最终针对性 33 项全通过，Android Debug／测试 APK 与 Lint 通过；[验证和原生截图](audit/2026-11-04/follow-up/VALIDATION.md)明确记录手机断开与 Windows 未验收的限制。

最新排查：[Bug 与优化报告](audit/2026-11-04/BUG-AND-OPTIMIZATION-REPORT.md)。基于当前未提交源码发现 13 项问题（6 P1、7 P2）与 9 项优化建议，尚未实施修复。本次 JVM 331 项中 322 通过、9 跳过，Release Lint 0 Error / 107 Warning；本地原生音频与协议补测 7 通过、1 跳过。Python 官网/中继/寻址/部署工具分别 42/11/7/2 项通过；设备、Windows 和线上验证边界见报告及[独立记录](changes/2026-11-04-08-bug-audit-and-optimization-report.md)。

最新本机安装：[手机 Release 覆盖更新与 Linux 桌面快捷方式](changes/2026-11-04-07-release-phone-update-and-linux-shortcut.md)。版本保持 0.6.5 / 39；设备安装与启动结果见记录。

最新原生 UI：[双端浅色方案落地与连续导航](changes/2026-11-04-06-native-light-ui-and-continuous-navigation.md)。桌面侧栏/工具栏/播放条、曲库封面网格、设置八组子页及原生转场已接入；实际验证与平台限制以该记录为准。

前轮本地交付：[布局与切页性能](changes/2026-11-03-11-macos-layout-and-navigation-cost.md)、[助手点播与队列](changes/2026-11-03-12-assistant-playable-music-links.md)。实现于 11 月 3 日，11 月 4 日核对交付。最终定向原生测试 18 项通过；Release、Debug 和测试 APK 构建通过。第一版布局包真机超时帧约 34%→6%，最终细节版真机复测按用户要求延期。

前轮界面补修：[全应用磨砂覆盖](changes/2026-11-03-09-app-wide-frosted-material.md)、[歌词高亮恢复](changes/2026-11-03-10-restore-lyric-focus.md)。此前首轮材质记录不能代表整应用已经视觉验收；本次验证与设备限制见新记录。

最新部署实测：[阿里云官网性能、中继排队与原生播放验证](changes/2026-11-03-07-aliyun-deployment-performance-validation.md)。新机回环页面 12 并发 p95 157 ms、零错误；混合下载与多路大文件中继存在秒级尾延迟。本地原生／协议 17 项通过、1 项公网验收跳过；新机跨网播放和长期稳定性尚未确认。

最新服务器迁移：[阿里云直接部署与一起听统一服务器连接](changes/2026-11-03-06-aliyun-hosting-and-server-routed-together.md)。客户端源码与部署包已准备，新机已恢复官网迁移副本并运行官网／一起听中继；回环页面、Range、配对和重启验证通过。公开切换待补足购买期、备案、正式数据同步及证书／DNS；原入口尚未替换。

最新 UI 调整：[一起听房间重排与固定邀请入口](changes/2026-11-03-03-together-room-and-pinned-invitation.md)。复制／分享始终位于房间顶栏，共享队列首页最多显示三首，完整列表单独滚动；成员与 AI 选歌默认收起。按当前会话要求不运行测试。

前轮 UI（当前布局以上方最新记录为准）：已按用户确认的 macOS 毛玻璃方向实施原生磨砂材质、封面背景、四项导航和播放器控制台，包含[连续歌词修复](changes/2026-11-03-01-continuous-lyrics-motion.md)。本轮用户要求直接构建 APK、不运行测试；本次结果与限制见[磨砂 UI 与 Release APK](changes/2026-11-03-02-frosted-ui-and-release-apk.md)。[旧 UI 方案](changes/2026-11-02-09-android-first-ui-plan.md)和[图片探索](changes/2026-11-02-10-ui-redesign-skills-and-frosted-material.md)保留作历史依据。

最新结果：[Android 0.6.5 Release APK 编译、签名验证与设备拉起验收](changes/2026-11-02-08-release-apk-build.md)，[播放加载与缓冲状态全链路实现、帧率实测及桌面可见窗口验收](changes/2026-11-02-07-loading-and-buffering-feedback.md)，[转场断言已修复](changes/2026-11-02-02-wait-for-detail-transition-idle.md)，[一起听服务器已部署并验证公网 TLS](changes/2026-11-02-04-public-relay-deployment.md)，[强制公网中继双端原生验收与完成握手](changes/2026-11-02-05-native-public-relay-acceptance.md)。本批接续入口为 [2026-11-02 剩余质量交接](HANDOFF-2026-11-02-REMAINING-QUALITY.md)。历史结果不替代当前验证。

第一批双主题/一起听 UI、第二批统计/偏好/错误归属修复、第三批加载与缓冲全链路状态已实施，并完成 MuMu 帧率实测与 Windows 真实桌面窗口验收。服务器已经用户批准上线；独立跨网蜂窝真机、多人平权及长时压力验收仍待后续环境具备。准确进度见 [OPTIMIZATION_REPORT.md](OPTIMIZATION_REPORT.md)；[PRODUCT_AUDIT.md](PRODUCT_AUDIT.md) 保留原始审计，不代表所有待办已完成。

当前版本为 **0.6.9 / 43**，唯一版本来源为 `version.properties`。普通构建默认沿用当前版本。[0.6.9 发布说明](RELEASE_NOTES-0.6.9.md)

已按[三项特色规划](changes/2026-10-30-01-product-positioning-and-listen-together-plan.md)和[陪伴房间需求](changes/2026-10-30-07-companion-room-chat-and-partner-scope.md)实施。新版 MuMu 七项聊天/配对/邀请、五项音频会话及反向原生通过，可选寻址完成本地互通。本次最终针对性桌面 70 项中 66 通过、4 项显式跳过，Android Release 48 项通过；[本次双端 Release 已保存并核验](changes/2026-10-31-13-current-release-artifacts-and-device-verification.md)，Windows 同次原生运行时启动通过，四份软件/校验/说明文件已向自己的[微信文件传输助手上传结束](changes/2026-10-31-14-windows-startup-and-wechat-transfer.md)。两小时 Windows/MuMu 原生和聊天验收已通过，正式 Android 包安装/启动成功但非冷启动；新宣传片的模型配置失败待修复。维护者此后新增[跨网、多人平权、卡顿与提醒需求](changes/2026-10-31-15-multiplayer-equal-control-and-cross-network-plan.md)，该新需求尚未完成。

## 新 AI 从这里开始

当前工作区包含 0.6.5 的队列管理、下一首播放、曲库排序与筛选；最新歌词修复和宣传素材见下方独立记录。历史发布结果不能代替本轮构建与设备验证。

1. 读根目录 [AGENTS.md](../AGENTS.md) 与 [AI 协作流程](AI-WORKFLOW.md)。
2. 根据 [开发说明的任务定位表](DEVELOPMENT.md#gaps) 找相关章节、实现、测试。
3. 阅读影响本任务的下表记录；文档不足时才扩大源码范围。
4. 完成每个独立功能/修复后新增带日期/具体修改的记录，并更新本索引和开发说明。

| 文档 | 用途 |
| --- | --- |
| [2026-11-02 剩余质量接手提示词](HANDOFF-2026-11-02-REMAINING-QUALITY.md) | 当前入口：状态反馈、性能测量、独立网络和最终验收；推荐模型与安全边界 |
| [2026-11-01 Antigravity 质量收尾交接](HANDOFF-2026-11-01-ANTIGRAVITY-QUALITY-FINISH.md) | 历史交接；服务已上线，当前状态以 11 月 2 日记录为准 |
| [累计优化结果](OPTIMIZATION_REPORT.md) | 双主题、统计与播放反馈、已部署 WSS、实际验证及剩余工作 |
| [2026-11-01 产品质量审计](PRODUCT_AUDIT.md) | 当前架构、确认功能、风险优先级、原生截图、真实基线及待确认优化计划 |
| [2026-11-01 早期 Antigravity 接线验收提示词](HANDOFF-2026-11-01-ANTIGRAVITY-VALIDATION.md) | 历史参考；本批接手范围以最新质量收尾交接为准 |
| [详细开发说明](DEVELOPMENT.md) | 当前模块/状态/存储、两端功能、AI工具、截图、推荐清理、翻译/悬浮、构建/排查 |
| [AI 协作流程](AI-WORKFLOW.md) | 最小上下文、实施顺序、文档/验证/交接规则 |
| [变更模板](CHANGE-TEMPLATE.md) | 日期、原因、具体修改、前后/数据、真实验证、限制 |
| [本轮体验优化与 Antigravity 交接](HANDOFF-2026-10-17-UX-ANTIGRAVITY.md) | 九项独立记录、实际检查结果、真机重点、最终构建与网站部署提示词 |
| [组件/外部服务/凭据](THIRD_PARTY.md) | 新模型、B站、WorkManager/JNA、请求内容与加密/兼容 |
| [项目使用说明](../README.md) | 用户入口与设置/推荐删除语义 |
| [官网运行与 Ubuntu 部署](../website/README.md) | 响应式官网、匿名论坛、下载发布、邮箱订阅、作者后台及备份 |
| [0.6.9 发布说明](RELEASE_NOTES-0.6.9.md) | 曲库与截图导入优化三端正式版 |
| [0.6.8 发布说明](RELEASE_NOTES-0.6.8.md) | 音乐助手下载指引三端正式版 |
| [0.6.7 发布说明](RELEASE_NOTES-0.6.7.md) | 助手联网检索、原生回答与独立播放修复三端正式版 |
| [0.6.5 发布说明](RELEASE_NOTES-0.6.5.md) | 全新播放队列深度管理、下一首播放置顶、曲库即时排序筛选与双语歌词舞台双端正式版 |
| [0.6.4 发布说明](RELEASE_NOTES-0.6.4.md) | Mineradio 沉浸音乐舞台、端到端加密双人一起听与自适应封面氛围双端正式版 |
| [0.6.3 发布说明](RELEASE_NOTES-0.6.3.md) | 听歌时长统计、跨平台音效调校、取消Token限制与每日推荐未听保护双端正式版 |
| [0.6.2 发布说明](RELEASE_NOTES-0.6.2.md) | 全面温润质感打磨、标志性波浪进度条与沉浸动效双端正式版 |
| [0.6.1 发布说明](RELEASE_NOTES-0.6.1.md) | 画廊展台立体原生 UI、轻量动效与全域交互打磨双端正式版 |
| [0.6.0 发布说明](RELEASE_NOTES-0.6.0.md) | AI 语义搜歌、曲库智能管家、实时伴奏模式、音频特征与品味画像双端正式版 |
| [0.5.0 发布说明](RELEASE_NOTES-0.5.0.md) | 体验优化集大成、统一搜索试听、截图核对、任务重试、离线重定位与原生睡眠双端正式版 |
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

Linux x86_64 原生桌面版新增 DEB 与免安装包；安装与平台差异见 [Linux 说明](LINUX.md)，本次结果见 [Linux 支持记录](changes/2026-11-03-04-linux-desktop-support.md)。

## 变更记录（最新在前）

| 修改日期 | 主要修改 | 详细记录 |
| --- | --- | --- |
| 2026-11-13 | 播放页按歌词视口调整字阶，修复窄屏长句占满歌词区 | [具体记录](changes/2026-11-13-01-fit-long-player-lyrics.md) |
| 2026-11-06 | 构建并发布 0.6.9 曲库与截图导入优化三端正式版 | [具体记录](changes/2026-11-06-07-publish-0.6.9-release.md) |
| 2026-11-06 | 批量重新定位音频时一次提交曲库索引 | [具体记录](changes/2026-11-06-06-batch-audio-relink-commit.md) |
| 2026-11-06 | 修复 BPM 搜索忽略明确歌手和文字条件 | [具体记录](changes/2026-11-06-05-semantic-search-text-and-bpm.md) |
| 2026-11-06 | 修复切歌准备期间暂停后恢复旧曲 | [具体记录](changes/2026-11-06-04-resume-selected-track-after-preparation.md) |
| 2026-11-06 | 保留截图导入失败及中断后的剩余草稿 | [具体记录](changes/2026-11-06-03-screenshot-import-checkpoints.md) |
| 2026-11-06 | 音乐助手下载指引和 0.6.8 三端发布 | [具体记录](changes/2026-11-06-02-publish-0.6.8-assistant-release.md) |
| 2026-11-06 | 用当前助手提示词构建本地 Android Release 试用包，核对 DEX 与正式签名 | [具体记录](changes/2026-11-06-01-android-assistant-trial-apk.md) |
| 2026-11-05 | 构建并发布 0.6.7 助手联网与独立播放三端正式版 | [具体记录](changes/2026-11-05-43-publish-0.6.7-harness-release.md) |
| 2026-11-05 | 移植 DeepSeek Harness Search／Fetch，接入原生 Markdown、来源链接与检索过程 | [具体记录](changes/2026-11-05-42-harness-web-research-and-native-replies.md) |
| 2026-11-05 | 助手新检索结果计入进展，受阻时单次文字收束，明确播客推荐能力并去重错误 | [具体记录](changes/2026-11-05-41-assistant-search-progress-and-final-answer.md) |
| 2026-11-05 | 本地BPM分析、来源校验缓存与真实节拍筛选 | [具体记录](changes/2026-11-05-39-offline-bpm-analysis-and-filtering.md) |
| 2026-11-05 | 批量预览并重新定位失效音频路径 | [具体记录](changes/2026-11-05-38-batch-relink-missing-audio.md) |
| 2026-11-05 | 截图歌单草稿、编辑及勾选跨进程保留 | [具体记录](changes/2026-11-05-37-persist-screenshot-import-drafts.md) |
| 2026-11-05 | 音频设备失败后保留进度重试与后台连续验收 | [具体记录](changes/2026-11-05-36-resume-playback-after-device-failure.md) |
| 2026-11-05 | 减少暂停及悬浮歌词后台开销并索引歌词时间戳 | [具体记录](changes/2026-11-05-35-playback-idle-work-and-lyric-index.md) |
| 2026-11-05 | 歌词工具独立滚动与校准面板层级 | [具体记录](changes/2026-11-05-34-lyrics-tools-scroll-and-layering.md) |
| 2026-11-05 | 移除一起听入口、房间网络、后台同步与服务器部署依赖，保留独立播放和本机数据 | [具体记录](changes/2026-11-05-33-remove-listen-together.md) |
| 2026-11-05 | 配对重连回归等待持久化状态刷新 | [具体记录](changes/2026-11-05-32-await-companion-restoration-in-regression.md) |
| 2026-11-05 | FFmpeg 4.4整轨响度参数兼容 | [具体记录](changes/2026-11-05-31-ffmpeg44-loudness-and-pairing-regression.md) |
| 2026-11-05 | 官网以当前真实原生截图更新首页/介绍/三端下载，修复预览切换并补DEB说明 | [具体记录](changes/2026-11-05-30-refresh-website-release-pages.md) |
| 2026-11-05 | 从当前修复源码构建Android/Windows/Linux正式包，核验后更新GitHub和官网 | [具体记录](changes/2026-11-05-29-publish-three-platform-release.md) |
| 2026-11-05 | 播放页按可用高度分配封面与歌词预览 | [具体记录](changes/2026-11-05-28-player-cover-preview-height-budget.md) |
| 2026-11-05 | 同步原生界面测试契约并按真实变化采集窗口帧 | [具体记录](changes/2026-11-05-27-native-test-contracts-and-frame-stimulus.md) |
| 2026-11-05 | 官网导航乱序响应、缓存和历史统一按最后意图提交 | [具体记录](changes/2026-11-05-26-website-latest-navigation-wins.md) |
| 2026-11-05 | Android同步偏好写失败向调用方报告 | [具体记录](changes/2026-11-05-25-android-preference-commit-errors.md) |
| 2026-11-05 | 删除失败保留记录、批量报告实际结果并保留共享封面 | [具体记录](changes/2026-11-05-24-delete-failures-and-shared-artwork.md) |
| 2026-11-05 | 语义搜索明确BPM尚未参与筛选 | [具体记录](changes/2026-11-05-23-semantic-search-bpm-capability-copy.md) |
| 2026-11-05 | 重复项保留录音版本并按完整批次确认 | [具体记录](changes/2026-11-05-22-smart-cleanup-safe-batch.md) |
| 2026-11-05 | 助手删除绑定具体音频并强制本机确认 | [具体记录](changes/2026-11-05-21-assistant-local-audio-delete-confirmation.md) |
| 2026-11-05 | 桌面专用偏好入口统一恢复备份 | [具体记录](changes/2026-11-05-20-recover-specialized-desktop-preferences.md) |
| 2026-11-05 | 保留暂时不可用的曲目、收藏与歌单引用 | [具体记录](changes/2026-11-05-19-retain-unavailable-library-tracks.md) |
| 2026-11-05 | 同步包完整校验、失败回退与资源预算 | [具体记录](changes/2026-11-05-18-sync-import-rollback-and-budgets.md) |
| 2026-11-05 | 手机重连后执行79项独立包回归，62通过/10失败/7跳过，保留首次异常与复测证据并更新功能报告 | [具体记录](changes/2026-11-05-17-android-device-functional-regression.md) |
| 2026-11-05 | 整理191项功能并执行隔离回归/异常验收，记录12项问题与手机断连、Windows和外部服务缺口 | [具体记录](changes/2026-11-05-16-comprehensive-feature-testing.md) |
| 2026-11-05 | 手机重连后构建当前完整源码正式包，保留数据更新播放页动效及助手修复 | [具体记录](changes/2026-11-05-15-player-animation-phone-install.md) |
| 2026-11-05 | 参考 Apple Music UI Demo，连接播放条与封面端点并增加原生跟手收起和弹簧回弹 | [具体记录](changes/2026-11-05-11-apple-music-player-expansion.md) |
| 2026-11-05 | 手机重连后保留数据覆盖最终歌词修复正式包，核对实际安装字节和持续启动状态 | [具体记录](changes/2026-11-05-10-complete-phone-lyrics-fix-installation.md) |
| 2026-11-05 | Android 正式覆盖包准备、Linux 歌词修复版保留数据安装与运行核对；手机待重连 | [具体记录](changes/2026-11-05-09-install-lyrics-fix-on-phone-and-linux.md) |
| 2026-11-05 | 恢复封面歌词预览、同步歌词检索优先与沉浸当前句淡底高亮，补实际播放器/手机回归 | [具体记录](changes/2026-11-05-08-restore-player-lyrics-and-synchronized-priority.md) |
| 2026-11-05 | 重构应用任务/退出、平台能力、导入/队列/歌词与桌面 PCM 输出，统一顺序写入与统计快照 | [具体记录](changes/2026-11-05-07-application-core-and-desktop-audio-refactor.md) |
| 2026-11-05 | 应用会话与桌面窗口分离、Linux 歌词表面降级与原生窗口回归，明确稳定性重构阶段 | [具体记录](changes/2026-11-05-06-desktop-lyrics-window-and-stability-boundaries.md) |
| 2026-11-05 | 普通文件选择/目录扫描/拖放支持 `.resonance`，恢复歌单并避免旧界面列表覆盖 | [具体记录](changes/2026-11-05-04-linux-resonance-package-import.md) |
| 2026-11-05 | 正常退出旧实例并从桌面固定入口启动最新完整程序，验证进程和原生窗口 | [具体记录](changes/2026-11-05-03-desktop-latest-running-instance.md) |
| 2026-11-05 | 按 AM++ 源码与截图重做三端原生播放页、歌词及玻璃导航 | [具体记录](changes/2026-11-05-02-am-reference-native-ui.md) |
| 2026-11-05 | 桌面支持音乐文件/目录多选和整窗拖入、路径去重及坏文件隔离 | [具体记录](changes/2026-11-05-01-desktop-files-and-drop-import.md) |
| 2026-11-04 | 优先更新本机 Linux 为包含最终歌单导出修复的完整版本 | [具体记录](changes/2026-11-04-21-install-final-export-fix-on-linux-desktop.md) |
| 2026-11-04 | 构建最终导出修复三端产物、推送 GitHub 并更新官网发布 | [具体记录](changes/2026-11-04-20-publish-0.6.6-three-platforms.md) |
| 2026-11-04 | 导出文件按歌单名命名、重名加序号，并直接打开保存子目录 | [具体记录](changes/2026-11-04-19-playlist-names-and-direct-export-directory.md) |
| 2026-11-04 | 导出中显示处理状态，完成后提示保存位置／文件名并提供查看入口 | [具体记录](changes/2026-11-04-18-export-location-feedback.md) |
| 2026-11-04 | 实机复现导出选项 Haze ANR、验证窗口隔离并改为有界歌单懒加载 | [具体记录](changes/2026-11-04-17-export-dialog-anr-and-lazy-scopes.md) |
| 2026-11-04 | 准备 0.6.6 三端发布、Windows 原生构建与本机桌面更新 | [具体记录](changes/2026-11-04-16-release-0.6.6-three-platforms.md) |
| 2026-11-04 | 隔离“音量”播放设置与通用原生弹窗的主窗口毛玻璃采样 | [具体记录](changes/2026-11-04-15-volume-dialog-window-isolation.md) |
| 2026-11-04 | 构建歌词与新增歌源修复后的 Android Release、核验签名与本次产物 | [具体记录](changes/2026-11-04-13-lyrics-and-sources-release-apk.md) |
| 2026-11-04 | 内置歌源从三源增至五源、保留用户选择并安全兼容 VPN Fake-IP | [具体记录](changes/2026-11-04-12-more-builtin-sources-and-vpn-dns.md) |
| 2026-11-04 | “我喜欢的音乐”仅显示歌曲数量 | [具体记录](changes/2026-11-04-11-favorites-count-only.md) |
| 2026-11-04 | 音效弹窗移除跨窗口模糊并使用有界预设列表 | [具体记录](changes/2026-11-04-10-audio-effect-dialog-window-isolation.md) |
| 2026-11-04 | 歌词查找接入真实歌源、模型拒绝解释与可滚动精简空态 | [具体记录](changes/2026-11-04-09-lyrics-source-recovery-and-compact-tools.md) |
| 2026-11-04 | 排查播放器/官网缺陷、复现数据与删除授权问题并交付优化报告 | [具体记录](changes/2026-11-04-08-bug-audit-and-optimization-report.md) |
| 2026-11-04 | 构建手机 Release、同签名覆盖更新并安装 Linux 桌面快捷方式 | [具体记录](changes/2026-11-04-07-release-phone-update-and-linux-shortcut.md) |
| 2026-11-04 | 将双端浅色方案落地到原生界面、设置分组与可中断导航 | [具体记录](changes/2026-11-04-06-native-light-ui-and-continuous-navigation.md) |
| 2026-11-04 | 重做HTML切页交接，保留固定导航/播放条并支持连续反向 | [具体记录](changes/2026-11-04-05-continuous-prototype-navigation.md) |
| 2026-11-04 | 补齐HTML原型主导航、曲库标签与详情的可中断切页动画 | [具体记录](changes/2026-11-04-04-prototype-page-transitions.md) |
| 2026-11-04 | 修复本地UI预览拒绝连接，改为独立用户服务运行 | [具体记录](changes/2026-11-04-03-persistent-ui-preview-service.md) |
| 2026-11-04 | 提供78个页面与关键状态的双端浅色图、全图总览和可试HTML动效原型 | [具体记录](changes/2026-11-04-02-light-ui-proposal-and-html-prototype.md) |
| 2026-11-04 | 安装官网设计、动效检查与宣传视频技能，记录来源和许可证 | [具体记录](changes/2026-11-04-01-install-design-motion-video-skills.md) |
| 2026-11-03 | 助手回复支持真实歌曲链接、点播与保序追加队列 | [具体记录](changes/2026-11-03-12-assistant-playable-music-links.md) |
| 2026-11-03 | 按 macOS 确认稿重排原生布局，合并底座并降低切页绘制开销 | [具体记录](changes/2026-11-03-11-macos-layout-and-navigation-cost.md) |
| 2026-11-03 | 恢复歌词当前行亮色文字与淡底焦点，保持连续滚动和稳定行高 | [具体记录](changes/2026-11-03-10-restore-lyric-focus.md) |
| 2026-11-03 | 补齐全应用磨砂页面、列表与弹窗材质，并构建本地试用 APK | [具体记录](changes/2026-11-03-09-app-wide-frosted-material.md) |
| 2026-11-03 | 修复每日推荐单次点击被旧暂停回调取消、声音与曲目错位及切歌混入整库 | [具体记录](changes/2026-11-03-08-daily-recommendation-playback-selection.md) |
| 2026-11-03 | 实测阿里云官网并发、下载与中继排队，验证本地原生音频输出并提供有界复测工具 | [具体记录](changes/2026-11-03-07-aliyun-deployment-performance-validation.md) |
| 2026-11-03 | 安装阿里云 Nginx／systemd 与数据副本，并改为服务器邀请不尝试设备直连（待备案切换） | [具体记录](changes/2026-11-03-06-aliyun-hosting-and-server-routed-together.md) |
| 2026-11-03 | 安装 Linux 当前用户版、接入 KDE 应用菜单并从正式入口启动 | [具体记录](changes/2026-11-03-05-linux-user-installation.md) |
| 2026-11-03 | 增加 Linux 原生桌面版、XDG／凭据适配、DEB 与免安装包及实际验证 | [具体记录](changes/2026-11-03-04-linux-desktop-support.md) |
| 2026-11-03 | 重排一起听房间、固定复制邀请入口并将长队列收纳为独立列表 | [具体记录](changes/2026-11-03-03-together-room-and-pinned-invitation.md) |
| 2026-11-03 | 落地 macOS 毛玻璃原生 UI、四项导航与播放器控制台，并构建试用 APK（未测试） | [具体记录](changes/2026-11-03-02-frosted-ui-and-release-apk.md) |
| 2026-11-03 | 歌词预览连续滚动、稳定高亮排版与手动浏览后恢复跟随 | [具体记录](changes/2026-11-03-01-continuous-lyrics-motion.md) |
| 2026-11-02 | 补入 Impeccable、探索全新 UI 图片方案并明确 macOS 毛玻璃材质（未实施） | [具体记录](changes/2026-11-02-10-ui-redesign-skills-and-frosted-material.md) |
| 2026-11-02 | 制定 Android 优先的全项目原生 UI 优化方案（待实施） | [具体记录](changes/2026-11-02-09-android-first-ui-plan.md) |
| 2026-11-02 | Android 0.6.5 Release APK 编译、签名验证与设备拉起验收 | [具体记录](changes/2026-11-02-08-release-apk-build.md) |
| 2026-11-02 | 播放加载与缓冲状态全链路实现、帧率实测及桌面可见窗口验收 | [具体记录](changes/2026-11-02-07-loading-and-buffering-feedback.md) |
| 2026-11-02 | 根据实际模型列表更正 Antigravity 接手推荐为 Gemini 3.8 Flash High | [具体记录](changes/2026-11-02-06-correct-handoff-model.md) |
| 2026-11-02 | 强制公网中继双端原生验收，修正测试结束握手并保存失败证据 | [具体记录](changes/2026-11-02-05-native-public-relay-acceptance.md) |
| 2026-11-02 | 自有一起听中继上线，公网证书锁定 TLS 与官网回归通过 | [具体记录](changes/2026-11-02-04-public-relay-deployment.md) |
| 2026-11-02 | 准备一起听独立守护与现网启动补丁（未部署） | [具体记录](changes/2026-11-02-03-public-relay-supervision-preparation.md) |
| 2026-11-02 | 等待真实动画空闲，修复转场背景像素断言 | [具体记录](changes/2026-11-02-02-wait-for-detail-transition-idle.md) |
| 2026-11-02 | 双端原生验收与本地候选包，保留失败证据 | [具体记录](changes/2026-11-02-01-native-validation-and-local-candidates.md) |
| 2026-11-01 | 按原生请求区分播放、试听与保存失败，阻止旧错误覆盖当前曲目 | [具体记录](changes/2026-11-01-19-scoped-playback-failures.md) |
| 2026-11-01 | 捕获完整随机/循环偏好，按操作顺序持久化并报告失败 | [具体记录](changes/2026-11-01-18-ordered-playback-preferences.md) |
| 2026-11-01 | 原生播放会话计时、暂停完播去重与共享统计写入器 | [具体记录](changes/2026-11-01-17-native-listening-session-accounting.md) |
| 2026-11-01 | 明确剩余工作并交接 Antigravity 设备验收、本地候选包与生产边界 | [具体记录](changes/2026-11-01-16-antigravity-quality-finish-handoff.md) |
| 2026-11-01 | 为所有安装用户接入自有服务器的 WSS 中继 | [具体记录](changes/2026-11-01-15-public-websocket-relay.md) |
| 2026-11-01 | 统一明暗银灰主题并简化一起听首页 | [具体记录](changes/2026-11-01-14-dual-theme-and-simpler-together-ui.md) |
| 2026-11-01 | 拖动结束再跳转并在播放器上显示失败 | [具体记录](changes/2026-11-01-13-seek-commit-and-visible-playback-errors.md) |
| 2026-11-01 | 保护听歌记录并把统计写盘移到 IO | [具体记录](changes/2026-11-01-12-listening-stats-integrity-and-batching.md) |
| 2026-11-01 | 完成产品只读质量审计、保存检查点与原生视觉基线，列出统计可靠性及播放交互优化方案 | [具体记录](changes/2026-11-01-11-product-quality-audit-and-native-baseline.md) |
| 2026-11-01 | 修复歌词译文隐藏、翻译错误无提示与迟到缓存覆盖，统一语言匹配并保护切歌结果 | [具体记录](changes/2026-11-01-10-lyrics-translation-feedback-and-result-lifecycle.md) |
| 2026-11-01 | 修复一起听群聊与单聊路由、客户端消息丢包并完成双端全量测试与 Release 交付 | [具体记录](changes/2026-11-01-09-together-chat-routing-and-full-validation.md) |
| 2026-11-01 | 整理 Antigravity 最终构建验收提示词与当前源码边界 | [具体记录](changes/2026-11-01-08-antigravity-validation-handoff.md) |
| 2026-11-01 | 应用内聊天未读与跳到最新；成员离开后恢复剩余聊天通道；未构建测试 | [具体记录](changes/2026-11-01-07-unread-chat-and-surviving-member-channel.md) |
| 2026-11-01 | 群快照修订/输入校验、就绪隔离与上传失败清理；未构建测试 | [具体记录](changes/2026-11-01-06-validate-room-snapshots-and-media-recovery.md) |
| 2026-11-01 | 原生作用域处理协调端控制、混合共享队列增删排序与界面投影；未构建测试 | [具体记录](changes/2026-11-01-05-native-host-controls-and-playable-queue.md) |
| 2026-11-01 | 群聊逐成员回执、落盘转发名单与有序补发；尚未构建测试 | [具体记录](changes/2026-11-01-04-persist-group-chat-recipient-receipts.md) |
| 2026-11-01 | 拒绝旧房间迟到的聊天附着和成员列表；尚未构建测试 | [具体记录](changes/2026-11-01-03-reject-stale-companion-room-attachments.md) |
| 2026-11-01 | AI 直接生成歌词文本，区分音频识别时间轴并保护切歌结果；尚未构建测试 | [具体记录](changes/2026-11-01-02-direct-ai-lyrics-text.md) |
| 2026-11-01 | 限制多人并发接收的房间音频缓存总量；尚未构建测试 | [具体记录](changes/2026-11-01-01-bound-concurrent-room-audio-cache.md) |
| 2026-10-31 | 将异步广播与播放器动作绑定对应房间会话；尚未构建测试 | [具体记录](changes/2026-10-31-35-bind-room-actions-to-session.md) |
| 2026-10-31 | 为成员保留限期的中继重连路由；尚未构建测试 | [具体记录](changes/2026-10-31-34-relay-routes-for-member-reconnect.md) |
| 2026-10-31 | 修正中继等待队列并发清理与房间上限；尚未运行测试 | [具体记录](changes/2026-10-31-33-relay-waiting-queue-cleanup.md) |
| 2026-10-31 | 将已校验的成员贡献音频接入创建者播放队列；尚未构建测试 | [具体记录](changes/2026-10-31-32-play-contributed-room-audio.md) |
| 2026-10-31 | 接通房间基础播放请求并同步创建者已有队列；尚未构建测试 | [具体记录](changes/2026-10-31-31-route-room-playback-controls-and-queue.md) |
| 2026-10-31 | 限定并续期 Android 活动房间运行锁；尚未构建测试 | [具体记录](changes/2026-10-31-30-bound-and-renew-android-room-locks.md) |
| 2026-10-31 | 为各成员串行化媒体传输并固定协程媒体 ID；尚未构建测试 | [具体记录](changes/2026-10-31-29-serialize-room-audio-transfers.md) |
| 2026-10-31 | 修正群聊会话与成员序号隔离、并发入房及上传边界；尚未构建测试 | [具体记录](changes/2026-10-31-28-group-chat-routing-and-admission-safety.md) |
| 2026-10-31 | 禁止 Windows 跟听微调反复触发解码重启；尚未构建测试 | [具体记录](changes/2026-10-31-27-avoid-desktop-tempo-restarts.md) |
| 2026-10-31 | 停止自动将 UDP STUN 结果视作 TCP 直连候选 | [具体记录](changes/2026-10-31-26-do-not-publish-udp-stun-as-tcp-candidate.md) |
| 2026-10-31 | 公网 STUN 候选发现、Android 锁屏防断连保活与歌曲详情分享至 AI 推荐相似曲目；尚未构建测试 | [具体记录](changes/2026-10-31-25-public-stun-discovery-and-ai-similar-tracks.md) |
| 2026-10-31 | 引入播放速率微调以平滑时间同步并缓解卡顿；尚未构建测试 | [具体记录](changes/2026-10-31-24-playback-tempo-sync-and-stutter-mitigation.md) |
| 2026-10-31 | 接入多人一起听房间协调器、平权控制与隔离群聊；尚未构建测试 | [具体记录](changes/2026-10-31-23-multiplayer-room-coordinator-and-equal-control.md) |
| 2026-10-31 | 防止旧连接收尾覆盖新连接、拒绝加入时延长恢复期限；尚未构建测试 | [具体记录](changes/2026-10-31-22-retire-disconnected-peer-before-reusing-slot.md) |
| 2026-10-31 | 编写多人平等控制和共享队列的内存状态基础；尚未接入房间 | [具体记录](changes/2026-10-31-21-multimember-room-state-foundation.md) |
| 2026-10-31 | 编写可选独立中继、内层房间 TLS 和直连失败回退；未构建测试、审计或部署 | [具体记录](changes/2026-10-31-20-opt-in-encrypted-relay-transport.md) |
| 2026-10-31 | 长房间稳定一段时间后重置连续断线重试次数；尚未构建测试 | [具体记录](changes/2026-10-31-19-retry-budget-per-stable-connection.md) |
| 2026-10-31 | 新版双人房间播放、切歌和进度控制免审批，保留旧版能力协商；尚未构建测试 | [具体记录](changes/2026-10-31-18-equal-two-person-playback-controls.md) |
| 2026-10-31 | Android 消息通知、权限引导及 Windows 托盘提醒接线；尚未构建测试 | [具体记录](changes/2026-10-31-17-chat-notifications-on-android-and-windows.md) |
| 2026-10-31 | 聊天提醒基础事件仅在新消息保存后发出，重复与清空后旧包不再次发出；系统通知尚未接线 | [具体记录](changes/2026-10-31-16-deduplicate-incoming-chat-alert-events.md) |
| 2026-10-31 | 维护者反馈移动网络跨网失败、卡顿、房主特权和人数限制；确认可选加密中继及多人平权改进方向 | [具体规划](changes/2026-10-31-15-multiplayer-equal-control-and-cross-network-plan.md) |
| 2026-10-31 | 本次 Windows 原生冷启动通过，软件、校验清单与说明已确认上传文件传输助手 | [具体记录](changes/2026-10-31-14-windows-startup-and-wechat-transfer.md) |
| 2026-10-31 | 当前陪伴房间双端 Release 构建、重新验签/哈希及真实设备验收状态 | [具体记录](changes/2026-10-31-13-current-release-artifacts-and-device-verification.md) |
| 2026-10-31 | 新宣传摄影机接入真实 AI 回答、加密聊天与配对，成片仍待设备拍摄验收 | [具体记录](changes/2026-10-31-12-real-ai-and-companion-promo-camera.md) |
| 2026-10-31 | 音频命令授权变化时有限排队，关闭旧控制器后拒绝晚到操作；MuMu 五项通过 | [具体记录](changes/2026-10-31-11-defer-unavailable-audio-commands-and-retire-old-controller.md) |
| 2026-10-31 | 没有配对伙伴时延后监听，减少首次启动的防火墙打扰 | [具体记录](changes/2026-10-31-10-defer-unpaired-invitation-listener.md) |
| 2026-10-31 | 原生跨端验收素材用应用私有目录，修复 shell 外部文件权限问题 | [具体记录](changes/2026-10-31-09-private-native-test-fixtures.md) |
| 2026-10-31 | 隔离构建/原生验收并记录失败、重测与 Release 真实状态 | [具体记录](changes/2026-10-31-08-isolated-native-validation-and-release-build.md) |
| 2026-10-31 | 官方 DeepSeek Flash/V4 短任务显式关闭默认思考，保留其它兼容接口 | [具体修改](changes/2026-10-31-07-request-final-content-from-deepseek-flash.md) |
| 2026-10-31 | 网络变化/关闭时显式中止预取并清理半成品，默认等待用户开启 | [具体修改](changes/2026-10-31-06-retire-cancelled-prefetch-and-opt-in-default.md) |
| 2026-10-31 | 本机音频中断保留房间和聊天，主动恢复后对齐；验证进行中 | [具体修改](changes/2026-10-31-05-keep-room-during-local-audio-interruption.md) |
| 2026-10-31 | 默认关闭的签名伙伴寻址与独立自托管服务；本地服务七项通过，原生集成验收进行中 | [具体修改](changes/2026-10-31-04-opt-in-signed-partner-rendezvous.md) |
| 2026-10-31 | 配对伙伴直接邀听、对方接受才进房、移除后拒绝新邀；验证进行中 | [具体修改](changes/2026-10-31-03-paired-partner-direct-invitations.md) |
| 2026-10-31 | 新增加密本机聊天、送达确认/重发去重、独立清空及设备配对；界面设备验收进行中 | [具体修改](changes/2026-10-31-02-encrypted-local-chat-and-device-pairing.md) |
| 2026-10-31 | 限制八个候选地址、取消全部未完成握手并去除原始异常回显 | [具体修改](changes/2026-10-31-01-cancel-bounded-direct-connections.md) |
| 2026-10-30 | 确认长时间双人房间、本机聊天与常听伙伴需求，先完善直连，独立评估会合服务 | [实施边界](changes/2026-10-30-07-companion-room-chat-and-partner-scope.md) |
| 2026-10-30 | 维护者明确选择 MIT，添加原创代码 LICENSE 并保留第三方许可边界 | [具体修改](changes/2026-10-30-06-mit-license.md) |
| 2026-10-30 | 一起听支持多候选地址自动探测与 IPv6 候选，实际跨网仍取决于网络可达 | [具体修改](changes/2026-10-30-05-multi-candidate-ipv6-listen-together.md) |
| 2026-10-30 | 导航改为 SavedState 恢复，修正 MuMu 输入配置重建后回首页；新包待复测 | [具体修改](changes/2026-10-30-04-preserve-navigation-across-input-configuration.md) |
| 2026-10-30 | 重做源码、AI 助手与真实原生加密一起听的 50 秒宣传主线；已获真实 AI 测试授权，模型配置/拍摄待完成，许可证待定 | [制作记录](changes/2026-10-30-03-source-ai-and-real-together-promo.md) |
| 2026-10-30 | 一起听邀请审批/TTL/二维码、有限恢复、原生同步与真实 ready、预取、共享队列/歌词和 AI 候选确认入队；最终验收与交付进行中 | [具体修改](changes/2026-10-30-02-listen-together-session-and-native-playback.md) |
| 2026-10-30 | 规划开源、AI 助手、端到端加密一起听的宣传主线，以及邀请、同步、重连、共享队列和跨网取舍；仅文档规划 | [具体规划](changes/2026-10-30-01-product-positioning-and-listen-together-plan.md) |
| 2026-10-29 | MuMu 实录原生播放器并制作原创配乐的 40 秒横竖版宣传片，提供可复现拍摄与剪辑脚本 | [具体修改](changes/2026-10-29-03-native-mumu-promo-film.md) |
| 2026-10-29 | 修正带留白的歌词居中坐标、平滑跟随、手动浏览后立即返回与自动恢复，收起歌词工具 | [具体修改](changes/2026-10-29-02-lyrics-centered-follow-and-manual-browsing.md) |
| 2026-10-29 | 播放队列深度管理、下一首播放插队、曲库多维排序筛选与歌词舞台交互打磨并发布 v0.6.5 | [具体修改](changes/2026-10-29-01-queue-play-next-library-sorting-and-lyrics-polish.md) |
| 2026-10-28 | 构建并发布 Resonance v0.6.4 正式版至 GitHub 与官网 | [具体修改](changes/2026-10-28-08-release-0.6.4.md) |
| 2026-10-28 | 引入双人加密一起听并极简交互体验，支持剪贴板智能识别与自然语言邀请提取 | [具体修改](changes/2026-10-28-07-encrypted-listen-together-and-simplified-interaction.md) |
| 2026-10-28 | 手机重连后安装回弹版，完成 37 次设备回归及首次/预热导航窗口诊断 | [具体修改](changes/2026-10-28-06-device-motion-diagnostics-and-regression.md) |
| 2026-10-28 | 封面取色氛围、流动波浪、跳跃回弹切换，完善空曲库与助手欢迎界面 | [具体修改](changes/2026-10-28-05-album-atmosphere-and-spring-motion-refinement.md) |
| 2026-10-28 | Android 16 真机调试音乐舞台，修正手机封面空间、助手标题与设备回归断言 | [具体修改](changes/2026-10-28-04-android16-device-ui-validation.md) |
| 2026-10-28 | 参考 Mineradio 重塑原生音乐舞台、大字歌词、悬浮控制台与歌单推荐主卡 | [具体修改](changes/2026-10-28-03-mineradio-inspired-native-music-stage.md) |
| 2026-10-28 | 构建并发布 Resonance v0.6.3 正式版至 GitHub 与官网 | [具体修改](changes/2026-10-28-02-release-0.6.3.md) |
| 2026-10-28 | 取消 AI Token 使用量与预算限制、每日推荐未听歌曲到期保留不换、取消助手 16 轮工具步骤限制 | [具体修改](changes/2026-10-28-01-remove-token-limits-retain-unlistened-recommendations-and-remove-16-step-limit.md) |
| 2026-10-27 | 新增听歌时长统计与歌曲详情页跨平台音效预设切换 | [具体修改](changes/2026-10-27-08-listening-duration-and-audio-effects.md) |
| 2026-10-27 | 安装 Media3 1.10.1 音频控制与播放统计两项 GitHub Skills 并规划听歌时长及音效切换 | [具体修改](changes/2026-10-27-07-install-media3-audio-and-telemetry-skills.md) |
| 2026-10-27 | 全面温润质感打磨、标志性波浪进度条与沉浸动效并发布 v0.6.2 | [具体修改](changes/2026-10-27-06-pervasive-ui-refinement-and-release-0.6.2.md) |
| 2026-10-27 | 升版 0.6.1、构建签名 Android Release APK 与 Windows 安装包并完成发布说明 | [具体修改](changes/2026-10-27-05-release-0.6.1.md) |
| 2026-10-27 | 落实选定的 18 号画廊展台方案：封面展架、定向投影、实体播放台和原生 UI 回归 | [具体修改](changes/2026-10-27-04-gallery-ledges-native-ui.md) |
| 2026-10-27 | 提供 20 张青绿立体 UI 概念预览，比较浮层、玻璃、陶瓷、金属和凹凸材质；尚未应用到原生代码 | [具体修改](changes/2026-10-27-03-dimensional-ui-concept-previews.md) |
| 2026-10-27 | 保留青绿与封面氛围，统一原生 UI 层级、导航与播放控件，完善减少动效及原生渲染回归 | [具体修改](changes/2026-10-27-02-native-ui-and-motion-polish.md) |
| 2026-10-27 | 在当前项目安装 Material Design 3 UI 设计技能，记录固定来源提交与内容校验值 | [具体修改](changes/2026-10-27-01-install-material-design-3-ui-skill.md) |
| 2026-10-26 | 回退全屏播放页波浪进度条与 UI 重构改动，完整恢复至 v0.6.0 稳定基线 | [具体修改](changes/2026-10-26-04-revert-wavy-progress-and-now-playing-refactor.md) |
| 2026-10-26 | （已回退）全屏播放页温润雅致重构、标志性双谐波波浪进度条与独立音乐美学质感升级 | [具体修改](changes/2026-10-26-03-wavy-progress-and-now-playing-refactor.md) |
| 2026-10-26 | 为 Android 听歌软件安装并配置 Google Android Skills 原生技能集与 Anthropic Frontend Design | [具体修改](changes/2026-10-26-02-install-android-and-frontend-design-skills.md) |
| 2026-10-26 | 引入自然语言语义找歌、曲库智能管家、实时伴奏消除、音频特征分析与音乐品味画像 | [具体修改](changes/2026-10-26-01-ai-features-semantic-search-manager-karaoke-taste.md) |
| 2026-10-18 | 官网下线 123 云盘备用下载通道并同步更新最新公告 | [具体修改](changes/2026-10-18-02-remove-123pan-and-update-announcement.md) |
| 2026-10-18 | 优化滑动手势门槛、详情页歌词换行与译文、高亮居中跟随及每日推荐完整性保护 | [具体修改](changes/2026-10-18-01-page-swipe-lyrics-display-and-recommendation-integrity.md) |
| 2026-10-17 | 恢复页面切换弹性晃动感并彻底根除切页顶边黑闪 | [具体修改](changes/2026-10-17-34-restore-spring-bounce-and-eliminate-top-edge-flicker.md) |
| 2026-10-17 | 升版 0.5.0、构建签名 Android Release APK 与 Windows 安装包并完成官网生产部署 | [具体修改](changes/2026-10-17-33-release-0.5.0.md) |
| 2026-10-17 | 修复 AndroidMusicAccess 中 Path.of 引发的 Release Lint NewApi 错误 | [具体修改](changes/2026-10-17-32-fix-android-access-newapi-lint.md) |
| 2026-10-17 | 进一步覆盖页面切换顶部背景并减少系统栏重设 | [具体修改](changes/2026-10-17-31-stable-page-viewport-and-top-edge.md) |
| 2026-10-17 | 自动推荐支持不计费网络限制与下次到期提示 | [具体修改](changes/2026-10-17-30-automatic-recommendation-network-and-schedule.md) |
| 2026-10-17 | 增加 Windows 搜索播放切歌和返回快捷键 | [具体修改](changes/2026-10-17-29-desktop-keyboard-navigation.md) |
| 2026-10-17 | 接通双端原生睡眠定时与播完当前歌曲停止 | [具体修改](changes/2026-10-17-28-native-player-sleep-timer.md) |
| 2026-10-17 | 新增推荐历史与个人反馈管理页 | [具体修改](changes/2026-10-17-27-recommendation-history-and-feedback-management.md) |
| 2026-10-17 | 区分离线音频状态并支持恢复和重新定位 | [具体修改](changes/2026-10-17-26-offline-audio-status-and-relink.md) |
| 2026-10-17 | 按一次操作汇总任务并批量重试失败媒体步骤 | [具体修改](changes/2026-10-17-25-grouped-tasks-and-failed-stage-retry.md) |
| 2026-10-17 | 截图歌单先核对勾选和编辑后再导入 | [具体修改](changes/2026-10-17-24-screenshot-playlist-confirmation-preview.md) |
| 2026-10-17 | 统一本地与在线搜索并增加候选试听和换来源 | [具体修改](changes/2026-10-17-23-unified-search-and-source-preview.md) |
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
