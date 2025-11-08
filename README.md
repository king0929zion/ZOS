# BrowserOS Android - 安卓版智能浏览器

## 项目简介

BrowserOS Android 是 BrowserOS 浏览器的安卓版本，是一个基于 Chromium WebView 的开源智能浏览器。它支持 AI 代理功能，可以自动化浏览任务，同时保护用户隐私。

## 核心特性

- 🏠 **熟悉的界面** - 类似 Chrome 的用户体验
- 🤖 **AI Agent 能力** - 原生 AI Agent 系统，支持工具调用（Function Calling），可以自动化执行浏览器操作
- 🔒 **隐私优先** - 使用您自己的 API 密钥，数据存储在本地设备
- 🚀 **开源免费** - 完全开源，社区驱动
- 📱 **移动优化** - 专为安卓设备优化的界面和交互
- 🛠️ **强大的工具系统** - AI 可以调用浏览器工具（导航、点击、输入、滚动、提取内容等）
- 🧠 **智能自动化** - AI Agent 可以理解复杂任务并自动执行多步操作

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
│   │   ├── ai/                        # AI Agent 功能
│   │   │   ├── AIService.java         # AI 服务接口
│   │   │   ├── OpenAIProvider.java    # OpenAI 实现（支持工具调用）
│   │   │   ├── AnthropicProvider.java # Anthropic 实现
│   │   │   ├── AgentTool.java         # Agent 工具接口
│   │   │   ├── AgentToolManager.java  # 工具管理器
│   │   │   ├── AgentExecutor.java     # Agent 执行引擎
│   │   │   └── PageContentExtractor.java # 页面内容提取器
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

### 2. AI Agent 系统

#### AgentExecutor
- **功能**: AI Agent 执行引擎，协调 AI 模型和工具的执行
- **特性**:
  - 支持多轮工具调用循环
  - 自动规划任务步骤
  - 根据工具结果决定下一步操作
  - 最大迭代次数限制（防止无限循环）

#### AgentToolManager
- **功能**: 管理所有可用的浏览器操作工具
- **可用工具**:
  - **导航工具**: `navigate`, `go_back`, `go_forward`, `reload`, `new_tab`
  - **页面操作**: `click`, `type`, `scroll`, `wait`
  - **内容提取**: `extract_content`, `get_page_info`, `execute_script`
- **工具注册**: 支持动态注册新工具

#### PageContentExtractor
- **功能**: 从 WebView 中提取页面内容
- **提取能力**:
  - 提取页面文本内容
  - 提取页面 HTML
  - 提取所有链接
  - 提取所有图片
  - 提取表单元素
  - 提取页面结构化信息

#### AIService
- **功能**: AI 服务统一接口，支持工具调用
- **主要方法**:
  - `chat(String message)`: 发送聊天消息
  - `chatWithTools(String message, JSONArray tools)`: 发送消息（支持工具调用）
  - `analyzePage(String url)`: 分析网页内容
  - `extractData(String url, String selector)`: 提取网页数据
  - `automateTask(String task)`: 自动化任务

#### OpenAIProvider
- **功能**: OpenAI API 实现，支持所有 OpenAI 规范的模型和工具调用（Function Calling）
- **配置**: 
  - 需要设置 OPENAI_API_KEY（必需）
  - 可选设置 OPENAI_API_URL（默认：https://api.openai.com/v1/chat/completions）
  - 可选设置 OPENAI_MODEL（默认：gpt-3.5-turbo）
- **支持模型**: 
  - OpenAI 官方模型：GPT-4, GPT-4 Turbo, GPT-3.5-turbo 等
  - 兼容 OpenAI API 的第三方模型：Claude（通过兼容端点）、Llama、Mistral 等
  - 支持自定义 API 端点，可连接任何兼容 OpenAI API 格式的服务
- **工具调用**: 
  - 支持 OpenAI Function Calling 规范
  - AI 可以自动选择并调用浏览器工具
  - 支持多轮工具调用循环
- **自定义配置**: 
  - 支持自定义 API URL，可连接代理服务或自建服务
  - 支持自定义模型名称，兼容所有 OpenAI API 规范的模型

#### AnthropicProvider
- **功能**: Anthropic Claude API 实现
- **配置**: 需要设置 ANTHROPIC_API_KEY
- **支持模型**: Claude 3 Opus, Claude 3 Sonnet, Claude 3 Haiku

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
   - 在应用设置中手动配置 API 密钥
   - OpenAI 支持自定义 API URL 和模型名称
   - Anthropic 使用官方 API 端点

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

