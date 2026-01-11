package com.geoping.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.geoping.datacollection.R;
import com.geoping.app.models.Message;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter para lista de mensagens
 */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Message> messages = new ArrayList<>();

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    public void addMessage(Message message) {
        // Evitar duplicação
        for (Message m : messages) {
            if (m.getId() == message.getId()) {
                return;
            }
        }
        messages.add(message);
        notifyItemInserted(messages.size() - 1); // Notifica inserção específica
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutMine;
        LinearLayout layoutOther;
        TextView textViewContentMine;
        TextView textViewTimeMine;
        TextView textViewSenderName;
        TextView textViewContentOther;
        TextView textViewTimeOther;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutMine = itemView.findViewById(R.id.layoutMine);
            layoutOther = itemView.findViewById(R.id.layoutOther);
            textViewContentMine = itemView.findViewById(R.id.textViewContentMine);
            textViewTimeMine = itemView.findViewById(R.id.textViewTimeMine);
            textViewSenderName = itemView.findViewById(R.id.textViewSenderName);
            textViewContentOther = itemView.findViewById(R.id.textViewContentOther);
            textViewTimeOther = itemView.findViewById(R.id.textViewTimeOther);
        }

        void bind(Message message) {
            if (message.isMine()) {
                // Mensagem própria
                layoutMine.setVisibility(View.VISIBLE);
                layoutOther.setVisibility(View.GONE);
                textViewContentMine.setText(message.getContent());
                textViewTimeMine.setText(formatTime(message.getSentAt()));
            } else {
                // Mensagem de outro usuário
                layoutMine.setVisibility(View.GONE);
                layoutOther.setVisibility(View.VISIBLE);
                textViewSenderName.setText(message.getSenderUsername());
                textViewContentOther.setText(message.getContent());
                textViewTimeOther.setText(formatTime(message.getSentAt()));
            }
        }

        private String formatTime(String timestamp) {
            try {
                SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                SimpleDateFormat sdfOutput = new SimpleDateFormat("HH:mm", Locale.getDefault());
                Date date = sdfInput.parse(timestamp);
                return date != null ? sdfOutput.format(date) : "";
            } catch (ParseException e) {
                return "";
            }
        }
    }
}

