# Resonance 原生宣传片制作

2026-11-05：一起听已移除，原房间／聊天特性摄影机及 `produce_features.py` 不再提供。当前 `capture.py` 仅保留原生播放器拍摄，不调用模型或启动房间。旧片与旧拍摄事实见[历史记录](../../docs/changes/2026-10-31-12-real-ai-and-companion-promo-camera.md)。

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

1. 从项目根目录构建 Debug/测试包（使用独立测试包，按实际包名显式选择 .codex 或 .debug）。
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
