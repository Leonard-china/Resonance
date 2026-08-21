# Resonance

<p align="center">
  <img src="assets/branding/resonance-icon-master.png" alt="Resonance 应用图标" width="128">
</p>

一个面向 Android 与 Windows 的本地音乐库：扫描和整理本机音频、转换受支持的 KGMA 文件、读取歌曲元数据与专辑封面、像常规音乐应用一样播放，并在不部署服务器的情况下同步两台设备。

当前版本：`v0.2.0`

> Resonance 坚持本地优先：不要求注册账号，不上传音乐库，也不依赖常驻云服务。酷狗歌单链接仅用于读取公开曲目目录并匹配本机已有歌曲；应用不会下载会员/受保护音频，也不会绕过账号、DRM 或版权限制。

## 功能概览

| 功能 | Android | Windows | 说明 |
| --- | :---: | :---: | --- |
| 本地音乐扫描 | ✅ | ✅ | 识别 MP3、FLAC、M4A 等平台可见音频 |
| 歌名、歌手、专辑、封面解析 | ✅ | ✅ | 优先读取音频标签，不直接把文件名当作完整歌曲信息 |
| 自动逐行歌词 | ✅ | ✅ | 通过 LRCLIB 自动匹配并本地缓存，无需粘贴歌词或配置 API Key |
| 播放控制 | ✅ | ✅ | 播放/暂停、上一首、下一首、进度、随机、列表循环、单曲循环 |
| 全屏播放与队列 | ✅ | ✅ | 沉浸式封面、歌词自动滚动、点按歌词跳转与播放队列 |
| 全部歌曲与收藏 | ✅ | ✅ | 扫描后直接浏览本地曲库，不必先创建歌单 |
| 歌单管理 | ✅ | ✅ | 新建、导入、删除歌单；删除歌单不会删除音频文件 |
| 酷狗公开歌单导入 | ✅ | ✅ | 导入曲目目录和封面，然后匹配本机已有音乐 |
| KGMA → MP3 | ✅ | ✅ | 当前支持 KGMA v3 / slot 1，递归处理所选文件夹 |
| 删除本地 MP3 | ✅ | ✅ | 仅允许删除 Resonance 自己转换或同步生成的 MP3，保护原始音乐 |
| 局域网同步 | 接收端 | 发送端 | Windows 生成 10 分钟有效的一次性二维码，Android 扫码接收 |
| 加密同步包 | ✅ | ✅ | 可离线导出/导入 `.resonance` 包，无需服务器 |
| 响应式界面 | ✅ | ✅ | 手机使用底部导航，宽屏使用导航栏/多栏布局 |

## 下载与安装

请从 [GitHub Releases](../../releases/latest) 下载当前版本。不要只复制 Windows 的单个 `Resonance.exe`，便携版必须完整解压后运行。

| 文件 | 用途 | SHA-256 |
| --- | --- | --- |
| `Resonance-0.2.0-release.apk` | Android 8.0（API 26）及以上 | `B3B7CBBAE97AAC2B4EDA94873E5EF8636844F1DB3FD4FBDAB86C90FBDD685AF6` |
| `Resonance-0.2.0.exe` | Windows 10/11 x64 安装器 | `EEAA564930093D21C162F8848B0E1E12890258538AE08AAE824BA8894F9090EA` |

> Windows 版请完整保留安装目录；单独复制 `Resonance.exe` 无法启动其 JVM 运行时。

### Android

1. 下载 APK，在系统设置中允许当前文件管理器/浏览器“安装未知应用”。
2. 安装后首次启动，按需授予“音乐和音频”权限。
3. Android 11 及以上若要转换 KGMA，请使用“选择文件夹并转换 KGMA”，在系统文件选择器中选择目标目录，再点“使用此文件夹/允许”。转换只读源文件，转换完成后保留源 KGMA。

### Windows

- 推荐普通用户运行 `Resonance-0.2.0.exe` 安装器；需要批量部署时也可单独构建 MSI。
- 便携版必须完整解压 `Portable.zip`，确保 `Resonance.exe`、`app` 与 `runtime` 保持在同一目录结构中。
- 若 Windows SmartScreen 提示未知发布者，请先核对上方 SHA-256，再选择是否运行。本项目当前没有商业代码签名证书。

在 PowerShell 中校验下载文件：

```powershell
Get-FileHash -Algorithm SHA256 .\Resonance-0.2.0-release.apk
```

## 快速开始

### 建立音乐库

首次打开应用时，如果音乐库为空，可以：

1. 扫描系统可见音乐；
2. 选择一个 KGMA 文件夹并转换；
3. 新建歌单；
4. 导入酷狗公开歌单链接，再与本机歌曲匹配；
5. 从另一台设备导入加密同步包。

扫描后，Resonance 会读取音频标签中的标题、歌手、专辑和内嵌封面。标签不完整时才使用安全的文件名回退规则。封面会进入本地缓存，原音频不会因扫描而被改写。

### 导入酷狗歌单

