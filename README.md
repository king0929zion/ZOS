# BrowserOS Android - 安卓版智能浏览器

## 项目简介

BrowserOS Android 是 BrowserOS 浏览器的安卓版本，是一个基于 Chromium WebView 的开源智能浏览器。它支持 AI 代理功能，可以自动化浏览任务，同时保护用户隐私。

## 核心特性

- 🏠 **熟悉的界面** - 类似 Chrome 的用户体验
- 🤖 **AI 代理功能** - 支持 OpenAI、Anthropic API，以及本地模型（Ollama/LMStudio）
- 🔒 **隐私优先** - 使用您自己的 API 密钥，数据存储在本地设备
- 🚀 **开源免费** - 完全开源，社区驱动
- 📱 **移动优化** - 专为安卓设备优化的界面和交互

## 技术架构

### 核心技术栈
- **Android SDK**: 最低支持 Android 7.0 (API 24)
- **WebView**: 基于 Chromium 的 Android WebView
- **AI 集成**: 支持多种 AI 服务提供商
- **本地存储**: SQLite 数据库存储浏览历史和设置
- **架构模式**: MVVM (Model-View-ViewModel)

### 项目结构
```
app/
├── src/main/
│   ├── java/com/browseros/android/
│   │   ├── MainActivity.java          # 主活动
│   │   ├── browser/                   # 浏览器核心功能
│   │   │   ├── BrowserEngine.java     # 浏览器引擎封装
│   │   │   ├── TabManager.java        # 标签页管理
│   │   │   └── HistoryManager.java    # 历史记录管理
│   │   ├── ai/                        # AI 代理功能
│   │   │   ├── AIService.java         # AI 服务接口
│   │   │   ├── OpenAIProvider.java    # OpenAI 实现
│   │   │   ├── AnthropicProvider.java # Anthropic 实现
│   │   │   └── OllamaProvider.java    # Ollama 本地模型实现
│   │   ├── privacy/                   # 隐私保护功能
│   │   │   ├── DataManager.java       # 数据管理
│   │   │   └── SecureStorage.java     # 安全存储
│   │   └── ui/                        # 用户界面
│   │       ├── fragments/             #  Fragment 组件
│   │       └── adapters/              # 列表适配器
│   ├── res/                           # 资源文件
│   └── AndroidManifest.xml            # 应用清单
└── build.gradle                       # 构建配置
```

## 功能说明

### 1. 浏览器核心功能

#### BrowserEngine
- **功能**: 封装 WebView，提供浏览器核心功能
- **主要方法**:
  - `loadUrl(String url)`: 加载网页
  - `goBack()`: 后退
  - `goForward()`: 前进
  - `reload()`: 刷新
  - `stopLoading()`: 停止加载

#### TabManager
- **功能**: 管理多个标签页
- **主要方法**:
  - `createTab(String url)`: 创建新标签页
  - `closeTab(int tabId)`: 关闭标签页
  - `switchTab(int tabId)`: 切换标签页
  - `getAllTabs()`: 获取所有标签页

#### HistoryManager
- **功能**: 管理浏览历史记录
- **主要方法**:
  - `addHistory(String url, String title)`: 添加历史记录
  - `getHistory()`: 获取历史记录列表
  - `clearHistory()`: 清空历史记录
  - `searchHistory(String keyword)`: 搜索历史记录

### 2. AI 代理功能

#### AIService
- **功能**: AI 服务统一接口
- **主要方法**:
  - `chat(String message)`: 发送聊天消息
  - `analyzePage(String url)`: 分析网页内容
  - `extractData(String url, String selector)`: 提取网页数据
  - `automateTask(String task)`: 自动化任务

#### OpenAIProvider
- **功能**: OpenAI API 实现
- **配置**: 需要设置 OPENAI_API_KEY
- **支持模型**: GPT-4, GPT-3.5-turbo

#### AnthropicProvider
- **功能**: Anthropic Claude API 实现
- **配置**: 需要设置 ANTHROPIC_API_KEY
- **支持模型**: Claude 3 Opus, Claude 3 Sonnet

#### OllamaProvider
- **功能**: Ollama 本地模型实现
- **配置**: 需要设置 OLLAMA_BASE_URL（默认 http://localhost:11434）
- **支持**: 所有本地运行的 Ollama 模型

### 3. 隐私保护功能

