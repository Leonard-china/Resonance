# 2026-10-16 — 优化后的 Android 与 Windows 发布版构建及回归验证

- 修改日期：2026-10-16（Asia/Shanghai）
- 类型：构建配置 / 回归验证 / 文档
- 适用版本：0.4.0 / 23；用户本次要求构建发布版，未指定升版，遵守 AGENTS.md 不自动升版
- 影响平台：Android、Windows

## 需求与具体修改

交付本轮七组优化及可安装的两端发布版；保留每个独立修改的开发记录，更新当前开发说明。

新语音 WSS 客户端使用 OkHttp 4.12.0。Windows ProGuard 首次构建因 jar 内可选 Android/Conscrypt/BouncyCastle/OpenJSSE 适配器缺失而失败；根据实际告警仅在 [compose-desktop.pro](../../composeApp/compose-desktop.pro) 忽略这些平台/可选 TLS 探测类，保留正常 JDK TLS 路径，未使用全局忽略告警。签名私钥与用户凭据不读取、不提交。

本次输出另存到 `release/0.4.0/2026-10-16/`，与旧 GitHub Release 校验值区分。本次不执行 Git 提交、推送、tag 或 GitHub 上传。

## 本次实际验证

<!-- OCT16-VALIDATION -->
| 命令/操作 | 环境 | 本次结果 |
| --- | --- | --- |
| `gradlew :composeApp:allTests :composeApp:lintRelease :composeApp:assembleRelease :composeApp:packageReleaseExe :composeApp:assembleDebug :composeApp:assembleDebugAndroidTest --no-configuration-cache --console=plain` | Windows / JDK17 / SDK36 | 最终完整构建成功，见 `build/oct16-complete-build.log`；桌面/共享 106 项，失败/错误0；Android两变体单元任务通过；Lint 0错误、88警告 |
| ADB 分组运行 LibraryNavigationTest / MusicFeaturesIntegrationTest / LibraryDetailTransitionTest / AudioSessionIntegrationTest | API32模拟器 127.0.0.1:16384 | 5 + 2 + 4 + 4 = 15 项全部通过；见 `build/oct16-*-verified.log` |
| Android实际MainActivity冷启动、主页面截图 | API32 / Debug包 | Status ok，冷启2131ms；人工核对推荐置顶和助手主导航。见`build/oct16-android-startup.log`、`build/oct16-android-main.png` |
| 新音频识别完整本机链路，实际 FFmpegKit PCM + 模拟识别返回 | API32 / 隔离测试目录 | 歌词写读、应用私有临时目录、PCM格式及结束清理通过；不调用收费服务 |
| `apksigner verify --min-sdk-version 21 --verbose --print-certs` / `zipalign -c -P 16 4` / aapt2 | 本次Release APK | v1/v2通过、16KiB对齐通过；com.resonance.player / 0.4.0 / 23 / min26；签名证书与历史官方0.4.0所用证书一致 |
| Windows 完整发布运行目录启动 | 同时隔离 RESONANCE_DATA / RESONANCE_LIBRARY | 通过：校验打包启动器的实际子进程主窗Resonance、Responding=true，内置FFmpeg可执行；见`build/oct16-windows-smoke.json`。初始只检查启动器PID未取到标题，修正探测后通过；未用真实音乐库做删除测试 |
| QQ / iTunes 公开目录及封面 | 网络只读检查 | QQ《晴天 / 周杰伦》→《叶惠美》，800px JPEG、HTTP200、183730字节；iTunes《Hello / Adele》→《25》。未命中回退仍保留视频图 |
| Markdown链接 / `git diff --check` | 当前工作区 | 本次文档路径全部有效、无空白错误；不含构建产物、用户Key或签名私钥 |

| 文件 | 实际大小 | SHA-256 |
| --- | --- | --- |
| Resonance-0.4.0-release.apk | 63,798,260 字节 / 60.84 MiB | `1f644261f1bd3cf8c1352923d484d8d02d20a6bede83ccf14884f0a3e3d2376a` |
| Resonance-0.4.0.exe | 204,127,744 字节 / 194.67 MiB | `ae36456530bc5a51386def7c0925b523ceaaaf75aa9cb770248335d0d69a6036` |

Android公开签名证书 SHA-256：`b2b4ad35b8c7136569d219103448eaee75d4bbd30f3872c601373df43b303fe5`。APK验签工具可能报告标准META-INF未签名元数据警告，不影响本次v1/v2验证通过。Windows安装器未添加Authenticode签名。

输出目录另含实际校验清单与BUILD-INFO.json。前两轮构建分别暴露可选TLS适配器及Android Path.of API34限制，修正后运行最终构建；旧失败日志保留在忽略的build目录，不当作成功依据。

<!-- END-OCT16-VALIDATION -->

## 平台与数据影响

Release APK 正式包名仍为 com.resonance.player；Debug 测试使用 com.resonance.player.debug，未清除正式应用数据。Windows 启动检查同时隔离 RESONANCE_DATA 和 RESONANCE_LIBRARY，避免写入用户音乐库。

不把旧产物、历史测试结果或仅能编译当成本次完整功能验证通过。语音 Key 不可用时不调用收费服务；真实人声识别准确性、B站账号在线下载及真实大曲库帧率仍需用户环境体验。

## 同步更新的文档

[开发说明](../DEVELOPMENT.md)、[文档入口](../README.md)、[第三方及网络](../THIRD_PARTY.md)、[本次构建说明](../RELEASE_NOTES-0.4.0-2026-10-16.md) 和根 README。2026-10-15 的功能/发布记录作为历史保留。
