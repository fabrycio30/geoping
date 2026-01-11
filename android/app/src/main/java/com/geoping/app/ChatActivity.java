package com.geoping.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.geoping.datacollection.R;
import com.geoping.app.adapters.ConversationAdapter;
import com.geoping.app.models.Conversation;
import com.geoping.app.models.Room;
import com.geoping.app.utils.ApiClient;
import com.geoping.app.utils.AuthManager;
import com.geoping.app.utils.SocketManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.socket.emitter.Emitter;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class ChatActivity extends AppCompatActivity implements ConversationAdapter.OnConversationClickListener {

    private static final String TAG = "ChatActivity";
    private static final int PERMISSION_REQUEST_CODE = 123;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private TextView textViewRoomName;
    private TextView textViewPresenceStatus;
    private TextView textViewOnlineUsers;
    private TextView textViewEmpty;
    private RecyclerView recyclerViewConversations;
    private FloatingActionButton fabNewConversation;
    private ImageView buttonBack;

    private Room currentRoom;
    private AuthManager authManager;
    private ApiClient apiClient;
    private SocketManager socketManager;
    private ConversationAdapter conversationAdapter;

    private boolean isPresent = false;
    private Handler presenceCheckHandler;
    private Runnable presenceCheckRunnable;

    // Listeners Socket.io
    private Emitter.Listener onNewConversationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_chat);
            Log.d(TAG, "Layout carregado com sucesso");

            authManager = new AuthManager(this);
            apiClient = new ApiClient(this);
            Log.d(TAG, "AuthManager e ApiClient inicializados");

            currentRoom = (Room) getIntent().getSerializableExtra("room");

            if (currentRoom == null) {
                Toast.makeText(this, "Erro: Sala não encontrada.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            Log.d(TAG, "Sala recebida: " + currentRoom.getRoomName());

            // Inicializar SocketManager (CORREÇÃO CRÍTICA)
            socketManager = SocketManager.getInstance();
            
            initializeComponents();
            Log.d(TAG, "Componentes inicializados");
            
            setupListeners();
            Log.d(TAG, "Listeners configurados");
            
            setupRecyclerView();
            Log.d(TAG, "RecyclerView configurado");
            
            // Reativar Socket.io
            setupSocketListeners();
            connectSocket();
            Log.d(TAG, "Socket.io inicializado");
            
            loadConversations();
            Log.d(TAG, "Conversas carregadas");
            
            checkPermissions(); // Verificar permissões antes de iniciar serviço
            
            startPresenceCheck();
            Log.d(TAG, "Verificação de presença iniciada");
            
        } catch (Exception e) {
            Log.e(TAG, "ERRO CRÍTICO no onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Erro ao abrir chat: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initializeComponents() {
        textViewRoomName = findViewById(R.id.textViewRoomName);
        textViewPresenceStatus = findViewById(R.id.textViewPresenceStatus);
        textViewOnlineUsers = findViewById(R.id.textViewOnlineUsers);
        textViewEmpty = findViewById(R.id.textViewEmpty);
        recyclerViewConversations = findViewById(R.id.recyclerViewConversations);
        fabNewConversation = findViewById(R.id.fabNewConversation);
        buttonBack = findViewById(R.id.buttonBack);

        textViewRoomName.setText(currentRoom.getRoomName());
        presenceCheckHandler = new Handler();
    }

    private void setupListeners() {
        buttonBack.setOnClickListener(v -> finish());
        fabNewConversation.setOnClickListener(v -> showNewConversationDialog());
    }

    private void setupRecyclerView() {
        conversationAdapter = new ConversationAdapter(this);
        recyclerViewConversations.setAdapter(conversationAdapter);
        recyclerViewConversations.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupSocketListeners() {
        onNewConversationListener = args -> runOnUiThread(() -> {
            try {
                JSONObject data = (JSONObject) args[0];
                JSONObject conversationJson = data.getJSONObject("conversation");
                String creatorUsername = data.getString("creatorUsername");

                Conversation newConversation = new Conversation();
                newConversation.setConversationId(conversationJson.getString("id"));
                newConversation.setTitle(conversationJson.getString("title"));
                newConversation.setCreatorId(conversationJson.getInt("creator_id"));
                newConversation.setCreatorUsername(creatorUsername);
                newConversation.setCreatedAt(conversationJson.getString("created_at"));
                newConversation.setMessageCount(0);

                conversationAdapter.addConversation(newConversation);
                updateEmptyState();
                Toast.makeText(this, "Nova conversa: " + newConversation.getTitle(), Toast.LENGTH_SHORT).show();

            } catch (JSONException e) {
                Log.e(TAG, "Erro ao processar nova conversa: " + e.getMessage());
            }
        });

        socketManager.onNewConversation(onNewConversationListener);
    }

    private void connectSocket() {
        try {
            String serverUrl = ApiClient.getBaseUrl();
            // Tentar pegar do SharedPreferences se o estático estiver padrão
            if (serverUrl.equals("http://192.168.100.56:3000")) {
                 serverUrl = apiClient.getServerUrl();
            }

            String authToken = authManager.getToken();
            
            if (authToken == null || authToken.isEmpty()) {
                Log.e(TAG, "Token de autenticação inválido. Abortando conexão socket.");
                Toast.makeText(this, "Erro de autenticação no chat.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!socketManager.isConnected()) {
                socketManager.connect(serverUrl, authToken);
            }

            socketManager.joinRoom(currentRoom.getRoomId());
            Log.d(TAG, "Tentativa de conexão à sala: " + currentRoom.getRoomId());
            
        } catch (Exception e) {
            Log.e(TAG, "Erro ao conectar socket: " + e.getMessage());
        }
    }

    private void loadConversations() {
        String url = apiClient.buildUrl("/api/conversations/room/" + currentRoom.getRoomId());
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", authManager.getAuthorizationHeader())
                .get()
                .build();

        ApiClient.getSharedHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Erro ao carregar conversas: " + e.getMessage());
                    Toast.makeText(ChatActivity.this, "Erro ao carregar conversas.", Toast.LENGTH_SHORT).show();
                    conversationAdapter.setConversations(new ArrayList<>());
                    updateEmptyState();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseBody = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            JSONArray conversationsJson = jsonResponse.getJSONArray("conversations");

                            List<Conversation> conversations = new ArrayList<>();
                            for (int i = 0; i < conversationsJson.length(); i++) {
                                JSONObject convJson = conversationsJson.getJSONObject(i);
                                Conversation conv = new Conversation();
                                conv.setConversationId(convJson.getString("conversation_id"));
                                conv.setTitle(convJson.getString("title"));
                                conv.setCreatorId(convJson.getInt("creator_id"));
                                conv.setCreatorUsername(convJson.getString("creator_username"));
                                conv.setCreatedAt(convJson.getString("created_at"));
                                conv.setMessageCount(convJson.getInt("message_count"));
                                conversations.add(conv);
                            }

                            conversationAdapter.setConversations(conversations);
                            updateEmptyState();

                        } catch (JSONException e) {
                            Log.e(TAG, "Erro JSON ao carregar conversas: " + e.getMessage());
                            Toast.makeText(ChatActivity.this, "Erro ao processar conversas.", Toast.LENGTH_SHORT).show();
                            conversationAdapter.setConversations(new ArrayList<>());
                            updateEmptyState();
                        }
                    } else {
                        Log.e(TAG, "Falha ao carregar conversas: " + responseBody);
                        Toast.makeText(ChatActivity.this, "Erro ao carregar conversas.", Toast.LENGTH_SHORT).show();
                        conversationAdapter.setConversations(new ArrayList<>());
                        updateEmptyState();
                    }
                });
            }
        });
    }

    private void showNewConversationDialog() {
        boolean isCreator = authManager.getUserId() == currentRoom.getCreatorId();
        if (!isPresent && !isCreator) {
            Toast.makeText(this, "Você precisa estar presente na sala para criar conversas.", Toast.LENGTH_LONG).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nova Conversa");

        final EditText input = new EditText(this);
        input.setHint("Título da conversa");
        builder.setView(input);

        builder.setPositiveButton("Criar", (dialog, which) -> {
            String title = input.getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(this, "Digite um título para a conversa.", Toast.LENGTH_SHORT).show();
                return;
            }
            createConversation(title);
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void createConversation(String title) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("room_id", currentRoom.getRoomId());
            payload.put("title", title);

            // CORREÇÃO: Endpoint correto é /api/conversations/create
            String url = apiClient.buildUrl("/api/conversations/create"); 
            RequestBody body = RequestBody.create(payload.toString(), JSON);
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", authManager.getAuthorizationHeader())
                    .post(body)
                    .build();

            ApiClient.getSharedHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        Log.e(TAG, "Erro ao criar conversa: " + e.getMessage());
                        Toast.makeText(ChatActivity.this, "Erro de rede ao criar conversa.", Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    final String responseBody = response.body() != null ? response.body().string() : "";
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Toast.makeText(ChatActivity.this, "Conversa criada com sucesso!", Toast.LENGTH_SHORT).show();
                            
                            try {
                                JSONObject jsonResponse = new JSONObject(responseBody);
                                JSONObject conversationJson = jsonResponse.getJSONObject("conversation");
                                
                                // Criar objeto Conversation e abrir Activity imediatamente
                                Conversation newConversation = new Conversation();
                                newConversation.setConversationId(conversationJson.getString("conversation_id"));
                                newConversation.setTitle(conversationJson.getString("title"));
                                newConversation.setCreatorId(conversationJson.getInt("creator_id"));
                                
                                // Agora o backend retorna o username
                                if (conversationJson.has("creator_username")) {
                                    newConversation.setCreatorUsername(conversationJson.getString("creator_username"));
                                } else {
                                    // Fallback: usar o próprio usuário se não vier
                                    newConversation.setCreatorUsername(authManager.getUsername());
                                }
                                
                                newConversation.setCreatedAt(conversationJson.getString("created_at"));
                                
                                onConversationClick(newConversation); // Reutiliza lógica de abrir chat
                                
                            } catch (JSONException e) {
                                Log.e(TAG, "Erro ao abrir nova conversa: " + e.getMessage());
                            }
                            
                        } else {
                            String errorMsg = "Erro ao criar conversa.";
                            try {
                                JSONObject jsonError = new JSONObject(responseBody);
                                errorMsg = jsonError.optString("message", errorMsg);
                            } catch (JSONException e) {
                                Log.e(TAG, "Erro JSON ao criar conversa: " + e.getMessage());
                            }
                            Toast.makeText(ChatActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Falha ao criar conversa: " + responseBody);
                        }
                    });
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "Erro ao criar JSON para conversa: " + e.getMessage());
            Toast.makeText(this, "Erro interno ao criar conversa.", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void startPresenceCheck() {
        presenceCheckRunnable = new Runnable() {
            @Override
            public void run() {
                checkPresenceStatus();
                presenceCheckHandler.postDelayed(this, 5000); // Verificar a cada 5 segundos
            }
        };
        presenceCheckHandler.post(presenceCheckRunnable);
    }

    private void checkPresenceStatus() {
        String url = apiClient.buildUrl("/api/presence/user/" + authManager.getUserId());
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", authManager.getAuthorizationHeader())
                .get()
                .build();

        ApiClient.getSharedHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Erro ao verificar presença: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseBody = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            
                            // Verificar qual chave o backend retornou
                            JSONArray rooms;
                            if (jsonResponse.has("rooms")) {
                                rooms = jsonResponse.getJSONArray("rooms");
                            } else if (jsonResponse.has("present_in_rooms")) {
                                rooms = jsonResponse.getJSONArray("present_in_rooms");
                            } else {
                                rooms = new JSONArray(); // Vazio se não encontrar chave
                            }

                            boolean foundInRoom = false;
                            for (int i = 0; i < rooms.length(); i++) {
                                JSONObject roomJson = rooms.getJSONObject(i);
                                if (roomJson.getString("room_id").equals(currentRoom.getRoomId())) {
                                    foundInRoom = roomJson.getBoolean("is_present");
                                    double confidence = roomJson.getDouble("confidence");
                                    updatePresenceUI(foundInRoom, confidence);
                                    break;
                                }
                            }

                            if (!foundInRoom) {
                                updatePresenceUI(false, 0.0);
                            }

                        } catch (JSONException e) {
                            Log.e(TAG, "Erro JSON ao verificar presença: " + e.getMessage());
                        }
                    }
                });
            }
        });
    }

    private void updatePresenceUI(boolean present, double confidence) {
        isPresent = present;
        boolean isCreator = authManager.getUserId() == currentRoom.getCreatorId();
        
        if (isCreator) {
            textViewPresenceStatus.setText("★ Criador da Sala (Acesso Total)");
            textViewPresenceStatus.setTextColor(0xFFF39C12); // Laranja/Dourado
            fabNewConversation.setEnabled(true);
        } else if (present) {
            textViewPresenceStatus.setText(String.format("✓ Você está dentro (%.0f%%)", confidence * 100));
            textViewPresenceStatus.setTextColor(0xFF27AE60); // Verde
            fabNewConversation.setEnabled(true);
        } else {
            textViewPresenceStatus.setText("✗ Você está fora da sala");
            textViewPresenceStatus.setTextColor(0xFFE74C3C); // Vermelho
            fabNewConversation.setEnabled(false);
        }
    }

    private void updateEmptyState() {
        if (conversationAdapter.getItemCount() == 0) {
            recyclerViewConversations.setVisibility(View.GONE);
            textViewEmpty.setVisibility(View.VISIBLE);
        } else {
            recyclerViewConversations.setVisibility(View.VISIBLE);
            textViewEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onConversationClick(Conversation conversation) {
        Intent intent = new Intent(this, ConversationActivity.class);
        intent.putExtra("conversation", conversation);
        intent.putExtra("room", currentRoom);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadConversations(); // Recarregar lista ao voltar para esta tela
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (presenceCheckHandler != null && presenceCheckRunnable != null) {
            presenceCheckHandler.removeCallbacks(presenceCheckRunnable);
        }
        socketManager.leaveRoom(currentRoom.getRoomId());
        socketManager.off("new_conversation", onNewConversationListener);
    }
}

