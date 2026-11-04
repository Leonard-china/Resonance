# 2026-11-03 — 增加 Linux 原生桌面版、凭据保护与 DEB／免安装包

- 修改日期：2026-11-03（Asia/Shanghai）
- 类型：功能 / 平台适配 / 构建配置
- 适用版本：`0.6.5 / 39`，不修改 `version.properties`
- 影响平台：Linux 桌面；Windows 桌面凭据调用点与 FFmpeg 共用定位；共享帮助文字
- 关联需求：用户要求给现有播放器制作 Linux 系统版本。
- 关联记录：[现有桌面启动边界](2026-10-31-14-windows-startup-and-wechat-transfer.md)、[当前原生磨砂界面](2026-11-03-02-frosted-ui-and-release-apk.md)。

## 需求或问题

项目已有 Compose/JVM 桌面端，构建仅声明 Windows EXE/MSI；播放和转换查找写死 `ffmpeg.exe` 及开发者 D 盘路径；新凭据保护强制 Windows DPAPI。因此直接在 Linux 启动不能作为可用 Linux 版本交付，模型设置与伙伴身份保存也会失败。

## 原因与依据

`desktopMain` 的文件扫描、Java Sound、FFmpeg 调用、Compose 窗口、托盘和大部分共享业务可直接复用，无需改成 Web 或创建另一套播放器。阻碍主要位于 `nativeDistributions`、`FfmpegExecutable`、转换器重复定位代码、`DesktopCredentialVault`、默认资料路径与系统网络计费查询。天气和 Windows JNA 点击穿透已有系统判断。

本轮首批产物为 Linux x86_64。保持现有原生界面、转换保留输入、播放请求代次、原子文件／备份、配置写入顺序与同步校验。

## 具体修改

| 文件 | 关键类／函数／配置 | 实际修改与目的 |
| --- | --- | --- |
| [Gradle 配置](../../composeApp/build.gradle.kts) | `nativeDistributions`、`prepareDesktopAppResources`、`packageLinuxPortable` | 增加 DEB/RPM、Linux 图标／AudioVideo 菜单分类（menuGroup）／快捷方式，Linux 携带完整 Java 模块与项目许可证；通过 jpackage `--linux-package-deps ffmpeg` 声明系统依赖；Windows 继续捆绑 FFmpeg，来源为环境变量或 PATH；新增完整 app image 的 tar.gz 任务 |
| [Linux 构建脚本](../../scripts/build-linux.sh) | `deb/rpm/portable` | 检查 JDK、FFmpeg、binutils 和相应打包工具，运行桌面测试、生成便携包与所选安装包；直接调用 Wrapper JAR 兼容当前 CRLF 检出，不改版本、不安装、不发布 |
| [FFmpeg 定位](../../composeApp/src/desktopMain/kotlin/com/resonance/player/audio/FfmpegExecutable.kt)、[转换器](../../composeApp/src/desktopMain/kotlin/com/resonance/player/conversion/DesktopManagedMp3Converter.kt) | `locate/resolve`、`locateFfmpeg` | 按 OS 选择 ffmpeg／ffmpeg.exe，统一显式配置、资源和 PATH 查找，Linux 检查执行权限；转换器复用定位器，移除两处本机 D 盘回退 |
| [桌面路径](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopPaths.kt)、[平台服务](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopPlatformServices.kt) | `dataDirectory`、`DesktopLibraryStore`、`credentialVault` | Linux 默认 XDG 数据目录；仅接受绝对 XDG 路径，显式隔离目录优先；兼容已有 ~/Resonance，不迁移合并；Windows 原默认路径不变，D 盘音频目录探测仅在 Windows 使用 |
| [凭据保护](../../composeApp/src/desktopMain/kotlin/com/resonance/player/features/DesktopCredentialVault.kt)、[音乐访问](../../composeApp/src/desktopMain/kotlin/com/resonance/player/features/DesktopMusicAccess.kt) | `DesktopCredentialVault`、`LinuxCredentialVault` | 从全局对象改为按资料目录实例化；Windows 保留 dpapi 前缀和实现，Linux 使用版本前缀、AES-256-GCM 和随机 nonce；0700 私有目录、0600 密钥、跨进程锁、临时文件 fsync／原子安装；拒绝缺钥、损坏、篡改及符号链接，不覆盖丢失的解密密钥 |
| [网络条件](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/AutomaticDownloadNetwork.kt) | `query`、`linuxUnmetered` | 通过只读 nmcli 查询连接／计费状态，英文稳定输出、3 秒超时、30 秒缓存；连接必须均可确认不计费，未知或工具缺失时等待 |
| [桌面歌词](../../composeApp/src/desktopMain/kotlin/com/resonance/player/ui/DesktopFloatingLyrics.kt)、[功能设置](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureScreens.kt)、[关于页](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/ToolsScreens.kt) | 平台帮助与锁定提示 | Linux 锁定按钮显示“锁定位置”，不承诺鼠标穿透；补充 Linux 网络／托盘／FFmpeg 和平台说明，不改 UI 布局 |
| [Linux 回归](../../composeApp/src/desktopTest/kotlin/com/resonance/player/platform/LinuxDesktopSupportTest.kt) | 7 项回归 | XDG／旧路径、Windows 路径保留、FFmpeg 资源／空格／PATH、凭据跨实例／权限／篡改／丢钥／符号链接、网络未知或计费拒绝 |
| [原生桥](../../composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherNativeBridgeTest.kt)、[反向桥](../../composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherReverseNativeBridgeTest.kt)、[寻址测试](../../composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherRendezvousTest.kt) | 联测凭据调用点 | 按夹具目录构造 vault，避免 Linux 凭据串到日常资料或另一份测试 |
| [清理测试](../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/StorageAndAssistantExtraTest.kt)、[解码失败测试](../../composeApp/src/desktopTest/kotlin/com/resonance/player/playback/DesktopPlaybackFailureTest.kt)、[视觉契约](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/UiVisualContractTest.kt) | 旧测试平台／当前行为假设 | POSIX 使用目录无写权限触发真实 unlink 失败并恢复权限；缺 URI 先断言 Buffering 再断言同请求 Error；已移除的装饰粒子改为断言磨砂背景稳定，紧凑助手按当前“助手选项”菜单检查历史／任务／设置仍可达，不改产品删除／播放／UI 逻辑 |
| [CI](../../.github/workflows/verify.yml) | `linux` job | 增加 Ubuntu 22.04 的 JDK17／SDK36／FFmpeg／binutils／fakeroot、桌面测试与 DEB 构建，并保存报告和包；本地修改不代表远程 CI 已执行 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| Linux 播放／转换 | 尝试 ffmpeg.exe 和作者 D 盘 | 使用系统 ffmpeg 或用户显式配置 |
| Linux 保存 API Key／伙伴身份 | 触发“当前凭据保护需要 Windows” | 使用当前资料目录的私有密钥加密，不写入新明文 |
| Linux 新资料目录 | ~/Resonance | 优先 XDG；已有旧目录继续使用，显式隔离目录优先 |
| Linux 安装／免安装 | 无配置与交付入口 | DEB／RPM 构建配置与完整 tar.gz，携带 Java 运行时 |
| Linux 自动下载限制 | 不计费模式始终拒绝 | NetworkManager 明确不计费时允许，未知时继续等待 |
| Linux 歌词锁定 | 文案承诺 Windows 专用穿透 | 只承诺锁定位置，窗口仍由系统合成器管理 |

