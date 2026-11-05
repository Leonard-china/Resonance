# 2026-11-05 — 手机重连后的Android实机续测

> 后续修复与本次重新验证见[修复验收报告](FIX-VALIDATION.md)。下文保留修复前的原始观察和计数，不用修复后的结果覆盖历史证据。

**有效结果：30个测试类、79个用例，62通过、10失败、7条件跳过。** 首轮59通过/10失败/7跳过，最后一组未产生3个用例结果；同包同源码重新独立安装后，该组3项通过，最终只计一次。此次仍不能判定手机全功能验收通过。

本报告承接[前轮总报告](FEATURE-TEST-REPORT.md)的手机断连缺口。10个失败是实际断言失败，不直接等于新增10个独立产品Bug：其中存在旧界面/旧转场契约、定位方式与当前源码不一致，也有尺寸验收和性能测量条件未满足。没有修改断言把失败改成通过，没有改产品代码；前轮[12项问题](BUGS.md)仍未修复。

## 基线、设备和隔离

- 手机：HONOR VKJ-AN00，Android 16 / API 36；本轮读取系统属性确认，屏幕唤醒、USB已授权。
- 源码：与前轮报告摘要完全一致，版本0.6.6 / 40；无源码变化，无需重建。主包输入SHA-256为 `3b1762a67a7b36fc8aa9b35d140fd08c0eeec4361c8a1495b15a6d9f991c03fa`，测试包为 `a15e4d89adad8f1d5f530668b1ef78cac8931e461f88bcba2a962d76e7c1d81e`，均在安装前重新计算匹配；不宣称本轮从设备拉回APK做了逐字节比较。
- 独立主包：`com.resonance.player.validation.qa.validation20261105refactor`；测试包为其`.test`。安装前确认二者不存在，仅安装这两个包，结束后两者均卸载成功。
- 正式 `com.resonance.player` 的版本、versionCode与更新时间在两轮操作前后相同，未覆盖、卸载或清除正式应用。没有读取正式应用私有资料来声称数据库逐字节相同。
- 音频、封面、歌词、凭据和存储均为独立包里的合成夹具；原生播放统计走真实Media3和静音音频，收费语音识别用假传输。未授予独立包系统悬浮、外部音频扫描或用户真实目录访问，不使用真实AI/B站/公网房间凭据。
- 两次host操作串行共用仓库验证锁，整个进程树 `MemoryMax=896M / MemoryHigh=640M / MemorySwapMax=0`，启动可用5999/6043MiB，最低门槛2432MiB并持续保留1536MiB。systemd记录峰值182.9/213.8MiB，没有本轮编译、OOM、放宽预算、改swap或关闭用户应用。

## 操作与计数

第一轮16:00开始，约5分19秒。执行现有30类时传入 `refactorLifecycle=true`、`isolatedNative=1`，使独立包生命周期/原生统计测试真正执行；其余公网/收费/宣传桥接的opt-in不启用。每类新一次 `am instrument`，使用本轮已有Debug应用和测试APK。

最后的 `UiInteractionContractTest` 首轮只返回 `INSTRUMENTATION_RESULT: shortMsg=Process crashed.`、`INSTRUMENTATION_CODE: 0`，没有用例级结果。未从这个消息推断为产品启动崩溃或内存不足。只重跑该类，重新安装相同哈希的独立包、等待安装收尾后执行，约3.786秒返回 `OK (3 tests)`。首轮原始异常仍保留，根因没有定位，不能宣称已修复。

复核安装遇到系统安装器对未上架应用的确认，只确认当前授权的独立测试APK；未开启/关闭可选设备安全模式或修改系统默认策略。两次应用/测试包安装与卸载均有本地日志。

- [Android有效汇总](android-summary.json)
- [首次异常与单独复测](android-process-recheck.json)
- [逐用例执行结果](TEST-RESULTS.md)、[全部有效记录JSON](test-results.json)
- [正式包信息与独立包清理核对](android-isolation-check.json)

## 已通过的主要实机路径

| 功能 | 本轮通过的断言 | 边界 |
| --- | --- | --- |
| 应用与媒体服务 | Activity连续三轮重建/关闭、主线程心跳、旧Controller不能控制新会话、命令授权、后台切歌全局音量 | 不是厂商锁屏/省电长时验收 |
| PCM/响度/统计 | 单声道PCM、FFmpeg整曲分析缓存、后台/重建/暂停/seek/原生循环计数与完播去重 | 生成静音/合成音频，不含真实音乐听感评价 |
| 歌词 | 预览可见、当前句焦点、双语换行居中、手动浏览与返回/自动恢复、离屏seek、大字/视口变化、切歌与翻译代次 | 相关类共16项通过；不含真实公网歌词服务 |
| OCR与媒体入库 | 本地模型读中英截图、长图解码边界、MP3标签/封面、内嵌封面回退、假ASR与临时音频清理、独立凭据加密 | 没有上传真实截图或调用收费接口 |
| 导出和原生弹窗 | 目录Intent边界、范围弹窗、结果反馈、音量/倍速弹窗、音效打开选择关闭 | 不代表本轮点击了真实用户大库导出/系统下载目录 |
| 导航与控件 | 多数列表/标签/助手/播放页开合、横向手势、原生进度和音量语义、最小触摸目标、封面颜色采样 | 失败项和未走到的步骤见下方，不能合并写成导航全通过 |
| 一起听 | 手机上的回环TLS证书和合成音频校验、伙伴/审批部分交互、外部邀请确认 | 不含Windows+手机跨网真实播放 |

