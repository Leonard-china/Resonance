# 2026-10-29 — 使用 MuMu 实录原生播放器并制作原创配乐横竖版宣传片

- 修改日期：2026-10-29（Asia/Shanghai）
- 类型：宣传素材 / 构建与测试工具
- 适用版本：`0.6.5 / 39`，不升版
- 影响平台：Android Debug 拍摄宿主；Windows 本地剪辑工具
- 关联需求：用户要求高质量软件宣传片，并明确使用 MuMu 拍摄和测试；[歌词滚动修复](2026-10-29-02-lyrics-centered-follow-and-manual-browsing.md)

## 需求或问题

需要可观看的成片，展示真实原生播放器的封面氛围、歌词跟随、手动浏览与返回、播放队列、浅色和深色。录制不依赖用户真机、真实歌曲、私人曲库或 API Key。

## 原因与依据

现有 `MusicStageDeviceTest` 只能输出测试截图，没有可重复拍摄与剪辑流程。直接使用默认测试 `ComponentActivity` 时，MuMu 反复连接/断开模拟输入设备会产生 `touchscreen` 配置变化并重建 Activity，拍摄在播放点击前报 `No compose hierarchies found`。设备日志包含每次输入设备变化对应的 Activity 重建；拍摄需要稳定、隔离的 Debug 宿主。

## 具体修改

| 文件 | 关键符号 | 具体修改与目的 |
| --- | --- | --- |
| [PromoCaptureActivity.kt](../../composeApp/src/androidDebug/kotlin/com/resonance/player/PromoCaptureActivity.kt) | `PromoCaptureActivity` | 仅 Debug 的沉浸式测试宿主，保持亮屏；固定竖屏并接收 MuMu 的配置变化 |
| [AndroidManifest.xml](../../composeApp/src/androidDebug/AndroidManifest.xml)、[build.gradle.kts](../../composeApp/build.gradle.kts) | Debug manifest 映射 | 活动 `exported=false`；Release 不包含该活动和 manifest |
| [PromoCaptureDeviceTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/PromoCaptureDeviceTest.kt) | `recordNativePlayerStory`、`sleeve` | 显式 `capturePromo=true` 才执行；调用真实 `NowPlayingOverlay`、滚动和队列操作；原创文本、程序绘制封面和时间轴状态；保存单调时钟剪辑标记 |
| [MusicStageDeviceTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/MusicStageDeviceTest.kt) | 稳定宿主 | 复用 Debug 宿主避免 MuMu 配置切换中断原界面断言与截图 |
| [capture.py](../../scripts/promo/capture.py) | `main` | 指定 ADB、设备序列号、主题；screenrecord 实录；运行拍摄测试；收集视频、日志与剪辑标记；临时旋转设置在结束后恢复 |
| [produce.py](../../scripts/promo/produce.py) | `soundtrack`、`plate`、`render` | 原创 96 BPM 立体声配乐、青绿米白排版、手机边框、中文字幕和转场；从原生录像剪辑 1920×1080 横版及 1080×1920 竖版，40 秒、30fps、H.264/AAC |
| [verify.py](../../scripts/promo/verify.py) | 媒体验收 | 全片解码；核对时长、H.264/AAC、分辨率、帧率、立体声；提取时间标注的关键帧联系表并计算本次文件 SHA-256 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 生成宣传素材 | 只有原生 UI 截图测试 | 可指定 MuMu 设备录制浅/深主题，再生成横竖两版成片 |
| 测试宿主遇到 MuMu 输入设备切换 | Activity 重建，拍摄内容丢失 | Debug 专用宿主接收配置变化；不改变正式 Activity |
| 宣传内容与用户数据 | 没有隔离拍摄流程 | 使用原创演示曲目、封面、歌词和独立配乐，无用户音频与账号 |

## 平台与数据影响

