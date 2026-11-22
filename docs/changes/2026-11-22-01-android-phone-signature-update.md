# 2026-11-22 — 为本机测试签名准备 Android 0.7.0 覆盖包

- 修改日期：2026-11-22（Asia/Shanghai）
- 类型：安装兼容性排查与本地交付
- 适用版本：Android `0.7.0 / 70`
- 关联记录：[上一份本机测试包](2026-11-21-05-clearwater-fresh-ui-apple-motion-and-apk.md)、[歌词校准](2026-11-21-06-lyrics-calibration-toolbar.md)、[正式发布](2026-11-21-07-publish-0.7.0-and-update-desktop.md)

## 需求或问题

用户尝试覆盖手机上的播放器时，系统提示签名不一致。用户说明应以手机当前安装的版本为准。

## 原因与依据

本机保留的上一份 0.7.0 测试 APK 与公开 Release APK 的包名均为 `com.resonance.player`，版本均为 `0.7.0 / 70`。测试包使用本机 Android 调试证书，公开包使用正式证书。两份 APK 的证书 SHA-256 分别为 `312045563f70aee1669a32edcbeeaaf1be39e4ca972515d3f62e0aaf2f55ef2e` 与 `b2b4ad35b8c7136569d219103448eaee75d4bbd30f3872c601373df43b303fe5`。手机如果安装的是本机测试包，正式包无法直接覆盖。历史实机备份和手机当前安装状态是不同的证据。

## 具体修改

基于 11 月 21 日正式 APK，使用本机测试包使用的证书重新签名，生成忽略目录 `build/phone-signature-2026-11-22/Resonance-0.7.0-phone-update.apk`。APK 的 SHA-256 为 `430755b07baca0e2a3adf100270ea308a9fec145e10b2c3e7895593d88cb2ee6`。包内保留正式构建的代码与资源，包括同步歌词校准。

## 修改前后行为

| 场景 | 原有下载包 | 本地覆盖包 |
| --- | --- | --- |
| 设备已安装同包名、同版本的本机测试签名 APK | 签名不同，系统拒绝覆盖 | 证书相同，满足原地覆盖的签名条件 |

## 平台与数据影响

覆盖包面向本机测试签名的 Android 安装。包名和版本来自现有 Release，使用普通覆盖安装保留应用数据。GitHub 和官网继续服务正式签名用户，桌面发布沿用 11 月 21 日的产物。

## 验证

| 命令或操作 | 实际结果 |
| --- | --- |
| `apksigner verify --verbose --print-certs` | 通过，覆盖包 v2/v3 签名有效，证书 SHA-256 与本机保留的测试包一致。 |
| `zipalign -c -P 16 4`、`aapt2 dump badging` | 通过，16 KiB 对齐、包名与版本匹配，应用未标记为 debuggable。 |
| ZIP 条目 SHA-256 对比 | 通过，318 个非签名条目逐项一致。 |
| 专用 `resonance-api36` 模拟器覆盖安装 | 受阻，两次冷启动在内存预算内均未就绪，未执行安装。启动日志保存在本次产物目录。 |
| `adb devices -l` | 本轮未检测到物理手机，未核对用户当前安装包，也未改动手机数据。 |

## 限制与后续

手机真实签名与安装结果需要以当前设备为准。连接后先通过 `adb -s <设备序列号> shell pm path com.resonance.player` 获取已安装 APK 的位置，再拉取并校验签名、包名与版本号。核对兼容后执行 `adb -s <设备序列号> install -r <覆盖包>`。

## 同步更新的文档

文档入口及开发说明的 Android Release 配置章节。
