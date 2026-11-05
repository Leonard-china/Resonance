# 2026-11-05 — 缺陷修复与重新验收

**本次有效结果：589项，575通过、0失败、14条件跳过。** 共处理原报告12项问题：11项行为修复，Q07按原验收选项明确能力边界；真实BPM分析/筛选仍未实现。此前手机10条失败断言现已全部通过对应类复验。本轮设备首次执行75通过/2失败/7跳过；修复确认层级及教程旧标题后，23项设备定点复验通过，有效计数按类取最后结果，不重复累加。

版本0.6.6 / 40不变。基线HEAD `846aa16`，含用户已有未提交重构/UI改动；在现有工作区定点修复，未回退这些改动。358份实现/测试/版本文件的新摘要见[源码校验](fixes/source-sha256.json)。[191项功能清单](FUNCTION-CHECKLIST.md)及[原问题](BUGS.md)、[原手机报告](ANDROID-DEVICE-REPORT.md)保留修复前观察，不把本轮定点通过扩展为所有系统/外部服务均已验收。

## 修复对应

| 原编号 | 实际变化与边界 | 原因、代码与数据影响 |
| --- | --- | --- |
| Q01/Q08 | 导入先校验歌单/引用/路径；提交异常回退，限制清单/空间并分段取消 | [独立记录](../../changes/2026-11-05-18-sync-import-rollback-and-budgets.md) |
| Q02 | 临时离线的曲目/收藏/关系保留，编辑其他曲目后可重连 | [独立记录](../../changes/2026-11-05-19-retain-unavailable-library-tracks.md) |
| Q03 | 桌面专用偏好统一恢复.bak，再保存且保留其他键 | [独立记录](../../changes/2026-11-05-20-recover-specialized-desktop-preferences.md) |
| Q04 | 模型仅提具体音频删除请求；本机确认、会话/期限/来源复核，单次消费 | [独立记录](../../changes/2026-11-05-21-assistant-local-audio-delete-confirmation.md) |
| Q05/Q06 | 保留录音版本、默认零删除选择；完整批次明细一次确认 | [独立记录](../../changes/2026-11-05-22-smart-cleanup-safe-batch.md) |
| Q07 | 明确尚不支持BPM筛选；消除标签误导，未实现音频节拍检索 | [独立记录](../../changes/2026-11-05-23-semantic-search-bpm-capability-copy.md) |
| Q09/Q11 | 删除IO失败保留记录；部分批次只移除成功项；保留共享/用户封面 | [独立记录](../../changes/2026-11-05-24-delete-failures-and-shared-artwork.md) |
| Q10 | Android同步commit=false抛错；受控多偏好回退验证 | [独立记录](../../changes/2026-11-05-25-android-preference-commit-errors.md) |
| Q12 | 导航响应/缓存/过渡/历史/当前页面点击均服从最新意图 | [独立记录](../../changes/2026-11-05-26-website-latest-navigation-wins.md) |

## 本次执行结果

| 验证 | 通过 | 失败 | 条件跳过 | 证据 |
| --- | ---: | ---: | ---: | --- |
| 共享/JVM/Linux桌面：99类，最新有效用例合并 | 427 | 0 | 7 | [逐用例](fixes/jvm-results.json) |
| HONOR Android16/API36：34类 | 77 | 0 | 7 | [逐用例](fixes/android-results.json) |
| 官网Python | 45 | 0 | 0 | [日志](fixes/python-website.log) |
| relay/rendezvous/deployment Python | 20 | 0 | 0 | [中继](fixes/python-relay.log)、[寻址](fixes/python-rendezvous.log)、[部署脚本](fixes/python-deployment.log) |
| 官网真实JS函数/点击处理器的Node隔离夹具 | 6 | 0 | 0 | [六项日志](fixes/navigation-tests.log) |

编译：完整桌面202份主源码、共享/桌面100份测试源码，完整Android195份主源码及34份设备测试源码均通过。用Kotlin/Compose 2.3.20 CLI完整编译，再由Gradle消费**全部本次类**生成Debug主APK与测试APK；不是旧产物或少量类覆盖。打包任务为 `:composeApp:assembleDebug :composeApp:assembleDebugAndroidTest --offline --max-workers=1 --no-daemon -Pkotlin.compiler.execution.strategy=in-process`；编译/打包整个进程树2GiB上限，phone控制896MiB，均禁用进程树swap，共享验证锁串行并保留主机1536MiB余量，未放宽限额/改swap/关闭用户应用。

执行方式：JUnitCore按类运行；图像类改为 `Request.method` 按22个方法独立进程运行，完整70场景渲染通过（单方法约89.3秒）。调用 `adb shell am instrument -w -r -e class <类> -e refactorLifecycle true -e isolatedNative 1 <隔离测试包>/androidx.test.runner.AndroidJUnitRunner`；Python逐目录 `python -m unittest discover -s <website/tests|services/relay|services/rendezvous|services/deploy/aliyun> -p 'test_*.py' -v`；Node为 `node --test website/tests/test_navigation.cjs`。

