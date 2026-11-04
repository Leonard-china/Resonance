# 2026-11-04 — 覆盖安装手机 Release 并更新 Linux 桌面快捷方式

- 修改日期：2026-11-04（Asia/Shanghai）
- 类型：本地构建、设备安装、桌面配置、文档
- 适用版本：0.6.5 / 39，不自动升版
- 影响平台：已连接 Android 手机、当前 Linux 用户
- 关联需求：用户明确要求“手机已连接，直接构建 release 覆盖安装，再做 Linux 桌面快捷方式”。承接[原生浅色 UI](2026-11-04-06-native-light-ui-and-continuous-navigation.md)。

## 需求或问题

上轮只交付候选包，未更新手机及本机既有 Linux 安装。手机现有正式包为 `com.resonance.player`、0.6.5 / 39；Linux 的固定启动链接仍指向 11 月 3 日的程序，应用菜单已有入口，但桌面没有本轮快捷方式。

## 原因与依据

Debug 包的 applicationId 与正式版不同，不能用它完成正式版覆盖。必须沿用项目现有 Release 签名，核实其证书与手机已安装包一致，再使用 ADB 的保留数据替换安装。Linux 需将完整 app image 放入长期目录，并更新 `current` 指针；不能让快捷方式依赖项目临时构建目录。

## 具体修改

| 位置 | 操作 | 目的 |
| --- | --- | --- |
| Gradle `:composeApp:assembleRelease` | 使用项目既有签名配置构建；不读取或输出真实 keystore 配置/私钥 | 生成可覆盖现有正式版的安装包 |
| 手机 `com.resonance.player` | 检查包版本、公开证书摘要；通过 `adb install -r` 覆盖并启动 | 更新 UI，保留应用数据和原授权，不卸载或清数据 |
| `~/.local/opt/resonance/0.6.5-20261104-light-ui/` | 校验上轮最终 Linux 便携包后完整解压 | 保留运行时、资源及原生库；旧 `0.6.5/` 目录不删除 |
| `~/.local/opt/resonance/current` | 原子替换为指向新程序目录的相对链接 | 现有 `~/.local/bin/resonance` 无需改变 |
| `~/.local/share/applications/resonance.desktop` 与 `~/Desktop/Resonance.desktop` | 统一 Exec/TryExec/Icon；桌面文件 0755 且 trusted | 应用菜单与桌面均可启动新版 |
| 本轮 `build/release-install-2026-11-04/` | 构建日志、安装前 APK、公开签名摘要、入口备份、安装与窗口验证记录 | 留存本机证据，不进入 Git |

只更新本地程序和桌面配置，不修改产品源码。Linux 原入口文件已备份，旧程序目录仍在；没有复制、迁移或删除用户音乐/凭据/配置。

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 手机启动正式包 | 11 月 3 日旧 UI 构建 | 本轮浅色原生 UI Release，包名/版本号不变 |
| Linux 启动链接 | 指向旧 `0.6.5/` | 指向本轮完整程序目录 |
| 桌面直接打开 | 无本轮快捷方式 | 桌面 `Resonance.desktop` 与菜单启动同一新版 |

## 平台与数据影响

Android 使用同签名覆盖路径，不执行 uninstall、pm clear 或权限重置。Linux 仅改变程序目录与启动入口，继续使用原 XDG 数据和音乐目录，不进入 apt/dpkg、不写系统目录、不更改文件关联。Windows、官网、论坛、服务器、版本文件和同步格式均不变。启动可能产生正常应用状态更新；没有为验证扫描/导入音乐或发起付费请求。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| ADB 设备/已安装包检查 | 已连接 VKJ-AN00、Android 16 | 正式包 0.6.5 / 39，先保存旧 APK 与公开签名摘要；不读取应用私有数据 |
| `assembleRelease packageLinuxPortable` | Linux / JDK17 / SDK36 | 通过，2m22s，退出码 0；本次新构建 Android Release，Linux 目标为 UP-TO-DATE，使用上轮同一最终产物；`build.log` |
| `apksigner verify --verbose --print-certs`、aapt2 包名/版本检查 | 新 Release 与手机旧 APK | 通过；公开证书 SHA-256 完全一致，正式包名和 0.6.5 / 39 匹配。不读取 keystore 内容 |
| `adb install -r` | 已连接手机 | **Success**；lastUpdateTime 为 2026-11-04 14:45:34，firstInstallTime 仍为原时间，未卸载/清数据；`adb-install.log` |
| `am start -W`、包进程/前台 Activity | 已连接手机 | 通过：Status=ok，进程存活，MainActivity 已恢复至前台；当前 PID 的 crash 缓冲无 FATAL EXCEPTION。`android-startup.txt`、`android-installation.json` |
| 手机新 `base.apk` 与本地产物 SHA-256 比对 | 已连接手机 | 一致，确认实际安装的就是本次 Release，不只依赖相同版本号判断 |
| Linux tar.gz SHA-256、完整解压与指针切换 | 当前 Linux 用户 | 通过；来源是上轮已校验的最终 Release 便携包；本轮 Gradle 同时检查该目标为最新 |
| `desktop-file-validate`、桌面缓存刷新、`gio metadata::trusted` | KDE / Wayland 会话 | 通过；桌面快捷方式具备执行权限且 trusted=true |
| 从桌面文件启动，检查 `/proc/<pid>/exe` 和 X11 窗口属性 | 本机原生窗口 | 启动通过：进程来自新安装目录，创建 920×612 的 Resonance 窗口，窗口 PID 与程序进程匹配；检查时窗口为 Iconic/最小化，不能写成可见画面验收。工具会话直接拉起未持久保持，随后通过临时用户服务执行同一 `gio launch`，不设置开机自启动 |
| Kotlin 全量测试、播放/共听/付费服务、真机帧率 | 本次仅构建安装，不改源码 | 未重新执行；不把前轮测试/离屏截图写成本次运行通过 |

本次 [Release APK](../../build/release-install-2026-11-04/Resonance-0.6.5-release.apk) 与 [SHA256SUMS](../../build/release-install-2026-11-04/SHA256SUMS.txt) 保留在本机忽略目录；校验值取自本次实际文件。未上传官网或发布外部下载。

## 限制与后续

本次验证安装和启动，不代表音乐播放、共听、全部交互或高刷新率性能完成实测。手机数据通过保留数据覆盖安装保留，没有读取私有内容逐项比对。Linux 为当前用户安装，不纳入系统包管理；旧目录与入口备份可用于回滚程序，不自动回滚应用运行时数据。

## 同步更新的文档

- `docs/README.md`：当前安装结果与变更索引。
- `docs/LINUX.md`、`docs/DEVELOPMENT.md` Linux 安装章节：新当前程序目录和桌面快捷方式。
