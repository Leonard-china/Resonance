# 2026-10-17 — Android 与 Windows 内置本地歌单截图 OCR，并保留云端 API 切换

- 修改日期：2026-10-17（Asia/Shanghai）
- 类型：功能
- 适用版本：0.4.6 / 29（正式发布统一升版，见本日发布记录）
- 影响平台：共享 / Android / Windows
- 关联需求：用户要求手机与电脑各自在本机识别歌单截图，并保留切换到 API 的能力；讨论后明确采用轻量 OCR。

## 需求或问题

此前截图必须配置视觉模型与 API Key，并把图片发送到云端。用户希望在各自设备上完成识图，降低接口费用，不依赖电脑常驻服务。

## 原因与依据

`JvmMusicAssistant.importScreenshots` 原来只调用 `ask(vision=true)`。聊天与视觉提供商已经独立，但缺少本地识别后端、平台解码、离线模型和识别方式设置。只改 API 名称不能形成端侧推理。

## 具体修改

| 文件 | 关键符号 | 修改与目的 |
| --- | --- | --- |
| [共享 OCR](../../composeApp/src/jvmMain/kotlin/com/resonance/player/ocr/PaddleScreenshotOcr.kt) | `PaddleScreenshotOcr` | ONNX Runtime CPU 加载随包 PP-OCRv5 mobile 检测/识别模型；核对模型 SHA-256，BGR 预处理、水平 DB 区域及 CTC 解码；逐片/逐行处理，张量、结果、模型会话都由 `use` 关闭。 |
| [截图结构解析](../../composeApp/src/jvmMain/kotlin/com/resonance/player/ocr/ScreenshotPlaylistParser.kt) | `parse` | 用文字坐标配对歌名/歌手，过滤排名、状态栏和操作文字；有播放栏时排除封面/创建者区域，提取明显标题并跨片去重。 |
| [Android 解码](../../composeApp/src/androidMain/kotlin/com/resonance/player/features/AndroidScreenshotOcr.kt)、[Windows 解码](../../composeApp/src/desktopMain/kotlin/com/resonance/player/features/DesktopScreenshotOcr.kt) | 平台工厂 | 只解码已有受限分片，Android 回收 Bitmap、Windows 释放图像/reader，模型从 assets/classpath 读取。两端任务运行入口均注入本地后端。 |
| [设置模型](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/MusicFeatureModels.kt)、[编解码](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/MusicFeatureCodec.kt) | `ScreenshotRecognition` | 新增 `LocalOcr` / `CloudApi`；新安装、旧设置缺键及未知枚举默认本地；API 连接和视觉提供商保留。 |
| [任务接线](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | `importScreenshots` | 本地识图绕过 API 和 Token 预算；失败明确报告、不自动上传；`ocr-v2` 缓存包含后端/模型、备注与图片摘要；结果消息标记实际 OCR 来源。 |
| [设置 UI](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureScreens.kt) | 设置与附件提示 | 外露本地/API 切换、联网边界与当前方式，选择视觉 API 时切换 CloudApi。 |
| [构建](../../composeApp/build.gradle.kts)、[模型资源](../../composeApp/src/ocrModels/README.md)、[复现脚本](../../scripts/fetch-ocr-models.py) | 平台依赖与资源打包 | 两端分别使用 ONNX Runtime（Android 1.23.2 / Windows 1.20.0）；模型共约 20.4 MiB，保存来源、SHA-256 和 Apache-2.0 许可，归档校验后可重现。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 无 API Key 发送截图 | 无法识别 | 默认本地识别后匹配/下载，图片不发给模型服务。 |
| 希望使用云端视觉 | 仅云端 | 设置手动切换，原有配置保留。 |
| 长截图 | 分片后全部发送 API | 本地模式逐片解码/推理，后续只保留文字；API 模式保留原流程。 |
| 本地识别失败 | 不适用 | 显示失败与可选切换提示，不隐式产生 API 请求/费用。 |

## 平台与数据影响

- 新设置键 `settings.screenshotRecognition`；JSON/备份、保存顺序、任务互斥和取消机制保留；歌单包格式不变。
- 图片仍受 40 片 / 30 MB 限制。推理 2 线程、禁用会话 arena 与 memory pattern 缓存；结束关闭模型会话不等于操作系统立即回收全部进程内存。
- 本地图片识别可离线；歌曲信息查询/下载仍联网。聊天、翻译、语音识别的 API 与计费规则不变。没有新增权限、自动删除音频或读取用户 Key。

## 验证

`:composeApp:allTests :composeApp:lintRelease :composeApp:assembleRelease :composeApp:createReleaseDistributable :composeApp:packageReleaseExe :composeApp:assembleDebug :composeApp:assembleDebugAndroidTest --no-configuration-cache` 最终通过（8m24s），其中桌面/共享回归共 131 项，0 失败。`ScreenshotOcrTest` 使用真实模型读取生成的中英文截图，验证布局、数字歌名与独立排名列、去重及设置；`MusicAssistantTest` 验证无 Key 导入、缓存、零识图 API 请求及失败不自动回退。

最终 Debug APK 在 Android 36 x86_64 模拟器执行 `LocalScreenshotOcrTest`，`OK (2 tests)`：真实离线模型连续识别两次，并验证 4K 截图的二次幂采样符合分片尺寸边界。PSS KiB：前 185860、峰值 348490、后 229258，两次识图共 8812ms；峰值增加约 159 MiB。结果只代表本次模拟器，不是手机真机内存承诺。详细日志、正式包及发布结果见 [本次发布与验证](2026-10-17-03-release-0.4.6.md)。

首轮编译因误把 ONNX `FloatBuffer` 当成 Optional 使用而失败，已修正；失败日志保留于忽略目录 `build/ocr/compile.log`，不能作为通过依据。

## 限制与后续

OCR 为水平歌单截图设计，不保证旋转照片、复杂多列排版、模糊字体或所有音乐软件均可解析；无法可靠配对时报告失败。测试图片为生成夹具，不是用户截图；手机真机耗时/内存不能由模拟器结果推定。

## 同步更新的文档

文档入口、开发说明的截图/存储/网络/任务定位、根 README、第三方说明和模型来源说明。
