package com.superai.workbench.ui.ai;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.superai.workbench.R;
import com.superai.workbench.data.local.ConfigManager;
import com.superai.workbench.data.model.AIModel;
import com.superai.workbench.data.model.ChatMessage;
import com.superai.workbench.data.model.VIPLevel;
import com.superai.workbench.data.network.AIModelManager;

import java.util.ArrayList;
import java.util.List;

/**
 * AI对话Fragment - 支持6大模型切换的聊天界面
 * 整合 VIP 权限校验 + 每日调用计数
 */
public class ChatFragment extends Fragment {

    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private EditText messageInput;
    private ImageButton sendButton;
    private ProgressBar progressBar;
    private TextView tokenInfo;
    private AutoCompleteTextView modelSelector;

    private ConfigManager configManager;
    private AIModel[] models;
    private AIModel currentModel;
    private List<ChatMessage> messages = new ArrayList<>();

    private String agentSystemPrompt = null;
    private String agentName = null;
    private String agentIcon = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        configManager = ConfigManager.getInstance(requireContext());
        models = AIModel.getAllModels();
        currentModel = getModelById(configManager.getDefaultModel());

        parseAgentArgs();
        initViews(view);
        setupModelSelector(view);
        setupRecyclerView();
        setupInput();

        if (agentName != null) {
            showAgentWelcome();
        }

