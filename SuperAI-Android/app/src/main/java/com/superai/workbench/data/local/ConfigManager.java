package com.superai.workbench.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.superai.workbench.data.model.AIModel;
import com.superai.workbench.data.model.CustomAgent;
import com.superai.workbench.data.model.VIPLevel;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置管理器 - 使用 EncryptedSharedPreferences 安全存储 API Key
 * 整合 VIP 会员体系、模型动态配置、每日调用计数
 */
public class ConfigManager {
    private static final String PREFS_FILE = "superai_encrypted_prefs";
    private static final String KEY_API_PREFIX = "api_key_";
    private static final String KEY_THEME = "theme";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_DEFAULT_MODEL = "default_model";
    private static final String KEY_CUSTOM_AGENTS = "custom_agents";

    // VIP 相关
    private static final String KEY_VIP_LEVEL = "vip_level";
    private static final String KEY_VIP_EXPIRE = "vip_expire";
    private static final String KEY_DAILY_CALLS = "daily_calls";
    private static final String KEY_LAST_CALL_DATE = "last_call_date";

    // 模型动态配置
    private static final String KEY_MODEL_CONFIGS = "model_configs";
    private static final String KEY_MODEL_BASE_URL_PREFIX = "model_url_";
    private static final String KEY_MODEL_NAME_PREFIX = "model_name_";

