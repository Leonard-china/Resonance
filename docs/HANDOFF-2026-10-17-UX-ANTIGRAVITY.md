# 2026-10-17 — 用户体验优化完成情况与 Antigravity 最终构建部署交接

- 当前工作区：`0.4.9 / 32`，版本号未改。
- 本轮实施目标：用户确认落地全部体验建议，并追加偶发页面顶端黑边反馈。
- 已修改源码和文档，未执行提交、推送、正式安装包发布或生产网站操作。
- 最终构建与网站部署按用户安排交给 Antigravity；本文件同时提供可直接粘贴的提示词。

## 1. 已完成的代码与独立记录

| 功能 | 记录 |
| --- | --- |
| 统一本地与在线搜索并增加候选试听和换来源 | [2026-10-17-23-unified-search-and-source-preview.md](changes/2026-10-17-23-unified-search-and-source-preview.md) |
| 截图歌单先核对勾选和编辑后再导入 | [2026-10-17-24-screenshot-playlist-confirmation-preview.md](changes/2026-10-17-24-screenshot-playlist-confirmation-preview.md) |
| 按一次操作汇总任务并批量重试失败媒体步骤 | [2026-10-17-25-grouped-tasks-and-failed-stage-retry.md](changes/2026-10-17-25-grouped-tasks-and-failed-stage-retry.md) |
| 区分离线音频状态并支持恢复和重新定位 | [2026-10-17-26-offline-audio-status-and-relink.md](changes/2026-10-17-26-offline-audio-status-and-relink.md) |
| 新增推荐历史与个人反馈管理页 | [2026-10-17-27-recommendation-history-and-feedback-management.md](changes/2026-10-17-27-recommendation-history-and-feedback-management.md) |
| 接通双端原生睡眠定时与播完当前歌曲停止 | [2026-10-17-28-native-player-sleep-timer.md](changes/2026-10-17-28-native-player-sleep-timer.md) |
| 增加 Windows 搜索播放切歌和返回快捷键 | [2026-10-17-29-desktop-keyboard-navigation.md](changes/2026-10-17-29-desktop-keyboard-navigation.md) |
| 自动推荐支持不计费网络限制与下次到期提示 | [2026-10-17-30-automatic-recommendation-network-and-schedule.md](changes/2026-10-17-30-automatic-recommendation-network-and-schedule.md) |
| 进一步覆盖页面切换顶部背景并减少系统栏重设 | [2026-10-17-31-stable-page-viewport-and-top-edge.md](changes/2026-10-17-31-stable-page-viewport-and-top-edge.md) |

内置教程和 AssistantHelp 已同步本地 OCR、确认预览、统一搜索、任务分组、反馈历史、睡眠、网络与键盘说明；根 README 修正了“点赞保留”的旧说法。docs/README 的旧版本头部也已核对为 0.4.9 工作区。

## 2. 本轮实际验证

验证平台：Windows，JDK 17.0.12，Gradle Wrapper。以下结果来自本次实际命令，不复用历史发布报告。

| 命令／检查 | 实际结果 |
| --- | --- |
| `gradlew.bat :composeApp:compileKotlinDesktop :composeApp:compileDebugKotlinAndroid --no-configuration-cache --console=plain` | 最初编译失败：局部函数声明顺序及桌面播放器初始化类型推断；修正后两端通过。 |
| `gradlew.bat :composeApp:compileDebugKotlinAndroid :composeApp:compileDebugAndroidTestKotlinAndroid --no-configuration-cache --console=plain` | 通过；包含浅／深色快速切页顶部像素测试的 Instrumentation 源码。 |
| 下列定点命令（主轮） | 通过，9 个类 **68 项**，失败 0、跳过 0。 |
| `gradlew.bat :composeApp:desktopTest --tests 'com.resonance.player.features.TutorialProgressTest' --no-configuration-cache --console=plain` | 通过，**6 项**，失败 0、跳过 0。本项 6 项不与主轮重叠。 |
| `gradlew.bat :composeApp:compileDebugKotlinAndroid :composeApp:compileDebugAndroidTestKotlinAndroid :composeApp:desktopTest --tests 'com.resonance.player.platform.LibraryPersistenceTest' --no-configuration-cache --console=plain` | 最后补齐来源持久化后复验通过，4 项（原 3 项加重定位重启回归 1 项）。 |
| Windows 系统连接计费状态只读查询 | 实际返回 `Unrestricted`。没有验证计费网络切换。 |
| `git -c core.safecrlf=false diff --check`、新增文档链接／编号检查 | 通过；9 份独立记录的链接、模板、索引和当天编号核对均无错误。 |
| 完整 allTests、Release Lint、Release APK／EXE、安装与签名／哈希 | **未执行**，由 Antigravity 最后构建。 |
| Android 设备 Instrumentation 与双端 GUI／音频操作 | **未执行**。测试源码编译不等于视觉或后台播放实测。 |
| 网站测试、生产部署、SMTP／下载校验 | **本轮未执行**；网站源码未修改，不复用之前版本的通过结论。 |

