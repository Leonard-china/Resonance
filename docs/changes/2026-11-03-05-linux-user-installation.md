# 2026-11-03 — 安装 Linux 当前用户版并接入应用菜单

- 修改日期：2026-11-03（Asia/Shanghai）
- 类型：本机安装 / 桌面配置 / 文档
- 适用版本：`0.6.5 / 39`，不升版、不重新构建
- 影响平台：当前 Ubuntu 26.04 x86_64 用户
- 关联需求：用户要求“帮我装上”。
- 关联记录：[Linux 原生版与本次产物](2026-11-03-04-linux-desktop-support.md)。

## 需求或问题

Linux 安装包已构建交付，但尚未安装，系统也没有 resonance DEB 记录。用户要求直接安装并使用。

## 原因与依据

当前用户没有无需交互认证的 sudo 权限，`sudo -n true` 提示需要认证。完整免安装包包含 Java 和全部程序资源，系统已有 FFmpeg，可以在当前用户目录长期安装并创建应用菜单入口，无需管理员认证。安装前检查确认目标程序目录、启动链接、菜单文件及默认 Resonance 数据目录均不存在。

## 具体修改

| 位置 | 配置／操作 | 目的 |
| --- | --- | --- |
| `~/.local/opt/resonance/0.6.5/` | 从已校验的 tar.gz 在临时目录解压，再移动完整应用目录 | 程序留在用户目录，运行不依赖项目 build 或临时 JDK |
| `~/.local/opt/resonance/current` | 相对链接指向 `0.6.5` | 固定当前版本入口 |
| `~/.local/bin/resonance` | 链接到 current/bin/Resonance | 固定启动路径，不修改 Shell 配置 |
| `~/.local/share/applications/resonance.desktop` | Exec/TryExec 指向启动链接，Icon 指向随包图标；AudioVideo/Audio/Player 分类，StartupWMClass 对应真实窗口 | 应用菜单中可找到 Resonance，正确匹配窗口和图标；不更改默认文件关联 |
| 用户桌面缓存 | `update-desktop-database`、`kbuildsycoca6 --noincremental` | 刷新当前桌面菜单 |
| 当前桌面会话 | `gtk-launch resonance.desktop` | 从实际菜单定义启动，保留窗口供用户使用 |

安装涉及的本机路径和检查 JSON 仅在忽略的 `build/linux-install-2026-11-03/` 保存。没有新增通用安装脚本或修改播放器源码。

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 程序位置 | 仅有项目内的交付包／测试解压目录 | 完整安装在当前用户目录 |
| 应用菜单 | 无 Resonance 入口 | 可搜索并打开 Resonance |
| 启动 | 需要找到并解压交付包 | 已从桌面入口启动并显示真实窗口 |

## 平台与数据影响

仅安装当前用户的应用和桌面入口，不写入 /opt、系统包数据库或系统权限策略；系统 DEB 状态仍未安装。播放器按原有 XDG 默认目录保存自身设置，不导入、删除、迁移或写回用户音频，不改共享模型／同步格式／凭据格式。Android、Windows、官网和版本号不变。未读取或输出 API Key、签名或本机凭据内容。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| 安装前 dpkg-query、目录检查 | Ubuntu 26.04 x86_64 | 确认无已有软件包或目标用户安装，保留现有工作区修改 |
| 安装前重新计算 tar.gz SHA-256 | 本次实际交付文件 | 通过；`37372c9cfc78632c9872f76c663a6ff90ae5bdb8fbe393a23fc94a356f4f7b1a`，与交付校验清单一致 |
| 完整解压、启动链接和图标检查 | 当前用户目录 | 通过；启动文件有执行权限，图标存在，完整 Java/runtime/lib 一并安装 |
| desktop-file-validate | 当前用户桌面文件 | 通过，无错误 |
| update-desktop-database、kbuildsycoca6 --noincremental | 当前 KDE 会话 | 通过 |
| gtk-launch resonance.desktop + /proc/exe 与窗口管理器核对 | 实际安装位置 | 通过；执行文件来自用户安装目录，Resonance 原生窗口 IsViewable，进程保持运行；结果保存在 startup.json |
| Kotlin/音频/跨平台回归、重新打包 | 不涉及产品源码或构建输入变化 | 本次未执行；不把前一轮的 300 项通过当作本次重新运行 |
| 系统 DEB 安装／卸载 | 需要管理员认证 | 未执行，当前采用用户目录安装 |

## 限制与后续

当前用户安装不进入 dpkg/apt 数据库；更新可换用新版本完整目录与 current 链接。卸载用户版只移除本次安装的程序目录、启动链接与 desktop 文件，保留 XDG 数据目录和音频；本次没有执行卸载。窗口已保持打开，用户可从当前欢迎页开始使用。

## 同步更新的文档

更新 [文档索引](../README.md)、[Linux 使用说明](../LINUX.md) 及 [开发说明 Linux 章节](../DEVELOPMENT.md#1221-linux-桌面版2026-11-03-核对)，区分当前用户安装与系统 DEB 安装；不修改协作规则。