3. **使用 AI Agent 功能**
   - 点击工具栏的 AI 图标
   - 输入任务描述，AI Agent 会自动执行
   - **示例任务**:
     - "打开百度并搜索天气" - AI 会自动导航、点击、输入
     - "提取这个页面的所有链接" - AI 会自动提取并返回链接列表
     - "帮我填写登录表单，用户名是test" - AI 会自动查找表单并填写
     - "滚动到页面底部" - AI 会自动滚动页面

4. **隐私设置**
   - 在设置中可以清除浏览数据
   - 可以导出/导入 API 密钥
   - 所有数据都存储在本地

## 配置说明

### API 密钥配置

#### OpenAI
- **API 密钥**: 在设置界面输入 OpenAI API 密钥
- **API URL**（可选）: 默认使用 `https://api.openai.com/v1/chat/completions`
  - 可自定义为其他兼容 OpenAI API 的端点
  - 例如：`https://api.example.com/v1/chat/completions`
- **模型名称**（可选）: 默认使用 `gpt-3.5-turbo`
  - 支持所有 OpenAI 官方模型：`gpt-4`, `gpt-4-turbo`, `gpt-3.5-turbo` 等
  - 支持兼容 OpenAI API 的第三方模型

#### Anthropic
- **API 密钥**: 在设置界面输入 Anthropic API 密钥
- 使用官方 API 端点，无需额外配置

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
- [x] OpenAI API 支持（支持所有 OpenAI 规范的模型）
- [x] OpenAI 自定义 URL 和模型配置
- [x] OpenAI Function Calling（工具调用）支持
- [x] AI Agent 执行引擎（AgentExecutor）
- [x] 浏览器工具系统（AgentToolManager）
- [x] 页面内容提取器（PageContentExtractor）
- [x] 12+ 个浏览器操作工具（导航、点击、输入、滚动、提取等）
- [x] Anthropic Claude API 支持
- [x] AI 对话界面
- [x] 隐私保护基础功能
- [x] 安全存储（Android Keystore 加密 API 密钥）
- [x] 数据管理（清除历史、缓存、Cookie）
- [x] 设置界面（API 密钥配置、隐私设置）

### 开发中功能 🚧
- [ ] 流式响应支持（Streaming）
- [ ] 标签页管理界面（显示所有标签页、切换、关闭）
- [ ] 书签管理界面优化
- [ ] 下载管理界面
- [ ] 历史记录搜索界面
- [ ] Agent 执行可视化（显示工具调用过程）

### 计划功能 📋
- [ ] AI 广告拦截器
- [ ] 暗黑模式
- [ ] 同步功能（可选）
- [ ] MCP 服务器支持
- [ ] 扩展支持
- [ ] 更多 Agent 工具（截图、下载、表单验证等）

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
- ✅ AI 服务集成（OpenAI、Anthropic）
- ✅ OpenAI 自定义 URL 和模型支持
- ✅ AI Agent 系统（工具调用、自动化执行）
- ✅ 浏览器工具系统（12+ 个操作工具）
- ✅ 页面内容提取和分析
- ✅ AI 对话界面
- ✅ 隐私保护功能（安全存储、数据管理）
- ✅ 设置界面（API 密钥配置）

### 已知问题
- 标签页管理界面尚未实现（可通过代码创建多个标签页，但 UI 未完成）
- Agent 工具调用需要页面完全加载后才能执行
- 部分复杂页面的内容提取可能需要优化
- 流式响应暂未实现（计划中）

### 下一步计划
1. 实现流式响应支持
2. 完善标签页管理 UI
3. 添加更多 Agent 工具
4. 优化 Agent 执行性能和错误处理
5. 添加 Agent 执行过程可视化

## AI Agent 使用指南

### Agent 能力说明

