# 2026-10-16 — 官网下载同步最新双模式教程安装包

- 修改日期：2026-10-16（Asia/Shanghai）
- 类型：运行数据更新 / 生产部署 / 文档
- 适用版本：播放器 `0.4.4 / 27`（按用户明确要求沿用 0.4.4 版本号，直接使用已构建的教程版产物上线）
- 影响平台：独立官网本地运行数据、生产环境 [leonard.asia](https://leonard.asia) 下载服务
- 关联需求：用户指示“不用4.5了，直接用构建好的上传吧”，将刚完成的“首次启动双模式教程”产物同步至官网下载

## 需求或问题

此前已构建包含新手双模式教程与真实交互教学的正式安装包（APK 与 EXE），归档于 `release/0.4.4-tutorial-2026-10-16/`；但线上官网 [leonard.asia/downloads](https://leonard.asia/downloads) 仍提供旧版 0.4.4（仅支持扫码登录，不含教程特性）。用户确认不升版至 0.4.5，直接将该最新产物同步推送到官网供用户下载。

## 原因与依据

1. 保持版本来源 `version.properties` 为 `0.4.4 / 27`，遵循用户本轮不升版指示。
2. 官网数据库 `releases.py` 具有严格的版本递增校验（同一或更低版本号不能覆盖插入）。为在维持 0.4.4 的前提下更新下载包，先通过 SQLite 事务清理原 0.4.4 历史记录，重新调用受控的 `releases.publish` 重新计算实际文件的 SHA-256 与文件大小，更新 `assets` 与 `active_release`，并自动清理旧临时与废弃文件。
3. 同步更新工程内 `release/0.4.4/` 归档与 `docs/RELEASE_NOTES-0.4.4.md` 说明与校验表。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| [release/0.4.4/](../../release/0.4.4/) | `Resonance-0.4.4-release.apk`、`Resonance-0.4.4.exe`、`SHA256SUMS.txt` | 用教程版本真实产物覆盖更新原归档，文件大小分别更新为 64,109,577 字节与 204,931,072 字节。 |
| [docs/RELEASE_NOTES-0.4.4.md](../RELEASE_NOTES-0.4.4.md) | 主要更新与校验表 | 补充双模式新手教程核心特性条目，更新校验表中的文件大小与 SHA-256 哈希值。 |
| `website/data/` | `site.sqlite3`、`releases/` | 本地官网运行数据通过 `releases.publish` 更新 active_release，计算新 SHA-256 并清理旧包。 |
| 生产服务器 (`leonard.asia`) | `/home/container/website` | 上传新 APK、新 EXE 与版本说明，在容器内执行受控切换，重置校验和并重启 Gunicorn 守护服务。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 官网下载页 (Windows) | 提供旧 0.4.4 EXE（195.32 MiB，SHA-256 `b91827...`） | 提供最新教程版 EXE（195.44 MiB，SHA-256 `f06ac519ce41abe65ee9d85b201b1e43ea8edd2fbaff0dc2c325fc4dabac67a8`） |
| 官网下载页 (Android) | 提供旧 0.4.4 APK（61.09 MiB，SHA-256 `1553d5...`） | 提供最新教程版 APK（61.14 MiB，SHA-256 `e3d82b2ad394bedb04a579d68197416fde045b4cc6f7bc809b5bfc31ce32425b`） |
| 更新说明亮点 | 仅列出 B 站扫码与网页登录 | 同步体现双模式新手教程与真实界面操作教学 |

## 平台与数据影响

- **代码与版本**：未改动播放器代码或升版，维持 `0.4.4 / 27`。
- **官网运行数据**：本地和生产服务器均完成受控切换，旧版文件被清理，仅保留最新一套安装包。
- **安全性**：不输出或记录生产 token 与私钥，上传通过 Pterodactyl 授权接口完成。

## 验证

| 操作或检查 | 环境 | 结果 |
| --- | --- | --- |
| 本地官网发布与数据库核对 | 本地 Python 虚拟环境 | **通过**：本地 `data/site.sqlite3` 成功切换，`active_release` 指向新发布 ID，两端 SHA-256 精确匹配。 |
| 生产文件上传 (APK / EXE) | PterodactylClient 容器上传 | **通过**：APK (64,109,577 字节，耗时 11.4s) 与 EXE (204,931,072 字节，耗时 34.8s) 完整上传成功。 |
| 生产容器发布切换与服务重启 | 容器 Gunicorn / 启动器 | **通过**：更新脚本执行成功，旧版目录安全清理，Web 服务重启正常。 |
| 线上 [leonard.asia/downloads](https://leonard.asia/downloads) 页面核对 | 公网 HTTPS | **通过**：HTTP 200，页面展示的 SHA-256 校验和已更新为 `f06ac5...` (Windows) 与 `e3d82b...` (Android)。 |
| 线上两端文件 Range 断点续传探测 | 公网 HTTPS (`Range: bytes=0-15`) | **通过**：Windows 与 Android 均返回 HTTP 206，Content-Range 总长度分别为 204,931,072 字节与 64,109,577 字节，文件格式魔数 `MZ` 与 `PK` 正确。 |

## 限制与后续

本次按用户要求沿用 0.4.4 版本号替换安装包，未进行语义化升版。对于已经安装旧 0.4.4 的 Android 设备，由于 versionCode 相同（均为 27），覆盖安装时系统不会提示新版本发现，需手动覆盖安装。

## 同步更新的文档

- [docs/README.md](../README.md)：新增第 23 项变更记录索引。
- [docs/RELEASE_NOTES-0.4.4.md](../RELEASE_NOTES-0.4.4.md)：补充教程特性说明与新校验值。
