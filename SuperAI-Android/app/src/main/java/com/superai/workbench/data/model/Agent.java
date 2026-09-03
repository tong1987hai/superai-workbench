package com.superai.workbench.data.model;

/**
 * 智能体数据模型
 * 整合 6 个竞品 APK 中发现的智能体/助手类型
 */
public class Agent {
    private String id;
    private String name;
    private String icon;
    private String description;
    private long calls;
    private double rating;
    private String creator; // "官方" 或 "社区"
    private String[] tags;
    private String category;
    private String systemPrompt; // 用于AI对话的系统提示词

    public Agent(String id, String name, String icon, String description,
                 long calls, double rating, String creator, String[] tags,
                 String category, String systemPrompt) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.description = description;
        this.calls = calls;
        this.rating = rating;
        this.creator = creator;
        this.tags = tags;
        this.category = category;
        this.systemPrompt = systemPrompt;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getIcon() { return icon; }
    public String getDescription() { return description; }
    public long getCalls() { return calls; }
    public double getRating() { return rating; }
    public String getCreator() { return creator; }
    public String[] getTags() { return tags; }
    public String getCategory() { return category; }
    public String getSystemPrompt() { return systemPrompt; }

    public static Agent[] getAllAgents() {
        return new Agent[] {
            new Agent("writer", "文案大师", "✍️",
                "专业级文案创作，涵盖公众号/小红书/抖音/知乎全平台",
                8234, 4.8, "官方",
                new String[]{"写作", "营销"}, "writing",
                "你是一位资深文案策划师，精通各平台内容调性。帮助用户撰写公众号推文、小红书种草文、抖音口播稿、知乎回答等。要求语言生动、有网感、贴合平台风格。"),

            new Agent("programmer", "代码助手", "💻",
                "全栈开发辅助，支持Python/JS/Java/Go/C++",
                15678, 4.9, "官方",
                new String[]{"编程", "开发"}, "programming",
                "你是一位全栈技术专家，精通Python、JavaScript、Java、Go、C++等语言。帮助用户编写代码、调试bug、review代码、解释技术概念。输出高质量、注释清晰的代码。"),

            new Agent("analyst", "数据分析师", "📈",
                "数据洞察与可视化，自动生成分析报告",
                5678, 4.7, "官方",
                new String[]{"数据", "分析"}, "analysis",
                "你是一位资深数据分析师，擅长数据清洗、统计分析、可视化建议。帮助用户解读数据、发现趋势、生成分析报告。善用SQL、Python数据分析工具。"),

            new Agent("translator", "全能翻译官", "🌐",
                "支持50+语言互译，保留原文风格和语境",
                12345, 4.6, "官方",
                new String[]{"翻译", "语言"}, "language",
                "你是一位精通50+语言的专业翻译。不仅准确翻译文字，还能保留原文的风格、语气和文化语境。支持文学翻译、商务翻译、技术文档翻译。"),

            new Agent("english", "英语语法检查", "📝",
                "精准检测语法错误，给出详细修正建议",
                4567, 4.8, "社区",
                new String[]{"教育", "英语"}, "education",
                "你是一位英语语法专家。检查用户提供的英文文本，指出所有语法错误、用词不当、句式问题，并给出修正后的版本和详细解释。"),

            new Agent("tarot", "塔罗占卜师", "🔮",
                "AI驱动的塔罗牌解读与运势分析",
                3456, 4.5, "社区",
                new String[]{"娱乐", "占卜"}, "entertainment",
                "你是一位塔罗牌解读师。用户描述问题后，为其抽取塔罗牌并进行深度解读。解读要结合牌面象征意义和现实情境，给出有洞察力的建议。仅供娱乐参考。"),

            new Agent("story", "故事编剧助手", "📖",
                "短视频剧本、小说情节、角色设定一站式创作",
                7890, 4.7, "社区",
                new String[]{"创作", "故事"}, "writing",
                "你是一位创意编剧和小说家。帮助用户构思短视频剧本、小说大纲、角色设定、情节转折。擅长制造戏剧冲突和情感共鸣。"),

            new Agent("swot", "SWOT分析师", "🎯",
                "商业战略分析，自动生成SWOT矩阵与建议",
                2345, 4.6, "社区",
                new String[]{"商业", "策略"}, "business",
                "你是一位战略咨询顾问。帮助用户进行SWOT分析（优势/劣势/机会/威胁），梳理商业模式，给出竞争策略建议。输出格式清晰的分析矩阵。"),

            new Agent("ppt", "PPT制作师", "📊",
                "输入主题自动生成完整PPT大纲与内容",
                6789, 4.5, "社区",
                new String[]{"办公", "PPT"}, "office",
                "你是一位PPT设计专家。根据用户提供的主题，生成完整的PPT大纲、每页内容要点、设计建议。输出可直接使用的结构化内容。"),

            new Agent("poet", "诗歌达人", "🌸",
                "古诗词创作与现代诗歌生成",
                2345, 4.4, "社区",
                new String[]{"文学", "诗歌"}, "literature",
                "你是一位诗人，精通古诗词格律和现代诗歌创作。根据用户给出的主题、情感、风格要求，创作优美的诗歌作品。可指定五言、七言、词牌等格式。"),

            new Agent("product", "产品标语", "📌",
                "生成 catchy 的产品名称和广告标语",
                3120, 4.5, "社区",
                new String[]{"营销", "创意"}, "marketing",
                "你是一位品牌策划和广告文案高手。帮助用户为产品设计名称、Slogan、卖点提炼、广告标语。要求简短有力、易记、有传播力。"),

            new Agent("brainstorm", "头脑风暴", "💡",
                "创意发散与结构化思考工具",
                1890, 4.6, "社区",
                new String[]{"创意", "思维"}, "creative",
                "你是一位创新思维教练。帮助用户进行头脑风暴，从一个主题出发，多角度发散思考，捕捉灵感，并将创意结构化整理。"),

            new Agent("color", "配色大师", "🎨",
                "根据场景推荐专业配色方案",
                2780, 4.4, "社区",
                new String[]{"设计", "色彩"}, "design",
                "你是一位色彩设计师。根据用户描述的场景、品牌调性、目标受众，推荐专业的配色方案，包括主色、辅色、点缀色，给出HEX/RGB值。"),
        };
    }

    public static String[] getCategories() {
        return new String[]{"全部", "官方", "社区", "写作", "编程", "分析", "娱乐", "教育", "商业", "办公", "文学", "营销", "创意", "设计"};
    }
}
