package com.superai.workbench;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * SuperAI Workbench 应用入口
 * 初始化主题、加密存储、网络层等全局组件
 */
public class SuperAIApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        // 根据配置初始化主题
        String theme = ConfigManager.getInstance(this).getTheme();
        if ("dark".equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if ("light".equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }
}