首次本轮桌面412项中有2个旧契约失败、7跳过，另图像整类150秒超时；[首次结果](fixes/jvm-first-run.json)保留。旧助手测试要求模型直接删除、旧歌词测试要求已经移出视口的Lazy行仍存在，现按新安全契约/真实虚拟化检查，同时保留确认后删除、当前行居中和高亮断言。[71项定点重跑](fixes/jvm-targeted-rerun.json)与[22项逐方法图像复验](fixes/jvm-visual-method-rerun.json)均通过，有效计数不重复累加重跑。默认PNG关闭仍不足以让图像整类在150秒内结束，因此如使用本地有界运行器应逐方法隔离；不宣称默认整类150秒已经通过。

首次设备执行还发现新增批量确认层被页面盖住（真实点击未进入回调），已将确认层移到同一Box内最后绘制；教程四页/完成状态已通过，但终态仍断言旧标题“你的歌单”，改为复核Library路由与实际曲库节点。[首次设备结果](fixes/android-first-run.json)与[23项复验](fixes/android-recheck.json)均保留；设备有效计数只取最后结果。最终UI层级改动后重做完整双端源码/测试编译与Debug打包、71项JVM定点和3项桌面UI复验，均通过。[桌面UI补验](fixes/jvm-visual-followup.json)不与原22项重复计数。

实现过程中完整编译曾因App状态入口、遗漏Android内容URI助手和一次正则编辑语法失败；均已修正并通过最终完整编译。中间日志留在忽略的 `build/quality-fixes-2026-11-05/`；没有将这些中间状态当作通过。

## 手机隔离与原失败复验

首次安装前核对独立包及测试包均不存在。主包安装等待荣耀身份验证导致ADB180秒超时，用户完成系统验证后安装成功；续跑从设备拉回该隔离包base.apk，SHA-256匹配本次产物才复用，随后补装测试包。安装等待不计功能用例失败，见[恢复事实](fixes/install-recovery.json)。第一次包的[输入校验](fixes/first-apk-inputs.json)单独保留；确认层修复后重建两份APK，再安装同一独立ID执行定点复验。仅使用 `com.resonance.player.validation.quality.validation20261105refactor` 和其`.test`，输入APK每次从本次实际文件计算SHA-256，见[本次产物](fixes/apk-verification.json)。音频、图片、歌词、对话与偏好全部合成；没有读正式应用私有数据、真实歌曲、API Key或签名私钥，没有收费API/生产论坛写入。测试后两个独立包均卸载成功；正式包版本与更新时间前后相同，不据此宣称私有数据库逐字节相同。

| 原失败 | 本次处理与保留的检查 |
| --- | --- |
| D01–D02 转场像素 | 透明交叉淡入/父背景与裁剪，终态、反向和减弱动态仍检查；不恢复旧opaqueCanvas |
| D03 推荐关闭 | 卡片隐藏符合当前设计，仍从设置到达推荐开关 |
| D04 封面 | 修改实际高度分配，正常高竖屏180dp门槛通过；歌词/队列/seek/关闭真实动作继续执行 |
| D05 帧样本 | 明确80次100ms间隔进度变化并切歌；实际窗口FrameMetrics通过样本门槛 |
| D06–D08 房间 | 当前标签、固定页头直接点击，仍验证显式加入、审批、创建/关闭 |
| D09–D10 教程 | 滚动到真实帮助入口，四页快速教程与跳过/不重复欢迎通过 |

新增设备用例验证本机删除按钮、两组重复项一次整批确认/取消、commit=false/多存储回退，以及受管文件暂离线后编辑另一首并重连。当前设备运行中的进程异常重跑情况以[设备逐项结果](fixes/android-results.json)的earlierAttempts为准；没有该字段则本轮未触发重跑。

帧诊断：144帧，p95 83.87ms，超deadline 89/144。刺激、采样量与窗口统计见[原始JSON](fixes/frame-metrics.json)。这是Debug/合成状态更新诊断，不是静止FPS、真实音频解码吞吐或Release性能承诺。本次89/144帧超过窗口deadline，采样用例通过只表示获得足够有效样本，不能称为性能优化验收通过；Release帧预算仍需独立测量。

原生截图：[手机封面](fixes/screenshots/phone-cover.png)、[手机歌词](fixes/screenshots/phone-lyrics.png)、[桌面竖屏](fixes/screenshots/desktop-portrait-cover.png)、[桌面大字](fixes/screenshots/desktop-large-text-cover.png)、[桌面横屏歌词](fixes/screenshots/desktop-landscape-lyrics.png)。只显示本次合成测试内容。手机封面/歌词/大字截图和帧JSON来自首轮本次隔离包；其后仅调整确认层绘制顺序与教程测试定位，这两处用最终重建包定点复验，保留[首轮源码摘要](fixes/first-device-source-sha256.json)。

## 仍未执行与能力边界

- Windows真实窗口/外置盘拔插/被占用文件，全部ROM、折叠屏/无障碍读屏、长时锁屏/节电、真实系统目录/删除授权故障未全部验收；条件跳过不算通过。
- 真实AI/ASR付费接口、账号歌源/B站、跨网多设备/生产公网、官网部署与SMTP未执行；本地TLS/注入传输不替代这些。
- 导入仅承诺运行期失败回退；断电、实际硬件损坏/磁盘满与回退同时失败不能保证多文件原子恢复。恢复失败保留副本及错误。
- BPM筛选未实现；疑似重复不是音频指纹证书。删除保留封面可能产生未回收图片；物理删除后的元数据写失败无法撤销已经删除的音频。
- 360帧宣传图导出未启用，不计功能用例通过。未发布、未升版；本次手机验收使用隔离Debug包。
