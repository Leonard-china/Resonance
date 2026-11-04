# 2026-10-30 — 改进一起听邀请确认、会话恢复、共同点歌与原生同步托管

- 修改日期：2026-10-30（Asia/Shanghai）
- 类型：功能 / 同步可靠性改进；最终设备验收与交付仍在进行
- 适用版本：version.properties 的 `0.6.5 / 39`，未升版
- 影响平台：commonMain、jvmMain、Android、Windows
- 关联：[实施规划](2026-10-30-01-product-positioning-and-listen-together-plan.md)、[歌词修复](2026-10-29-02-lyrics-centered-follow-and-manual-browsing.md)

## 需求或问题

用户要求按一起听规划开工，完成后构建 Release 并发送到本人的微信「文件传输助手」。无手机可用，使用 MuMu 测试与拍摄；同一 MuMu 的两个 ADB 端口不能当两台设备。此授权不包括 GitHub/官网公开发布。

原有邀请缺少单独有效期与审批，断开即失败，播放同步依赖 Compose；Connected 只说明连接建立，无法证明原生解码器已就绪。朋友无法共同点歌、接收歌词、预备下一首，退出没有恢复原本地队列。

## 原因与依据

复用原有 TLS、完整音频校验和代次清理，增加会话能力与原生播放所有者。保留证书锁定、房间密钥、256MiB 单曲及 512MiB 总缓存上限，不为缩短首曲等待而提前播放未验证音频。接入原生 ready 时还发现 Windows 相同速度设置会重启解码器、仅打开输出设备不能代表解码完成，已一起修正。

## 具体修改

