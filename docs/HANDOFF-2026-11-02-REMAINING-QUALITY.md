# 2026-11-02 — Music App Renaissance 剩余质量工作交接

最新结果：强制公网中继原生复测 Windows 1 项、Android 1 项全部通过，约 90 秒；首轮失败证据保留。最后额度读取为五小时窗口已用 90%、周窗口 44%，不能换算本任务美元费用。当前没有运行中的测试或自动任务；已结束隔离 validation 应用，未关闭用户正式应用。

## 推荐工具与模型

首选 **Antigravity：Gemini 3.8 Flash High**，以用户 2026-11-02 提供的实际选择器截图为准。此前推荐 Gemini 3.1 Pro 不适用于该列表，现已更正。[官方模型可用范围](https://antigravity.google/docs/models) 当前将 3.1 Pro 列为仅 Enterprise 可用，3.8 Flash 列为各档可用；High 档位由用户截图确认。Gemini 4 Argon 在该截图中不可用，不要求升级套餐。此选择是针对当前剩余实现与验收的建议，不是效果或费用保证。原接手任务、安全边界与验证要求不变。

只做纯代码回归/构建时，备选 **DeepSeek Harness：`deepseek-v4-pro`，思考模式开启**。[官方 API 文档](https://api-docs.deepseek.com/quick_start/pricing/) 当前注明该型号不支持视觉；不要让它仅凭源码声称截图已验收。这里没有启动这两个工具或新付费调用，没有可核实的本任务美元账单。

## 可直接粘贴的接手提示词

```text
继续 D:/Project/Resonance 的 Music App Renaissance 优化。先读 AGENTS.md、docs/README.md、docs/AI-WORKFLOW.md、docs/DEVELOPMENT.md 的相关章节，以及 docs/changes/2026-11-02-04-public-relay-deployment.md 和 05-native-public-relay-acceptance.md（完整文件名见索引）、docs/OPTIMIZATION_REPORT.md。不要重做全库审计。

Git 有大量他人/先前未提交改动；先检查并建立包含未跟踪文件的安全检查点，不 reset/clean/stash/覆盖。原生 Kotlin/Compose 项目，两端 Android/Windows，website 是独立官网。版本保持 0.6.5/39，不自动升版、推送、发布、发消息、清除数据或读取/输出凭据。用户允许实现、测试和本地构建，无只写代码限制。

已经完成：明暗主题、简化一起听、统计原子恢复与 IO 批量写入、原生计时和完播去重、偏好顺序保存、请求归属错误反馈。转场背景断言保持原像素阈值与生产弹簧，改为等待真实动画空闲；最新联合设备 10 项通过（历史证据，当前改动后不得冒充重新通过）。

生产中继已获明确批准并上线：wss://leonard.asia/resonance-relay/v1，loopback 45902，每 UTC 日 2 GiB 应用负载。现网 Tunnel 是本地管理，不是 dashboard 路由；.cloudflared/config.yml 精确路径在前，其他仍到官网 50034。不要再部署一次、改 DNS/Token/启动器，不能扩大预算或压测。后续额外生产配置变更需先准备具体方案再征求批准。当前 Linux 9 项、公网 Kotlin 4 项、官网 200 的证据在变更 04；原生公网测试的最新成功/失败以变更 05 为准。

仍有实现工作，不只是打包。按下面顺序做有界迭代，每项完成独立记录：
1. 确认变更 05 双端原生复测最终状态，失败先定位，不删断言。Windows 是真实 FFmpeg/Java Sound，Android 是 MuMu 12 的隔离 validation 包；只使用合成音频。该测试没有覆盖独立运营商网络、多人或长期运行。
2. 设计并最小实现完整加载/缓冲反馈。先读 commonMain/model/LibraryModels.kt 的 PlayerState、platform/PlatformServices.kt、playback/PlaybackStateStore.kt、App.kt，以及 AndroidPlatformServices.kt 的 MediaController 回调、PlaybackService.kt、DesktopPcmPlayer.kt 与 DesktopPlatformServices.kt。准备任务、用户播放意图、原生实际输出、Buffering、错误必须区分；沿用请求代次，防 A→B→A 旧事件，暂停/失焦不被迟到 Ready 重播。不要在 UI 自行计时猜测已就绪，不把保存失败当成解码失败，不改变旧快照/用户配置格式。检查一起听本机状态与远端意图的边界，不把新增状态字段偷偷塞进协议。
3. 对以上实际修改增加有意义的回归：准备中暂停/快速切歌、缓冲后恢复、失败退出加载、后台附着、旧请求事件不覆盖当前曲目。原生两端编译，必要的 targeted tests，再一次整体验证；不每改一行全库重跑。
4. 在已有全屏播放器/迷你播放器接入明确而克制的状态提示，保持明暗主题、键盘可访问性和减少动态。必须实际原生截图/预览验证，不用生成图伪装当前软件。依据新 UI 反馈需求才读相关 skill。
5. Windows 本地候选之前只证明进程存活，未证明可见窗口和操作。用隔离 RESONANCE_DATA/RESONANCE_LIBRARY 启动实际运行目录，经可用 computer-use 工具检查首页、导航、播放器/设置；不要关闭用户原实例或用真实库做试验。改动产品代码后旧包失效，应完成检查后生成本地候选，不发布。
6. 性能先测量：使用已有 MusicStageFrameMetricsTest 等诊断入口，找到有证据的瓶颈再局部优化。Debug/模拟器数据不能声称 Release 真机满帧。未发现瓶颈则记录测量和局限，不为代码美观重构。
7. 独立网络 Android↔Windows、多人/长时、断线重连仍需可用设备和网络；缺少时准确记录阻碍，不用同机两连接冒充。边缘限速只先评估，不新增生产规则/购买服务。最后按实际改动运行类型/编译、Lint、回归、构建和截图验收，更新 docs/OPTIMIZATION_REPORT.md。

每个独立功能/修复必须新增 docs/changes/实际Asia-Shanghai日期-NN-主题.md（使用 CHANGE-TEMPLATE），更新 README 索引与 DEVELOPMENT 当前事实。保留失败/跳过/环境受阻，不把旧产物或旧日志记为本次通过。额度不足先保存代码、日志、检查点和接手提示词；不启动多模型并行、不无限自主重试。
```

## 本机验证入口与定位

- 仓库：`D:/Project/Resonance`。最早候选及完整验证日志：`build/final-20261102/`；后续转场：`build/transition-relay-20261102/`；服务上线：`build/relay-deployment-20261102/`。
- 原生公网：`build/relay-native-20261102/`；`run2/` 保存完成握手复测；私有邀请/身份/合成音频不提交。两端只测试当前房间，不联系真实用户。
- Android 设备目前 `127.0.0.1:16384`，目标包 `com.resonance.player.validation20261102`，runner 包尾缀 `.test`。临时隔离构建脚本 `build/final-20261102/isolated-android.init.gradle`。先确认仍连接/包名准确；不要给正式包 clear data。
- 原生公网 Windows opt-in 参数及 Android 单方法命令见变更 05；现有 Python 驱动 `build/relay-native-20261102/run-android-guest.py` 只适用于本机路径，修改输出目录后再用，不复制为通用生产资源。
- 历史完整基线：desktop 290（286 过/4 跳过）、Android unit 55、Lint 0 error/107 warning；最新项目源码改变后必须重新运行相关检查。
- 当前没有新的播放器 Release：`build/final-20261102/candidates/` 属于变更 01，候选不是正式发布。产品源码若继续修改，必须重新建立产物对应关系。

这里的源码定位是下一步设计入口，完整加载/缓冲方案尚未实现或验证；不要把交接建议记作已完成成果。
