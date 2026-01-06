package com.geoping.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.geoping.datacollection.R;
import com.geoping.app.models.Conversation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter para lista de conversas
 */
public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {

    private List<Conversation> conversations = new ArrayList<>();
    private OnConversationClickListener listener;

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
    }

    public ConversationAdapter(OnConversationClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        Conversation conversation = conversations.get(position);
        holder.bind(conversation);
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    public void setConversations(List<Conversation> conversations) {
        this.conversations = conversations;
        notifyDataSetChanged();
    }

    public void addConversation(Conversation conversation) {
        conversations.add(0, conversation); // Adicionar no início
        notifyItemInserted(0);
    }

    class ConversationViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTitle;
        TextView textViewCreatedBy;
        TextView textViewMessageCount;
        TextView textViewCreatedAt;

        ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.textViewConversationTitle);
            textViewCreatedBy = itemView.findViewById(R.id.textViewCreatedBy);
            textViewMessageCount = itemView.findViewById(R.id.textViewMessageCount);
            textViewCreatedAt = itemView.findViewById(R.id.textViewCreatedAt);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onConversationClick(conversations.get(position));
                    }
                }
            });
        }

        void bind(Conversation conversation) {
            textViewTitle.setText(conversation.getTitle());
            textViewCreatedBy.setText("Criado por: " + conversation.getCreatorUsername());
            
            int count = conversation.getMessageCount();
            textViewMessageCount.setText(count + (count == 1 ? " mensagem" : " mensagens"));
            
            textViewCreatedAt.setText(formatRelativeTime(conversation.getCreatedAt()));
        }

        private String formatRelativeTime(String timestamp) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                Date date = sdf.parse(timestamp);
                if (date == null) return "agora";

                long diff = System.currentTimeMillis() - date.getTime();
                long seconds = diff / 1000;
                long minutes = seconds / 60;
                long hours = minutes / 60;
                long days = hours / 24;

                if (days > 0) return days + "d atrás";
                if (hours > 0) return hours + "h atrás";
                if (minutes > 0) return minutes + "m atrás";
                return "agora";

            } catch (ParseException e) {
                return "agora";
            }
        }
    }
}

