# 2026-11-01 — 统一明暗银灰主题并简化一起听首页

- 修改日期：2026-11-01（Asia/Shanghai）
- 类型：功能 / Bug 修复
- 适用版本：0.6.5（39），未升版
- 影响平台：共享 / Android / Windows；中继另为独立 Python 服务
- 关联：[产品审计](../PRODUCT_AUDIT.md)、[本轮结果](../OPTIMIZATION_REPORT.md)
- 路径缩写：C = composeApp/src/commonMain/kotlin/com/resonance/player；J = 对应 jvmMain。

## 需求或问题

用户明确要求不能只有深色主题、一起听 UI 简化。原页面多处重复解释、连接设置与高级选项抢占主操作；默认色调和排版层级不一致。

## 原因与具体修改

- `C/design/ResonanceTheme.kt`：暗色石墨背景/银灰强调、浅色雾白/深灰强调；保留系统主题和 IceBlue 选项，修正 display 字号顺序，沿用现有圆角、阴影、间距和 Material 图标体系。
- `C/ui/TogetherScreen.kt`：主区保留创建/加入；加入审批、预读、端口合并在房间选项；说明缩短；伙伴/连接面板移到主操作后。
- `C/ui/TogetherCompanionPanel.kt`：连接设置默认折叠；展示默认服务，支持恢复默认、自定义与关闭；WSS 不要求密钥，私有 TLS 保留密钥输入。
- `UiVisualContractTest`：用明确标记的固定夹具渲染真实 Compose/Skia；覆盖浅/深、360/640/1280 宽和 1.5 倍字体，默认服务夹具不发网络请求。

## 修改前后与平台、数据影响

不迁移框架、不移除功能、不清除原主题偏好。动画沿用已有前后台及减少动画开关，没有引入新的持续动画或大依赖。截图使用已有 PreviewLibrary 与原创歌词夹具，不代表真实用户库。可访问性含文字对比度和原生操作语义；真机与所有系统缩放组合仍需验收。

## 验证

完整 `:composeApp:desktopTest` 中 `UiVisualContractTest` 16/16 通过；生成 70 张页面和 2 张错误截图，实际查看重点明暗主题及窄屏大字；完整页面/测试夹具证据见优化报告，未做真实 Android 设备验收。

最终共同命令：`./gradlew.bat :composeApp:desktopTest :composeApp:lintRelease :composeApp:assembleDebug --offline --no-daemon --no-configuration-cache --max-workers=2 --console=plain`，成功；桌面 276 通过 / 4 条件跳过 / 0 失败，Android Lint 0 errors / 110 warnings。

本次详细命令、数量、截图与限制记录在 [优化报告验证台账](../OPTIMIZATION_REPORT.md#验证台账)，不使用旧产物充当本轮通过。中间编译失败与修正也在台账保留。

## 限制与后续

上述未验收项保持待办；没有修改生产数据、签名配置、版本号或发布产物。检查点为 `.git/audit-checkpoints/20261101-203513`，只按本轮变更文件回退，不能丢弃此前脏工作区。

## 同步更新的文档

`docs/README.md` 索引、`docs/DEVELOPMENT.md` 当前状态、`docs/OPTIMIZATION_REPORT.md`；服务变更另更新 relay README 与部署说明。