## 平台与数据影响

共享数据模型、偏好键、同步包和播放协议不变；不移动／合并／删除用户曲库，不写回原音频标签。Android 原生实现和权限不变。Windows DPAPI 密文及旧明文读取兼容；Windows 的本机工具路径回退改为 PATH/显式配置。普通开发构建不升版。

Linux 密钥文件不是系统钥匙串，能读取同用户资料目录的程序仍能解密；备份需同时保存 credentials/key.bin。跨 Windows／Linux 的受保护凭据不能直接解密，需重新填写；旧明文和已有备份不自动清除。不读取或输出真实密钥、API Key 或签名配置。Linux 产物使用系统 FFmpeg，不复制本机动态链接二进制或用户音频。

## 验证

环境：本机 Ubuntu 26.04.1 x86_64、已有临时 JDK17／Android SDK36、系统 FFmpeg。本机缺 fakeroot／binutils，在忽略的 `build/linux-2026-11-03/tooling/` 下载 Ubuntu 软件包并私有解包，不使用 sudo、不改系统安装；这些工具与本机路径不进入可提交源码或产物。

原始日志均位于本次独立目录 `build/linux-2026-11-03/`。先前配置／测试编译与初始化尝试出现过错误，已修正；首次完整回归的 4 项失败分别为 POSIX unlink、遗漏 Buffering、已移除的粒子、已收纳到菜单的助手入口，均保留失败日志并通过定向和全量复测。首次 jlink 因缺 objcopy 失败，补齐私有 binutils 后通过，不把首次失败说成通过。

```bash
# 定向与完整测试使用项目现有临时 JDK17 执行 Wrapper JAR；以下省略本机工具路径。
RESONANCE_FFMPEG=/usr/bin/ffmpeg RESONANCE_DESKTOP_AUDIO_TEST=1 \
  "$JAVA_HOME/bin/java" -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain \
  :composeApp:desktopTest --no-configuration-cache --console=plain

# 实际使用当前新增脚本执行完整检查和 DEB／免安装包构建；工具通过私有 PATH 提供。
RESONANCE_DESKTOP_AUDIO_TEST=1 bash scripts/build-linux.sh deb

# 仅菜单分类的最终修正后重新生成两种产物，不重复与该打包配置无关的测试。
"$JAVA_HOME/bin/java" -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain \
  :composeApp:packageLinuxPortable :composeApp:packageReleaseDeb --no-configuration-cache --console=plain
```