1. 在酷狗中复制公开歌单分享链接，例如 `https://t1.kugou.com/...`。
2. 打开“导入”→“导入酷狗歌单链接”。
3. 粘贴链接并确认。
4. Resonance 读取公开目录、歌单名称和封面，并按歌曲标题/歌手匹配本机音乐。

“匹配完成”表示公开目录里的曲目已与本机库建立对应关系，不代表应用从酷狗下载了音频。若提示无法解析酷狗服务器，请检查网络、VPN、私人 DNS 或防火墙后重试。

### 转换 KGMA

1. 打开“导入”→“选择文件夹并转换 KGMA”。
2. 选择包含 `.kgm`/`.kgma` 的目录；应用会递归查找子目录。
3. 每首歌先解密到临时文件：
   - 原始载荷已经是 MP3 时，直接无损复制，不重复编码；
   - FLAC/Ogg 等载荷通过 FFmpeg 转为 `320 kbps` MP3。
4. MP3 成功写入 Resonance 管理目录后即完成转换；源 KGMA 始终保留，不会被应用删除。
5. 普通系统扫描同样不会修改或删除原始文件。

当前转换器只支持 **KGMA v3 / slot 1**。其他版本会明确提示“不支持”，不会修改源文件。

### 播放与删除

- 点按歌曲开始播放；迷你播放器和完整播放器会同步显示当前曲目与封面。
- 播放模式支持顺序/列表循环、单曲循环和随机播放。
- 歌曲菜单中的“删除本地 MP3”只作用于 Resonance 转换或同步生成的托管文件；系统扫描到的原始音乐只会从音乐库移除，不会被应用越权删除。
- 删除歌单仅删除歌单关系，不会删除其中的音乐文件。

### 自动歌词与全屏播放

1. 播放任意本地歌曲，点按底部迷你播放器进入全屏播放页。
2. 切换到“歌词”，Resonance 会按歌名、歌手、专辑和时长自动匹配；无需手动输入歌词文本。
3. 有时间轴的歌词会跟随进度自动滚动，点按某一行即可跳到对应位置；没有时间轴时仍会展示普通歌词。
4. 成功结果缓存 30 天，“未找到”结果缓存 24 小时；可随时点“重新匹配”强制刷新。