## 10项失败及判定

以下编号D01–D10只是失败记录，不与前轮Q01–Q12的已确认产品缺陷混算。完整堆栈见[失败JSON](android-failures.json)。

| 编号 | 用例 | 期望与实际 | 当前判定与剩余验收 |
| --- | --- | --- | --- |
| D01 | `LibraryDetailTransitionTest.openingCoversTheListAndClipsAtTheViewport` | 期望进入中面板以Canvas完全遮住红色旧页；实际采样约 `(0.263,0,0)` | 当前 `LibraryDetailTransition` 委托透明短位移/淡变，旧测试还要求转场自身补不透明底色。组件旧契约与实现不一致；没有直接证实真实页面发生同样闪底 |
| D02 | `LibraryDetailTransitionTest.returningKeepsTheDetailAboveTheListInDarkMode` | 期望深色Canvas约 `(0.043,0.043,0.047)`，实际约 `(0.318,0,0)` | 同上；需要按现行页面背景/过渡规范重新核对，不能把颜色断言取消后就宣称解决 |
| D03 | `LibraryNavigationTest.disabledRecommendationCardAppearsBeforeUserPlaylistsAndOpensSettings` | 未找到“每日推荐” | `PlaylistGrid` 当前仅在启用推荐或教程步骤进行中显示卡片；用例仍假设未开启时始终显示。失败发生在找卡片阶段，未继续验证此路径打开设置 |
| D04 | `MusicStageDeviceTest.lyricsQueueSeekAndDismissUseRealControls` | 封面宽151.42856dp，小于180dp门槛 | 尺寸验收未通过。该方法在尺寸断言停止，后续seek/歌词/队列/收起未执行；其他专门用例通过不能补写成本方法通过。原生样例中播放控制可见，封面目标尺寸仍需确认 |
| D05 | `MusicStageFrameMetricsTest.playingStageReportsActualWindowFrameDurations` | 要求至少60个有效FrameMetrics样本，仅15个 | 采样条件失败，未生成有效p95/丢帧率报告。静止UI不必持续产帧，不能把15个样本除以采样时间当FPS，也不能据此判定Release卡顿 |
| D06 | `TogetherUiDeviceTest.roomCreationAndClosureUseExplicitActionsAndKeepPortVisible` | 未找到“创建双人房间” | 当前按钮文案为“创建房间”，旧定位失效；该方法尚未执行创建/关闭后续步骤 |
| D07 | `TogetherUiDeviceTest.openingOrPastingAnInvitationDoesNotConnectUntilExplicitJoin` | 加入后未找到“跟听中 · 由房主控制播放” | 当前房间页已重排状态标题，该完整旧文案在实现中不存在。确认加入前步骤已运行，不能把旧状态文案断言失败写成TLS或加入失败；后续退出步骤未走到 |
| D08 | `TogetherUiDeviceTest.incomingFriendMustBeApprovedAndControlsRemainAccessible` | 对“结束房间”调用 `performScrollTo`，报告无滚动父布局 | 房间结束/退出已移到固定页头，按钮不再位于滚动容器内。与当前结构不一致，失败不证明按钮本身不可点击；须更新定位后再测结束操作 |
| D09 | `TutorialNavigationTest.settingsOpensQuickGuideAndAllFourPagesCanBeRead` | 点击“使用教程”后找不到“开始浏览” | 设置教程入口已在索引下方“帮助”组，用例未先滚动到入口；疑似定位假设失效。未补充滚动后的完整四页操作，不能判快速教程流程通过 |
| D10 | `TutorialNavigationTest.welcomeCanBeSkippedAndIsNotOfferedAgain` | 跳过后“使用教程”节点不在可见区 | 跳过状态保存与欢迎框消失断言已执行；末尾直接要求教程入口首屏可见，与当前可滚动设置布局不一致。实际后续可达性仍需补测 |

当前证据支持把若干项列为“测试契约/定位需同步”，并保留D04尺寸要求、D05测量方法及真实页面转场的验收缺口；**没有擅自修改测试、设计或入口来消除失败**。

## 条件跳过7项

- 3个宣传/真实AI配置与录制用例：没有启用相关opt-in。
- Windows主机→物理Android的一起听桥接1项：没有传入外部邀请。
- 原生跨设备播放/真实助手可用性探测3项：没有启用真实主机/公网/AI配置。

这些用例由运行器以条件跳过结束，不属于“未启动整个手机测试”。本地双Peer TLS用例已执行通过，但不能代替跨网双设备播放。

## 原生图像与限制

- [手机原生播放界面](screenshots/phone-native-playing.png)：真实手机测试内容截图，使用原创歌词与合成曲目；不是用户日常曲库。
- [手机原生歌词预览](screenshots/phone-native-lyrics.png)：第2句可见并高亮。
- [转场颜色失败夹具](screenshots/phone-transition-fixture.png)：人为红旧页/蓝标记/黑背景的组件夹具，不是产品页面截图，不能作为真实产品红屏证据。

本轮未执行真实外部音频/SAF/悬浮/通知授权、真实文件管理器与系统分享、Windows、付费AI/语音、B站账号、公网歌源/一起听、两小时后台和故障断电。原有1个桌面批量截图方法仍超时。以上缺口与10个断言失败保留，不能称为全功能或全平台通过。
