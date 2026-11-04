# 2026-10-31 — 构建当前陪伴房间 Release，保存独立产物并核对真实验收状态

- 修改日期：2026-10-31（Asia/Shanghai）
- 类型：构建 / 验证 / 交付记录（尚未完成传输）
- 适用版本：0.6.5 / 39，按维护者要求不升版
- 影响平台：Android、Windows
- 关联：[原生验收排查](2026-10-31-08-isolated-native-validation-and-release-build.md)、[启动监听](2026-10-31-10-defer-unpaired-invitation-listener.md)、[Android 音频控制器修复](2026-10-31-11-defer-unavailable-audio-commands-and-retire-old-controller.md)

## 需求或问题

维护者要求代码完成后先交付安装包，宣传片随后补充。前次中间产物未包含后续原生验收发现的音频控制器修复，必须重新构建，避免把旧包或旧哈希当作本次结果。

## 原因与依据

本轮保持与其他 AI 工作隔离的构建输出及 `.codex` 设备测试包。正式 Release ID 不变；构建不自动提交、公开发布或覆盖历史正式版本下载。所有校验值从本次实际 APK/EXE 计算。

## 具体修改

| 文件 / 位置 | 关键符号 | 修改与目的 |
| --- | --- | --- |
| 本机忽略的 `night/final-release-5.log` | 最终产品构建 | 以 JDK17、2GB 堆、两 worker 顺序完成针对性桌面回归、Release 单测/Lint、APK、Windows app image/EXE |
| 本机忽略的 `output/delivery-2026-10-31-r5/` | 独立交付目录 | 保存本次 APK、EXE、MIT LICENSE、SHA256SUMS，后续拍摄编译不替换这份安装包 |
| 本记录及现行文档 | 真实状态 | 分开记录通过、仍在运行、尚未交付；保留 10 月 29 日公开版本历史哈希 |

## 本次实际产物

| 文件 | 字节数 | SHA-256 |
| --- | --- | --- |
| `Resonance-0.6.5-release.apk` | 167186107 | `d4fb101ab346bb9e8cae327aa3f5d25a71e45b6ceb9898989cb3c556c08b6d6f` |
| `Resonance-0.6.5.exe` | 322155008 | `95670f44c9e75917e4f38adc5475fa022ce186d8d4ece8afc6352d28fa721912` |

APK 本次 `apksigner verify --verbose` 通过，v2 签名、1 位签名者。Windows EXE 的 Authenticode 状态为 **NotSigned**，不能称为已签名 Windows 安装包。

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 音频会话授权变化 | 中间 APK 可能因增益命令不可用退出 | 本次正式 APK 包含有限等待与旧控制器失效保护 |
| 无伙伴启动/普通加入 | 部分中间包提前监听伙伴端口 | 本次包只在已有伙伴启动或明确配对时准备伙伴监听 |
| 获取本轮安装包 | 共享输出可能被后续编译替换 | 独立目录保存本次实际文件及校验清单 |

## 平台与数据影响

原有曲库/歌单、转换与元数据流程保持原实现；新增的聊天/伙伴与可选寻址数据边界见各独立记录。普通构建不读取/打印真实签名密钥或模型凭据。没有升版、公开部署或推送 Git；同版本本地重建与此前公开版本必须按具体文件哈希区分。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `desktopTest --tests together.* --tests MusicAiWireTest --tests MusicAssistantTest` | 本次独立 v31 输出 | 70 项：66 通过、4 项显式 opt-in 跳过，0 失败 |
| `testReleaseUnitTest` | Android Release | 48 项通过，0 失败/跳过 |
| `lintRelease` | 本次 APK 源码 | 通过；报告 109 条 Warning，无 Error，未把历史警告当作本轮新修复需求 |
| `assembleRelease createReleaseDistributable packageReleaseExe` | JDK17 / Windows | 通过，12m36s，85 tasks；正式 APK 与 EXE 已保存 |
| `Get-FileHash`、`apksigner verify`、`Get-AuthenticodeSignature` | 独立交付目录实际文件 | 如上：APK 验签通过，EXE 未签名，哈希重新计算 |
| 聊天/配对/直邀核心测试 | MuMu `.codex`，`native-isolated-20261031-064404/core-mumu.log` | 7 项通过，6.853s |
| 原音频会话集成与新增生命周期回归 | 同包 `audio-lifecycle-mumu.log` | 5 项通过，5.520s |
| MuMu 原生主机 → Windows PCM 跟听 | 同次 reverse 两端日志 | Windows/Android 各 1 项通过，覆盖后台、暂停、恢复、seek |
| Windows PCM → MuMu 原生跟听、聊天与本机暂停 | 同次 two-hour 日志 | 通过，Windows 7224.409s、Android 7225.488s；持久聊天核对 12 条发出均已 ACK、2 条收到 |
| 本次正式 APK 安装/启动 | 长时验收结束后，ADB 与 MuMu | 安装成功、启动成功、15 秒后进程仍在；启动前已有进程，不称为冷启动 |
| 本次 Windows 启动、微信文件传输助手发送 | 用户已授权的本机环境 | 维护者已手动处理提示；R5 同次 app image 冷启动并进入曲库通过，07:29 四份文件上传结束，见[交付核对](2026-10-31-14-windows-startup-and-wechat-transfer.md)；未向其他联系人发送 |

## 限制与后续

软件包已构建保存并向自己的文件传输助手上传结束，Windows 同次 app image 启动、两小时原生及正式 Android 包非冷启动已验证。没有实测真实电话、蓝牙、手机流量跨网或任意异地 NAT 可达。新版宣传摄影机的模型配置在输入校验阶段失败，尚未发出模型请求或完成影片，须修复并重新验收。维护者随后新增的跨网、多人平权、卡顿与提醒需求另见[新需求](2026-10-31-15-multiplayer-equal-control-and-cross-network-plan.md)；目前已发的 R5 软件不包含这些新改动。

## 同步更新的文档

`docs/README.md` 索引与当前状态；`docs/DEVELOPMENT.md` 开发/验收边界；历史构建记录增加本次结果入口。`docs/RELEASE_NOTES-0.6.5.md` 中的 10 月 29 日公开产物哈希保留为历史事实，本次同版本本地重建以本记录和独立交付目录为准。