BrowserOS Android 实现了完整的 AI Agent 系统，参考了 [BrowserOS](https://github.com/browseros-ai/BrowserOS) 的设计理念，AI 可以：

1. **理解自然语言指令**
   - "打开百度并搜索天气"
   - "帮我填写这个表单"
   - "提取这个页面的所有链接"

2. **自动执行多步操作**
   - AI 会自动规划步骤
   - 调用相应的浏览器工具
   - 根据结果决定下一步操作
   - 完成任务后给出总结

3. **智能页面操作**
   - 点击按钮和链接
   - 填写表单
   - 滚动页面查找内容
   - 提取页面数据

### 使用示例

**示例 1：导航和搜索**
```
用户: "打开百度并搜索'Android开发'"
AI Agent: 
  [调用 navigate 工具导航到百度] 
  → [调用 click 工具点击搜索框] 
  → [调用 type 工具输入'Android开发'] 
  → [调用 click 工具点击搜索按钮]
  → "已完成搜索"
```

**示例 2：数据提取**
```
用户: "提取这个页面的所有链接"
AI Agent: 
  [调用 extract_content 工具提取链接] 
  → "找到以下链接：[链接列表]"
```

**示例 3：表单填写**
```
用户: "帮我填写登录表单，用户名是test，密码是123456"
AI Agent: 
  [调用 extract_content 工具查找表单] 
  → [调用 type 工具填写用户名] 
  → [调用 type 工具填写密码] 
  → [调用 click 工具提交]
  → "已填写并提交表单"
```

### 工具调用流程

1. 用户发送消息
2. AI 分析意图，决定需要调用的工具
3. AgentExecutor 执行工具调用
4. 工具执行结果返回给 AI
5. AI 根据结果决定下一步操作
6. 重复步骤 3-5 直到任务完成
7. AI 返回最终结果

### 可用工具列表

| 工具名称 | 功能 | 参数示例 |
|---------|------|---------|
| `navigate` | 导航到URL | `{"url": "https://example.com"}` |
| `click` | 点击元素 | `{"selector": "#button"}` |
| `type` | 输入文本 | `{"selector": "#input", "text": "hello"}` |
| `scroll` | 滚动页面 | `{"direction": "down", "amount": 300}` |
| `wait` | 等待时间 | `{"ms": 1000}` |
| `extract_content` | 提取内容 | `{"selector": ".content", "type": "text"}` |
| `get_page_info` | 获取页面信息 | `{}` |
| `execute_script` | 执行JS | `{"script": "alert('hello')"}` |
| `go_back` | 后退 | `{}` |
| `go_forward` | 前进 | `{}` |
| `reload` | 刷新 | `{}` |
| `new_tab` | 新标签 | `{"url": "https://example.com"}` |

## OpenAI 自定义配置详解

### 支持的模型类型

OpenAIProvider 支持所有符合 OpenAI API 规范的模型，包括但不限于：

1. **OpenAI 官方模型**
   - `gpt-4` - GPT-4 模型
   - `gpt-4-turbo` - GPT-4 Turbo 模型
   - `gpt-3.5-turbo` - GPT-3.5 Turbo 模型（默认）
   - `gpt-4o` - GPT-4 Optimized 模型

2. **兼容 OpenAI API 的第三方服务**
   - 通过设置自定义 API URL，可以使用任何兼容 OpenAI API 格式的服务
   - 例如：OpenRouter、Together AI、Anyscale 等
   - 支持的模型包括：Claude（通过兼容端点）、Llama、Mistral、Gemini 等

### 自定义 API URL 示例

1. **使用 OpenRouter**
   ```
   API URL: https://openrouter.ai/api/v1/chat/completions
   模型: anthropic/claude-3-opus
   ```

2. **使用 Together AI**
   ```
   API URL: https://api.together.xyz/v1/chat/completions
   模型: meta-llama/Llama-2-70b-chat-hf
   ```

3. **使用自建服务**
   ```
   API URL: https://your-server.com/v1/chat/completions
   模型: your-custom-model
   ```

### 配置步骤

1. 打开应用设置
2. 输入 OpenAI API 密钥（如果使用第三方服务，输入对应服务的 API 密钥）
3. （可选）输入自定义 API URL
4. （可选）输入自定义模型名称
5. 保存设置

### 注意事项

- API URL 如果只输入基础地址（如 `https://api.example.com`），系统会自动添加 `/v1/chat/completions` 路径
- 如果 API URL 已包含完整路径，则直接使用
- 模型名称必须与所选 API 服务支持的模型名称一致
- 使用第三方服务时，请确保该服务完全兼容 OpenAI API 格式

---

**注意**: 本项目正在积极开发中，功能可能不完整。欢迎反馈和建议！