| 命令／操作 | 结果与本次证据 |
| --- | --- |
| LinuxDesktopSupportTest、转换不改源文件、LibraryPersistenceTest 定向回归 | 通过；targeted-tests-pass.log。本次真实 FFmpeg 来自系统，未借用 Windows 二进制 |
| 4 项旧契约修正 + Linux 7 项 | 11 项通过、无跳过；corrected-contract-tests.log、corrected-contract-summary.json |
| RESONANCE_DESKTOP_AUDIO_TEST=1 原生音频 | 2 项通过、无跳过；native-audio.log 与 native-audio-results，真实 FFmpeg + Java Sound，生成并静音播放合成音频；包含暂停／恢复、seek、变速、快速替换与统计落盘 |
| 最终完整桌面测试 | 307 项：300 通过、7 显式跳过、0 失败／错误；final-tests-summary.json、final-test-results。跳过需要专用原生桥、生产中继、Python 联测或真实模型等显式环境的夹具；不是把这些外部联测算作通过 |
| scripts/build-linux.sh deb | 通过，9m29s，22 项任务；final-build.log。生成完整运行目录、tar.gz 和 DEB，Java 模块／ProGuard／jpackage 均实际执行；既有重复类和可选反射探测提示未导致失败 |
| 成品 tar.gz 解压、执行权限及带空格目录启动 | 通过；移除 JAVA_HOME 和 RESONANCE_FFMPEG 后使用包内运行时；native-verified-summary.json。独立 RESONANCE_DATA／RESONANCE_LIBRARY，未使用日常资料 |
| 原生窗口／中文画面／本机托盘 | 通过；先以无诊断 agent 的包直接启动，由窗口管理器确认 Resonance 窗口；随后仅在忽略目录加载诊断 agent，读取应用自身 Skia 表面，native-verified.png 可见欢迎页／中文导航，窗口 920×640，托盘 supported=true。截图代码不进入产品包 |
| 首次屏幕截图 | 未完成；Wayland 下 Robot 截图阻塞，进程与窗口实际正常。终止隔离进程后改为应用内部渲染截图，未改系统截图权限，也未把截图失败误报为产品崩溃 |
| Linux 原生资料权限 | 本次真实启动生成 credentials/key.bin，32 字节、0600；只检查权限／大小，不读取或输出密钥 |
| 最终菜单分类、DEB 结构与归档哈希 | 通过；package-final.log：1m13s、17 项任务，6 执行／11 up-to-date；deb-final-metadata.txt 为 Categories=AudioVideo，包名 resonance、0.6.5-1、amd64，依赖含 ffmpeg；最终 DEB 再次解包并以包内 Java 启动，deb-final-summary.json 全项通过、deb-native-verified.png 中文欢迎页正常；SHA-256 从最终重新归档的两包生成 |
| bash -n、非法参数退出、workflow YAML、文档链接与 diff | 通过；非法格式返回 2 和 Usage；YAML 可解析；新增文档本地链接均存在；按仓库 CRLF 使用 git -c core.whitespace=cr-at-eol diff --check 无新增空白错误 |
| apt 安装／卸载、其他发行版、ARM64、RPM、GitHub Linux CI、Android／Windows 本机包 | 本次未执行；不把 DEB 解包检查或 Linux 运行算成这些项目通过 |

本次 Linux DEB 的系统依赖含 libasound2t64、libpng16-16t64 和 ffmpeg；Ubuntu 22.04 等无这些包名的系统应使用便携版或在本机重建。不能把当前包宣传为所有 Linux 发行版通用安装包。


### 本地试用产物

目录：`release/0.6.5/2026-11-03-linux-x86_64/`，另含 `README.txt` 与 `SHA256SUMS.txt`。未覆盖既有 Android／Windows 正式包；没有执行发布。

| 文件 | 字节数 | SHA-256 |
| --- | --- | --- |
| `resonance_0.6.5-1_amd64.deb` | 245,448,174 | `779e81ab7019c3d689bc252188186e2ae53a0710a4c5f5b5715a05046b477d3e` |
| `Resonance-0.6.5-linux-x86_64.tar.gz` | 266,522,067 | `37372c9cfc78632c9872f76c663a6ff90ae5bdb8fbe393a23fc94a356f4f7b1a` |

## 限制与后续

- 本轮只生成 Linux x86_64 本地试用产物，不公开发布、不替换官网、不推送 Git、不升版。
- 其他发行版／旧 glibc、ARM64、原生 Wayland 与不同窗口管理器需分别验收；RPM 有配置但本机没有 rpmbuild，未构建。
- 托盘／透明置顶取决于桌面环境；Linux 尚无鼠标穿透与系统天气定位，保留手动城市。
- KGG 依赖合法匹配的酷狗数据库，Linux 不自动发现 Windows AppData；使用原有 FLYINGMOUSE_KGG_DB_PATH 入口。
- Android／Windows 设备和一起听独立跨网联测不由本次 Linux 启动／单机音频测试代替；新增远程 CI 未运行。

## 同步更新的文档

新增 [Linux 使用与构建说明](../LINUX.md)；更新根 README、docs/README 变更索引、DEVELOPMENT 平台范围／源集／Linux 构建与路径／FFmpeg 定位／产物／任务表、THIRD_PARTY 凭据及 FFmpeg 边界。AGENTS、AI-WORKFLOW 与 CHANGE-TEMPLATE 增加 Linux 平台检查及记录约定，保持用户数据和交付规则。
