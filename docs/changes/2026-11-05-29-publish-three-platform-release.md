# 2026-11-05 — 构建并发布本轮修复后的Android、Windows和Linux正式包

- 修改日期：2026-11-05（Asia/Shanghai）
- 类型：正式发布 / 构建验证 / 文档
- 适用版本：0.6.6 / 40，来自version.properties；更新未公开草稿，本次不升版
- 影响平台：Android、Windows、Linux x86_64及独立官网下载
- 关联需求：用户要求构建发布版并推送GitHub和官网，明确三个平台都要；[修复验收](../audit/2026-11-05/FIX-VALIDATION.md)

## 需求与原因

当前575项通过的修复仅存在工作区与隔离测试包，正式安装与公开下载尚未包含。本次必须从当前完整源码重建APK、Windows EXE、Linux便携包和DEB，再计算实际校验值，不能复用此前同版本产物。

## 具体修改

| 文件/入口 | 关键符号 | 修改 |
| --- | --- | --- |
| [发布说明](../RELEASE_NOTES-0.6.6.md) | 主要更新 / 验证边界 | 更新为11月5日全部原生界面、歌词、导入与删除安全修复，保留尚未支持BPM等边界 |
| [Windows工作流](../../.github/workflows/release-windows.yml) | source_ref / windows | 使用本次完整源码提交，在原生Windows环境执行回归并生成EXE |
| [Linux构建入口](../../scripts/build-linux.sh) | packageLinuxPortable / packageReleaseDeb | 构建完整运行目录、便携包与DEB，核对系统FFmpeg依赖 |
| [Android构建](../../composeApp/build.gradle.kts) | assembleRelease / signingConfigs | 使用已有正式签名配置，不读取或输出私钥/密码 |
| GitHub与官网受控发布 | draft release / releases.publish | 先校验全部产物，再公开GitHub并事务更新官网；保留论坛、账号、邮件与运行数据 |

## 修改前后行为

| 场景 | 发布前 | 本次结果 |
| --- | --- | --- |
| GitHub | 0.6.5公开，0.6.6草稿 | 公开本次完整三端正式包与真实校验 |
| 官网 | 旧安装包/旧界面预览 | 新版介绍、截图及三端同版本实际下载 |
| 本轮修复 | 仅源码/独立测试包 | 正式发行产物包含这些修复 |

## 平台与数据影响

维持版本0.6.6 / 40、用户数据格式、正式签名及原生平台。发布包不含用户音乐、API Key、私钥、官网.env、作者入口/密码、数据库、上传图片或本机工具。官网更新前做在线数据库与源码/原包备份，不发送订阅邮件。本次不自动覆盖用户电脑或手机上的正式安装。

## 验证

本次应用源码为`1874eb53b7d5fc3fe0ec5e42fb7bd6c655031a80`；第一轮87b0d86产物因CI发现FFmpeg兼容及测试等待问题而没有公开，修正见31/32号记录。全部四份最终产物均对应修正后的源码。

