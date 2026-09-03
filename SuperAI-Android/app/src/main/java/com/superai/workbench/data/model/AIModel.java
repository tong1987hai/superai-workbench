package com.superai.workbench.data.model;

/**
 * AI 大模型配置数据模型
 * 整合 DeepSeek / 豆包 / GPT-4o / 通义 / Kimi / Claude
 * vipRequired: 0=免费可用, 1=月会员+, 2=年会员+, 3=终身会员
 */
public class AIModel {
    private String id;
    private String name;
    private String provider;
    private String icon;
    private String description;
    private String baseUrl;
    private String modelName;
    private int maxTokens;
    private boolean supportsStream;
    private int vipRequired;
    private String apiKeyEnv;  // 环境变量名或配置键名

    public AIModel(String id, String name, String provider, String icon,
                   String description, String baseUrl, String modelName,
                   int maxTokens, boolean supportsStream, int vipRequired) {
        this.id = id;
        this.name = name;
        this.provider = provider;
        this.icon = icon;
        this.description = description;
        this.baseUrl = baseUrl;
        this.modelName = modelName;
        this.maxTokens = maxTokens;
        this.supportsStream = supportsStream;
        this.vipRequired = vipRequired;
    }

    // 带 apiKeyEnv 的完整构造器
    public AIModel(String id, String name, String provider, String icon,
                   String description, String baseUrl, String modelName,
                   int maxTokens, boolean supportsStream, int vipRequired,
                   String apiKeyEnv) {
        this(id, name, provider, icon, description, baseUrl, modelName,
             maxTokens, supportsStream, vipRequired);
        this.apiKeyEnv = apiKeyEnv;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getProvider() { return provider; }
    public String getIcon() { return icon; }
    public String getDescription() { return description; }
    public String getBaseUrl() { return baseUrl; }
    public String getModelName() { return modelName; }
    public int getMaxTokens() { return maxTokens; }
    public boolean isSupportsStream() { return supportsStream; }
    public int getVipRequired() { return vipRequired; }
    public String getApiKeyEnv() { return apiKeyEnv != null ? apiKeyEnv : ""; }

    // Setters (用于动态配置)
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public void setApiKeyEnv(String apiKeyEnv) { this.apiKeyEnv = apiKeyEnv; }

    /**
     * 预定义的模型配置
     * 免费版可用：DeepSeek / 豆包 / 通义
     * 月会员+可用：GPT-4o / Kimi
     * 终身会员可用：Claude-3.5
     */
    public static AIModel[] getAllModels() {
        return new AIModel[] {
            new AIModel("deepseek-v3", "DeepSeek-V3", "DeepSeek", "🔮",
                "深度求索最新通用大模型，推理能力强",
                "https://api.deepseek.com/v1", "deepseek-chat", 8192, true, 0,
                "deepseek_api_key"),
            new AIModel("doubao-pro", "豆包Pro", "ByteDance", "🌋",
                "字节跳动豆包大模型，中文场景优化",
                "https://ark.cn-beijing.volces.com/api/v3", "doubao-pro-32k-241215", 4096, true, 0,
                "doubao_api_key"),
            new AIModel("qwen-max", "通义千问", "Alibaba", "🐉",
                "阿里云通义千问最强版本",
                "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-max", 8192, true, 0,
                "qwen_api_key"),
            new AIModel("gpt-4o", "GPT-4o", "OpenAI", "🧠",
                "OpenAI 多模态旗舰模型",
                "https://api.openai.com/v1", "gpt-4o", 8192, true, 1,
                "openai_api_key"),
            new AIModel("kimi-k1.5", "Kimi-k1.5", "Moonshot", "🌙",
                "月之暗面超长上下文模型",
                "https://api.moonshot.cn/v1", "kimi-k1.5", 200000, true, 1,
                "kimi_api_key"),
            new AIModel("claude-3.5", "Claude-3.5", "Anthropic", "💎",
                "Anthropic 安全优先模型",
                "https://api.anthropic.com/v1", "claude-3-5-sonnet-20241022", 8192, true, 3,
                "claude_api_key")
        };
    }
}
