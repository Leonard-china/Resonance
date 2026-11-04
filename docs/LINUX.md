# 2026-11-03 — Linux 桌面版使用与构建

Linux 版复用原生 Compose Desktop 界面、FFmpeg 解码及 Java Sound 播放，不需要浏览器。首批交付面向 x86_64，版本继续来自 `version.properties`，本轮不升版。具体构建及实测结果见 [Linux 支持变更记录](changes/2026-11-03-04-linux-desktop-support.md)。

## 安装与运行

Ubuntu／Debian 桌面环境：在安装包所在目录执行（文件名以实际产物为准）：

```bash
sudo apt install ./resonance_0.6.5-1_amd64.deb
```

本次 DEB 构建于 Ubuntu 26.04，依赖 `libasound2t64`、`libpng16-16t64` 等本机发行版包名。Ubuntu 22.04 等没有这些包名的系统可使用免安装版或在本机重建；其他系统的安装兼容性未验证。

安装后从应用菜单打开 Resonance。包内包含 Java 运行时，FFmpeg 由系统包管理器安装。需要图形会话、可用的音频输出，以及系统字体；中文缺字时安装发行版的 Noto CJK 字体包。

免安装版需要先安装 FFmpeg，然后完整解压并启动：

```bash
tar -xzf Resonance-0.6.5-linux-x86_64.tar.gz
./Resonance/bin/Resonance
```

不要只复制 `bin/Resonance`；`lib/` 内含程序、原生库和 Java 运行时。免安装包仍依赖系统的图形／音频库；不同发行版和旧系统的兼容性需分别验证。

### 当前用户安装（无需管理员认证）

可将免安装包的完整程序目录长期安装在 `~/.local/opt/resonance/<版本>/`，以 `current` 链接选择版本，`~/.local/bin/resonance` 指向其 `bin/Resonance`。在 `~/.local/share/applications/resonance.desktop` 中将 Exec/TryExec 指向启动链接，Icon 指向 `current/lib/Resonance.png`，分类为 `AudioVideo;Audio;Player;`，再刷新桌面数据库。

2026-11-03 已在当前电脑完成 `0.6.5` 用户安装并从菜单入口打开，见 [实际安装记录](changes/2026-11-03-05-linux-user-installation.md)。用户安装不进入 apt/dpkg；应用数据和音乐继续使用下述原有目录。系统 DEB 安装方式仍如上，需管理员认证。

2026-11-04 按用户要求更新到浅色 UI 版本：保留旧 `0.6.5/`，`current` 指向 `0.6.5-20261104-light-ui/`；新增 `~/Desktop/Resonance.desktop`，与应用菜单共用固定启动链接。来源为已校验的完整 Release 便携包，保留既有数据与音乐目录；见[本轮安装记录](changes/2026-11-04-07-release-phone-update-and-linux-shortcut.md)。

## 文件与平台差异

- 数据默认位于 `${XDG_DATA_HOME:-~/.local/share}/resonance`；相对 XDG 路径无效时使用默认值。若新目录不存在且旧 `~/Resonance` 已存在，继续使用旧目录，不自动移动或合并。
- 管理的音频默认位于 `~/Music/Resonance`。可用 `RESONANCE_DATA`、`RESONANCE_LIBRARY` 分别覆盖数据与音频目录；调试时两者均指向独立目录。
- FFmpeg 按系统属性 `resonance.ffmpeg`、环境变量 `RESONANCE_FFMPEG`、包资源、PATH 顺序查找，Linux 使用 `ffmpeg`。路径带空格可用，不经过 Shell 拼接。
- 新 API Key、Cookie、伙伴身份等使用 AES-256-GCM；每次随机 nonce，密钥放在当前数据目录的 `credentials/key.bin`。目录权限 0700、密钥 0600；不是系统钥匙串，能读取整个用户目录的同用户程序仍能取得密钥。备份应包含密钥；跨 Windows／Linux 需重新填写凭据，不能直接解密 DPAPI。旧明文配置仍按原兼容逻辑读取，再次保存时加密，不自动删除历史备份。
- 有系统托盘时关闭窗口收至托盘，否则关闭即退出。后台播放／推荐依赖应用进程保持运行。
- “仅不计费网络”通过 NetworkManager 的 `nmcli` 判断；无 NetworkManager、查询失败或计费状态未知时等待，手动更新仍可使用。
- 天气使用手动设置的城市；没有接入 Linux 系统定位。桌面歌词使用透明置顶窗口，窗口管理器决定最终效果；Linux 锁定位置不提供 Windows 的鼠标穿透。
- KGMA 转换复用桌面实现并保留原文件。KGG 仍需要用户合法持有的匹配酷狗密钥库；Linux 不会自动找到 Windows 的 AppData，可用已有 `FLYINGMOUSE_KGG_DB_PATH` 指定数据库。

## 从源码构建

需要 JDK 17–21（含 `jpackage`）、Android SDK 36／Build Tools 36.0.0（当前共享 Gradle 工程仍配置 Android 插件）、FFmpeg、binutils（jlink 使用 objcopy）。生成 DEB 另需 `dpkg-deb`、`fakeroot`；RPM 另需 `rpmbuild`。无需签名配置即可构建桌面包；不要查看或提交真实签名文件。

```bash
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/android-sdk
./scripts/build-linux.sh            # 桌面测试 + 免安装包 + DEB
./scripts/build-linux.sh portable   # 桌面测试 + 免安装包
./scripts/build-linux.sh rpm        # 桌面测试 + 免安装包 + RPM
```

脚本不修改版本，不安装到系统，不发布。输出在 `composeApp/build/compose/binaries/main-release/{deb,rpm,tar}/`。

可单独执行 Gradle 任务（Wrapper 文件为 LF 的检出可直接使用 `./gradlew`）：

```bash
"$JAVA_HOME/bin/java" -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain \
  :composeApp:desktopTest :composeApp:packageLinuxPortable :composeApp:packageReleaseDeb \
  --no-configuration-cache --console=plain
```

Linux 包必须在 Linux 构建，不能由 Windows 的 EXE 构建任务代替。当前平台打包与运行时模块依据 [Compose 官方分发文档](https://kotlinlang.org/docs/multiplatform/compose-native-distribution.html)；网络读取依据 [NetworkManager nmcli 文档](https://networkmanager.dev/docs/api/latest/nmcli.html)。
