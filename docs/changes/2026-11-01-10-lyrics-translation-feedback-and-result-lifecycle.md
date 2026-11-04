# 2026-11-01 — 修复歌词翻译无译文和失败无提示

- 修改日期：2026-11-01（Asia/Shanghai）
- 类型：Bug 修复
- 适用版本：0.6.5 / 39，未升版
- 影响平台：Android、Windows 共享歌词界面及 JVM 翻译服务
- 关联：[直接生成歌词文本](2026-11-01-02-direct-ai-lyrics-text.md)、[翻译和后台歌词](2026-10-15-10-floating-lyrics-and-translation.md)

## 需求或问题

用户反馈 Android 点击歌词翻译后没有译文。没有连接用户设备，也未读取用户 API Key，尚不能确认其实际请求是失败还是结果未显示。本次定点检查发现可解释该现象的显示条件和请求竞争问题，并补充对应回归。

## 原因与依据

- `MusicFeatureController.translate` 通过通用 `launch` 把异常写到 `message`，但 `TranslationControls` 没有显示该字段。未配置模型、网络失败或有其它音乐任务占用时，歌词面板没有错误反馈。
- 翻译服务将语言 `trim()` 后写到结果，`App` 却与原始设置字符串严格比较；目标语言为 ` 中文 ` 时，新译文被过滤。`cachedTranslation` 的语言标记也与新请求不一致。
- 用户设置为原文显示时，主动翻译成功仍只显示原文，无法直接看到结果。
- 缓存异步加载无请求代次检查，先前读取到的空缓存可能在主动翻译成功后清空结果。翻译没有独立取消任务，切歌后旧请求还占用忙状态；仅曲目和文本生成代次不能完整覆盖换语言、替换原词等情况。

## 具体修改

| 文件 | 关键类/函数 | 具体修改与目的 |
| --- | --- | --- |
| [MusicFeatureController.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureController.kt) | `translate`、`reloadTranslation`、`translatedLyrics` | 独立错误和任务代次；捕获点击时语言；防同帧重复；切换上下文取消旧任务并核对曲目/原词/语言；缓存迟到不覆盖新结果；成功时原文模式切双语，显示合并沿用原时间轴及最新偏移 |
| [App.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | 译文选择及缓存 `LaunchedEffect` | 使用控制器统一匹配；按曲目/歌词内容（整体偏移除外）/语言恢复缓存，新生成或新识别歌词也能重新复用译文；单独调整偏移不重启翻译 |
| [MusicFeatureScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureScreens.kt) | `TranslationControls` | 翻译按钮附近显示错误；失败后按钮恢复可重试；音频识别期间禁用翻译 |
| [JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | `translate`、`cachedTranslation` | 统一语言归一化；空目标、空响应和非 JSON 响应给出歌词翻译相关提示；仍核对行数后才缓存 |
| [LyricsTranslationControllerTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/LyricsTranslationControllerTest.kt) | 控制器回归 | 覆盖语言空格、原文模式、时间轴/偏移、失败保留和重试、迟到空缓存、不可取消旧请求、切歌/换语言/替换原词、空目标及缺服务 |
| [MusicAssistantTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/MusicAssistantTest.kt) | 翻译服务回归 | 增加语言归一化及空/非 JSON 响应不缓存；沿用等长、缓存和时间戳测试 |
| [LyricsTranslationDeviceTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/LyricsTranslationDeviceTest.kt) | 原生 UI 回归 | 通过实际歌词工具按钮验证译文显示和错误后重试，使用合成回复，不访问用户文件或网络 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 请求失败或其它任务占用 | 歌词工具未显示错误 | 按钮附近显示原因，可直接重试 |
| 目标语言带首尾空格 | 新译文被界面过滤 | 请求、缓存和界面使用一致的语言 |
| 原文显示模式下主动翻译 | 翻译成功仍只显示原文 | 成功后保存双语模式并显示译文 |
| 空缓存读取晚于主动翻译完成 | 可能清空新译文 | 通过代次拒绝旧缓存结果 |
| 切歌/换语言/替换原词 | 旧翻译可能阻塞后续点击或返回过期内容 | 上下文切换取消旧任务，拒绝迟到结果 |
| 重译失败 | 原文保留，但原因不在歌词面板显示 | 原文/旧译文保留，并显示具体失败提示 |

## 平台与数据影响

Android 与 Windows 共用控制器和 JVM 服务，未改平台接口。缓存仍使用 `translation-v1:<原文hash>:<归一化语言>`，无需迁移旧缓存；不改变同步包、时间轴、歌词偏移、音频或标签。沿用当前模型配置，只有主动点击才请求翻译，发送原词及目标语言。没有使用或输出真实密钥，没有新增权限或外部服务。仅主动翻译成功且当前显示为原文时保存双语偏好。

保留工作区原有未提交改动，本次不升版、提交或发布。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| 首轮定点桌面回归：控制器、音乐助手、AI 请求协议、悬浮歌词 | Windows / JDK 17 / Gradle Wrapper | 47 项中 46 通过、1 失败：切歌后过滤旧译文的断言失败。编译开始后补入了该保护，首轮产物未包含；以完整重编最终源码的复测结果为准 |
| 最终源码定点桌面回归 | Windows / JDK 17 / Gradle Wrapper | 通过：47 项、0 失败、0 跳过；控制器 6、音乐助手 36、AI 协议 1、悬浮歌词 4 |
| `:composeApp:assembleDebug :composeApp:assembleDebugAndroidTest` | Windows / Android SDK / Gradle Wrapper | 通过；最终组合命令 `BUILD SUCCESSFUL in 3m 55s`，新设备测试类编译通过，未执行设备用例 |
| `git diff --check` 与变更文档相对链接检查 | 当前工作区 | 通过；仅 Git CRLF 提示及构建既有弃用/非空检查警告 |
| `adb devices -l` | 本机 Android SDK | 没有连接设备，不能执行设备用例或复现用户安装包 |
| 真实模型请求 | 用户 Android 配置 | 未执行；未读取真实 API Key |

实际命令：

```powershell
.\gradlew.bat :composeApp:desktopTest --tests 'com.resonance.player.ui.LyricsTranslationControllerTest' --tests 'com.resonance.player.features.MusicAssistantTest' --tests 'com.resonance.player.features.MusicAiWireTest' --tests 'com.resonance.player.FloatingLyricsTest' :composeApp:assembleDebug :composeApp:assembleDebugAndroidTest --no-configuration-cache --console=plain
```

本次实际生成调试包：`composeApp/build/outputs/apk/debug/Resonance-0.6.5-debug.apk`，176883499 字节，包名 `com.resonance.player.debug`，版本 0.6.5 / 39；SHA-256 为 `b8fe0d09b762f00a63045144287e53793ecefe91780cfdbc28d3110b4523de8a`。该独立调试版不共享正式版模型配置。未安装到用户手机，未作正式发布。

## 限制与后续

合成响应能验证请求与显示逻辑，不能证明用户当前服务、凭据、网络或模型输出正常。Android 原生按钮用例须在连接设备后运行；若用户安装版本仍失败，新的歌词面板错误文字可用于继续定位实际接口原因。

## 同步更新的文档

`docs/README.md` 变更索引、`docs/DEVELOPMENT.md` 第 8 节歌词翻译和第 12.3 节测试定位。
