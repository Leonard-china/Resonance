# 2026-10-31 — 官方 DeepSeek Flash 短任务显式请求最终回答

- 修改日期：2026-10-31（Asia/Shanghai）
- 类型：接口兼容修复，实际凭据验证尚未完成
- 适用版本：0.6.5 / 39，不升版
- 影响平台：Android、Windows 共用 AI 请求
- 关联：[AI 与一起听宣传计划](2026-10-30-03-source-ai-and-real-together-promo.md)

## 需求或问题

用户要求宣传片展示真实 AI 助手，且一起听的短编号选曲需得到最终内容。核对官方接口发现，当前 `deepseek-flash`/V4 默认开启 high 思考；旧请求只设置 temperature 和较短 max_tokens，不能据此保证会在预算内给出最终 content。这里记录的是有官方依据的兼容风险，不冒充用户凭据下已复现的失败。

## 原因与依据

[DeepSeek 模型说明](https://api-docs.deepseek.com/quick_start/pricing/) 指定当前稳定 Flash 名称及默认思考；[Thinking Mode](https://api-docs.deepseek.com/guides/thinking_mode/) 定义 `thinking.type=disabled`。现有音乐工具按最终正文和 JSON/编号处理，无思考模式设置。只识别名称带 reasoner 不能覆盖默认 Flash。

## 具体修改

| 文件 | 关键符号 | 修改 |
| --- | --- | --- |
| [MusicAiClient.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/ai/MusicAiClient.kt) | `complete` | 只对官方 api.deepseek.com 上明确列出的 Flash/V4 模型发送 thinking disabled；其它兼容接口及显式 reasoner 保持原有语义，不乱加厂商参数 |
| 同文件 | 连接工厂参数 | 支持测试注入 HTTPS 连接替身，不更改生产 TLS、重定向/大小/取消/错误遮蔽策略 |
| [MusicAiWireTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/MusicAiWireTest.kt) | 实际序列化回归 | 检查官方 Flash/V4、reasoner、其它域名兼容接口的线上字段；保留正文、token 统计与禁重定向 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 官方默认 Flash 短任务 | 隐式默认 high 思考 | 显式非思考，直接请求最终内容 |
| 用户选择 reasoner | 最少 1024 输出 token、不传 temperature | 保持原有行为 |
| 同名模型在其它兼容接口 | 通用 OpenAI 格式 | 保持通用格式，不注入 DeepSeek 私有字段 |

## 平台与数据影响

不改变保存的模型、凭据或用户选择，不读取或打印实际 API Key。仅改变符合白名单的正式请求参数；不新增服务、数据上传类别或供应商。API Key 仍由系统凭据保护，测试只使用 fixture 字符串。

## 验证

已新增线上请求序列化测试，最新构建 `output/together-2026-10-30/night/final-release-1.log` 正在运行 AI 测试、Android 单元测试、lint 与双端 Release。结果待补；真实用户模型配置与回答仍待应用内验证，不把合成响应写成真实回答。

## 限制与后续

厂商未来模型别名和行为可能变化，当前仅覆盖 2026-10-31 官方文档列出的模型。需要在应用内配置并验证真实助手后再录制宣传片；许可证已明确 MIT，软件交付不等影片完成。

## 同步更新的文档

`docs/README.md`、`docs/DEVELOPMENT.md` AI 请求章节、`docs/THIRD_PARTY.md`。