| 文件 | 关键符号 | 修改与目的 |
| --- | --- | --- |
| [ListenTogetherService.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/ListenTogetherService.kt) | TogetherState / TogetherPlayback / TogetherRequest / TogetherQr | 审批、准备、重连、队列、请求、序号、歌词和邀请预览扩展；默认方法保留旧实现兼容 |
| [JvmListenTogetherService.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/JvmListenTogetherService.kt) | hostPeer / connectGuest / receiveGuest / lyricsMessage | 能力握手、TTL/刷新、审批、恢复凭据、有限重连、即时唤醒、RTT 中位数、预取、共享队列及歌词、请求审批；保留安全分帧与缓存所有权 |
| [TogetherPlaybackDriver.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/playback/TogetherPlaybackDriver.kt)、[TogetherSyncPolicy.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/playback/TogetherSyncPolicy.kt) | 原生同步调度 | 旧准备任务取消、原生 ready 后对齐、纠偏滞回；离开/重连期间不恢复旧声音 |
| [AndroidTogetherRuntime.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/together/AndroidTogetherRuntime.kt)、[PlaybackService.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/PlaybackService.kt) | get / bind / unbind | Application 持有会话，MediaSessionService 持有同步作用域；真实 ExoPlayer 播放头，焦点丢失/拔耳机退出跟听 |
| [AndroidPlatformServices.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt)、[DesktopPlatformServices.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopPlatformServices.kt) | managesTogetherPlayback / awaitSynchronizedReady | 两端原生管理；Windows 独立同步作用域，非房主降低轮询；Android Activity 关闭不直接 close Application 会话 |
| [DesktopPcmPlayer.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/audio/DesktopPcmPlayer.kt) | snapshot / isPrepared / setPlaybackSpeed | 真实输出播放头；首段 PCM 已解码才 ready；暂停准备不播放；相同速度不重启解码 |
| [TogetherPlaybackBinding.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TogetherPlaybackBinding.kt)、[App.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | UI 绑定 / TogetherLocalSnapshot / 审批回调 | 原生平台不重复操作播放器；退出恢复本地上下文且暂停；仅共享显式队列或当前曲；接受后才执行控制；分享当前已显示歌词 |
| [TogetherScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TogetherScreen.kt)、[NowPlayingOverlay.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | 房间与播放页入口 | 审批/准备/重连、邀请倒计时、刷新、原生分享、QR、队列和点歌；播放页顶部可返回房间；开关语义标签 |
| [TogetherInvitationPrompt.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TogetherInvitationPrompt.kt)、[MainActivity.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/MainActivity.kt)、[AndroidManifest.xml](../../composeApp/src/androidMain/AndroidManifest.xml) | VIEW 邀请入口 | 严格验证 resonance-listen，展示地址等待确认，不自动联网/发声，不转发嵌套 Intent、extras 或 URI 权限 |
| [TogetherAiQueue.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TogetherAiQueue.kt)、[TogetherSuggestions.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/TogetherSuggestions.kt)、[MusicFeatureController.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureController.kt) | AI 候选 / onQueueNext | 房主显式发送要求及候选标题/歌手，严格验证 1–5 个编号，预览勾选后才入队；不调用自由工具或直接切歌 |

### 状态、协议和数据边界

1. 生产 UI 默认需要房主确认。增强客户端显示等待审批；旧版仍在审批通过前不读取音频。邀请十分钟有效，刷新撤销旧邀请，不中断现有朋友。
2. 意外 IO 中断暂停跟听，按 1/2/4 秒退避最多三次。房主提供三十秒短期恢复窗口，恢复凭据独立于邀请并在成功时轮换。证书/认证/协议错误不自动重试，手动离开/解码失败清除凭据。不加入信令/中继。
3. 房主原生播放头每 100ms 观察，重要播放/暂停/seek/换歌唤醒发送，其余心跳 500ms。增强序号拒绝旧状态；RTT 最多七个样本取中位数。播放误差大于 250ms 才纠偏、通常冷却两秒，超过 1.5 秒立即纠偏；暂停误差超过 80ms 纠偏。这不是实测 P95 或物理出声承诺。
4. 音频完整校验后才交给原生播放器准备。房主开启并通过创建时非计费网络检查后，预取显式队列下一首。保留两首可播放媒体，淘汰文件延迟释放，缓存总上限与代次清理继续有效；回到已淘汰的歌曲可请求重传，不能请求任意路径/未共享曲目。会话中网络切换尚未动态重评计费状态。
5. 队列最多 50 首，用随机不透明 ID、标题/歌手，不传本机路径、整库或好友历史。控制请求有频率/数量限制，房主接受后才在本机执行。
6. 当前歌词最多 320 行、原文/译文各 128 字符、约 44KB 行数据，截断显示“歌词节选”；偏移也共享。好友端使用自己的临时 trackId 和“朋友分享”来源，不额外请求歌词服务。内嵌封面随音频读取；独立/远程封面尚未单独传输。
7. 退出后恢复当前 Compose 会话保存的原歌曲、队列、位置、随机和循环，保持暂停。完整 Activity/进程重建后的原上下文恢复仍待进一步验证。
8. AI 只发送房主最多 60 首候选标题/歌手及当前要求。不读取好友文件/历史；仅接受候选中不重复的 1–5 个编号，不把模型文本解释成路径/命令。失败不改队列，生成和确认时均检查房间身份。

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 打开/粘贴邀请 | 直接连接 | 展示地址确认，生产房间默认房主审批 |
| 邀请过期/泄露 | 房间结束前一直有效 | 十分钟失效，刷新立即撤销 |
| 网络短暂中断 | 立刻失败 | 暂停、显示恢复、有限重连，主动退出不自动重入 |
| 快速换歌 | Compose 准备与心跳同步 | 原生取消旧任务，仅当前媒体能 ready/播放 |
| 下一首 | 重新等完整传输 | 开启且符合条件时预取、校验后复用 |
| 共同选歌 | 无队列或点歌请求 | 分享显式队列，申请后由房主接受 |
| 歌词 | 只提示同步音频 | 分享当前歌词、译文与偏移 |
| AI 选歌 | 需切到通用助手 | 房间内生成候选，预览确认入队 |

## 平台与数据影响

没有新增权限、账号、云端服务或持久化偏好键；新增 Android VIEW intent-filter，仍 singleTop。邀请、QR、恢复令牌只存内存，不写 SavedState/配置。`.resonance` 格式、配置原子写、用户音频和元数据写回流程不变。保留前轮未提交修改。

TLS 是传输保护，临时磁盘文件未额外加密；朋友仍可看到对等地址及共享音频明文，不宣传 DRM/IP 隐匿。QR 复用 ZXing 3.5.3，无相机权限；通过支持链接的外部扫码工具打开，内置实时相机扫描未实现。没有读取/输出用户 Key、签名私钥或真实 keystore.properties。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| 定点阅读、Git 状态与原安全回归核对 | Windows | 已执行，保留既有歌词/拍摄改动 |
| desktopTest（together.* / TogetherPlaybackDriverTest）+ compileDebugKotlinAndroid | JDK17 | engine-build-7/8 通过；审批、过期、请求、代理故障恢复、预取与旧安全回归；早于后续 QR/歌词/AI 修改 |
| desktopTest（together.* / Together*）+ compileDebugKotlinAndroid | JDK17 | engine-build-9 通过；早于歌词与首 PCM 准备修改 |
| desktopTest + assembleDebug + assembleDebugAndroidTest | JDK17 | full-build-1 失败：241 项中 239 通过、1 可选桥接跳过、1 视觉夹具缺 currentTimeMillis；已补齐夹具，不能据此认定 APK 已生成 |
| 上述全量命令重跑 | JDK17 | full-build-2 通过，244 项：242 通过、2 可选桥接跳过；包含 30 次加入退出、QR 往返、歌词边界、淘汰缓存重传，生成新的 Debug/测试 APK，报告另存 full-test-results |
| Windows / MuMu 原生后台桥接 | Java Sound / MediaSessionService | native-windows-1 / native-mumu-1 均通过；Windows 实际首 PCM/输出准备、原生播放头推进；MuMu 实际播放器 ready 后退到后台，完成暂停、seek、继续约一分钟。使用 ADB reverse 受控直连，不证明外网可达或物理扬声器误差 |
| 十五分钟后台桥接及 Release 检查 | JDK17 / MuMu / Windows | release-and-native-15m.log 最终 BUILD SUCCESSFUL（24m30s），包含原生主机900秒、Android两种UnitTest、lintRelease、assembleRelease及packageReleaseExe；MuMu客机native-mumu-15m.log为OK1、917.478秒，CSV均已保存。晚于此轮编译的新导航修复仍需最终重建，尚未验签/发送 |
| 本轮 UI / 歌词 / 原生播放回归 | MuMu | 首轮20项两项失败：测试代理equals及一次歌词Compose层级丢失；修正代理后 mumu-regression-2.log 19项通过、1项需显式邀请的可选跨端测试跳过（runner 汇总为 OK20）。真机电话/蓝牙/移动网络未验证 |
| Release、验签、哈希、微信传输 | Windows | 首轮双端 Release 构建通过；含最新导航修复的最终重建、验签、哈希与微信传输尚未完成，将另留交付记录 |

日志：忽略目录 `output/together-2026-10-30/`。启动命令、旧包或夹具成功不能等同于新包验收。

## 限制与后续

- 已异步询问项目许可证与跨网协商/可选密文中继路线，尚无回复。没有擅自添加 LICENSE 或部署新服务。公开仓库不等于已有开源许可。
- MuMu :16384 与 :7555 是同一实例；已完成 Windows→MuMu 原生播放器一分钟及十五分钟后台桥接。反向桥接代码已新增但未编译执行；独立双模拟器、弱网与跨网测试仍待执行。
- Activity/进程重建后的原上下文、网络切换计费检查、独立封面传输、内置相机扫码、Windows 外部深链注册待后续验收或补齐。
- 新宣传片需另记真实素材与 AI 可用性、媒体 QA，不能伪造模型结果或将协议夹具当原生发声证明。

## 同步更新的文档

`docs/README.md` 索引、`docs/DEVELOPMENT.md` 7.1 节/能力及定位表、`docs/THIRD_PARTY.md` 的 ZXing/TLS 现行说明。协作规则未变。

