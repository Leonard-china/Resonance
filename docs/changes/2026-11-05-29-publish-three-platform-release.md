# 2026-11-05 — 构建并发布本轮修复后的Android、Windows和Linux正式包

- 修改日期：2026-11-05（Asia/Shanghai）
- 类型：正式发布 / 构建验证 / 文档
- 适用版本：0.6.6 / 40，来自version.properties；GitHub仍是未公开草稿，本次不升版
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

| 场景 | 发布前 | 本次目标 |
| --- | --- | --- |
| GitHub | 0.6.5公开，0.6.6草稿 | 公开本次完整三端正式包与真实校验 |
| 官网 | 旧安装包/旧界面预览 | 新版介绍、截图及三端同版本实际下载 |
| 本轮修复 | 仅源码/独立测试包 | 正式发行产物包含这些修复 |

## 平台与数据影响

维持版本0.6.6 / 40、用户数据格式、正式签名及原生平台。发布包不含用户音乐、API Key、私钥、官网.env、作者入口/密码、数据库、上传图片或本机工具。官网更新前做在线数据库与源码/原包备份，不发送订阅邮件。本次不自动覆盖用户电脑或手机上的正式安装。

## 验证

构建和发布进行中。实际命令、源码提交、CI链接、资源预算、四份产物的大小/哈希、签名、安装包结构、官网发布回执与公开Range检查将在完成后填入，不将历史结果写成通过。

## 限制与后续

以本记录最终结果为准。共享Linux桌面整个构建进程树2GiB、单worker和共享验证锁；若超限保留日志并收窄/转移验证，不放宽资源上限或关闭用户应用。

## 同步文档

[文档索引](../README.md)、[开发说明](../DEVELOPMENT.md)、[版本说明](../RELEASE_NOTES-0.6.6.md)。官网页面的独立变更另留记录。
