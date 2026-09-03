# SuperAI Workbench - Android

超级AI工作台 Android 原生应用，整合 DeepSeek / 豆包 / GPT-4o / 通义千问 / Kimi / Claude 六大模型。

## 功能模块

| 模块 | 状态 | 说明 |
|------|------|------|
| AI对话 | ✅ 已实现 | 6大模型切换，流式SSE响应 |
| AI绘画 | 🚧 骨架 | DALL-E 3 API接入待完善 |
| AI视频 | 🚧 骨架 | 视频生成API接入待完善 |
| 知识库 | 🚧 骨架 | 文档管理功能待完善 |
| 智能体 | 🚧 骨架 | 智能体市场待完善 |
| IM通讯 | 🚧 骨架 | 即时通讯功能待完善 |
| 直播中心 | 🚧 骨架 | 直播功能待完善 |
| 支付中心 | 🚧 骨架 | 支付功能待完善 |
| 数据看板 | 🚧 骨架 | 图表分析待完善 |
| 安全中心 | 🚧 骨架 | 安全审计待完善 |

## 编译环境

- Android Studio Hedgehog (2023.1.1) 或更新版本
- JDK 17
- Android SDK 34
- Gradle 8.0

## 编译步骤

1. 打开 Android Studio
2. File → Open → 选择 `SuperAI-Android` 目录
3. 等待 Gradle Sync 完成
4. 连接设备或启动模拟器
5. 点击 Run (Shift+F10)

## API Key 配置

**方式一：应用内配置（推荐）**
1. 打开应用 → 点击左上角菜单 → 设置
2. 在对应模型输入框填入 API Key
3. 点击右上角 "保存"

**方式二：环境变量（仅限开发调试）**
在 `local.properties` 中添加：
```
DEEPSEEK_API_KEY=sk-...
OPENAI_API_KEY=sk-...
```

## 各模型Key获取

- **DeepSeek**: https://platform.deepseek.com
- **豆包**: https://console.volcengine.com
- **OpenAI**: https://platform.openai.com
- **通义千问**: https://dashscope.aliyun.com
- **Kimi**: https://platform.moonshot.cn
- **Claude**: https://console.anthropic.com

## 安全特性

- API Key 使用 `EncryptedSharedPreferences` AES-256-GCM 加密存储
- 禁用明文传输 (`usesCleartextTraffic="false"`)
- Certificate Pinning 配置（需替换为真实证书哈希）
- 禁止备份和数据提取
- ProGuard 代码混淆

## 项目结构

```
SuperAI-Android/
├── app/
│   ├── src/main/
│   │   ├── java/com/superai/workbench/
│   │   │   ├── data/
│   │   │   │   ├── local/ConfigManager.java
│   │   │   │   ├── model/AIModel.java, ChatMessage.java
│   │   │   │   └── network/AIModelManager.java
│   │   │   ├── ui/
│   │   │   │   ├── ai/ChatFragment, PaintFragment, VideoFragment
│   │   │   │   ├── agent/AgentFragment
│   │   │   │   ├── dashboard/DashboardFragment
│   │   │   │   ├── im/IMFragment
│   │   │   │   ├── knowledge/KnowledgeFragment
│   │   │   │   ├── live/LiveFragment
│   │   │   │   ├── pay/PayFragment
│   │   │   │   ├── security/SecurityFragment
│   │   │   │   └── settings/SettingsActivity
│   │   │   ├── MainActivity.java
│   │   │   └── SuperAIApplication.java
│   │   └── res/...
│   ├── build.gradle
│   └── proguard-rules.pro
├── build.gradle
├── settings.gradle
└── gradle/wrapper/gradle-wrapper.properties
```

## 技术栈

- **UI**: Material Design 3, ViewBinding, BottomNavigation + DrawerLayout
- **网络**: OkHttp 4.12 + Retrofit 2.9 (SSE流式)
- **图片**: Glide 4.16
- **图表**: MPAndroidChart 3.1
- **安全**: EncryptedSharedPreferences, ProGuard, Certificate Pinning
- **JSON**: Gson 2.10

## License

MIT
