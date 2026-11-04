# 2026-10-31 — 宣传拍摄改用真实 AI 助手、加密聊天和双向配对

- 修改日期：2026-10-31（Asia/Shanghai）
- 类型：宣传拍摄与测试夹具
- 适用版本：0.6.5 / 39，不升版
- 影响平台：Android 独立测试包、Windows 测试主机及离线剪辑
- 关联：[三项特色宣传稿](2026-10-30-03-source-ai-and-real-together-promo.md)、[MIT 许可](2026-10-30-06-mit-license.md)

## 需求或问题

维护者认为前版没有突出开源、AI 助手、端到端加密一起听。此前新版夹具只能显示助手入口和输入，没有真实回答；陪伴房间后来增加了聊天和常听伙伴，宣传内容也需要跟进。

## 原因与依据

原镜头用空状态代理展示助手，不能把它宣传成真实模型演示。窗口目前受 Windows 系统安全提示遮挡，已交由用户决定；独立 MuMu 测试包可直接调用同一生产助手服务，无需操作或绕过系统安全提示。

## 具体修改

| 文件 | 关键符号 | 修改 |
| --- | --- | --- |
| [PromoAiSetupDeviceTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/PromoAiSetupDeviceTest.kt) | 显式 opt-in 设置夹具 | 仅 `.codex` 包允许；随机名 Android 本地套接字只接受 ADB UID，一次读取有限长度凭据，保存走生产 Android Keystore 加密，不把密钥放命令参数/文件/日志 |
| [PromoFeaturesCaptureDeviceTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/PromoFeaturesCaptureDeviceTest.kt) | 真实助手与房间摄影机 | 改用 `AndroidMusicAccess`、实际发送按钮和真实 DeepSeek 回答，保存不含凭据的原始问答证据；拍摄真实 TLS 房间、送达聊天和双方同意配对，不代理成功状态 |
| [TogetherNativeBridgeTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherNativeBridgeTest.kt) | `RESONANCE_PROMO_SCENE` | 显式拍摄模式使用原创陪伴对话，测试主机同意拍摄设备配对；普通稳定性测试原行为保留 |
| [capture.py](../../scripts/promo/capture.py) | 私有目录/录像进程 | 支持独立包，特性拍摄强制 `.codex`；从应用私有目录读取问答与标记，只停止本次明确拥有的录像 PID，移除原先全局 pkill |
| [produce_features.py](../../scripts/promo/produce_features.py) | 三项主线与镜头选择 | MIT 主标题；从真实输入、回答、连接、配对、聊天标记剪出片段；缺真实回答证据则拒绝制作正式特性片 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| AI 特性镜头 | 空会话/输入，未回答 | 必须有真实模型回答并保存原文依据 |
| 朋友镜头 | 原生同步播放 | 同步、加密聊天、送达、同意配对 |
| 开源文案 | 许可待确认预览稿 | 维护者已选 MIT，正式文案突出 MIT 开源 |
| 结束录像 | 停止全部 screenrecord | 验证并停止本次拥有的进程 |

## 平台与数据影响

正式 APK/EXE 没有新增调试入口；套接字及摄影机仅 androidInstrumentedTest，拍摄宿主仅 androidDebug。配置与原创曲库只进入独立 `.codex` 应用；模型请求使用维护者明确授权的凭据与原创文本，凭据由生产 Keystore 保护。没有公开服务部署或新增媒体中继，没有读取用户歌曲。AI 套接字具体名字每次随机，凭据不放入自动化、源文件或输出证据。

## 验证

Python `py_compile` 通过；`--prepare-only --license MIT` 通过，横版源码、竖版 AI/一起听版式已逐张检查。独立测试 APK 编译 `private-promo-6.log` 通过（1m，69 tasks）；桌面摄影机类与运行时导出 `promo-runtime-6.log` 通过（39s，15 tasks）。真实模型请求、原生拍摄与横竖成片验收待补。后台串行流程已排队，只有当前双小时原生验收成功结束才安装摄影机，失败即停；尚未请求模型。此记录不代表片已拍完或已传输。

09:48 更新：两小时原生验收成功后，正式 Android APK 安装/启动通过（此前进程已存在），摄影流程在 `PromoAiSetupDeviceTest` 的输入校验处失败并停止；模型请求与影片均未完成。异常日志未确定具体传输原因，不能据此断言账户凭据无效。不得读取或打印真实凭据排查。

## 限制与后续

先完成软件验收/交付，再拍摄、检查字幕排版和原始回答、全片解码与声轨、生成本次文件哈希。真实异地 NAT 成功率、电话/蓝牙等尚未实测，不在片中承诺任意网络可达。

## 同步更新的文档

`docs/README.md` 索引、`scripts/promo/README.md` 新版操作说明。