        return view;
    }

    private void parseAgentArgs() {
        Bundle args = getArguments();
        if (args != null) {
            agentSystemPrompt = args.getString("system_prompt", null);
            agentName = args.getString("agent_name", null);
            agentIcon = args.getString("agent_icon", null);
        }
    }

    private void showAgentWelcome() {
        String welcome = (agentIcon != null ? agentIcon : "🤖") + " " + agentName + "\n\n"
            + "你好！我是你的专属智能体助手。输入你的需求，我来帮你处理。";
        ChatMessage welcomeMsg = new ChatMessage(ChatMessage.TYPE_ASSISTANT, welcome);
        messages.add(welcomeMsg);
        adapter.notifyItemInserted(0);
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.chat_recycler);
        messageInput = view.findViewById(R.id.message_input);
        sendButton = view.findViewById(R.id.send_button);
        progressBar = view.findViewById(R.id.progress_bar);
        tokenInfo = view.findViewById(R.id.token_info);
        modelSelector = view.findViewById(R.id.model_selector);
    }

    private void setupModelSelector(View view) {
        int userLevel = configManager.getVipLevel();
        List<String> modelNames = new ArrayList<>();
        List<AIModel> availableModels = new ArrayList<>();

        for (AIModel model : models) {
            String vipTag = "";
            if (model.getVipRequired() > userLevel) {
                vipTag = " [" + VIPLevel.getLevelName(model.getVipRequired()) + "+]";
            }
            modelNames.add(model.getIcon() + " " + model.getName() + vipTag);
            availableModels.add(model);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
            android.R.layout.simple_dropdown_item_1line, modelNames);
        modelSelector.setAdapter(adapter);

        for (int i = 0; i < availableModels.size(); i++) {
            if (availableModels.get(i).getId().equals(currentModel.getId())) {
                modelSelector.setText(modelNames.get(i), false);
                break;
            }
        }

        modelSelector.setOnItemClickListener((parent, v, position, id) -> {
            AIModel selected = availableModels.get(position);
            // VIP 权限校验
            if (selected.getVipRequired() > configManager.getVipLevel()) {
                String hint = VIPLevel.getUpgradeHint(selected.getVipRequired());
                Toast.makeText(requireContext(),
                    hint + "\n请前往支付中心升级会员",
                    Toast.LENGTH_LONG).show();
                // 恢复之前的选择
                return;
            }
            currentModel = selected;
            configManager.setDefaultModel(currentModel.getId());
            updateTokenInfo();
        });

        updateTokenInfo();
    }

    private void updateTokenInfo() {
        int level = configManager.getVipLevel();
        VIPLevel vip = VIPLevel.getAllLevels()[level];
        if (vip.getMaxDailyCalls() == Integer.MAX_VALUE) {
            tokenInfo.setText("模型: " + currentModel.getName() + " | " + vip.getName() + " 无限调用");
        } else {
            int used = configManager.getDailyCalls();
            tokenInfo.setText("模型: " + currentModel.getName() + " | 今日 " + used + "/" + vip.getMaxDailyCalls());
        }
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ChatAdapter(messages);
        recyclerView.setAdapter(adapter);
    }

    private void setupInput() {
        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        if (text.isEmpty()) return;

        // 1. VIP 权限校验
        if (currentModel.getVipRequired() > configManager.getVipLevel()) {
            Toast.makeText(requireContext(),
                VIPLevel.getUpgradeHint(currentModel.getVipRequired()) + "\n请前往支付中心升级",
                Toast.LENGTH_LONG).show();
            return;
        }

        // 2. 每日调用次数检查
        if (!configManager.canCall()) {
            Toast.makeText(requireContext(),
                "今日免费调用次数已用完（50次/天）\n升级会员即可无限使用",
                Toast.LENGTH_LONG).show();
            return;
        }

        // 3. API Key 检查
        if (!configManager.hasApiKey(currentModel.getId())) {
            Toast.makeText(requireContext(),
                "请先在设置中配置 " + currentModel.getName() + " 的 API Key",
                Toast.LENGTH_LONG).show();
            return;
        }

        // 添加用户消息
        ChatMessage userMsg = new ChatMessage(ChatMessage.TYPE_USER, text);
        messages.add(userMsg);
        adapter.notifyItemInserted(messages.size() - 1);
        recyclerView.scrollToPosition(messages.size() - 1);

        messageInput.setText("");
        progressBar.setVisibility(View.VISIBLE);
        sendButton.setEnabled(false);

        // 增加每日调用计数
        configManager.incrementDailyCalls();

        // 添加AI占位消息
        ChatMessage aiMsg = new ChatMessage(ChatMessage.TYPE_ASSISTANT, "");
        aiMsg.setStreaming(true);
        aiMsg.setModelId(currentModel.getId());
        messages.add(aiMsg);
        final int aiPosition = messages.size() - 1;
        adapter.notifyItemInserted(aiPosition);

        // 发送请求
        AIModelManager.getInstance().sendChatStream(
            configManager, currentModel, messages, agentSystemPrompt,
            new AIModelManager.ChatStreamCallback() {
                private final Handler mainHandler = new Handler(Looper.getMainLooper());
                private final StringBuilder fullContent = new StringBuilder();

                @Override
                public void onChunk(String content) {
                    mainHandler.post(() -> {
                        fullContent.append(content);
                        aiMsg.setContent(fullContent.toString());
                        adapter.notifyItemChanged(aiPosition);
                        recyclerView.scrollToPosition(aiPosition);
                    });
                }

                @Override
                public void onComplete() {
                    mainHandler.post(() -> {
                        aiMsg.setStreaming(false);
                        progressBar.setVisibility(View.GONE);
                        sendButton.setEnabled(true);
                        updateTokenInfo();
                    });
                }

                @Override
                public void onError(Exception e) {
                    mainHandler.post(() -> {
                        aiMsg.setContent("❌ 请求失败: " + e.getMessage());
                        aiMsg.setStreaming(false);
                        progressBar.setVisibility(View.GONE);
                        sendButton.setEnabled(true);
                        Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }
        );
    }

    private AIModel getModelById(String id) {
        for (AIModel model : models) {
            if (model.getId().equals(id)) return model;
        }
        return models[0];
    }

    @Override
    public void onResume() {
        super.onResume();
        if (tokenInfo != null) {
            updateTokenInfo();
        }
    }
}