最终合计 **75 项不同测试通过，失败 0、跳过 0**：主轮 68 + 教程 6 + 重定位新增 1；最后重跑的其余 3 项不重复计数。

主轮实际执行命令：

```powershell
.\gradlew.bat :composeApp:compileDebugKotlinAndroid :composeApp:compileDebugAndroidTestKotlinAndroid :composeApp:desktopTest --tests 'com.resonance.player.features.MusicAssistantTest' --tests 'com.resonance.player.features.MusicSourceRegistryTest' --tests 'com.resonance.player.features.RecommendationPolicyTest' --tests 'com.resonance.player.features.RecommendationFeedbackPolicyTest' --tests 'com.resonance.player.features.StorageAndAssistantExtraTest' --tests 'com.resonance.player.playback.PlaybackSleepTimerTest' --tests 'com.resonance.player.playback.PlaybackStateStoreTest' --tests 'com.resonance.player.MusicTaskGroupsTest' --tests 'com.resonance.player.platform.LibraryPersistenceTest' --tests 'com.resonance.player.TutorialProgressTest' --no-configuration-cache --console=plain
```

主轮的教程过滤名未匹配实际包名，故随后使用 `com.resonance.player.features.TutorialProgressTest` 独立运行 6 项，并未把未匹配过滤器当成通过。主轮 XML 和摘要已保留到忽略目录 `build/ux-validation/targeted-desktop-2026-10-17/`，教程 XML 在 `build/ux-validation/tutorial-desktop-2026-10-17/`，最后重定位回归在 `build/ux-validation/relink-desktop-2026-10-17/`；通常报告仍在 `composeApp/build/reports/tests/desktopTest/`，后续过滤运行可能覆盖它。

编译仍有项目原有弃用提示，以及状态栏对比度属性的弃用提示；没有把它们写成 Lint 已通过。

## 3. 必须保留的行为与数据边界

