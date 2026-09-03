package com.superai.workbench.data.model;

import java.util.UUID;

/**
 * 自定义智能体数据模型（可序列化为JSON用于导出分享）
 */
public class CustomAgent {
    private String id;
    private String name;
    private String icon;
    private String description;
    private String systemPrompt;
    private String[] tags;
    private String category;
    private long createdAt;
    private long updatedAt;

    public CustomAgent() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public CustomAgent(String name, String icon, String description,
                       String systemPrompt, String[] tags, String category) {
        this();
        this.name = name;
        this.icon = icon;
        this.description = description;
        this.systemPrompt = systemPrompt;
        this.tags = tags;
        this.category = category;
    }

    // 转换为内置Agent模型（用于市场展示和对话）
    public Agent toAgent() {
        return new Agent(
            id, name, icon, description,
            0, 0.0, "自定义", tags, category, systemPrompt
        );
    }

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    public String[] getTags() { return tags; }
    public void setTags(String[] tags) { this.tags = tags; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
