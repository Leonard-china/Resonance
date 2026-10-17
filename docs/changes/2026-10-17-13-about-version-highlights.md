# 2026-10-17 — 关于页展示当前版本发布日期和主要更新

- 修改日期：2026-10-17（Asia/Shanghai）
- 类型：功能 / 构建配置
- 适用版本：0.4.6 / 29 工作区，未升版、未发布
- 影响平台：共享 / Android / Windows
- 关联需求：关于Resonance除版本号外还应展示当前版本更新功能。

## 需求或问题

原关于页面有APP_VERSION、应用介绍与链接，但用户无法在应用内直接了解这个版本更新了哪些功能。

## 原因与依据

版本号由version.properties生成，发布说明已有docs/RELEASE_NOTES-<版本>.md；应使用同一版本对应文档作为更新内容来源，避免手写另一份版本或误显示未发布工作区内容。

## 具体修改

| 文件 | 关键符号 | 具体修改与目的 |
| --- | --- | --- |
| [build.gradle.kts](../../composeApp/build.gradle.kts) | generateAppVersion | 以version.properties选择RELEASE_NOTES，提取标题日期与“主要更新”的一级列表；生成APP_RELEASE_DATE及APP_RELEASE_HIGHLIGHTS，声明文档为任务输入 |
| [ToolsScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/ToolsScreens.kt) | AboutScreen | 应用信息下新增当前版本更新、日期、分条说明与该版本完整发布链接；无对应说明时明确提示 |
| [LibraryNavigationTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/LibraryNavigationTest.kt) | aboutShowsHighlightsForInstalledVersion | 从工具进入关于，检查当前版本与生成的首条更新可见 |

生成器对反斜杠、引号、美元符号与换行转义；没有手写生成BuildVersion.kt，没有改版本号。缺少文件或“主要更新”章节时输出空更新列表，不借用其他版本。仅内置摘要，更新说明可离线读取；完整发布按钮交系统浏览器。

## 修改前后行为

| 场景 | 修改前 | 修改后（待构建/运行验证） |
| --- | --- | --- |
| 打开关于 | 版本号和通用介绍 | 版本号下有对应发布日期和主要更新列表 |
| 后续版本构建 | 关于无更新内容来源 | 读取对应版本发布文档生成，不需手工同步UI文本 |
| 找不到对应说明 | 无模块 | 显示“当前版本暂无内置更新说明” |

## 平台与数据影响

共享原生页面，两端编译都依赖generateAppVersion；构建输入新增对应发布文档。无新依赖、权限、持久键、自动网络或用户数据修改。当前0.4.6生成的是其已发布的本地OCR/完整歌名等5项更新，本轮新增尚未发布功能需在实际升版发布文档中维护。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| 静态读取0.4.6发布文档标题及“主要更新”列表 | Python / Windows | 通过，章节中5项更新；这不是运行Gradle生成器 |
| git -c core.safecrlf=false diff --check -- composeApp docs | Windows | 通过 |
| generateAppVersion、双端编译、关于页设备测试 | Gradle / Android / Windows | 未执行，按用户要求不构建，交Antigravity |

## 限制与后续

当前提取约定是“## 主要更新”下的单行“- ”列表，多行续写/复杂Markdown不作完整富文本渲染。发布时保持此结构并核对关于页；生成器尚未实际运行，需验证增量构建、文档缺失、特殊字符和两端展示。详见 [交接](../HANDOFF-2026-10-17.md)。

## 同步更新的文档

docs/README.md、docs/DEVELOPMENT.md、docs/HANDOFF-2026-10-17.md。
