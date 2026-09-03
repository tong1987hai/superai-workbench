package com.superai.workbench.ui.agent;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.superai.workbench.R;
import com.superai.workbench.data.model.Agent;

public class AgentAdapter extends RecyclerView.Adapter<AgentAdapter.ViewHolder> {

    private Agent[] agents;
    private final OnAgentClickListener clickListener;
    private final OnAgentLongClickListener longClickListener;

    public AgentAdapter(Agent[] agents, OnAgentClickListener clickListener, OnAgentLongClickListener longClickListener) {
        this.agents = agents;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_agent, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Agent agent = agents[position];
        holder.icon.setText(agent.getIcon());
        holder.name.setText(agent.getName());
        holder.desc.setText(agent.getDescription());
        holder.calls.setText(String.format("%,d次调用", agent.getCalls()));
        holder.rating.setText(String.format("★ %.1f", agent.getRating()));

        // Join tags
        StringBuilder tagsBuilder = new StringBuilder();
        for (String tag : agent.getTags()) {
            if (tagsBuilder.length() > 0) tagsBuilder.append(" · ");
            tagsBuilder.append(tag);
        }
        holder.tags.setText(tagsBuilder.toString());

        // Badge: 官方/自定义
        if ("官方".equals(agent.getCreator())) {
            holder.badge.setVisibility(View.VISIBLE);
            holder.badge.setText("官方");
            holder.badge.setChipBackgroundColorResource(R.color.primary);
        } else if ("自定义".equals(agent.getCreator())) {
            holder.badge.setVisibility(View.VISIBLE);
            holder.badge.setText("自定义");
            holder.badge.setChipBackgroundColorResource(R.color.success);
        } else {
            holder.badge.setVisibility(View.GONE);
        }

        holder.card.setOnClickListener(v -> clickListener.onAgentClick(agent));
        holder.card.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onAgentLongClick(agent, v);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return agents.length;
    }

    public void updateAgents(Agent[] newAgents) {
        this.agents = newAgents;
        notifyDataSetChanged();
    }

    public interface OnAgentClickListener {
        void onAgentClick(Agent agent);
    }

    public interface OnAgentLongClickListener {
        void onAgentLongClick(Agent agent, View view);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView icon, name, desc, calls, rating, tags, badge;

        ViewHolder(View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.agent_card);
            icon = itemView.findViewById(R.id.agent_icon);
            name = itemView.findViewById(R.id.agent_name);
            desc = itemView.findViewById(R.id.agent_desc);
            calls = itemView.findViewById(R.id.agent_calls);
            rating = itemView.findViewById(R.id.agent_rating);
            tags = itemView.findViewById(R.id.agent_tags);
            badge = itemView.findViewById(R.id.agent_badge);
        }
    }
}