| 命令/操作 | 环境 | 本次结果 |
| --- | --- | --- |
| 完整源码K2编译→`:composeApp:assembleRelease` | 本机受限进程树，实际Release依赖/Compose插件 | Android 194份完整源码通过；AGP精确D8输入共1544个，1539个应用class及全部输入class完整覆盖，4份DEX签名/校验正确 |
| `apksigner verify --print-certs`、`aapt2 dump badging/xmltree`、`zipalign -c -P 16 4` | 本次最终APK | 正式签名与旧版一致，com.resonance.player，0.6.6/40，非debuggable，无调试捕获Activity，16KiB对齐通过 |
| 完整桌面202份源码→`packageLinuxPortable packageReleaseDeb` | 本机Linux/Release ProGuard | 通过；便携包392份文件与当前image哈希一致，DEB匹配391份；唯一JAR差异仅ZIP元数据且所有entry名称/内容哈希一致，只省略jpackage打包元数据 |
| 隔离Xvfb启动当前Release image | 合成数据/独立显示服务 | 持续12秒且原生Resonance窗口IsViewable；未覆盖本机正式安装或连接真实音频 |
| [Windows正式工作流42013729491](https://github.com/Leonard-china/Resonance/actions/runs/42013729491) | windows-2025，源码完整SHA | 成功；435项桌面回归416通过、0失败、19条件跳过，EXE/内置FFmpeg一致性及12秒进程存活通过；MainWindowHandle=0，不宣称可见窗口或实机播放通过 |
| [双系统CI42013729108](https://github.com/Leonard-china/Resonance/actions/runs/42013729108) | Windows与Ubuntu 22.04 | 两项job成功；Windows桌面435项0失败/19跳过，Android Debug/Release单测各77项全通过，Release Lint 0 Error/0 Fatal/96 Warning；Ubuntu桌面435项中420通过、0失败、15条件跳过，实际FFmpeg 4.4回归及打包通过 |
| 官网Python、Node与真实浏览器 | 独立临时库/合成截图 | 45项Python、6项导航测试通过，7组浏览器交互及320/390/768/1440下载页核对通过，详见30号记录 |

构建使用共享锁、单worker、短生命周期Gradle/进程内Kotlin，整个进程树MemoryMax=2GiB、MemoryHigh=1536MiB、MemorySwapMax=0，启动和持续运行保留宿主机1536MiB。采样曾观察到约1.52GiB；结束后systemd已卸载cgroup，未将采样值写成最终全程峰值。准备阶段保留了512MiB堆合并DEX失败、临时ProGuard堆参数格式失败及默认多线程DEB被停止的日志；改为精确独立D8、任务内堆参数和DPKG_DEB_THREADS_MAX=1后完成，不上调进程树总限额或关闭用户应用。

### 本次实际产物

| 文件 | 字节 | SHA-256 |
| --- | ---: | --- |
| `Resonance-0.6.6.exe` | 275987456 | `de02fcf4c4f5a08abf9d2de1a32983bc79aded90a500ba5b9547214e67e47b3b` |
| `Resonance-0.6.6-release.apk` | 167484252 | `a300cbea3321fbdeb5267462d213486da31856a4bb95ed71b42c3c5e25d30a8b` |
| `Resonance-0.6.6-linux-x86_64.tar.gz` | 266896255 | `a13d9900081c90f230d637a60d7f0c95b8d57769c11e8fd9b15b0c34907a34a3` |
| `Resonance-0.6.6-linux-x86_64.deb` | 245892814 | `8d3ef53f7522d43e75e43241075cd6cc90c245b9227e43274cfe0b7b6b9f6e97` |

### 公开发布与官网

- [GitHub v0.6.6](https://github.com/Leonard-china/Resonance/releases/tag/v0.6.6)已公开且为latest，非草稿/预发布。标签指向发布元数据提交`832043067a714c3cfec2e6eba9d494dcea3c1710`，与应用源码1874eb5之间只有文档改动。七个附件包含四份正式产物、SHA256SUMS、SOURCE-COMMIT及本次说明；GitHub官方asset digest与本地七份文件逐一匹配。
- 官网14份白名单源码/静态资源已更新，只向Gunicorn master发送HUP，不重启Java启动器、Tunnel或一起听中继。SQLite在线备份、旧安装包、原源码及会话/作者文件保存在服务器私有备份目录`/home/container/resonance-release-backups/20261105-066-1793877104`；失败首轮备份也保留。
- 服务器先下载并完整校验三端实际文件，候选副本45项Python回归通过，再由releases.publish事务切换到0.6.6。首次候选回归为2失败/8错误，原因是容器默认临时盘仅58,785,792字节可用，低于发布保护所需100MiB；未触及线上源码或版本。将夹具TMPDIR/TEMP/TMP指向网站所在文件系统后通过，保留第一次日志，不修改生产空间保护或跳过测试。
- 官网真实资产记录三端大小/SHA与上表一致，受控下载文件全量SHA再次匹配。帖子、回复及订阅行数前后一致；未建立邮件任务、未发送邮件，不替换.env/用户上传/运行配置。
- 公网首页、下载页、论坛均200；带`?v=0.6.6`的页面已显示新版介绍/五张图片/三端下载与DEB入口。公开CSS、JS及五张WebP均与当前本地SHA一致；三个官网下载端点Range `bytes=0-31`均206、Content-Range总尺寸正确、ETag为各最终SHA，首32字节与本地文件一致。
- 普通`/`和`/downloads`在当前边缘节点仍命中旧Cloudflare缓存（检查Age约3385秒）；no-cache请求也未刷新。已向用户说明，用户明确选择“等待缓存自然更新，先交付”。立即查看[新版首页](https://leonard.asia/?v=0.6.6)和[新版下载](https://leonard.asia/downloads?v=0.6.6)。未声称全网缓存已经清除。

## 限制与后续

用户已接受普通页面等待缓存自然更新，该项不再等待人工清缓存。DEB依赖本次构建系统的t64库；Linux两种包均需系统FFmpeg，未完成全部发行版安装矩阵。Windows CI证明打包/测试及进程存活，未证明可见真实桌面播放；未覆盖付费接口、长时跨网共听或Release帧预算。本次没有覆盖安装手机或用户正式电脑。

## 同步文档

[文档索引](../README.md)、[开发说明](../DEVELOPMENT.md)、[版本说明](../RELEASE_NOTES-0.6.6.md)。官网页面的独立变更另留记录。
