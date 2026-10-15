# 2026-10-15 — 深度优化的构建、设备回归与开发交接

- 修改日期：2026-10-15（Asia/Shanghai）
- 类型：验证 / 文档 / 交接
- 版本：0.3.5 / versionCode22，未升版、未发布正式安装包
- 平台：Windows、Android API32模拟器
- 关联：[02–11独立功能记录](../README.md)

## 交付内容与文档

完成音量简化、设备同步/酷狗链接移除、转换进度取消、封面与标签切换、B站音轨及自定义信息、多模型Agent/截图、推荐生命周期、双端悬浮歌词和翻译、补全改进。各项原因/代码/数据影响分别留记录，不用一条“优化”取代独立文档。

更新 [详细开发说明](../DEVELOPMENT.md)、[项目说明](../../README.md)、[组件/服务/凭据](../THIRD_PARTY.md)、[变更索引](../README.md)，保留 [AI协作规则](../../AGENTS.md)、模板及原文档重建记录。发布入口明确旧Release附件不含当前工作树功能。

## 验证环境和隔离

JDK17、Gradle Wrapper8.11.1、SDK36/BuildTools36.0.0，Windows图形会话和API32模拟器。未读取/输出真实签名私钥、API Key或Cookie。

Windows运行测试同时将RESONANCE_DATA、RESONANCE_LIBRARY指向build/feature-desktop-smoke独立目录。AI/推荐/OCR/翻译测试注入MusicAiTransport/MusicVideoSource，仅操作生成夹具；Android新原生用例有独立文件根/偏好前缀，结束清理。用户原始音频未用于删除测试。

## 构建、单元测试和Lint

实际最终命令：

```powershell
.\gradlew.bat :composeApp:allTests :composeApp:lintRelease :composeApp:assembleDebug :composeApp:assembleDebugAndroidTest --console=plain
```

本地忽略日志 `build/feature-release-readiness.log`：**BUILD SUCCESSFUL，1分21秒**。desktopTest XML合计 **95项，0失败，0跳过**（含commonTest）；Android Debug/Release单元任务随allTests通过。Lint **0错误，86警告**，未声称零警告。

| 场景 | 具体通过内容 |
| --- | --- |
| 播放/音量 | 全局音量、快速切歌、旧任务/原生事件保护、快照恢复 |
| 转换/取消 | 真实WAV→MP3、KGMA内FLAC、源hash保留、取消解密清staging |
| 歌单文件 | ZIP/hash/大小/路径、原FLAC扩展/MIME/封面往返、明文夹具 |
| Agent/截图 | 真实工具ID、否定删除/仅删歌单、歌单管理、稳定前缀、预算、OCR缓存/下载去重 |
| 推荐 | 收藏/入单保护、临时曲清理、失败留旧批、当前播放延期、拒绝已保存曲也物理删除 |
| 翻译/歌词 | 目标语言与缓存、错行拒绝、原时间轴、原生位置不依赖未准备时长 |
| 恢复 | JSON主损坏恢复备份，后续写不毁备份，双损坏停止 |
| 图片 | Windows PNG/WebP有界解码；Android缩略图异常回退到真实内嵌封面 |

Windows隔离环境实际运行主窗口，并以32号字开启“Resonance 桌面歌词”窗口，无启动异常；测试进程已关闭。日志 `build/feature-desktop-smoke.log`、`feature-desktop-floating-smoke.log`。没有声称启动检查覆盖所有鼠标/多屏交互。

## Android设备回归

安装本次Debug/测试APK，最终按类分组执行相同runner，**三组共10项通过**：

```powershell
adb install -r composeApp/build/outputs/apk/debug/Resonance-0.3.5-debug.apk
adb install -r composeApp/build/outputs/apk/androidTest/debug/composeApp-debug-androidTest.apk
adb shell am instrument -w -r -e class com.resonance.player.MusicFeaturesIntegrationTest com.resonance.player.debug.test/androidx.test.runner.AndroidJUnitRunner
adb shell am instrument -w -r -e class com.resonance.player.LibraryDetailTransitionTest com.resonance.player.debug.test/androidx.test.runner.AndroidJUnitRunner
adb shell am instrument -w -r -e class com.resonance.player.AudioSessionIntegrationTest com.resonance.player.debug.test/androidx.test.runner.AndroidJUnitRunner
```

