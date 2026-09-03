package com.superai.workbench.data.model;

/**
 * 聊天消息数据模型
 */
public class ChatMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_ASSISTANT = 1;
    public static final int TYPE_SYSTEM = 2;
    
    private String id;
    private int type;
    private String content;
    private long timestamp;
    private String modelId;
    private boolean isStreaming;
    
    public ChatMessage(int type, String content) {
        this.type = type;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.id = String.valueOf(timestamp);
    }
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getType() { return type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public long getTimestamp() { return timestamp; }
    public String getModelId() { return modelId; }
    public void setModelId(String modelId) { this.modelId = modelId; }
    public boolean isStreaming() { return isStreaming; }
    public void setStreaming(boolean streaming) { isStreaming = streaming; }
}
