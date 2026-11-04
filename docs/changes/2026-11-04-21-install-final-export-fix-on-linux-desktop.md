# 2026-11-04 — 优先安装包含最终歌单导出修复的本机 Linux 版本

- 修改日期：2026-11-04（Asia/Shanghai）
- 类型：本机安装、验证记录
- 适用版本：0.6.6 / 40，不升版
- 影响平台：用户当前 Linux x86_64 电脑
- 关联需求：三端发布过程中用户要求「先给我的电脑装上」
- 关联记录：[三端正式发布](2026-11-04-20-publish-0.6.6-three-platforms.md)、[导出命名与目录](2026-11-04-19-playlist-names-and-direct-export-directory.md)、[前次桌面更新](2026-11-04-16-release-0.6.6-three-platforms.md)

## 需求与原因

当前桌面及应用菜单通过固定启动链接指向今晚较早的 `0.6.6-20261104-release`，该完整程序先于导出修复构建。用户要求优先安装最终版本；不能仅因版本号相同就复用较早的 Linux 包。

## 实际修改

从本次构建的完整 Linux 便携包解压到新的当前用户程序目录，检查 ELF 启动器、Java 运行时、应用版本和导出反馈／命名类；保留旧程序，备份原指针及桌面入口，再原子切换 `current`。原桌面和菜单继续使用同一固定启动链接，安装后从真实桌面文件启动并核对进程路径及原生可见窗口。

本机安装脚本及证据只放忽略的 `build/install-export-fix-2026-11-04/`，不提交本机工具或用户数据。不更改播放器业务源码、音乐、标签、曲库和设置，不进入 apt/dpkg、不修改系统权限。

## 修改前后

| 场景 | 安装前 | 安装后 |
| --- | --- | --- |
| 桌面入口 | 较早的 0.6.6 app image | 本次最终导出修复的完整 app image |
| 旧程序／用户数据 | 已有旧版本与真实曲库 | 保留，只切换程序入口 |

## 验证

- 本轮 `:composeApp:desktopTest :composeApp:lintRelease :composeApp:assembleRelease :composeApp:packageLinuxPortable`：**通过**，11m 51s。桌面总计 358 项，348 通过、10 跳过，零失败／错误；具体发布边界见记录 20。
- 本次 Linux TAR.GZ SHA-256：`25b282af3d61c9ff8072d811c6e2ae99078e890ca4bd2ea0320096cfa043d637`，由本次文件计算。
- 安装路径：`~/.local/opt/resonance/0.6.6-20261104-export-fix/`；`current` 原子切换到该目录，旧 `0.6.6-20261104-release/` 保留。ELF、内置运行时、0.6.6 配置和本轮导出类均通过检查。
- 两份 `desktop-file-validate`：**通过**，原桌面和应用菜单继续使用 `~/.local/bin/resonance`。
- 初次经 `gio launch` 的自动拉起未找到持续进程，不能记为启动通过。随后直接运行同一固定启动入口成功；`/proc/211855/exe` 指向新安装目录，窗口 `0x220002d` 标题为 Resonance、状态 Normal／IsViewable。应用启动日志无输出。
- 证据：忽略目录 `build/install-export-fix-2026-11-04/installation.json`、`launch.log`、`entry-backup/`。

## 限制与后续

本机完整程序安装及可见窗口验收已完成，未操作真实歌曲进行破坏性回归。用户实际听歌／长期运行未在本次重新验证。GitHub／官网三端发布仍沿原需求继续，不将本机更新当作公开发布完成。

## 同步文档

[文档索引](../README.md)、[Linux 使用说明](../LINUX.md)及[开发说明](../DEVELOPMENT.md)。