#### DataManager
- **功能**: 管理用户数据
- **特性**:
  - 所有数据存储在本地设备
  - 支持数据加密存储
  - 可选择性清除数据

#### SecureStorage
- **功能**: 安全存储 API 密钥和敏感信息
- **特性**:
  - 使用 Android Keystore 加密
  - 密钥不会上传到服务器
  - 支持密钥导入/导出

## 安装和使用

### 环境要求
- Android Studio Arctic Fox 或更高版本
- JDK 11 或更高版本
- Android SDK API 24 或更高版本

### 编译步骤

1. **克隆项目**
```bash
git clone <repository-url>
cd BrowserOS-Android
```

2. **配置 API 密钥（可选）**
   - 在 `app/src/main/res/values/config.xml` 中配置默认 API 密钥
   - 或在应用设置中手动配置

3. **编译安装**
```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 使用说明

1. **首次启动**
   - 打开应用后，会提示配置 AI 服务
   - 可以选择使用云端 API 或本地模型

2. **浏览网页**
   - 在地址栏输入网址或搜索关键词
   - 支持多标签页浏览
   - 长按链接可以打开新标签页

3. **使用 AI 功能**
   - 点击工具栏的 AI 图标
   - 输入任务描述，AI 会帮助完成
   - 例如："帮我搜索今天北京的天气"

4. **隐私设置**
   - 在设置中可以清除浏览数据
   - 可以导出/导入 API 密钥
   - 所有数据都存储在本地

## 配置说明

### API 密钥配置

#### OpenAI
```xml
<string name="openai_api_key">your-api-key-here</string>
```

#### Anthropic
```xml
<string name="anthropic_api_key">your-api-key-here</string>
```

#### Ollama（本地模型）
```xml
<string name="ollama_base_url">http://localhost:11434</string>
```

### 权限说明

应用需要以下权限：
- `INTERNET`: 访问网络
- `ACCESS_NETWORK_STATE`: 检查网络状态
- `WRITE_EXTERNAL_STORAGE`: 保存下载文件（Android 10 以下）
- `READ_EXTERNAL_STORAGE`: 读取下载文件（Android 10 以下）

## 开发计划

### 已完成功能 ✅
- [x] 基础浏览器功能（导航、标签页、历史记录）
- [x] WebView 集成和页面加载
- [x] 多标签页管理
- [x] 浏览历史记录（SQLite 数据库存储）
- [x] AI 服务集成框架
- [x] OpenAI API 支持
- [x] Anthropic Claude API 支持
- [x] Ollama 本地模型支持
- [x] AI 对话界面
- [x] 隐私保护基础功能
- [x] 安全存储（Android Keystore 加密 API 密钥）
- [x] 数据管理（清除历史、缓存、Cookie）
- [x] 设置界面（API 密钥配置、隐私设置）

### 开发中功能 🚧
- [ ] AI 代理自动化任务（需要 JavaScript 注入和页面交互）
- [ ] 标签页管理界面（显示所有标签页、切换、关闭）
- [ ] 书签管理
- [ ] 下载管理
- [ ] 历史记录搜索界面

### 计划功能 📋
- [ ] AI 广告拦截器
- [ ] 暗黑模式
- [ ] 同步功能（可选）
- [ ] MCP 服务器支持
- [ ] 扩展支持
- [ ] 无痕浏览模式

## 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. Fork 本项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 许可证

本项目采用 AGPL-3.0 许可证。

## 联系方式

- GitHub Issues: 报告问题或提出建议
- 项目主页: https://browseros.com

## 更新日志

### v0.1.0 (当前版本)
- ✅ 初始版本发布
- ✅ 基础浏览器功能（导航、标签页、历史记录）
- ✅ AI 服务集成（OpenAI、Anthropic、Ollama）
- ✅ AI 对话界面
- ✅ 隐私保护功能（安全存储、数据管理）
- ✅ 设置界面（API 密钥配置）

### 已知问题
- 标签页管理界面尚未实现（可通过代码创建多个标签页，但 UI 未完成）
- AI 自动化任务功能需要进一步开发（需要 JavaScript 注入）
- 部分错误处理可能需要优化

### 下一步计划
1. 完善标签页管理 UI
2. 实现 AI 自动化任务功能
3. 添加书签管理
4. 优化用户体验和性能

---

**注意**: 本项目正在积极开发中，功能可能不完整。欢迎反馈和建议！