自动歌词使用 [LRCLIB](https://lrclib.net/docs) 的公开只读 API。只有当前播放曲目的歌名、歌手、专辑和时长会用于匹配，不会上传音频文件、封面、歌单或本地路径。

## 无服务器同步

### 局域网二维码同步

适合 Windows 向 Android 发送当前音乐库：

1. 让电脑和手机连接同一个可信局域网，并临时关闭会隔离局域网设备的 VPN。
2. Windows 版打开“同步”→“开启局域网扫码同步”。
3. Windows 启动一次性 HTTP 端点并生成二维码，端点 10 分钟后或完成一次传输后自动关闭。
4. 用 Android 系统相机扫描二维码，选择 Resonance 打开链接。
5. Android 验证地址必须是私有局域网 IPv4，下载后再验证同步包口令、完整性和曲目哈希，最后合并歌单、封面与缺失 MP3。

若无法连接，请确认：两台设备在同一网段；Windows 防火墙允许当前专用网络通信；路由器没有开启 AP/客户端隔离；二维码仍在有效期内。

### 离线加密同步包

1. 发送端选择“导出加密同步包”，设置口令并保存 `.resonance` 文件。
2. 通过 U 盘、USB 数据线、局域网共享等方式传到另一台设备。
3. 接收端选择“导入加密同步包”，输入相同口令。

同步包包含歌单、封面和可用的托管 MP3。导入器限制文件数量、单项大小和解压总量，并防止压缩包路径越界。

## 项目结构

项目基于 Kotlin Multiplatform 与 Compose Multiplatform，共享界面、状态与领域逻辑，同时保留平台原生文件访问和播放能力。

```text
Mp3/
├─ composeApp/
│  ├─ src/commonMain/     # 共享 UI、状态、模型、转换基础逻辑
│  ├─ src/commonTest/     # 共享单元测试
│  ├─ src/jvmMain/        # Android/Windows 共用的 JVM 实现
│  │                       # 自动歌词仓库、LRC 解析、歌单与同步包
│  ├─ src/androidMain/    # Android MediaStore、Media3、SAF、前台播放服务
│  ├─ src/desktopMain/    # Windows 文件系统、JavaFX Media、局域网发送端
│  └─ src/desktopTest/    # 转换、歌单导入、同步及安全边界测试
├─ docs/                  # 设计、第三方依赖与发布说明
├─ scripts/               # 本机构建脚本（默认把缓存与工具放在 D:）
├─ gradle/                # Gradle Wrapper 与版本目录
└─ README.md
```

主要技术：

- Kotlin `2.3.20`
- Compose Multiplatform `1.11.0`
- Android Gradle Plugin `8.8.2`
- Android Media3 `1.10.1`
- JavaFX Media `21.0.8`
- FFmpegKit Maintained / FFmpeg（音频转码）
- jaudiotagger `3.0.1`（音频标签与封面）
- LRCLIB API（自动匹配普通/逐行歌词，无需 API Key）

## 本地构建

### 环境要求

- JDK 17–21（构建脚本会拒绝当前不兼容的 JDK 25）
- Android SDK 36 / Build Tools 36.0.0
- Gradle Wrapper，或项目现有的 Gradle 8.10.2
- Windows 10/11 x64（构建 EXE/MSI）
- 一份可用的 `ffmpeg.exe`，通过 `RESONANCE_FFMPEG` 指定

为了避免占用 C 盘，可把 Gradle 缓存、Android SDK 与 FFmpeg 放到其他磁盘：

```powershell
$env:GRADLE_USER_HOME = 'D:\Development\GradleCache'
$env:ANDROID_HOME = 'D:\Development\AndroidSdk'
$env:RESONANCE_FFMPEG = 'D:\Tools\ffmpeg\bin\ffmpeg.exe'
```

运行全部 JVM 测试并构建 Android Release APK：

```powershell
.\gradlew.bat :composeApp:allTests :composeApp:assembleRelease --no-configuration-cache
```

构建 Windows Release 便携目录、EXE 和 MSI：

```powershell
.\gradlew.bat :composeApp:createReleaseDistributable `
  :composeApp:packageReleaseExe `
  :composeApp:packageReleaseMsi `
  --no-configuration-cache
```

开发机也可以直接运行 `scripts\build.cmd`。该脚本使用项目作者当前的 D 盘工具路径；在其他机器上请先修改脚本或设置相应环境变量。

### Android Release 签名

真实签名库与口令不会提交到 Git。复制示例文件并填写自己的值：

```powershell
Copy-Item .\keystore.properties.example .\keystore.properties
```

`keystore.properties` 格式：

```properties
storeFile=signing/your-release-key.jks
storePassword=change-me
keyAlias=your-key-alias
keyPassword=change-me
```

`signing/`、`*.jks`、`*.keystore` 和 `keystore.properties` 均被忽略。请离线备份签名库；丢失后无法用同一签名升级已安装 APK。

## 测试与发布质量门槛

`v0.2.0` 发布前质量门槛：

- Gradle `allTests` 完整通过，0 失败；
- Android `lintRelease` 完整通过，0 错误；
- KGMA v3/slot 1 解密、MP3 原样复制与转换失败保护，转换后保留源文件；
- 酷狗公开歌单解析、分页、主机限制和本地匹配；
- 加密同步包的口令、完整性、大小限制与路径安全；
- Windows 局域网一次性分享端点与跨设备同步夹具；
- MuMu 模拟器完成手机竖屏、横屏、完整播放页、歌词状态、队列与旋转状态保持的视觉/交互检查；
- Windows 完成当前 Release 代码启动、宽屏响应式布局、导入、同步与设置页的视觉检查；
- APK v2 签名校验通过，APK 与 EXE 的 SHA-256 已写入下载表。

完整版本说明见 [`docs/RELEASE_NOTES-0.2.0.md`](docs/RELEASE_NOTES-0.2.0.md)。

## 数据、安全与限制

- 音乐库、封面缓存、歌词缓存与播放状态保存在设备本地；局域网同步由用户主动开启，不使用中心服务器。
- 播放歌曲时会把曲目元数据发送给 LRCLIB 做歌词匹配，但不会发送音频、封面或本地文件路径。
- 局域网端点只接受一次下载并自动失效；建议只在可信网络使用。
- Android 采用系统 MediaStore 与 Storage Access Framework，不请求“管理所有文件”权限。
- Resonance 不提供在线音频下载服务；酷狗接口或分享链接规则发生变化时，公开目录导入可能暂时失效。
- KGMA 是平台相关格式，当前仅实现项目已验证的 v3 / slot 1 变体。
- Windows 内置 FFmpeg 的分发许可与对应源码信息见 [`docs/THIRD_PARTY.md`](docs/THIRD_PARTY.md)。

## 常见问题

**为什么歌单显示“全部匹配”，转换时仍有提示？**

歌单匹配与文件转换是两条流程。KGMA 转换只读源文件并始终保留源 KGMA，转换成功与否不会影响已匹配的歌单。

**为什么匹配到本地 MP3 后封面变了或消失？**

本地歌曲优先使用音频文件内嵌封面；若标签没有封面，则保留已匹配的歌单封面缓存。重新扫描不会无条件清空已有封面。

**为什么 Windows 便携版只复制 EXE 后提示 Failed to launch JVM？**

`Resonance.exe` 依赖同目录的 `app` 和 `runtime`。请完整解压 Portable ZIP，或使用 Setup/MSI 安装包。

**应用可以直接从酷狗下载 KGMA 吗？**

不可以。公开歌单导入仅获取公开元数据并匹配用户已经合法保存在本机的文件。

## 许可与第三方组件

本仓库当前未附加开源许可证；除第三方组件按各自许可证使用外，项目源码默认保留所有权利。第三方运行时与许可证注意事项见 [`docs/THIRD_PARTY.md`](docs/THIRD_PARTY.md)。
