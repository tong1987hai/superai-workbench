package com.superai.workbench.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.superai.workbench.R;
import com.superai.workbench.data.local.ConfigManager;
import com.superai.workbench.data.model.AIModel;
import com.superai.workbench.data.model.VIPLevel;

import java.util.Map;

/**
 * 设置页面
 * 1. API Key 管理（6大模型）
 * 2. 模型端点配置（Base URL / 模型名可自定义）
 * 3. 界面设置（主题/语言/默认模型）
 */
public class SettingsActivity extends AppCompatActivity {

    private ConfigManager configManager;
    private TextInputEditText etDeepseek, etOpenai, etQwen, etDoubao, etKimi, etClaude;
    private AutoCompleteTextView spinnerTheme, spinnerLanguage, spinnerDefaultModel;
    private LinearLayout modelConfigContainer;

    // 模型ID -> API Key 输入框 ID 映射
    private static final String[][] KEY_FIELDS = {
        {"deepseek-v3", "et_deepseek"},
        {"gpt-4o", "et_openai"},
        {"qwen-max", "et_qwen"},
        {"doubao-pro", "et_doubao"},
        {"kimi-k1.5", "et_kimi"},
        {"claude-3.5", "et_claude"}
    };

    public static void start(Context context) {
        context.startActivity(new Intent(context, SettingsActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.settings);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        configManager = ConfigManager.getInstance(this);
        initViews();
        loadConfig();
    }

    private void initViews() {
        etDeepseek = findViewById(R.id.et_deepseek);
        etOpenai = findViewById(R.id.et_openai);
        etQwen = findViewById(R.id.et_qwen);
        etDoubao = findViewById(R.id.et_doubao);
        etKimi = findViewById(R.id.et_kimi);
        etClaude = findViewById(R.id.et_claude);
        spinnerTheme = findViewById(R.id.spinner_theme);
        spinnerLanguage = findViewById(R.id.spinner_language);
        spinnerDefaultModel = findViewById(R.id.spinner_default_model);

        // 模型选择器
        AIModel[] models = AIModel.getAllModels();
        String[] modelNames = new String[models.length];
        for (int i = 0; i < models.length; i++) {
            modelNames[i] = models[i].getIcon() + " " + models[i].getName();
        }
        spinnerDefaultModel.setAdapter(new ArrayAdapter<>(this,
            android.R.layout.simple_dropdown_item_1line, modelNames));

        // 主题选项
        spinnerTheme.setAdapter(new ArrayAdapter<>(this,
            android.R.layout.simple_dropdown_item_1line,
            new String[]{"dark", "light", "auto"}));

        // 语言选项
        spinnerLanguage.setAdapter(new ArrayAdapter<>(this,
            android.R.layout.simple_dropdown_item_1line,
            new String[]{"zh", "en"}));

        // 构建模型端点配置卡片
        modelConfigContainer = findViewById(R.id.model_config_container);
        buildModelConfigCards();
    }

    /**
     * 动态构建每个模型的 Base URL 和模型名配置卡片
     */
    private void buildModelConfigCards() {
        modelConfigContainer.removeAllViews();
        AIModel[] models = AIModel.getAllModels();

        for (AIModel model : models) {
            View cardView = getLayoutInflater().inflate(R.layout.item_model_config, modelConfigContainer, false);

            TextView tvTitle = cardView.findViewById(R.id.tv_model_title);
            TextView tvVipTag = cardView.findViewById(R.id.tv_model_vip_tag);
            TextInputEditText etBaseUrl = cardView.findViewById(R.id.et_model_url);
            TextInputEditText etModelName = cardView.findViewById(R.id.et_model_name);
            Button btnReset = cardView.findViewById(R.id.btn_reset_model);

            tvTitle.setText(model.getIcon() + " " + model.getName());

            // VIP 标签
            String vipTag;
            if (model.getVipRequired() == 0) {
                vipTag = "免费可用";
            } else {
                vipTag = VIPLevel.getLevelName(model.getVipRequired()) + "+";
            }
            tvVipTag.setText(vipTag);

            // 加载当前配置
            String baseUrl = configManager.getModelBaseUrl(model.getId());
            String modelName = configManager.getModelName(model.getId());
            etBaseUrl.setText(baseUrl);
            etModelName.setText(modelName);

            // 设置 tag 用于保存时识别
            etBaseUrl.setTag("url_" + model.getId());
            etModelName.setTag("name_" + model.getId());

            // 重置按钮
            final String modelId = model.getId();
            final AIModel defaultModel = model;
            btnReset.setOnClickListener(v -> {
                etBaseUrl.setText(defaultModel.getBaseUrl());
                etModelName.setText(defaultModel.getModelName());
                configManager.resetModelConfig(modelId);
                Toast.makeText(this, model.getName() + " 已重置为默认配置", Toast.LENGTH_SHORT).show();
            });

            modelConfigContainer.addView(cardView);
        }
    }

    private void loadConfig() {
        // API Keys: 已保存的显示脱敏占位符
        for (String[] pair : KEY_FIELDS) {
            String key = configManager.getApiKey(pair[0]);
            TextInputEditText et = findViewById(getResources().getIdentifier(pair[1], "id", getPackageName()));
            if (et != null && !key.isEmpty()) {
                et.setHint(configManager.maskKey(key));
            }
        }

        // 主题
        String theme = configManager.getTheme();
        spinnerTheme.setText(theme, false);

        // 语言
        spinnerLanguage.setText(configManager.getLanguage(), false);

        // 默认模型
        String defaultModel = configManager.getDefaultModel();
        AIModel[] models = AIModel.getAllModels();
        for (AIModel model : models) {
            if (model.getId().equals(defaultModel)) {
                spinnerDefaultModel.setText(model.getIcon() + " " + model.getName(), false);
                break;
            }
        }
    }

    private void saveConfig() {
        // 保存API Keys（只保存非空且非脱敏的）
        for (String[] pair : KEY_FIELDS) {
            TextInputEditText et = findViewById(getResources().getIdentifier(pair[1], "id", getPackageName()));
            if (et != null) {
                String val = et.getText() != null ? et.getText().toString().trim() : "";
                if (!val.isEmpty() && !val.contains("****")) {
                    configManager.setApiKey(pair[0], val);
                }
            }
        }

        // 保存模型端点配置
        for (int i = 0; i < modelConfigContainer.getChildCount(); i++) {
            View child = modelConfigContainer.getChildAt(i);
            TextInputEditText etUrl = child.findViewById(R.id.et_model_url);
            TextInputEditText etName = child.findViewById(R.id.et_model_name);
            if (etUrl != null && etUrl.getTag() != null) {
                String urlTag = (String) etUrl.getTag();
                String modelId = urlTag.substring(4); // 去掉 "url_" 前缀
                String urlVal = etUrl.getText() != null ? etUrl.getText().toString().trim() : "";
                if (!urlVal.isEmpty()) {
                    configManager.setModelBaseUrl(modelId, urlVal);
                }
            }
            if (etName != null && etName.getTag() != null) {
                String nameTag = (String) etName.getTag();
                String modelId = nameTag.substring(5); // 去掉 "name_" 前缀
                String nameVal = etName.getText() != null ? etName.getText().toString().trim() : "";
                if (!nameVal.isEmpty()) {
                    configManager.setModelName(modelId, nameVal);
                }
            }
        }

        // 主题
        if (spinnerTheme.getText() != null) {
            configManager.setTheme(spinnerTheme.getText().toString());
        }

        // 语言
        if (spinnerLanguage.getText() != null) {
            configManager.setLanguage(spinnerLanguage.getText().toString());
        }

        // 默认模型
        String selectedModelText = spinnerDefaultModel.getText() != null ? spinnerDefaultModel.getText().toString() : "";
        AIModel[] models = AIModel.getAllModels();
        for (AIModel model : models) {
            if (selectedModelText.contains(model.getName())) {
                configManager.setDefaultModel(model.getId());
                break;
            }
        }

        Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            saveConfig();
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
