# 2026-11-04 — 用户反馈修复与新增歌源验证

范围：歌词查找和空态、音效弹窗、收藏数量、QQ／Internet Archive Netlabels 与 VPN Fake-IP DNS 兼容。与[原始全面排查](../BUG-AND-OPTIMIZATION-REPORT.md)分开；原报告的 13 项发现没有因此全部修复，也未擅自处理音频删除或元数据写回。

## 环境和命令

Linux x86_64，Temurin OpenJDK 17.0.21，仓库 Gradle Wrapper 8.11.1，现有 Android SDK／Build Tools 36。实际以 `java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain` 启动以下任务；设置有效 `JAVA_HOME`、`ANDROID_HOME`、`RESONANCE_FFMPEG`，没有提交本机工具路径或真实凭据。

```text
RESONANCE_LYRICS_ACCEPTANCE=1
:composeApp:desktopTest :composeApp:assembleDebug :composeApp:assembleDebugAndroidTest :composeApp:lintRelease
--no-daemon --max-workers=2 -Dorg.gradle.jvmargs="-Xmx3g -Dfile.encoding=UTF-8"
-Pkotlin.compiler.execution.strategy=in-process --no-configuration-cache --console=plain
```

全量运行成功，耗时 10m17s。桌面 346 项，337 通过、9 项环境／显式集成入口跳过；具体跳过名称及公开歌词输出见 [本次全量摘要](full-jvm-summary.json)。APK 构建与 Release Lint 成功；Lint 0 Error／107 Warning。此全量结果发生在最后一次歌词空态间距、展开工具布局及提示文案调整之前；最后调整由下方针对性验收覆盖，没有把全量历史结果当作最终未改动源码的证明。

最终间距／展开工具／提示文案调整后，同一 Wrapper 执行以下针对性选择及构建，**成功，耗时 3m**：

```text
:composeApp:desktopTest
--tests '*LyricsAcquisitionTest' --tests '*LrclibLyricsRepositoryTest'
--tests '*MusicSourceRegistryTest' --tests '*PublicSourceDnsTest'
--tests '*BuiltinMusicSourcesTest' --tests '*PlayerRecoveryUiTest'
--tests '*LyricsTranslationControllerTest' --tests '*DeepSeekClientTest'
:composeApp:assembleDebug :composeApp:assembleDebugAndroidTest :composeApp:lintRelease
```

最终 33 项全部通过、0 跳过（包括原生公网歌曲联测），见[最终摘要](final-targeted-summary.json)。Debug 和仪器测试 APK 重新生成，Release Lint 仍为 0 Error／107 Warning；实际文件大小与 SHA-256 见[本次产物摘要](artifacts.json)，没有新 Release、升版或手机安装。

原生界面夹具截图已逐张查看：[390×844 常规字号](lyrics-empty-390-844-1.0.png)、[360×640／1.5 倍字](lyrics-empty-360-640-1.5.png)、[720×360／1.5 倍字](lyrics-empty-720-360-1.5.png)、[展开工具](lyrics-tools-360-640-1.5.png)。默认查找与工具开关可见，展开后减少重复空态并可继续滚动。截图是桌面 Compose 离屏渲染，不是 Android 实机图片。

## 公网与设备边界

| 操作 | 本次结果 | 边界 |
| --- | --- | --- |
| 原生 JVM／QuickJS／PublicSourceNetwork 安装网易云和 QQ 后查《夏天的风》／温岚／4:01 | 通过：QQ 音乐，46 行，真实同步时间轴；另一次直接 JUnit 执行 5 项全通过 | 只保存元数据及行数，不保存原词；未用模型 Key、未上传／下载音频，证据见 [原生结果](native-public-lyrics.json) |
| 新 Archive JS 实际检索 ambient，并对返回公开音频做短 Range 读取 | 通过：返回 27 个单曲条目；媒体 HTTP 206、audio/mpeg，读取 1024 字节后取消 | Node 执行真实内置脚本，非原生整曲播放验收，见 [小段响应](archive-live.json) |
| QQ 匿名完整音频解析（所报曲目） | 未提供可用媒体地址 | 接口没有 purl，不能把搜索／歌词可用写成该曲可播；没有绕过登录或付费 |
| Android 音效回归仪器测试 | 环境受阻 | APK 已编译；`adb devices -l` 无设备，安装返回 no devices，未执行最终点击验证，未改变用户手机数据 |
| Windows 原生 UI／网络 | 未执行 | Linux 共享逻辑测试不能代替 Windows 窗口／系统验收 |
| DeepSeek 真实付费生成和 ASR | 未执行 | 模拟 unavailable／ok 只核对错误解释和时间戳处理；没有用户模型 Key 或收费请求 |

首次歌源公网联测失败：系统 DNS 返回 198.18/15，原公网规则拒绝；独立 HTTPS DNS 后真实请求恢复。本轮新增回退代码还曾只取结果列表第一项，前源无命中时漏掉 QQ 成功结果；已改为跳过空项，并在固定测试加入前源空、后源成功。它们是开发中的失败，不计入最终成功证据。

音效打开已排除跨窗口 Haze 采样，并通过桌面语义操作回归；没有手机 ANR 栈，不能认定唯一根因或宣称原机完全解决。多窗口离屏截图的混合层次不能用于证明真实设备视觉效果，音效只按语义交互和编译记录结果。

## 当前变更入口

- [歌词检索／布局](../../../changes/2026-11-04-09-lyrics-source-recovery-and-compact-tools.md)
- [音效弹窗](../../../changes/2026-11-04-10-audio-effect-dialog-window-isolation.md)
- [收藏数量](../../../changes/2026-11-04-11-favorites-count-only.md)
- [新增歌源／DNS](../../../changes/2026-11-04-12-more-builtin-sources-and-vpn-dns.md)

`source-sha256.txt` 只覆盖本轮相关实现和回归，不包含签名、真实配置、用户音频；原审计的源摘要仍对应原审计时刻。

文档收尾核对：四份独立变更文档及验证报告的本地链接均有效，没有待填占位；22 份相关实现／回归的 SHA-256 逐项核对通过。仓库既有 CRLF 文件用 `git -c core.whitespace=blank-at-eol,blank-at-eof,space-before-tab,cr-at-eol diff --check -- <相关路径>` 检查通过，新变更文档没有行尾空白。没有归一化或清理无关工作区内容。
