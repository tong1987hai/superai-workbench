package com.superai.workbench.ui.agent;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.GridLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.superai.workbench.R;
import com.superai.workbench.data.local.ConfigManager;
import com.superai.workbench.data.model.CustomAgent;

/**
 * 自定义智能体创建/编辑界面
 * 支持图标选择、名称/描述/系统提示词/标签/分类输入
 */
public class AgentEditActivity extends AppCompatActivity {

    private static final String EXTRA_AGENT_ID = "agent_id";
    private static final String[] ICONS = {
        "🤖", "✍️", "💻", "📈", "🌐", "🔮", "📖", "🎯", "📊", "📝",
        "🌸", "📌", "💡", "🎨", "🔍", "🧠", "🚀", "⚡", "🔥", "⭐"
    };
    private static final String[] CATEGORIES = {
        "writing", "programming", "analysis", "education", "entertainment",
        "business", "office", "literature", "marketing", "creative", "design"
    };

    private TextInputEditText etName, etDesc, etPrompt, etTags;
    private AutoCompleteTextView spinnerCategory;
    private GridLayout iconGrid;
    private String selectedIcon = "🤖";
    private String editingId = null;

    public static void startCreate(Context context) {
        context.startActivity(new Intent(context, AgentEditActivity.class));
    }

    public static void startEdit(Context context, String agentId) {
        Intent intent = new Intent(context, AgentEditActivity.class);
        intent.putExtra(EXTRA_AGENT_ID, agentId);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agent_edit);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("创建智能体");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initViews();
        setupIconGrid();
        setupCategorySpinner();

        // 检查是否是编辑模式
        editingId = getIntent().getStringExtra(EXTRA_AGENT_ID);
        if (editingId != null) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("编辑智能体");
            }
            loadAgentData(editingId);
        }
    }

    private void initViews() {
        etName = findViewById(R.id.et_name);
        etDesc = findViewById(R.id.et_desc);
        etPrompt = findViewById(R.id.et_prompt);
        etTags = findViewById(R.id.et_tags);
        spinnerCategory = findViewById(R.id.spinner_category);
        iconGrid = findViewById(R.id.icon_grid);
    }

    private void setupIconGrid() {
        for (String icon : ICONS) {
            android.widget.TextView tv = new android.widget.TextView(this);
            tv.setText(icon);
            tv.setTextSize(28);
            tv.setPadding(8, 8, 8, 8);
            tv.setBackgroundResource(R.drawable.bg_icon_unselected);
            tv.setOnClickListener(v -> {
                // 清除之前选中的样式
                for (int i = 0; i < iconGrid.getChildCount(); i++) {
                    iconGrid.getChildAt(i).setBackgroundResource(R.drawable.bg_icon_unselected);
                }
                selectedIcon = icon;
                tv.setBackgroundResource(R.drawable.bg_icon_selected);
            });
            if (icon.equals(selectedIcon)) {
                tv.setBackgroundResource(R.drawable.bg_icon_selected);
            }
            iconGrid.addView(tv);
        }
    }

    private void setupCategorySpinner() {
        String[] displayNames = {"写作", "编程", "分析", "教育", "娱乐", "商业", "办公", "文学", "营销", "创意", "设计"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_dropdown_item_1line, displayNames);
        spinnerCategory.setAdapter(adapter);
        spinnerCategory.setText(displayNames[0], false);
    }

    private void loadAgentData(String agentId) {
        CustomAgent agent = ConfigManager.getInstance(this).getCustomAgent(agentId);
        if (agent == null) return;
        etName.setText(agent.getName());
        etDesc.setText(agent.getDescription());
        etPrompt.setText(agent.getSystemPrompt());
        etTags.setText(String.join(",", agent.getTags()));
        selectedIcon = agent.getIcon();

        // 设置分类
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (CATEGORIES[i].equals(agent.getCategory())) {
                String[] displayNames = {"写作", "编程", "分析", "教育", "娱乐", "商业", "办公", "文学", "营销", "创意", "设计"};
                spinnerCategory.setText(displayNames[i], false);
                break;
            }
        }

        // 更新图标选中状态
        for (int i = 0; i < iconGrid.getChildCount(); i++) {
            android.widget.TextView tv = (android.widget.TextView) iconGrid.getChildAt(i);
            tv.setBackgroundResource(ICONS[i].equals(selectedIcon) ? R.drawable.bg_icon_selected : R.drawable.bg_icon_unselected);
        }
    }

    private void saveAgent() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String desc = etDesc.getText() != null ? etDesc.getText().toString().trim() : "";
        String prompt = etPrompt.getText() != null ? etPrompt.getText().toString().trim() : "";
        String tagsStr = etTags.getText() != null ? etTags.getText().toString().trim() : "";
        String categoryText = spinnerCategory.getText() != null ? spinnerCategory.getText().toString() : "写作";

        if (name.isEmpty()) {
            Toast.makeText(this, "请输入智能体名称", Toast.LENGTH_SHORT).show();
            return;
        }
        if (prompt.isEmpty()) {
            Toast.makeText(this, "请输入系统提示词", Toast.LENGTH_SHORT).show();
            return;
        }

        // 解析分类
        String[] displayNames = {"写作", "编程", "分析", "教育", "娱乐", "商业", "办公", "文学", "营销", "创意", "设计"};
        String category = CATEGORIES[0];
        for (int i = 0; i < displayNames.length; i++) {
            if (displayNames[i].equals(categoryText)) {
                category = CATEGORIES[i];
                break;
            }
        }

        String[] tags = tagsStr.isEmpty() ? new String[]{"自定义"} : tagsStr.split(",");
        for (int i = 0; i < tags.length; i++) {
            tags[i] = tags[i].trim();
        }

        CustomAgent agent = new CustomAgent(name, selectedIcon, desc, prompt, tags, category);
        if (editingId != null) {
            agent.setId(editingId);
            agent.setUpdatedAt(System.currentTimeMillis());
        }

        ConfigManager.getInstance(this).saveCustomAgent(agent);
        Toast.makeText(this, editingId != null ? "智能体已更新" : "智能体已创建", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        menu.findItem(R.id.action_save).setTitle("保存");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            saveAgent();
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
