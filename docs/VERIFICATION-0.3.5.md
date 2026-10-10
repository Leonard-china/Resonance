# v0.3.5 验证报告

日期：2026-10-10 至 2026-10-11。

环境：Windows、JDK 17、Android SDK 36、MuMu Android 12 / x86_64。版本为 0.3.5，versionCode 为 22。

新增回归覆盖：快速切歌、晚到的响度分析、手动音量覆盖、关闭后的任务取消、播放前应用缓存设置、播放快照、真实 FFmpeg 响度分析、PCM 正增益/限幅/渐变/静音、曲库并发合并与收藏、损坏恢复、写入顺序、万首曲库索引和搜索。

## 自动测试与构建

- `:composeApp:allTests`：桌面及共享测试 79 项、Android debug 单元测试 25 项、release 单元测试 25 项，全部通过。
- `:composeApp:lintRelease`：0 错误；现有的建议及弃用 API 警告仍保留，未将其表述为零警告。
- `:composeApp:assembleRelease`、`:composeApp:packageReleaseExe` 和 `:composeApp:createReleaseDistributable`：成功。
- 版本常量由 version.properties 生成；重复构建保持 0.3.5 / code 22。
- GitHub Actions 在 Windows 上安装 SDK、FFmpeg，运行共享/两端单元测试、Lint 和 debug APK 构建，并保留报告。

## Android 运行验证

安装独立的 debug 包及测试包，直接调用 AndroidJUnitRunner，8 项测试全部通过：

1. 会话授予增益及主音量自定义命令，命令返回成功。
2. 无 Activity 的服务切歌后应用逐曲音量，更新播放队列快照。测试等待服务音量事件，避免将控制器的即时跳转事件误认为后台处理完成。
3. PCM 处理器实际放大采样，并支持单声道和立体声。
4. APK 内置 FFmpeg 对低音量、高音量及静音 WAV 做完整响度分析；分别验证正增益、负增益、版本化缓存，以及静音不写入成功缓存。
5. Android 正则引擎解析多行响度 JSON；修复 JSON 右花括号未转义导致 Android 返回零增益的问题。
6. 歌单转场浅色打开、深色返回、快速返回、减少动态效果，共 4 项画面测试；在动画中途采样实际渲染像素，验证页面遮挡、裁剪和完成后的背景恢复。

上述编号按验证场景归类，后台音频共 4 项测试，画面共 4 项。Gradle 的 UTP 主机依赖下载遇到 TLS 握手失败，测试改用相同 APK 和 runner 通过 ADB 执行，未跳过测试。

```powershell
adb install -r composeApp/build/outputs/apk/debug/Resonance-0.3.5-debug.apk
adb install -r composeApp/build/outputs/apk/androidTest/debug/composeApp-debug-androidTest.apk
adb shell am instrument -w -r com.resonance.player.debug.test/androidx.test.runner.AndroidJUnitRunner
```

## Windows 运行验证

- FFmpeg 整曲响度测试通过；低、高音量真实 WAV 的增益方向正确。
- 使用实际 Java Sound 输出进行静音音频检查，验证暂停恢复、播放、暂停、跳转、变速和播放结束，无播放错误。
- 当前版本的打包启动器在隔离的数据目录下启动，进程保持运行并响应，标准错误无启动异常。
- 一万首曲库后台建索引及复用索引的 20 次搜索通过，不作为其他设备的性能承诺。

## 签名与产物

APK 包名 `com.resonance.player`，最低 API 26，目标 API 36。APK v2 签名校验通过；签名证书 SHA-256 与 GitHub v0.3.4 APK 一致：

`b2b4ad35b8c7136569d219103448eaee75d4bbd30f3872c601373df43b303fe5`

APK、Windows EXE 的大小及 SHA-256 见 [发布说明](RELEASE_NOTES-0.3.5.md)，发布附件同时包含 SHA256SUMS.txt。

未在本轮验证真实 Android 手机、蓝牙设备、外接 DAC 或全部厂商的后台进程策略。