| 分组 | 最终结果 | 覆盖 |
| --- | --- | --- |
| MusicFeaturesIntegrationTest | OK(2)，0.7秒 | MP3真实歌名/歌手/专辑/封面，缩略图回退，库/歌单/收藏/物理删除，1700宽长图分片<=1600，KeyStore |
| LibraryDetailTransitionTest | OK(4)，2.61秒 | 深浅色、打开/返回/快速返回/减少动态效果的中途像素 |
| AudioSessionIntegrationTest | OK(4)，0.593秒 | 命令授权、后台全局音量、PCM单声道、FFmpegKit整曲响度缓存 |

最终本地日志 `build/feature-native-music-final.log`、`feature-native-ui-final.log`、`feature-native-audio-final.log`。

完整10项runner曾通过，但重复运行/调整显示尺寸后出现过截图等待未结束和一次reduced-motion用例“No compose hierarchies found”。中断/失败不计通过；恢复默认显示、重新启动runner后，相同用例按类全部通过。后续修改设备测试/转场仍需留意模拟器、Compose测试Activity与截图等待的稳定性。

## 界面、文档与APK

实际观察横屏/窄屏UI树并检查截图：工具、设置、独立音量帮助、助手、模型编辑/文本视觉角色可打开。截图位于忽略的build，未作为正式视觉基准提交；显示尺寸/旋转配置已恢复。UI打开不等于付费服务在线通过。

`build/verify-feature-docs.py` 检查20份Markdown本地链接、12份带日期记录、移除实现和版本不变，通过；`git diff --check`通过。版本属性未修改，未更新正式发布校验值。

本次测试APK：`composeApp/build/outputs/apk/debug/Resonance-0.3.5-debug.apk`，包ID `com.resonance.player.debug`，可与正式包并存。实际SHA-256：

```text
35828C0D834F263C64EFE45F5F361D7705DDD6D93A01C52C6CF182C3AF0D2CFE
```

未生成新的正式签名APK、Windows安装器或发布页，不把本机旧Release/EXE当本轮产物。

## 实际发现并修正的问题

- 编译缺导入、递归类型等已修复，失败构建未计通过。
- 取消夹具最初用不支持的KGMA WAV载荷，改为可识别MP3载荷后真正验证取消。
- Path.of不兼容低版本Android，改Paths.get。
- Android封面采样可能初始为0，设备回归后从1显式开始。
- ImageIO无内置WebP，Windows加Skia受控下采样并验证。
- 缓存修订号防止旧解码回写及现有图片不重载。
- 后台歌词按原生毫秒选行；Android拖动/旋转限屏内，Windows大字号适应高度。

## 真实服务和平台限制

- 当前网络对B站公共metadata探测为 **HTTP412**。应用提示官方登录/验证/网络限制，允许自有Cookie；**真实账号搜索→下载未验通**。注入音源不等于真实服务通过，不绕过限制。
- 无用户模型Key，**千问/DeepSeek/豆包收费请求未执行**；模型工具/视觉能力、账户区域、缓存时效和账单需实际账户验证。[服务参考](../THIRD_PARTY.md)。
- Android KGG不支持，需Windows对应本机密钥转换。
- API32不能代表API36后台配额/OEM省电/悬浮策略；周期非精确闹钟。未自动化验证系统overlay触摸穿透或所有Windows多显示器/缩放。
- 模拟测试不保证口味、原版真伪、OCR/歌词事实准确度；错误按实际报告。
- 旧明文DeepSeek兼容残留；AI信息工具改曲库覆盖值，新下载MP3才两端真实写标签。

## 交接

按 [任务定位表](../DEVELOPMENT.md#gaps) 和对应02–11记录接手。获得真实账号/Key后可继续端到端联调并单独留新日期记录，不重扫全库、不覆盖本轮历史，也不把未验证项写成通过。
