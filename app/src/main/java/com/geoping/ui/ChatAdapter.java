package com.geoping.ui;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.geoping.R;
import com.geoping.model.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter do RecyclerView para exibir mensagens de chat.
 * 
 * Este adapter gerencia a lista de mensagens e determina como cada mensagem
 * deve ser exibida (posição, cor, formatação) baseado em seu tipo.
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {
    
    private List<ChatMessage> messages;
    private SimpleDateFormat timeFormat;
    
    /**
     * Construtor do adapter.
     */
    public ChatAdapter() {
        this.messages = new ArrayList<>();
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }
    
    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.bind(message);
    }
    
    @Override
    public int getItemCount() {
        return messages.size();
    }
    
    /**
     * Atualiza a lista de mensagens e notifica o adapter.
     * 
     * @param newMessages Nova lista de mensagens
     */
    public void setMessages(List<ChatMessage> newMessages) {
        this.messages = newMessages;
        notifyDataSetChanged();
    }
    
    /**
     * ViewHolder para cada item de mensagem.
     */
    class MessageViewHolder extends RecyclerView.ViewHolder {
        
        private LinearLayout messageContainer;
        private TextView usernameText;
        private TextView messageText;
        private TextView timestampText;
        
        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            
            messageContainer = itemView.findViewById(R.id.messageContainer);
            usernameText = itemView.findViewById(R.id.usernameText);
            messageText = itemView.findViewById(R.id.messageText);
            timestampText = itemView.findViewById(R.id.timestampText);
        }
        
        /**
         * Vincula os dados da mensagem aos elementos da UI.
         * 
         * @param message Mensagem a ser exibida
         */
        void bind(ChatMessage message) {
            // Define o nome do usuário
            usernameText.setText(message.getUsername());
            
            // Define o conteúdo da mensagem
            messageText.setText(message.getMessage());
            
            // Formata e define o timestamp
            String timeString = timeFormat.format(new Date(message.getTimestamp()));
            timestampText.setText(timeString);
            
            // Configura o estilo baseado no tipo de mensagem
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) messageContainer.getLayoutParams();
            
            if (message.getUsername().equals("Sistema")) {
                // Mensagem do sistema - centralizada com fundo amarelo
                params.gravity = Gravity.CENTER;
                messageContainer.setBackgroundResource(R.drawable.message_bubble_system);
                usernameText.setVisibility(View.GONE);
                
            } else if (message.isOwnMessage()) {
                // Mensagem própria - alinhada à direita com fundo azul claro
                params.gravity = Gravity.END;
                messageContainer.setBackgroundResource(R.drawable.message_bubble_own);
                usernameText.setVisibility(View.GONE);
                
            } else {
                // Mensagem de outros - alinhada à esquerda com fundo cinza
                params.gravity = Gravity.START;
                messageContainer.setBackgroundResource(R.drawable.message_bubble_other);
                usernameText.setVisibility(View.VISIBLE);
            }
            
            messageContainer.setLayoutParams(params);
        }
    }
}

