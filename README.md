# Resonance

Android / Windows 原生音乐播放器：本地曲库、歌单、自动音量平衡、双语与桌面歌词、B 站音频导入及多模型音乐助手。

开发入口：[文档索引](docs/README.md) · [详细开发说明](docs/DEVELOPMENT.md) · [AI 协作规则](AGENTS.md)。当前版本 **v0.4.1 / versionCode 24**，最新发布包见 [v0.4.1 Release](https://github.com/Leonard-china/Resonance/releases/tag/v0.4.1) 与 [版本说明](docs/RELEASE_NOTES-0.4.1.md)。

## 功能

| 功能 | 两端行为 |
| --- | --- |
| 本地曲库 | 标签/封面、歌单/收藏、专辑/歌手、本地搜索、多选 |
| 播放 | 后台播放、队列、循环、随机、变速、暂停恢复 |
| 音量平衡 | 一个开关，自动分析/缓存；独立说明页 |
| 歌词 | 本地 LRC/KRC、LRCLIB、按需 AI、任意目标语言翻译与双语显示 |
| 悬浮歌词 | Windows 置顶、Android 系统悬浮窗，拖动/字号/锁定点击穿透 |
| B 站音频 | BV/链接/分 P，自定义歌名、歌手、专辑、封面后下载入库 |
| 音乐助手 | 千问、DeepSeek、豆包、自定义接口；文本/视觉模型分开选择 |
| 截图导歌单 | 长图分片 OCR、本地优先匹配、搜索原版候选、下载/整理 |
| 推荐 | 间隔/数量/后台可选，自动下载、点赞保留、拒绝删除及可选原因 |
| 转换 | KGMA v3/slot1 两端；KGG v5 仅 Windows，需本机酷狗密钥 |
| 歌单文件 | 导入/导出 .resonance，携带音频、封面、信息与歌单 |
| 智能补全 | 公共来源优先、可选 AI，取消保留已完成结果 |

设备/LAN/二维码同步和酷狗歌单链接导入已移除；已有音乐与歌单保留。酷狗元数据补全、Windows KGG 密钥读取不受此移除影响。

## 使用

1. 打开“导入”，扫描系统音频或选择文件夹。转换页显示阶段、文件和进度，可取消，源文件保留。Android 不支持 KGG，请在 Windows 转换后通过歌单文件导入。
2. 播放器/设置开启“统一播放音量”；详细操作在单独的“音量平衡使用说明”页面。系统音量和播放器主音量仍由你控制。
3. 在设置配置模型地址、模型名、API Key，保存并测试连接；分别选择“用于音乐管理”和“用于截图识别”。DeepSeek 文本可搭配千问/豆包视觉；豆包需填自己的实际模型/接入点 ID。
4. 主导航“音乐助手”可以聊天管理曲库，或发送歌单长截图。截图发送后直接匹配/下载并整理，不需要逐首确认。
5. “从 B 站导入音频”先读取视频信息，再改短歌名、歌手、专辑、封面和可选歌单，最后下载。平台要求登录/验证时，可在设置填自己的 Cookie；访问受限会明确报错。
6. 歌单页第一张固定卡为“每日推荐”；未开启时点击进入设置。每日推荐默认关闭。开启后设间隔和数量，可仅打开应用检查，也可允许后台；Windows 需在托盘运行，Android 受系统调度影响。
7. 推荐曲目收藏或加入自建歌单后保留；只试听未保存的，下批成功生成时自动删除。**不喜欢会立即删除音频、取消收藏并移出所有歌单，即使之前已保存**；原因可跳过。
8. 歌词页按需翻译，设置目标可用中文、English 等，支持原文/译文/双语。翻译缓存复用，不改原时间轴。设置或播放菜单开启桌面歌词；Android 需授予“显示在其他应用上层”权限。锁定后在设置解锁。
9. 歌词页或播放菜单可“生成同步歌词”：配置百炼语音Key（或复用千问Key），确认后上传音频并按时长计费；设置可限制每月分钟数，结果保存复用。原文与纯文本歌词保留。
10. 音乐库右上角或助手页可打开任务列表，查看下载/封面/歌词阶段并单独重试。已下载B站歌曲可批量匹配专辑封面，未找到时保留视频封面；播放菜单支持自定义封面。
11. 歌单导入/导出是普通明文 ZIP 文件，带可打开音频/封面/元数据，不带 API Key、Cookie 和播放偏好。

## AI 用量与数据

聊天使用稳定排序的曲库摘要、固定工具前缀及短历史；OCR/译文落盘缓存。设置可设月 Token 上限，助手显示输入、缓存命中和输出计数；服务商缓存期限和真实账单以平台为准。

选中的截图、歌词、曲目/歌单文本与聊天会按功能发给用户选中的模型，聊天/截图功能不上传整库音频；主动生成同步歌词会上传当前整首音频并独立按时长计费。新凭据在 Windows 用 DPAPI，Android 用 KeyStore AES-GCM；旧 DeepSeek 配置兼容保留，可能仍有历史明文。详见 [第三方及数据说明](docs/THIRD_PARTY.md)。

音乐助手通过实际音乐工具执行，不开放任意命令行/其他软件控制。删除音频工具需要本轮明确删除指令；歌单删除只删关系。不喜欢和临时推荐清理由专门流程实现。

## 开发与构建

需要 JDK17（脚本支持17–21）、Android SDK36 / Build Tools36.0.0、Gradle Wrapper。Windows 播放/转换/打包需 ffmpeg.exe，可用 RESONANCE_FFMPEG 指定。真实签名配置仅保存在本机，模板为 keystore.properties.example。

```powershell
.\gradlew.bat :composeApp:allTests :composeApp:lintRelease :composeApp:assembleDebug
.\gradlew.bat :composeApp:connectedDebugAndroidTest
.\gradlew.bat :composeApp:run
# 正式发布按开发说明准备签名和版本，再构建：
.\gradlew.bat :composeApp:assembleRelease :composeApp:createReleaseDistributable :composeApp:packageReleaseExe
```

Windows 调试必须同时设置 RESONANCE_DATA、RESONANCE_LIBRARY 到独立目录；不能用用户音乐库测删除。应用目录依赖完整 app/runtime/resources，单独复制 EXE 无法启动。

普通构建不升版，version.properties 是唯一来源；显式发布另写版本说明、验签/哈希。功能验证见 [深度优化回归](docs/changes/2026-10-15-12-verification-and-handoff.md)，历史签名与发布见 [v0.4.0 发布记录](docs/changes/2026-10-15-13-release-0.4.0-android.md)；每次任务需新增 [变更记录](docs/README.md)。

## 当前限制

B 站接口可能要求登录/验证，当前开发网络探测返回 HTTP412，未验通实际账号在线下载；未提供用户 Key，收费模型服务尚未实测。模拟回归不能代替真实服务验证。Android KGG 不支持，后台调度不保证精确执行时间；纯文本歌词不伪造时间轴。

本仓库尚无项目级 LICENSE；第三方按各自许可核对，Windows FFmpeg 以实际二进制为准。[组件与服务](docs/THIRD_PARTY.md)。