不改变 Release UI 或播放后端。视频是原生组件的实际录制，使用测试驱动的歌曲/播放状态，配乐为独立合成音乐，不宣称录制了真实音频解码或收费歌词生成。片内标明「MuMu 模拟器实录 · 原创演示数据」。没有对外发布、上传、替换官网素材或触发版本发布。生成的媒体放在 Git 忽略的 `output/promo/`；可复用的脚本与测试源文件入库。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `produce.py ... --prepare-only` | 本地 Python / Pillow / NumPy | 通过，生成原创建图与 40 秒立体声配乐 |
| 默认测试宿主初次录制 | MuMu | 失败，配置变化重建宿主；已保存失败日志并据此增加 Debug 宿主 |
| `gradlew.bat :composeApp:assembleDebug :composeApp:assembleDebugAndroidTest` | Windows / JDK 17 / SDK 36 | 通过，生成本次 Debug 与测试 APK；未升版 |
| `capture.py --adb <本机ADB> --serial 127.0.0.1:16384 --theme light/dark --output output/promo` | MuMu / Android 12 | 通过，两组最终拍摄均 `OK (1 test)`；浅色重拍收窄手动翻阅幅度，使当前句附近保持足够上下文 |
| `gradlew.bat :composeApp:processReleaseMainManifest`，再核对 Release manifest | Windows | 通过（任务 UP-TO-DATE，Release 输入未变化）；manifest 无 `PromoCaptureActivity`，不代表本次重建了 Release 安装包 |
| `python -m py_compile scripts/promo/capture.py scripts/promo/produce.py scripts/promo/verify.py` | 本地 Python | 通过 |
| `produce.py ... --format both` | Python / Pillow / NumPy / FFmpeg 8.1.1 | 通过，最终两版重新渲染，日志 `output/promo/render-final.log` |
| `verify.py --ffmpeg <本机FFmpeg> --output output/promo` | FFmpeg | 通过，最终两版均 40.00 秒、30fps、H.264/AAC 立体声，全片解码无错误；见 `qa.json`、`verification.log` |
| FFmpeg `ebur128=peak=true` 与 `blackdetect=d=0.2:pix_th=0.02` | 最终横版 | 通过，1200 帧，集成响度 -16.0 LUFS、真峰值 -5.7dBFS；无持续 0.2 秒以上的黑帧区间；日志 `signal-qa.log` |
| 提取并查看两版 1.5/6/13/20.5/26/32/37.5 秒关键帧及歌词/返回镜头 | 本地 PNG/JPG | 通过，片头、浅色封面、居中歌词、手动浏览/返回、队列、深色封面和落版无字幕裁切；最终重拍收窄浏览幅度以减少上部空白 |

录制阶段还修正了测试查找器的合并语义树问题；剪辑阶段修正 FFmpeg duration 字符串需要 `0.4` 而不是 `.4` 的参数错误。这些尝试没有被记为成功成片。最终稿以 `qa.json`、`SHA256SUMS.txt` 与下列校验值为准。

## 本次交付

| 文件（本机忽略目录） | 规格 / 字节数 | 本次 SHA-256 |
| --- | --- | --- |
| [Resonance-landscape-40s.mp4](../../output/promo/Resonance-landscape-40s.mp4) | 1920×1080 / 5,292,165 字节 | `5b5de8e4aa8938a8a49c3b5502e6d5108981842f69a06b4cb92f60b3173c1b57` |
| [Resonance-vertical-40s.mp4](../../output/promo/Resonance-vertical-40s.mp4) | 1080×1920 / 5,967,630 字节 | `ac68c50e99f89a078a6d6a3d0dd9256697e726c16f82c342f67c1b0f96cf491c` |

两版均为 40 秒、30fps、H.264 / yuv420p 与 48kHz AAC 立体声，附 faststart。原创 WAV、原片、联系表和校验日志留在同一目录，未纳入 Git。

## 限制与后续

没有手机与 Windows 实机拍摄；字幕中的 Android / Windows 描述产品支持平台，不代表本次录制了 Windows。原创演示歌词不对应演唱，成片使用无声原生录像加原创器乐配乐。未执行发布或上传。

本次范围内无已知未完成项。若后续变更文案、拍摄分辨率或演示数据，重新跑剪辑与 QA，不能沿用本记录的校验值。

## 同步更新的文档

- `docs/README.md` 变更索引。
- `docs/DEVELOPMENT.md` Debug 拍摄宿主、脚本和验证边界。
- `scripts/promo/README.md` 可复现录制、剪辑与本次交付路径。