    private static ConfigManager instance;
    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    private ConfigManager(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
            prefs = EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            prefs = context.getSharedPreferences(PREFS_FILE + "_fallback", Context.MODE_PRIVATE);
        }
    }

    public static synchronized ConfigManager getInstance(Context context) {
        if (instance == null) {
            instance = new ConfigManager(context.getApplicationContext());
        }
        return instance;
    }

    // ==================== API Key 管理 ====================
    public void setApiKey(String modelId, String apiKey) {
        prefs.edit().putString(KEY_API_PREFIX + modelId, apiKey).apply();
    }

    public String getApiKey(String modelId) {
        return prefs.getString(KEY_API_PREFIX + modelId, "");
    }

    public boolean hasApiKey(String modelId) {
        return !getApiKey(modelId).isEmpty();
    }

    public void removeApiKey(String modelId) {
        prefs.edit().remove(KEY_API_PREFIX + modelId).apply();
    }

    // ==================== 模型动态配置 ====================

    /**
     * 保存模型的自定义 Base URL
     */
    public void setModelBaseUrl(String modelId, String url) {
        prefs.edit().putString(KEY_MODEL_BASE_URL_PREFIX + modelId, url).apply();
    }

    /**
     * 获取模型的 Base URL（优先自定义，回退默认）
     */
    public String getModelBaseUrl(String modelId) {
        String custom = prefs.getString(KEY_MODEL_BASE_URL_PREFIX + modelId, "");
        if (!custom.isEmpty()) return custom;
        // 回退到默认配置
        for (AIModel m : AIModel.getAllModels()) {
            if (m.getId().equals(modelId)) return m.getBaseUrl();
        }
        return "";
    }

    /**
     * 保存模型的自定义模型名
     */
    public void setModelName(String modelId, String name) {
        prefs.edit().putString(KEY_MODEL_NAME_PREFIX + modelId, name).apply();
    }

    /**
     * 获取模型的实际模型名（优先自定义，回退默认）
     */
    public String getModelName(String modelId) {
        String custom = prefs.getString(KEY_MODEL_NAME_PREFIX + modelId, "");
        if (!custom.isEmpty()) return custom;
        for (AIModel m : AIModel.getAllModels()) {
            if (m.getId().equals(modelId)) return m.getModelName();
        }
        return "";
    }

    /**
     * 重置模型配置为默认
     */
    public void resetModelConfig(String modelId) {
        prefs.edit()
            .remove(KEY_MODEL_BASE_URL_PREFIX + modelId)
            .remove(KEY_MODEL_NAME_PREFIX + modelId)
            .apply();
    }

    /**
     * 获取所有模型的动态配置状态
     * @return modelId -> [baseUrl, modelName, hasApiKey]
     */
    public Map<String, String[]> getAllModelConfigs() {
        Map<String, String[]> configs = new HashMap<>();
        for (AIModel m : AIModel.getAllModels()) {
            String baseUrl = getModelBaseUrl(m.getId());
            String modelName = getModelName(m.getId());
            String apiKey = getApiKey(m.getId());
            String maskedKey = apiKey.isEmpty() ? "" : maskKey(apiKey);
            configs.put(m.getId(), new String[]{baseUrl, modelName, maskedKey});
        }
        return configs;
    }

    // ==================== VIP 会员管理 ====================

    public int getVipLevel() {
        if (isVipExpired()) {
            return VIPLevel.FREE;
        }
        return prefs.getInt(KEY_VIP_LEVEL, VIPLevel.FREE);
    }

    public void setVipLevel(int level) {
        prefs.edit().putInt(KEY_VIP_LEVEL, level).apply();
    }

    public long getVipExpire() {
        return prefs.getLong(KEY_VIP_EXPIRE, 0);
    }

    public void setVipExpire(long timestamp) {
        prefs.edit().putLong(KEY_VIP_EXPIRE, timestamp).apply();
    }

    public boolean isVipExpired() {
        long expire = getVipExpire();
        if (expire == 0) return true; // 从未购买
        // 终身会员永不过期
        if (getVipLevelRaw() == VIPLevel.LIFETIME) return false;
        return System.currentTimeMillis() > expire;
    }

    private int getVipLevelRaw() {
        return prefs.getInt(KEY_VIP_LEVEL, VIPLevel.FREE);
    }

    /**
     * 购买/激活会员
     * @param level VIPLevel.FREE / MONTHLY / YEARLY / LIFETIME
     */
    public void purchaseVip(int level) {
        setVipLevel(level);
        Calendar cal = Calendar.getInstance();
        switch (level) {
            case VIPLevel.MONTHLY:
                cal.add(Calendar.MONTH, 1);
                setVipExpire(cal.getTimeInMillis());
                break;
            case VIPLevel.YEARLY:
                cal.add(Calendar.YEAR, 1);
                setVipExpire(cal.getTimeInMillis());
                break;
            case VIPLevel.LIFETIME:
                setVipExpire(Long.MAX_VALUE);
                break;
            case VIPLevel.FREE:
            default:
                setVipExpire(0);
                break;
        }
    }

    public String getVipLevelName() {
        return VIPLevel.getLevelName(getVipLevel());
    }

    public String getVipBadge() {
        return VIPLevel.getLevelBadge(getVipLevel());
    }

    /**
     * 获取VIP剩余天数
     */
    public int getVipRemainingDays() {
        if (getVipLevel() == VIPLevel.LIFETIME) return Integer.MAX_VALUE;
        long expire = getVipExpire();
        if (expire == 0) return 0;
        long remaining = expire - System.currentTimeMillis();
        if (remaining <= 0) return 0;
        return (int) (remaining / (24 * 60 * 60 * 1000));
    }

    // ==================== 每日调用计数 ====================

    /**
     * 获取今日已调用次数
     */
    public int getDailyCalls() {
        checkAndResetDaily();
        return prefs.getInt(KEY_DAILY_CALLS, 0);
    }

    /**
     * 增加一次调用计数
     */
    public void incrementDailyCalls() {
        checkAndResetDaily();
        int calls = prefs.getInt(KEY_DAILY_CALLS, 0);
        prefs.edit().putInt(KEY_DAILY_CALLS, calls + 1).apply();
    }

    /**
     * 检查是否还可以调用
     */
    public boolean canCall() {
        int level = getVipLevel();
        VIPLevel vip = VIPLevel.getAllLevels()[level];
        if (vip.getMaxDailyCalls() == Integer.MAX_VALUE) return true;
        return getDailyCalls() < vip.getMaxDailyCalls();
    }

    /**
     * 检查并重置每日计数（跨天自动归零）
     */
    private void checkAndResetDaily() {
        String today = getTodayStr();
        String lastDate = prefs.getString(KEY_LAST_CALL_DATE, "");
        if (!today.equals(lastDate)) {
            prefs.edit()
                .putInt(KEY_DAILY_CALLS, 0)
                .putString(KEY_LAST_CALL_DATE, today)
                .apply();
        }
    }

    private String getTodayStr() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH);
    }

    // ==================== 通用配置 ====================
    public void setTheme(String theme) {
        prefs.edit().putString(KEY_THEME, theme).apply();
    }

    public String getTheme() {
        return prefs.getString(KEY_THEME, "dark");
    }

    public void setLanguage(String language) {
        prefs.edit().putString(KEY_LANGUAGE, language).apply();
    }

    public String getLanguage() {
        return prefs.getString(KEY_LANGUAGE, "zh");
    }

    public void setDefaultModel(String modelId) {
        prefs.edit().putString(KEY_DEFAULT_MODEL, modelId).apply();
    }

    public String getDefaultModel() {
        return prefs.getString(KEY_DEFAULT_MODEL, "deepseek-v3");
    }

    // ==================== 自定义智能体管理 ====================
    public void saveCustomAgent(CustomAgent agent) {
        List<CustomAgent> agents = getCustomAgents();
        boolean found = false;
        for (int i = 0; i < agents.size(); i++) {
            if (agents.get(i).getId().equals(agent.getId())) {
                agents.set(i, agent);
                found = true;
                break;
            }
        }
        if (!found) {
            agents.add(agent);
        }
        String json = gson.toJson(agents);
        prefs.edit().putString(KEY_CUSTOM_AGENTS, json).apply();
    }

    public List<CustomAgent> getCustomAgents() {
        String json = prefs.getString(KEY_CUSTOM_AGENTS, "[]");
        Type type = new TypeToken<List<CustomAgent>>(){}.getType();
        List<CustomAgent> agents = gson.fromJson(json, type);
        return agents != null ? agents : new ArrayList<>();
    }

    public CustomAgent getCustomAgent(String id) {
        for (CustomAgent agent : getCustomAgents()) {
            if (agent.getId().equals(id)) {
                return agent;
            }
        }
        return null;
    }

    public void removeCustomAgent(String id) {
        List<CustomAgent> agents = getCustomAgents();
        for (int i = 0; i < agents.size(); i++) {
            if (agents.get(i).getId().equals(id)) {
                agents.remove(i);
                break;
            }
        }
        String json = gson.toJson(agents);
        prefs.edit().putString(KEY_CUSTOM_AGENTS, json).apply();
    }

    public String exportCustomAgentsJson() {
        return gson.toJson(getCustomAgents());
    }

    public int importCustomAgentsJson(String json) {
        try {
            Type type = new TypeToken<List<CustomAgent>>(){}.getType();
            List<CustomAgent> imported = gson.fromJson(json, type);
            if (imported == null || imported.isEmpty()) return 0;
            List<CustomAgent> existing = getCustomAgents();
            int added = 0;
            for (CustomAgent agent : imported) {
                agent.setId(java.util.UUID.randomUUID().toString());
                agent.setCreatedAt(System.currentTimeMillis());
                existing.add(agent);
                added++;
            }
            prefs.edit().putString(KEY_CUSTOM_AGENTS, gson.toJson(existing)).apply();
            return added;
        } catch (Exception e) {
            return -1;
        }
    }

    // ==================== 工具方法 ====================
    public String maskKey(String key) {
        if (key == null || key.length() < 13) return "";
        return key.substring(0, 8) + "****" + key.substring(key.length() - 4);
    }
}
