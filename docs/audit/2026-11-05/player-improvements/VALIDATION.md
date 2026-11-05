# 2026-11-05 — 歌词校准、播放、草稿恢复、路径修复与BPM验收

本次独立记录：[歌词面板](../../../changes/2026-11-05-34-lyrics-tools-scroll-and-layering.md)、[空闲开销／歌词索引](../../../changes/2026-11-05-35-playback-idle-work-and-lyric-index.md)、[设备失败恢复](../../../changes/2026-11-05-36-resume-playback-after-device-failure.md)、[截图草稿](../../../changes/2026-11-05-37-persist-screenshot-import-drafts.md)、[批量路径修复](../../../changes/2026-11-05-38-batch-relink-missing-audio.md)、[本地BPM](../../../changes/2026-11-05-39-offline-bpm-analysis-and-filtering.md)。

本次范围为独立变更34—39号；保留前轮一起听移除及用户未提交改动。版本保持0.6.6／40。本轮不安装或发布客户端，不操作线上服务，不读取真实签名或Key，也不使用用户真实音频。

**最终有效结果：143项通过，0失败、0跳过。** 按类／方法去重，普通回归中条件跳过的soak由本次显式300秒结果替代，不重复计数。完整[逐项结果](test-results.json)、[机器摘要](verification.json)及[当前源码摘要](source-sha256.json)随本记录保存。

## 实际验证方式

使用既有同版本Kotlin／Compose 2.3.20 CLI完整编译当前共享／JVM／桌面与Android生产源码、两端测试源码，随后运行JUnitCore及实际原生ImageComposeScene／Xvfb场景。最终源码摘要与编译清单在忽略目录`build/player-improvements-2026-11-05/`；测试运行类路径排除旧应用JAR。不是旧类局部拼接，也不是普通Gradle Release打包。

重量级工作串行持有`build/resonance-heavy-validation.lock`：主编译／回归进程树2GiB／1536MiB／Swap0，后台soak为1GiB／768MiB／Swap0；启动需硬上限外另留1536MiB，运行中不足时停止自身任务。未改系统swap或共享默认配置，未关闭用户应用。

## 本轮命令与结果

- `python3 build/player-improvements-2026-11-05/final_validation.py`：完整双端主源码／测试编译、定向核心回归与六项原生UI复查；完整桌面185／Android177份生产源码、桌面92／Android29份测试源码编译通过。核心137项中136通过、1项soak默认跳过；六项UI全部通过，另显式soak1项通过，按方法去重合计143通过。
- `python3 build/player-improvements-2026-11-05/soak.py`：显式`RESONANCE_PLAYBACK_SOAK_SECONDS=300`，真实FFmpeg＋JavaSound静音输出、原创合成音频、隔离数据目录。无主窗口持续运行，循环暂停／seek／恢复／重新打开并检查统计不因seek膨胀；通过：306秒、37轮暂停／seek／恢复／重开，听歌统计252秒，无seek膨胀；服务峰值120.5MiB。
- `LyricTimelineBenchmarkTest`：2000行歌词、20000次查询、五轮中位数，二分与线性参考逐结果校验一致。本次线性中位数60.831ms、索引1.049ms，约58.0倍；只测时间戳定位CPU耗时，不测整页FPS、GPU或功耗。
- 真实`DesktopAnalysisDecodeTest`：91秒合成120BPM WAV经生产FFmpeg解码，输出严格90秒／2880000字节，检测115—125BPM，原文件保留。
- `git -c core.whitespace=cr-at-eol diff --check`与本轮文档链接核对：通过；保留仓库混合CRLF及用户原有文件。
- `adb devices -l`：设备列表为空，未进行手机安装、系统设置操作或Perfetto记录；`LyricsToolsDeviceTest`只做Android测试源码编译。

## 首轮失败与修正

Android首轮编译发现共享代码使用`Files.readString`，在Android编译类路径不可用；改用已有MusicJsonStore的Android兼容读取和原子／备份机制后，完整双端编译通过。

首轮核心回归有两项测试夹具问题：新草稿测试的runBlocking推断为非Unit导致JUnit拒绝；新输出失败测试第一次只写入1帧，时间取整仍为0ms。分别显式`runBlocking<Unit>`、使用真实可测的2048字节部分写入后，原断言保留并定点复验通过。首轮结果保留在`desktop/results-first/`，不是隐藏失败；最终计数按类／方法去重，不累加重跑。

## 覆盖与限制

[本次原生校准面板截图](screenshots/lyrics-tools-short-large.png)。截图草稿底部同时通过390×720、1.6倍字体三项动作可达检查；Windows盘符／反斜杠和Android编码URI文件名有明确匹配回归。歌词修复通过390×360、1.6倍字体的实际滚动、点击偏移500ms和关闭验证；工具是独立原生窗口，歌词容器裁剪。路径修复检查唯一匹配、歧义拒绝、ID／收藏／最新元数据和过期预览保护；SAF／Windows真实选择器仍需设备。草稿检查磁盘恢复、空选择、编辑、丢弃防复活；最新300ms未落盘输入在强杀时可能丢失，显式稍后继续会等待保存。

BPM只在用户点击后逐首本地分析前90秒；缓存按来源／大小／修改时间复核，0表示未知。静音／短音频不伪造120BPM；节拍半倍／双倍及选段不代表整曲属于估算边界。分析不会上传音频或调用收费模型。

当前手机未连接，不能把本机回归当作Android锁屏／来电／蓝牙／节电验证；Windows物理设备／休眠唤醒、两小时持续运行、正式Release帧率和功耗未执行。没有将算法查询加速等同于整体播放器帧率提升。官网未改动，沿用上轮源码状态，本轮不引用上轮网站测试为新验证。

设计资料：Impeccable提示`.impeccable/design.json`旧于DESIGN.md，本轮以当前设计文档和源码为准；未扩展重写用户已有缓存，可后续通过document同步。
