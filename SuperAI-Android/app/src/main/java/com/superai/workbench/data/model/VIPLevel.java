package com.superai.workbench.data.model;

/**
 * VIP 会员等级数据模型
 * 4级体系：免费 / 月会员 / 年会员 / 终身会员
 */
public class VIPLevel {
    public static final int FREE = 0;
    public static final int MONTHLY = 1;
    public static final int YEARLY = 2;
    public static final int LIFETIME = 3;

    private int level;
    private String name;
    private double price;
    private String period;
    private String desc;
    private String[] features;
    private int maxDailyCalls;
    private boolean allowCustomModels;
    private boolean allowImageGen;
    private boolean allowVideoGen;
    private boolean allowAllModels;

    public VIPLevel(int level, String name, double price, String period,
                    String desc, String[] features, int maxDailyCalls,
                    boolean allowCustomModels, boolean allowImageGen,
                    boolean allowVideoGen, boolean allowAllModels) {
        this.level = level;
        this.name = name;
        this.price = price;
        this.period = period;
        this.desc = desc;
        this.features = features;
        this.maxDailyCalls = maxDailyCalls;
        this.allowCustomModels = allowCustomModels;
        this.allowImageGen = allowImageGen;
        this.allowVideoGen = allowVideoGen;
        this.allowAllModels = allowAllModels;
    }

    public int getLevel() { return level; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public String getPeriod() { return period; }
    public String getDesc() { return desc; }
    public String[] getFeatures() { return features; }
    public int getMaxDailyCalls() { return maxDailyCalls; }
    public boolean isAllowCustomModels() { return allowCustomModels; }
    public boolean isAllowImageGen() { return allowImageGen; }
    public boolean isAllowVideoGen() { return allowVideoGen; }
    public boolean isAllowAllModels() { return allowAllModels; }

    /**
     * 判断当前VIP等级是否有权限使用某个模型
     * @param modelVipRequired 模型要求的最低VIP等级
     */
    public boolean canUseModel(int modelVipRequired) {
        return this.level >= modelVipRequired;
    }

    public static VIPLevel[] getAllLevels() {
        return new VIPLevel[] {
            new VIPLevel(FREE, "免费版", 0, "",
                "每日50次基础AI调用",
                new String[]{"DeepSeek/豆包/通义免费使用", "每日50次AI对话", "社区智能体", "基础绘画"},
                50, false, false, false, false),
            new VIPLevel(MONTHLY, "月会员", 29.9, "月",
                "解锁全部模型，无限调用",
                new String[]{"全部6大模型（含GPT-4o/Kimi）", "无限AI对话", "AI绘画（DALL-E 3）", "AI视频生成", "私有知识库"},
                Integer.MAX_VALUE, false, true, true, true),
            new VIPLevel(YEARLY, "年会员", 269, "年",
                "9折优惠，全年畅用",
                new String[]{"月会员全部权益", "自定义智能体", "团队协作", "高级数据分析", "优先客服"},
                Integer.MAX_VALUE, true, true, true, true),
            new VIPLevel(LIFETIME, "终身会员", 999, "终身",
                "一次购买，终身使用",
                new String[]{"年会员全部权益", "Claude-3.5企业级模型", "私有化部署支持", "模型微调", "7×24专属技术支持"},
                Integer.MAX_VALUE, true, true, true, true)
        };
    }

    public static String getLevelName(int level) {
        switch (level) {
            case FREE: return "免费版";
            case MONTHLY: return "月会员";
            case YEARLY: return "年会员";
            case LIFETIME: return "终身会员";
            default: return "免费版";
        }
    }

    public static String getLevelBadge(int level) {
        switch (level) {
            case FREE: return "🆓";
            case MONTHLY: return "⭐";
            case YEARLY: return "👑";
            case LIFETIME: return "💎";
            default: return "🆓";
        }
    }

    public static String getUpgradeHint(int requiredLevel) {
        switch (requiredLevel) {
            case MONTHLY: return "该模型需要月会员及以上权限";
            case YEARLY: return "该模型需要年会员及以上权限";
            case LIFETIME: return "该模型仅终身会员可用";
            default: return "";
        }
    }
}
