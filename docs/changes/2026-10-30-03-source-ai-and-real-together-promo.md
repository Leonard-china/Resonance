# 2026-10-30 — 重排源码、AI 助手与真实加密一起听的宣传片主线

- 修改日期：2026-10-30（Asia/Shanghai）
- 类型：宣传素材 / 拍摄与剪辑工具；仍在制作
- 适用版本：version.properties 的 `0.6.5 / 39`，未升版
- 影响平台：Android Debug 测试宿主、Windows 本地制作脚本
- 关联：[产品定位规划](2026-10-30-01-product-positioning-and-listen-together-plan.md)、[原生一起听实现](2026-10-30-02-listen-together-session-and-native-playback.md)、[前版 40 秒影片](2026-10-29-03-native-mumu-promo-film.md)

## 需求或问题

2026-10-31 续作状态：维护者已选择 MIT，最新摄影机已改为真实助手回答、加密聊天与双向配对，见[最新独立记录](2026-10-31-12-real-ai-and-companion-promo-camera.md)。下文未配置/许可待定是 10 月 30 日初稿事实，不能当作当前产品限制；新版尚待真实录制与成片验收。

用户指出原片主要展示播放器外观，没有突出开源、AI 助手和端到端加密一起听，需要重拍并保留真实功能证据。

## 原因与依据

原片是测试驱动的播放器界面与独立原创配乐，没有真实双端音频、AI 或源码镜头。GitHub 仓库公开，但项目没有 LICENSE；尚未获得维护者选择，不能直接替用户授予开源许可。2026-10-30 通过正式应用服务检查 Windows 与 MuMu 配置，两者均未配置可用 AI，未发出模型请求，不能虚构成功回答。

## 具体修改

| 文件 | 关键符号 | 修改与目的 |
| --- | --- | --- |
| [PromoFeaturesCaptureDeviceTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/PromoFeaturesCaptureDeviceTest.kt) | recordAssistantEntryAndRealEncryptedRoom | opt-in 稳定宿主；拍生产 AssistantScreen 的欢迎与需求输入，随后通过真实 Windows 主机连接、原生 MediaSessionService 播放，拍 TogetherScreen 和 NowPlayingOverlay；不伪造 AI 回答 |
| [PromoAssistantProbeTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/together/PromoAssistantProbeTest.kt)、[TogetherNativePlaybackDeviceTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/TogetherNativePlaybackDeviceTest.kt) | AI 可用性探测 | 显式 opt-in，只返回是否配置的布尔值；Windows 仅在已配置时请求原创演示文本。不导出凭据，不读取用户歌曲用于宣传 |
| [capture.py](../../scripts/promo/capture.py) | --scene features | 保留 player 默认场景；新增 features 原片与单调时钟标记；实录数据声明含 ai_live_reply=false |
| [produce_features.py](../../scripts/promo/produce_features.py) | plate / render / main | 50 秒横竖稿：品牌 3s、源码 9.5s、助手 11.5s、一起听 15.5s、歌词 6.5s、落版 6.5s，五次各 0.5s 转场；仓库真实代码节选、原生录像、独立原创配乐 |
| [produce.py](../../scripts/promo/produce.py) | soundtrack | 小节数量按时长计算，默认 40 秒行为相同，新片 50 秒不出现末尾十秒缺音乐 |
| [verify.py](../../scripts/promo/verify.py) | --duration / --suffix | 默认旧片验收保持 40s；可验收 features-50s，仍全片解码/核对规格/联系表/新哈希 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 产品定位 | 外观、歌词、队列 | 源码可见、助手入口、真实加密一起听为主线，歌词为短补充 |
| AI 镜头 | 无 | 真实原生助手界面和需求输入；当前无模型配置，字幕说明需配置后使用，不展示伪造回答 |
| 一起听镜头 | 无真实双端 | 拍摄夹具读取 opt-in 临时邀请，用真实 Windows 原生音频发送给 MuMu 原生播放器 |
| 开源文案 | 规划中 | 暂写“源码公开”和“预览稿，项目开源许可待确认”；许可证获确认后才能更新正式开源文案 |

## 平台与数据影响

全部拍摄宿主/夹具只进 Debug/androidTest；正式播放器仍是 Kotlin/Compose。素材仅用原创音频/文本及仓库代码节选，不采集私人曲库。AI 入口镜头用生产组件的空会话状态，未按发送、不冒充模型调用；原生房间与音频来自真实服务。独立配乐不等同于录下扬声器声音。

默认不公开发布或替换官网。媒体输出忽略目录 `output/promo-features-2026-10-30/`；前版 `output/promo/` 保留，歌词镜头复用前版明确标注的原创 UI 演示。

## 验证

| 命令/操作 | 环境 | 结果 |
| --- | --- | --- |
| GitHub 仓库页与本地 LICENSE 核对 | 官方仓库 / 本地 Git | 仓库公开；无项目级许可证，不等于已获得任意开源许可 |
| Windows / MuMu AI 配置布尔探测 | 实际应用服务 | 通过，均 configured=false；没有模型请求或真实回答素材 |
| python -m py_compile capture.py produce.py produce_features.py verify.py | Python | 通过 |
| produce_features.py --prepare-only | Pillow/NumPy | 通过，生成 50 秒原创配乐与六张版式预览 |
| 查看横版源码、竖版一起听版式 | 本地 PNG | 已检查，修正代码长行省略和源码页素材标注；最终合成后仍需逐段检查 |
| 新拍摄夹具编译 / MuMu 录制 / 横竖合成 / 全片解码 / 哈希 | 本轮环境 | 尚未执行；需等待十五分钟后台桥接和 Release 构建完成后运行新测试包 |

## 限制与后续

影片仍未拍摄或交付。项目许可证及 AI 镜头处理已询问维护者；未答复前，保留准确的预览标识与配置说明。没有内置 AI 配置时不伪造回答；若后续用户在应用内配置连接，再补拍真实操作并重新验收。正文中时长是当前剪辑设计，不是成片已完成的证明。

2026-10-30 后续：用户已经提供 DeepSeek 凭据授权真实测试，尚未配置到应用。下一阶段须完成加密设置、真实调用与重拍，不能继续将未配置的入口镜头当作最终 AI 演示；不在文档或脚本中复述凭据。发现正式 App 在 MuMu 输入配置重建后退回首页，已新增[导航保存修复](2026-10-30-04-preserve-navigation-across-input-configuration.md)，新包待验证。

## 同步更新的文档

`docs/README.md` 索引、`scripts/promo/README.md` 拍摄命令与边界。最终成片需要补齐本记录的真实验证结果与哈希。
