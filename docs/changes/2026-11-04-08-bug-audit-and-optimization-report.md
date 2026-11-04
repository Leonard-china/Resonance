# 2026-11-04 — 排查播放器与官网缺陷并交付可复核优化报告

- 修改日期：2026-11-04（Asia/Shanghai）
- 类型：文档 / 审计
- 适用版本：`0.6.5 / 39`，未改变版本号
- 影响平台：共享、Android、Windows、Linux；独立官网/中继/寻址服务
- 关联需求：用户要求“全面排查这个软件的 bug 和可优化的点，给我报告”

## 需求或问题

需要基于当前工作区核对现存问题，提供优先级、具体触发、代码证据、验证结果与优化顺序；不能将历史质量报告或测试通过直接等同于本次无缺陷。

## 原因与依据

当前自动测试覆盖大量正常路径，但导入提交顺序、临时离线曲库、部分设置备份、AI 删除授权、版本去重和批量 UI 接线仍有缺口。本次通过代码调用链和隔离实验定位 13 项问题（6 P1、7 P2），另列 9 项优化入口。细节、证据强度和未验证边界见完整报告。

## 具体修改

| 文件 | 关键内容 | 修改与目的 |
| --- | --- | --- |
| [完整报告](../audit/2026-11-04/BUG-AND-OPTIMIZATION-REPORT.md) | B01–B13、O01–O09 | 记录发现、复现、影响、建议和回归验收；本次没有实施产品修复 |
| [验证台账](../audit/2026-11-04/verification.json) | 77 suite、Lint、Python、补测及跳过项 | 保存本次机器计数，避免复用旧报告 |
| [源码摘要](../audit/2026-11-04/source-sha256.json) | 39 个关键源码 SHA-256 | 绑定当前脏工作区的重点证据 |
| [JVM 探针](../audit/2026-11-04/AuditProbe.java)、[导航探针](../audit/2026-11-04/navigation-probe.cjs) | 当前类/实际导航函数 | 只使用合成输入与临时目录；结果保存在同目录 JSON |
| `AuditJUnitRunner.java`、`classpath.init.gradle` | 隔离 JUnit 监听器/运行时 classpath | 可重跑本地原生音频和协议联测；工具路径仅输出到忽略目录 |
| [开发说明](../DEVELOPMENT.md) | 能力表、审计提示 | 勘误音频特征提取/BPM 搜索接入程度，链接本次已知缺陷 |
| [文档索引](../README.md) | 报告入口/变更记录 | 增加本次交付入口 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 追踪当前未修复缺陷 | 历史报告、功能描述与当前源码混杂 | 独立报告按证据/优先级列明问题与验收建议 |
| 查阅 BPM/音频特征能力 | 能力表描述为已接入 | 明确算法/缓存未接生产调用，BPM 仅展示不筛选 |
| 应用实际运行 | 当前未提交实现 | 未修改业务源码，13 项问题仍待修复 |

## 平台与数据影响

没有修改播放代次、转换、持久化、删除、标签写回、同步包/协议、配置、权限、网络默认值或版本。测试使用临时数据库、生成音频和回环服务；未读取真实密钥、用户音频或线上资料。没有安装、打包、发布、提交或部署。保留工作区原有 372 条 Git 状态记录中的用户/其他 AI 改动。

## 验证

本机 Linux；使用已有本地 JDK17/Android SDK36，Python 网站依赖另建忽略的 venv。以下为本次执行；原始日志 `build/audit-2026-11-04/`，计数见机器台账。

```bash
# 通过环境提供实际 JAVA_HOME/ANDROID_HOME，不把本机路径提交。
RESONANCE_FFMPEG=/usr/bin/ffmpeg "$JAVA_HOME/bin/java" \
  -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain \
  :composeApp:desktopTest :composeApp:lintRelease --rerun-tasks --no-daemon --max-workers=2 \
  '-Dorg.gradle.jvmargs=-Xmx3g -Dfile.encoding=UTF-8' \
  -Pkotlin.compiler.execution.strategy=in-process --no-configuration-cache --console=plain

build/audit-2026-11-04/venv/bin/python -m unittest discover -s website/tests -v
python3 -m unittest discover -s services/relay -p 'test_*.py' -v
python3 -m unittest discover -s services/rendezvous -p 'test_*.py' -v
python3 -m unittest discover -s services/deploy/aliyun -p 'test_*.py' -v
```

| 验证 | 结果 |
| --- | --- |
| 桌面全量 + Release Lint，强制重跑 | 通过；8m56s；331 项中 322 通过、9 跳过、0 失败/错误；Lint 0 Error / 107 Warning |
| 启用 `RESONANCE_DESKTOP_AUDIO_TEST=1`、`RESONANCE_RELAY_TEST_PYTHON`、`RESONANCE_RENDEZVOUS_PYTHON` 后，由 `AuditJUnitRunner` 运行三类测试 | 8 项中 7 通过、1 生产地址验收跳过；Linux 真实静音音频/FFmpeg、回环 WSS 内层 TLS/寻址通过；与全量有重叠 |
| 网站 / 中继 / 寻址 / 部署工具 Python 回归 | 分别 42 / 11 / 7 / 2 项通过；无真实邮件/远程压测 |
| JVM 合成数据与实际 JS 导航函数 | 成功输出复现证据；不作为产品修复通过 |
| 两张本次原生离屏截图人工检查 | 查看短横屏 1.5 字号设置、390 宽智能管家；不代表真机 UI/帧率通过 |
| 审计报告、JSON、链接与文档差异 | 通过；13 项编号/9 项优化、JSON 解析、新增文档链接、39 份源码摘要一致；本次文档路径的 CRLF 兼容 diff 检查通过 |
| Android 设备操作、Windows 原生/安装器、跨网多人与长时、公网生产、真实第三方 API、SMTP、发布构建 | 未执行；不复用历史产物或上线结果 |

网站旧 venv 缺 Linux 可执行文件，先建立审计专用 venv 后成功运行。临时 JVM 探针初版误用 Java `Map.of()` 查询 null，修正为真实公开导入入口后成功；该夹具失败不计产品缺陷。原始 Gradle 任务退出码 0。

## 限制与后续

本次按报告需求执行，报告中的修复建议尚未实施。后续按报告顺序逐项修复并新增独立变更记录。手机在线只证明连接可用，本次没有操作用户日常应用。线上容量/独立网络、Windows 输出、实际 AI/歌源及 Android 生命周期仍需专门验收。

## 同步更新的文档

更新 `docs/README.md` 的报告入口和变更索引、`docs/DEVELOPMENT.md` 对应能力表与核对提示；保留历史报告和协作规则。
