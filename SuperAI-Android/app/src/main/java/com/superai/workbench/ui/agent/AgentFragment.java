package com.superai.workbench.ui.agent;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.superai.workbench.R;
import com.superai.workbench.data.local.ConfigManager;
import com.superai.workbench.data.model.Agent;
import com.superai.workbench.data.model.CustomAgent;

import java.util.ArrayList;
import java.util.List;

/**
 * 智能体市场 Fragment
 * 支持搜索、分类筛选，整合官方智能体 + 用户自定义智能体
 * 自定义智能体支持创建、编辑、删除、导出分享、导入
 */
public class AgentFragment extends Fragment {

    private RecyclerView recyclerView;
    private AgentAdapter adapter;
    private TextInputEditText searchInput;
    private ChipGroup categoryChips;
    private LinearLayout emptyState;
    private FloatingActionButton fabCreate;

    private Agent[] officialAgents;
    private List<Agent> allAgents = new ArrayList<>();
    private List<Agent> filteredAgents = new ArrayList<>();
    private String currentCategory = "全部";
    private String currentSearch = "";

    private static final int REQUEST_IMPORT_FILE = 1001;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_agent, container, false);

        officialAgents = Agent.getAllAgents();
        reloadAgents();

        initViews(view);
        setupRecyclerView();
        setupSearch();
        setupCategoryFilter();
        setupFab();
        applyFilter();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 返回时刷新自定义智能体列表
        reloadAgents();
        applyFilter();
    }

    /**
     * 重新加载所有智能体（官方 + 自定义）
     */
    private void reloadAgents() {
        allAgents.clear();
        // 添加官方智能体
        for (Agent a : officialAgents) {
            allAgents.add(a);
        }
        // 添加自定义智能体
        List<CustomAgent> customAgents = ConfigManager.getInstance(requireContext()).getCustomAgents();
        for (CustomAgent ca : customAgents) {
            allAgents.add(ca.toAgent());
        }
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.agent_recycler);
        searchInput = view.findViewById(R.id.search_input);
        categoryChips = view.findViewById(R.id.category_chips);
        emptyState = view.findViewById(R.id.empty_state);
        fabCreate = view.findViewById(R.id.fab_create);
    }

    private void setupRecyclerView() {
        int spanCount = getResources().getConfiguration().smallestScreenWidthDp >= 600 ? 3 : 2;
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), spanCount));
        adapter = new AgentAdapter(
            filteredAgents.toArray(new Agent[0]),
            this::onAgentClick,
            this::onAgentLongClick
        );
        recyclerView.setAdapter(adapter);
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                currentSearch = s.toString().trim().toLowerCase();
                applyFilter();
            }
        });
    }

    private void setupCategoryFilter() {
        categoryChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                currentCategory = "全部";
            } else {
                Chip chip = group.findViewById(checkedIds.get(0));
                currentCategory = chip != null ? chip.getText().toString() : "全部";
            }
            applyFilter();
        });
    }

    private void setupFab() {
        fabCreate.setOnClickListener(v -> AgentEditActivity.startCreate(requireContext()));
    }

    private void applyFilter() {
        filteredAgents.clear();

        for (Agent agent : allAgents) {
            boolean categoryMatch = "全部".equals(currentCategory)
                    || ("官方".equals(currentCategory) && "官方".equals(agent.getCreator()))
                    || ("社区".equals(currentCategory) && "社区".equals(agent.getCreator()))
                    || ("自定义".equals(currentCategory) && "自定义".equals(agent.getCreator()))
                    || agent.getCategory().equals(currentCategory);

            boolean searchMatch = currentSearch.isEmpty()
                    || agent.getName().toLowerCase().contains(currentSearch)
                    || agent.getDescription().toLowerCase().contains(currentSearch)
                    || containsTag(agent.getTags(), currentSearch);

            if (categoryMatch && searchMatch) {
                filteredAgents.add(agent);
            }
        }

        adapter.updateAgents(filteredAgents.toArray(new Agent[0]));

        if (filteredAgents.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    private boolean containsTag(String[] tags, String query) {
        if (tags == null) return false;
        for (String tag : tags) {
            if (tag.toLowerCase().contains(query)) return true;
        }
        return false;
    }

    /**
     * 点击智能体卡片：跳转到AI对话并注入系统提示词
     */
    private void onAgentClick(Agent agent) {
        Bundle args = new Bundle();
        args.putString("agent_id", agent.getId());
        args.putString("agent_name", agent.getName());
        args.putString("agent_icon", agent.getIcon());
        args.putString("system_prompt", agent.getSystemPrompt());

        com.superai.workbench.ui.ai.ChatFragment chatFragment = new com.superai.workbench.ui.ai.ChatFragment();
        chatFragment.setArguments(args);

        requireActivity().getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, chatFragment)
            .addToBackStack(null)
            .commit();

        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav =
            requireActivity().findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_chat);
        }
    }

    /**
     * 长按智能体卡片：显示操作菜单
     */
    private void onAgentLongClick(Agent agent, View view) {
        if (!"自定义".equals(agent.getCreator())) {
            // 官方/社区智能体不支持编辑删除
            return;
        }

        String[] options = {"编辑", "删除", "导出分享", "取消"};
        new AlertDialog.Builder(requireContext())
            .setTitle(agent.getName())
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // 编辑
                        AgentEditActivity.startEdit(requireContext(), agent.getId());
                        break;
                    case 1: // 删除
                        new AlertDialog.Builder(requireContext())
                            .setTitle("确认删除")
                            .setMessage("确定要删除智能体「" + agent.getName() + "」吗？")
                            .setPositiveButton("删除", (d, w) -> {
                                ConfigManager.getInstance(requireContext()).removeCustomAgent(agent.getId());
                                reloadAgents();
                                applyFilter();
                                Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("取消", null)
                            .show();
                        break;
                    case 2: // 导出分享
                        shareAgent(agent);
                        break;
                }
            })
            .show();
    }

    /**
     * 导出分享自定义智能体为JSON
     */
    private void shareAgent(Agent agent) {
        CustomAgent customAgent = ConfigManager.getInstance(requireContext()).getCustomAgent(agent.getId());
        if (customAgent == null) return;

        String json = ConfigManager.getInstance(requireContext()).exportCustomAgentsJson();
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/json");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "SuperAI智能体分享: " + agent.getName());
        shareIntent.putExtra(Intent.EXTRA_TEXT, json);
        startActivity(Intent.createChooser(shareIntent, "分享智能体"));
    }

    /**
     * 导入智能体（通过系统文件选择器）
     */
    public void importAgent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent, REQUEST_IMPORT_FILE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMPORT_FILE && resultCode == androidx.appcompat.app.AppCompatActivity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    StringBuilder sb = new StringBuilder();
                    try (java.io.InputStream is = requireContext().getContentResolver().openInputStream(uri);
                         java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                    }
                    int imported = ConfigManager.getInstance(requireContext()).importCustomAgentsJson(sb.toString());
                    if (imported > 0) {
                        reloadAgents();
                        applyFilter();
                        Toast.makeText(requireContext(), "成功导入 " + imported + " 个智能体", Toast.LENGTH_SHORT).show();
                    } else if (imported == 0) {
                        Toast.makeText(requireContext(), "文件中没有有效的智能体", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "导入失败，请检查文件格式", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "导入失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
