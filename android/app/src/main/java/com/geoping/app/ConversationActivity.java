package com.geoping.app;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.geoping.datacollection.R;
import com.geoping.app.adapters.MessageAdapter;
import com.geoping.app.models.Conversation;
import com.geoping.app.models.Message;
import com.geoping.app.models.Room;
import com.geoping.app.utils.ApiClient;
import com.geoping.app.utils.AuthManager;
import com.geoping.app.utils.SocketManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import io.socket.emitter.Emitter;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ConversationActivity extends AppCompatActivity {

    private static final String TAG = "ConversationActivity";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private TextView textViewConversationTitle;
    private TextView textViewCreatedBy;
    private TextView textViewEmpty;
    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;
    private ImageButton buttonSend;
    private ImageView buttonBack;

    private Conversation currentConversation;
    private Room currentRoom;
    private AuthManager authManager;
    private ApiClient apiClient;
    private SocketManager socketManager;
    private MessageAdapter messageAdapter;

    // Listeners Socket.io
    private Emitter.Listener onNewMessageListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        authManager = new AuthManager(this);
        apiClient = new ApiClient(this);
        socketManager = SocketManager.getInstance();

        currentConversation = (Conversation) getIntent().getSerializableExtra("conversation");
        currentRoom = (Room) getIntent().getSerializableExtra("room");

        if (currentConversation == null || currentRoom == null) {
            Toast.makeText(this, "Erro: Conversa não encontrada.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeComponents();
        setupListeners();
        setupRecyclerView();
        setupSocketListeners();
        loadMessages();
    }

    private void initializeComponents() {
        textViewConversationTitle = findViewById(R.id.textViewConversationTitle);
        textViewCreatedBy = findViewById(R.id.textViewCreatedBy);
        textViewEmpty = findViewById(R.id.textViewEmpty);
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);
        buttonBack = findViewById(R.id.buttonBack);

        textViewConversationTitle.setText(currentConversation.getTitle());
        textViewCreatedBy.setText("Criado por: " + currentConversation.getCreatorUsername());
    }

    private void setupListeners() {
        buttonBack.setOnClickListener(v -> finish());
        buttonSend.setOnClickListener(v -> sendMessage());
    }

    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter();
        recyclerViewMessages.setAdapter(messageAdapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Começar do final (mensagens mais recentes)
        recyclerViewMessages.setLayoutManager(layoutManager);
    }

    private void setupSocketListeners() {
        onNewMessageListener = args -> runOnUiThread(() -> {
            try {
                JSONObject data = (JSONObject) args[0];
                JSONObject messageJson = data.getJSONObject("message");
                String senderUsername = data.getString("senderUsername");

                // Verificar se a mensagem é da conversa atual
                if (messageJson.getInt("conversation_id") == Integer.parseInt(currentConversation.getConversationId())) {
                    Message newMessage = new Message();
                    newMessage.setId(messageJson.getInt("id"));
                    newMessage.setConversationId(messageJson.getInt("conversation_id"));
                    newMessage.setSenderId(messageJson.getInt("sender_id"));
                    newMessage.setSenderUsername(senderUsername);
                    newMessage.setContent(messageJson.getString("content"));
                    newMessage.setSentAt(messageJson.getString("sent_at"));
                    newMessage.setMine(messageJson.getInt("sender_id") == authManager.getUserId());

                    messageAdapter.addMessage(newMessage);
                    recyclerViewMessages.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
                    updateEmptyState();
                }

            } catch (JSONException e) {
                Log.e(TAG, "Erro ao processar nova mensagem: " + e.getMessage());
            }
        });

        socketManager.onNewMessage(onNewMessageListener);
    }

    private void loadMessages() {
        String url = apiClient.buildUrl("/api/messages/" + currentConversation.getConversationId());
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", authManager.getAuthorizationHeader())
                .get()
                .build();

        ApiClient.getSharedHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Erro ao carregar mensagens: " + e.getMessage());
                    Toast.makeText(ConversationActivity.this, "Erro ao carregar mensagens.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseBody = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            JSONArray messagesJson = jsonResponse.getJSONArray("messages");

                            java.util.List<Message> messages = new java.util.ArrayList<>();
                            for (int i = 0; i < messagesJson.length(); i++) {
                                JSONObject msgJson = messagesJson.getJSONObject(i);
                                Message msg = new Message();
                                msg.setId(msgJson.getInt("id"));
                                msg.setSenderId(msgJson.getInt("sender_id"));
                                msg.setSenderUsername(msgJson.getString("sender_username"));
                                msg.setContent(msgJson.getString("content"));
                                msg.setSentAt(msgJson.getString("sent_at"));
                                msg.setMine(msgJson.getInt("sender_id") == authManager.getUserId());
                                messages.add(msg);
                            }

                            messageAdapter.setMessages(messages);
                            updateEmptyState();
                            if (messageAdapter.getItemCount() > 0) {
                                recyclerViewMessages.scrollToPosition(messageAdapter.getItemCount() - 1);
                            }

                        } catch (JSONException e) {
                            Log.e(TAG, "Erro JSON ao carregar mensagens: " + e.getMessage());
                            Toast.makeText(ConversationActivity.this, "Erro ao processar mensagens.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "Falha ao carregar mensagens: " + responseBody);
                        Toast.makeText(ConversationActivity.this, "Erro ao carregar mensagens.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void sendMessage() {
        String content = editTextMessage.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Digite uma mensagem.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject payload = new JSONObject();
            payload.put("conversation_id", currentConversation.getConversationId());
            payload.put("content", content);

            String url = apiClient.buildUrl("/api/messages/send");
            RequestBody body = RequestBody.create(payload.toString(), JSON);
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", authManager.getAuthorizationHeader())
                    .post(body)
                    .build();

            editTextMessage.setText(""); // Limpar campo imediatamente

            ApiClient.getSharedHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        Log.e(TAG, "Erro ao enviar mensagem: " + e.getMessage());
                        Toast.makeText(ConversationActivity.this, "Erro de rede ao enviar mensagem.", Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    final String responseBody = response.body() != null ? response.body().string() : "";
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            // A mensagem aparecerá via Socket.io, MAS vamos adicionar localmente também para UX instantânea
                            Log.d(TAG, "Mensagem enviada com sucesso!");
                            
                            try {
                                JSONObject jsonResponse = new JSONObject(responseBody);
                                JSONObject messageJson = jsonResponse.getJSONObject("message");
                                
                                Message newMessage = new Message();
                                newMessage.setId(messageJson.getInt("id"));
                                newMessage.setConversationId(messageJson.getInt("conversation_id"));
                                newMessage.setSenderId(messageJson.getInt("sender_id"));
                                newMessage.setSenderUsername(messageJson.getString("sender_username"));
                                newMessage.setContent(messageJson.getString("content"));
                                newMessage.setSentAt(messageJson.getString("sent_at"));
                                newMessage.setMine(true); // Se enviei com sucesso, é minha

                                messageAdapter.addMessage(newMessage);
                                recyclerViewMessages.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
                                updateEmptyState();
                                
                            } catch (JSONException e) {
                                Log.e(TAG, "Erro ao processar resposta de envio: " + e.getMessage());
                            }
                            
                        } else {
                            String errorMsg = "Erro ao enviar mensagem.";
                            try {
                                JSONObject jsonError = new JSONObject(responseBody);
                                errorMsg = jsonError.optString("message", errorMsg);
                            } catch (JSONException e) {
                                Log.e(TAG, "Erro JSON ao enviar mensagem: " + e.getMessage());
                            }
                            Toast.makeText(ConversationActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Falha ao enviar mensagem: " + responseBody);
                        }
                    });
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "Erro ao criar JSON para mensagem: " + e.getMessage());
            Toast.makeText(this, "Erro interno ao enviar mensagem.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateEmptyState() {
        if (messageAdapter.getItemCount() == 0) {
            recyclerViewMessages.setVisibility(View.GONE);
            textViewEmpty.setVisibility(View.VISIBLE);
        } else {
            recyclerViewMessages.setVisibility(View.VISIBLE);
            textViewEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        socketManager.off("new_message", onNewMessageListener);
    }
}