- 搜索／试听：普通歌源不调用模型。试听先准备最多 64 MiB 的临时音频，播放最多 30 秒；暂停主播放，停止／离页不自动恢复主播放，释放独立播放器和临时文件。来源可能失效，不绕过版权或试听限制。
- 截图：默认本地 OCR，不自动切换付费；预览可取消、勾选和编辑；确认前不创建歌单、不下载。草稿只在内存，确认 ID 消费一次；已入库的歌曲和失败任务持久保存。
- 任务：父级 ID 可选，旧记录兼容。最多保留 100 条及当前父链，过早子步骤可能淘汰。批量只重试免费、未完成的媒体步骤，不重放聊天、删除或收费同步歌词。
- 离线：`ownedAudio.evicted` 与真实 URI 可读性分别判断。重新定位保留旧 ID、收藏、歌单，仅更新来源与 MIME，通过 saveReplacementAudio 显式写入来源索引和歌单，不修改所选文件；下载恢复沿用原有路径和所有权保护。
- 反馈：点赞和收藏独立；普通踩不删音频；“不喜欢并删除”保持明确物理删除语义。新 `recommendationHistoryDetails` 从本次更新起保存可回看的名称和时间，365 天裁剪；旧哈希不虚构名称，旧反馈仍可编辑。
- 睡眠：15／30／45／60 分钟或曲终停止，暂停期间仍倒计时；Android 由 PlaybackService 拥有，Windows 随应用／托盘作用域继续，进程终止不恢复。Android 曲终使用 [Media3 原生暂停接口](https://developer.android.com/reference/androidx/media3/exoplayer/ExoPlayer#setPauseAtEndOfMediaItems(boolean))，再抑制共享队列推进。
- 网络：新偏好 `recommendationsUnmeteredOnly=false` 兼容旧配置；自动推荐等待适合的网络，手动更新明确立即联网。Windows 查询无法判断时不自动放行。只沿用已有网络状态权限。
- 黑边：固定视口、每页 Canvas、禁用尺寸插值、主题窗口背景和系统栏更新均已修改；**本轮没有在用户出现问题的设备上复现或验收，不能保证所有 ROM 已消除**。
- 播放代次、配置写入屏障、原子文件／备份恢复、原音频转换、删除所有权和同步包边界保持原有机制；不得用真实用户歌曲做删除或中断实验。

## 4. 设备验收建议

1. 全新空库先搜索在线歌曲；本地／在线切换保留关键词，下载错误能原位看到并选择同歌其他来源。
2. 试听播放、停止、切页、取消准备、正在试听时播放曲库歌曲；确认无两路音频、无遗留句柄与临时文件。
3. 多张截图预览，修改歌名并去选一部分，取消后零下载；确认后只导入所选。重复确认、离页和中断后失败重试不重复已完成音频。
4. 删除隔离夹具／释放下载后，检查四类状态、恢复、重新定位，以及重启后的旧 ID、收藏、歌单关系。
5. 推荐换批后，历史点赞／踩／评论仍可筛选编辑，撤销踩能解除旧拒绝；点赞不会误变成收藏。
6. 睡眠计时期间切页、锁屏、后台、托盘；取消和替换；曲终停止覆盖队列、单曲循环、全曲循环。
7. Windows 输入框中空格和 Ctrl+方向键保持编辑功能；输入框外可播放／切歌；Esc 正确关闭播放器／退出多选／返回；Ctrl+F 立即进入并聚焦搜索。
8. Android／Windows 计费与不计费网络切换，自动等待，手动请求可继续；核对下次到期文案和后台调度。
9. Android 浅／深色快速切换主导航、工具、设置、返回；滑动中取消、快速返回、软键盘与减少动态模式；重点录屏检查顶部状态栏和内容顶边。新增像素测试只覆盖 Compose 内容，系统栏仍需真机观察。

## 5. 最后构建与官网部署

- 按 docs/DEVELOPMENT 的构建章节跑完整检查与双端正式包，不把当前机器上的旧 APK／EXE 当成本次产物。
- 本轮没有升版。先核对 version.properties 与现网版本；正式发布若需更高版本，显式更新唯一版本源并新建对应 RELEASE_NOTES。同步当前版本相关测试断言与发布说明输入，不手改 BuildVersion.kt。
- 在隔离目录检查升级和数据保留，从本次真实包生成大小、签名检查及 SHA-256；同一版本的安装包与网页公告必须一致。
- 先读 website/README.md、website/OPERATIONS.md。现网是翼龙容器与 Cloudflare Tunnel，不是直接替换 systemd 的通用 Ubuntu 主机。
- 备份源码、SQLite 在线备份与必要私有运行数据；保留 .env、data、.venv、SMTP、Java 启动器、start.sh 和 tunnel token。不要把凭据、日志中的认证参数或真实运行数据带进 Git。
- 跑网站测试，按受控 releases 发布流程两端一起校验后切换，再更新实际版本公告／介绍。只同步源码或 version.properties 不等于安装包更新。
- 核对所有下载通道实际指向的新版本，官网直连 Range、文件大小、哈希，以及公开页缓存失效；未更新的备用通道不能继续标为新版。论坛与后台保持动态。
- 发布安装包和群发邮件是独立操作，本次不向真实订阅者发送测试邮件。
- 部署失败恢复备份代码并保护原数据；只有实际 HTTP、版本、下载与校验验证后才报告上线。

## 6. 可直接粘贴给 Antigravity 的提示词

```text
请接手 D:\Project\Resonance，完成这轮体验优化的最终验收、Android／Windows 正式构建和官网部署。先读 AGENTS.md、docs/README.md、docs/DEVELOPMENT.md、docs/AI-WORKFLOW.md、docs/HANDOFF-2026-10-17-UX-ANTIGRAVITY.md，以及 website/README.md 和 website/OPERATIONS.md，保留当前未提交改动。

本轮已实现统一搜索与试听、截图勾选编辑后确认导入、任务分组与失败批量重试、离线状态与恢复／重新定位、推荐历史反馈、双端原生睡眠定时、Windows 快捷键、自动推荐不计费网络限制，以及页面顶部黑边的进一步处理。双端源码和 Android 测试源码已编译，75 项相关回归通过；真机 UI、完整发布检查、正式安装包和网站部署尚未执行。

先用隔离测试数据验收完整流程，重点检查手机浅／深色下快速切主导航、工具、设置与返回时的顶部黑边；不要把仅编译通过写成闪烁已消失。还要检查截图取消／确认／中断重试、试听停止和临时文件清理、离线恢复后的 ID／收藏／歌单、睡眠后台与循环、输入框内外的快捷键和计费网络切换。发现问题定点修复并新增变更记录。

随后执行完整单元测试、Release Lint、Android Release APK 和 Windows EXE 构建。当前 version.properties 仍为 0.4.9 / 32，先核对线上版本；若需要正式升版，只从 version.properties 显式更新，并维护对应发布说明、关于页输入及版本相关测试。安装包大小与 SHA-256 必须从本次实际产物重新生成。

官网按 OPERATIONS.md 的现有翼龙容器／Cloudflare Tunnel 方式部署，保留生产数据、SMTP 配置、私有凭据、启动器和隧道。先备份并运行网站测试，使用受控发布流程同时更新两端安装包、版本介绍与公告；逐一检查下载通道、公开页面缓存和 Range 下载，确认提供的是本次实际产物。不要只上传版本文件后就宣称发布完成，不向真实订阅者群发测试邮件。

最后更新文档与索引，交付实际版本号、APK／EXE 路径及校验值、网站核验结果，并分别列出通过、失败、未执行和环境受阻事项。
```

