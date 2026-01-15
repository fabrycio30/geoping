package com.geoping.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.geoping.datacollection.R;

import org.json.JSONObject;

import java.util.List;

public class SubscriberAdapter extends RecyclerView.Adapter<SubscriberAdapter.ViewHolder> {

    private List<JSONObject> subscribers;

    public SubscriberAdapter(List<JSONObject> subscribers) {
        this.subscribers = subscribers;
    }

    public void updateData(List<JSONObject> newSubscribers) {
        this.subscribers = newSubscribers;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JSONObject sub = subscribers.get(position);
        String username = sub.optString("username", "Unknown");
        boolean isOnline = sub.optBoolean("is_online", false);
        double confidence = sub.optDouble("confidence", 0.0);

        // Bolinha status
        String statusIcon = isOnline ? "🟢" : "🔴";
        String statusText = isOnline 
            ? String.format("Online (%.0f%%)", confidence * 100) 
            : "Offline";

        holder.text1.setText(statusIcon + " " + username);
        holder.text2.setText(statusText);
    }

    @Override
    public int getItemCount() {
        return subscribers != null ? subscribers.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView text1;
        TextView text2;

        ViewHolder(View itemView) {
            super(itemView);
            text1 = itemView.findViewById(android.R.id.text1);
            text2 = itemView.findViewById(android.R.id.text2);
        }
    }
}
