# Resonance v0.1.4

发布日期：2026-08-14

## 本版本重点

- 修复 Android 16 设备启动阶段的兼容性问题，并重新验证 Release APK 安装与启动。
- 修复 Windows Release 便携版因裁剪 JavaFX 反射类而出现的 `Failed to launch JVM`。
- Android KGMA 入口改为系统文件夹授权，支持递归转换和持久化读写权限。
- 将“转换失败”与“转换完成但源 KGMA 无法删除”分开统计，避免歌单已完整匹配时出现误导性的失败数量。
- 补全 KGMA 运行时依赖；MP3 载荷原样复制，FLAC/Ogg 载荷以 320 kbps 转码。
- 修复本地歌曲匹配后歌单封面被空值覆盖的问题，并让 Android/Windows 共用一致的封面回退规则。
- 修复局域网同步的一次性接收流程、私有 IPv4 校验和加密同步包导入。
- 新增删除托管 MP3 的功能，同时保护系统扫描到的原始音乐。

## KGMA 安全策略

- 当前支持 KGMA v3 / slot 1。
- 转换时先写入临时文件，通过格式识别并成功生成托管 MP3 后再提交。
- 只有写入成功后才尝试删除源 KGMA。
- 删除失败只产生清理警告，不会删除已生成的 MP3，也不会再计入转换失败。
- 普通系统扫描不会修改或删除原始文件。

## 验证结果

- 15 个测试套件、30 个测试：全部通过。
- Android Release APK：安装、启动、SAF 文件夹授权、KGMA 转换、源文件删除、媒体库刷新通过。
- Windows Release：便携目录实际启动通过，EXE/MSI 打包通过。
- 局域网同步、离线加密包、酷狗公开歌单解析与安全边界测试通过。
- APK v2 签名验证通过。

## 发布文件

| 文件 | SHA-256 |
| --- | --- |
| `Resonance-Android-v0.1.4.apk` | `8FDBEEA29A81581B965653C329602DE161DA0E6100C740950EC67BC36DC4E818` |
| `Resonance-Windows-v0.1.4-Portable.zip` | `192B463CCB1F827E3D403A6A89DE7323BC5CCBAABBC5491ED0B09081A17C04F9` |
| `Resonance-Windows-v0.1.4-Setup.exe` | `BB4C9422B7F11F31D60C74E5BBBDAC0C34740E9AC71B2F95E2390B60913EE9B5` |
| `Resonance-Windows-v0.1.4.msi` | `CD8B2F04B9DBF6A0D334A43134EF6B5D615198A1BE18963564317CBAC9C52975` |

## 已知限制

- 酷狗链接只导入公开目录与封面，不下载会员或受保护音频。
- KGMA v3 / slot 1 以外的变体暂不支持。
- Windows 作为局域网发送端，Android 作为扫码接收端；双向实时同步尚未实现。
- Windows 安装程序当前没有商业代码签名证书，首次运行可能触发 SmartScreen。
