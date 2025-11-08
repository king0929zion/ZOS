# Android 应用签名配置指南

## 概述

为了在 Android 设备上安装应用，需要对 APK 进行签名。本指南将帮助您配置应用签名。

## 生成签名密钥（Keystore）

### 方法一：使用命令行工具（推荐）

1. **打开终端/命令提示符**

2. **生成 keystore 文件**：
```bash
keytool -genkeypair -v -storetype PKCS12 -keystore release.keystore -alias browseros -keyalg RSA -keysize 2048 -validity 10000
```

3. **按提示输入信息**：
   - 密钥库密码：输入一个强密码（请妥善保管）
   - 再次输入密码：确认密码
   - 您的名字和姓氏：输入您的姓名或组织名称
   - 组织单位：输入组织单位（可选）
   - 组织：输入组织名称
   - 城市：输入城市名称
   - 省/市/自治区：输入省份
   - 国家/地区代码：输入国家代码（如 CN）

4. **确认信息**：输入 `yes` 确认

5. **生成成功**：会在当前目录生成 `release.keystore` 文件

### 方法二：使用 Android Studio

1. 打开 Android Studio
2. 选择 `Build` → `Generate Signed Bundle / APK`
3. 选择 `APK` → `Next`
4. 点击 `Create new...` 创建新的 keystore
5. 填写信息并保存 keystore 文件

## 配置本地签名（用于本地开发）

### 1. 创建 `keystore.properties` 文件

在项目根目录创建 `keystore.properties` 文件（**不要提交到 Git**）：

```properties
KEYSTORE_FILE=release.keystore
KEYSTORE_PASSWORD=您的密钥库密码
KEY_ALIAS=browseros
KEY_PASSWORD=您的密钥密码（通常与密钥库密码相同）
```

### 2. 更新 `app/build.gradle`

签名配置已经配置完成，会自动从 `keystore.properties` 读取配置。

### 3. 将 keystore 文件放在项目根目录

将生成的 `release.keystore` 文件放在项目根目录（**不要提交到 Git**）。

## 配置 GitHub Actions 自动签名（用于 CI/CD）

### 1. 准备密钥信息

您需要以下信息：
- Keystore 文件路径
- Keystore 密码
- Key 别名（默认：`browseros`）
- Key 密码

### 2. 将 Keystore 转换为 Base64

在终端中运行：
```bash
# Linux/Mac
base64 -i release.keystore > keystore_base64.txt

# Windows PowerShell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore")) | Out-File keystore_base64.txt
```

### 3. 在 GitHub 仓库中配置 Secrets

1. 打开 GitHub 仓库
2. 进入 `Settings` → `Secrets and variables` → `Actions`
3. 点击 `New repository secret` 添加以下 secrets：

   - **KEYSTORE_BASE64**：keystore 文件的 Base64 编码（从 `keystore_base64.txt` 复制）
   - **KEYSTORE_PASSWORD**：密钥库密码
   - **KEY_ALIAS**：密钥别名（默认：`browseros`）
   - **KEY_PASSWORD**：密钥密码（通常与密钥库密码相同）

### 4. 验证配置

配置完成后，GitHub Actions 会自动：
1. 从 secrets 读取 keystore（Base64 格式）
2. 解码并创建 keystore 文件
3. 使用 keystore 对 APK 进行签名
4. 生成已签名的 APK

## 安全注意事项

⚠️ **重要安全提示**：

1. **永远不要将 keystore 文件提交到 Git**
   - `release.keystore` 已在 `.gitignore` 中
   - `keystore.properties` 也应该添加到 `.gitignore`

2. **妥善保管 keystore 文件**
   - 丢失 keystore 文件将无法更新应用
   - 建议备份到安全的位置

3. **使用强密码**
   - 密钥库密码和密钥密码应该足够复杂
   - 建议使用密码管理器保存

4. **定期更新密钥**
   - 密钥有效期设置为 10000 天（约 27 年）
   - 到期前需要重新生成密钥

## 验证签名

构建完成后，可以验证 APK 是否已正确签名：

```bash
# 检查 APK 签名信息
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk

# 或使用 apksigner（Android SDK Build Tools）
apksigner verify --verbose app/build/outputs/apk/release/app-release.apk
```

## 故障排除

### 问题：安装时显示"缺少开发人员证书"

**原因**：APK 未签名或签名无效

**解决方案**：
1. 确保已配置签名密钥
2. 检查 GitHub Secrets 是否正确配置
3. 验证 keystore 文件是否有效

### 问题：GitHub Actions 构建失败

**可能原因**：
- Secrets 未配置或配置错误
- Keystore Base64 编码不正确
- 密码错误

**解决方案**：
1. 检查 GitHub Secrets 是否全部配置
2. 重新生成 Base64 编码
3. 验证密码是否正确

## 相关文件

- `app/build.gradle` - 签名配置
- `.github/workflows/android.yml` - CI/CD 签名配置
- `.gitignore` - 已排除 keystore 文件

## 参考资源

- [Android 官方签名文档](https://developer.android.com/studio/publish/app-signing)
- [Keytool 文档](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/keytool.html)

