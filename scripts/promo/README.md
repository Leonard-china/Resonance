# Resonance 原生宣传片制作

2026-10-31 新版三项特色稿仍在制作，见[最新摄影机记录](../../docs/changes/2026-10-31-12-real-ai-and-companion-promo-camera.md)。`produce_features.py` 输出 50 秒横竖版，当前项目采用 MIT。新版摄影机使用生产助手和真实模型回答，包含真实加密聊天与双向配对；未拍摄前不能把脚本当成成片。

特性摄影机必须用独立 `.codex` Debug/测试包。只在用户明确授权模型使用后，运行 `PromoAiSetupDeviceTest`，传 `configurePromoAi=true` 和随机 32 位小写十六进制 `promoSocketNonce`。它以同名 `resonance-promo-ai-<nonce>` 本地套接字等待 ADB UID，60 秒后关闭；主机通过本次拥有的 ADB forward 仅在内存传入凭据，禁止放到参数、文件、日志或自动化提示。设置由正式 Android Keystore 路径加密保存，原创曲库仅写入独立包。摄影机随后点击真实发送按钮，不模拟回答。

新版拍摄：先以 opt-in `TogetherNativeBridgeTest.windowsNativeHostForMuMu` 启动真实 Windows 原生主机（仅传原创测试音频，`RESONANCE_NATIVE_BRIDGE=1`、`RESONANCE_BRIDGE_COMPANIONS=1`、`RESONANCE_PROMO_SCENE=1`、`RESONANCE_NATIVE_AUDIO=<绝对音频路径>`、`RESONANCE_NATIVE_DURATION=<实际秒数>`、`RESONANCE_NATIVE_SECONDS=30`）。摄影机离房后主机结束；主机 30 秒循环结束还会保留最多 30 秒等待离房。使用固定运行时快照及独立 `RESONANCE_BRIDGE_OUTPUT`，根据其 `port.txt` 建立本次拥有的 ADB reverse，通过 ADB 暂存文件、run-as cp 写入私有 `files/native-bridge/invite.txt`，核对字节数并移除暂存。不要输出或提交临时邀请。

构建安装包含 `PromoFeaturesCaptureDeviceTest` 的独立包后，执行 `capture.py --scene features --theme light --package com.resonance.player.codex --adb <ADB> --serial <MuMu序列号> --output <本次输出目录>`。先确认没有本线程长时测试或其他同包仪器测试，不能安装打断它们。录像只停止本次明确拥有的 PID；完成后移除本次 ADB reverse/forward。问答证据不含凭据，剪辑标记包含 assistant-input、assistant-answer、together、paired、chat。

剪辑：`produce_features.py --ffmpeg <FFmpeg> --font <中文字体> --bold-font <粗体> --code-font <等宽字体> --source composeApp/src/jvmMain/kotlin/com/resonance/player/together/TogetherTransport.kt --output <本次输出目录> --player-output output/promo --license MIT`。`--prepare-only` 先检查排版；正式渲染必须有真实回答证据。

新版验收：`verify.py --ffmpeg <FFmpeg> --output output/promo-features-2026-10-30 --duration 50 --suffix features-50s`。必须另外查看联系表/原片、核对文字和素材边界；启动脚本不代表新片完成。以下为已完成的前版 40 秒流程，保留其历史事实。

使用实际 `NowPlayingOverlay` 在 MuMu 中录制，再合成原创中文排版与器乐配乐。正式应用维持 Kotlin / Compose 原生架构；本工具不会建立播放器 Web 版本。

## 交付与边界

成片位于忽略目录 `output/promo/`：

- `Resonance-landscape-40s.mp4`：1920×1080，30fps，横版。
- `Resonance-vertical-40s.mp4`：1080×1920，30fps，竖版。
- `soundtrack-original.wav`：40 秒原创 96 BPM 立体声配乐。
- `raw-light.mp4`、`raw-dark.mp4`：MuMu 原生录像。
- `capture-*.log`、`promo-*-marks.csv`、`production.json`、`qa.json`：拍摄、剪辑和媒体验证证据。

演示曲目、歌词和封面均为原创夹具。播放时间和队列由测试驱动；影片使用独立器乐配乐，不声称录制了实际音频解码或歌词识别。片内标明「MuMu 模拟器实录 · 原创演示数据」。不访问用户歌曲、凭据或收费服务，不自动发布、上传或替换官网内容。

## 录制

需要 Android SDK/ADB、MuMu、JDK 17、Python（Pillow、NumPy）、FFmpeg。工具路径由参数提供，不提交本机路径。

1. 从项目根目录构建 Debug/测试包（当前特性摄影机用独立 .codex 构建；下方旧版 player 流程可显式选择原 .debug 包）。
2. 使用 `adb devices -l` 确认 MuMu 序列号；所有命令都显式 `-s`，不要使用未选设备的 ADB。
3. 在指定模拟器安装本次的 Debug APK 与 androidTest APK。`version.properties` 是当前 APK 文件名的版本来源。
4. 运行以下命令，路径参数替换为本机值：

```powershell
python scripts/promo/capture.py --adb <adb.exe> --serial <MuMu序列号> --package com.resonance.player.debug --theme light --output output/promo
python scripts/promo/capture.py --adb <adb.exe> --serial <MuMu序列号> --package com.resonance.player.debug --theme dark --output output/promo
```

`PromoCaptureDeviceTest` 只有显式传入 `capturePromo=true` 才执行拍摄，普通设备测试会跳过。Debug 独立宿主固定竖屏、隐藏系统栏并接收 MuMu 的输入设备配置变化；`exported=false`，不进入 Release。脚本临时锁定旋转，在结束/失败时恢复原设置。不会清空应用数据或用户媒体。

## 剪辑

```powershell
python scripts/promo/produce.py --ffmpeg <ffmpeg.exe> --font <中文常规字体> --bold-font <中文粗体字体> --output output/promo --format both
python scripts/promo/verify.py --ffmpeg <ffmpeg.exe> --ffprobe <ffprobe.exe> --output output/promo
```

拍摄宿主是无系统栏 1080×1920，默认不用裁切。`--crop-top/--crop-bottom` 只用于另行录制存在系统栏的素材；改变裁切时需检查最终比例。`--prepare-only` 可先生成原创配乐和排版预览；`--format landscape/vertical` 可以单独渲染一种比例。

`verify.py` 的 `--ffprobe` 可省略，此时使用 FFmpeg 媒体头读取同样的规格。仅修正最终编码/混音，且本次片段的录像、剪辑点和排版完全未变时，可用 `--assemble-only` 复用本轮片段；不能用它复用历史未知产物。

40 秒结构：品牌开场 → 封面氛围 → 双语歌词居中 → 自由浏览与返回 → 队列管理 → 深色播放器 → 品牌落版。剪辑标记使用 Android 单调时钟，与主机无关。解码、分辨率、帧率、声轨与截图联系表需再次核对，不能把脚本运行完成等同于完整视觉验收。
