package com.geoping.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.geoping.R;
import com.geoping.model.ChatMessage;
import com.geoping.services.RoomManager;
import com.geoping.services.WifiProximityService;
import com.geoping.viewmodel.ChatViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity Principal do GeoPing.
 * 
 * Esta é a única activity do aplicativo. Ela gerencia a interface de chat
 * e coordena a inicialização dos serviços de background.
 * 
 * Funcionalidades:
 * - Exibe lista de mensagens em tempo real
 * - Permite envio de mensagens
 * - Mostra status de conexão e sala atual
 * - Solicita e gerencia permissões necessárias
 * - Inicia o serviço de proximidade Wi-Fi
 */
public class MainActivity extends AppCompatActivity {
    
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 123;
    private static final int ROOM_SELECTOR_REQUEST_CODE = 456;
    
    // ViewModels e Managers
    private ChatViewModel chatViewModel;
    private RoomManager roomManager;
    
    // Views
    private RecyclerView messagesRecyclerView;
    private EditText messageInput;
    private Button sendButton;
    private Button btnConfigureRoom;
    private TextView currentRoomText;
    private TextView detectedWifiText;
    private TextView connectionStatusText;
    private View connectionIndicator;
    
    // Adapter
    private ChatAdapter chatAdapter;
    
    // Permissões necessárias
    private String[] requiredPermissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        Log.d(TAG, "MainActivity onCreate");
        
        // Inicializa o ViewModel
        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        
        // Inicializa o RoomManager
        roomManager = RoomManager.getInstance(this);
        
        // Inicializa as views
        initViews();
        
        // Configura o RecyclerView
        setupRecyclerView();
        
        // Configura observers do ViewModel
        setupObservers();
        
        // Configura listeners de UI
        setupListeners();
        
        // Verifica e solicita permissões
        checkAndRequestPermissions();
    }
    
    /**
     * Inicializa todas as views da activity.
     */
    private void initViews() {
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        btnConfigureRoom = findViewById(R.id.btnConfigureRoom);
        currentRoomText = findViewById(R.id.currentRoomText);
        detectedWifiText = findViewById(R.id.detectedWifiText);
        connectionStatusText = findViewById(R.id.connectionStatusText);
        connectionIndicator = findViewById(R.id.connectionIndicator);
    }
    
    /**
     * Configura o RecyclerView de mensagens.
     */
    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter();
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Novas mensagens aparecem no final
        
        messagesRecyclerView.setLayoutManager(layoutManager);
        messagesRecyclerView.setAdapter(chatAdapter);
    }
    
    /**
     * Configura os observers do ViewModel (LiveData).
     */
    private void setupObservers() {
        // Observer para lista de mensagens
        chatViewModel.getMessages().observe(this, new Observer<List<ChatMessage>>() {
            @Override
            public void onChanged(List<ChatMessage> messages) {
                Log.d(TAG, "Mensagens atualizadas: " + messages.size() + " mensagens");
                chatAdapter.setMessages(messages);
                
                // Rola para a última mensagem
                if (messages.size() > 0) {
                    messagesRecyclerView.smoothScrollToPosition(messages.size() - 1);
                }
            }
        });
        
        // Observer para sala selecionada (para envio)
        chatViewModel.getSelectedRoom().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String roomId) {
                if (roomId != null) {
                    // Busca nome da sala
                    com.geoping.model.Room room = roomManager.getRoomById(roomId);
                    String roomName = room != null ? room.getRoomName() : roomId;
                    
                    currentRoomText.setText("📤 Enviando para: " + roomName);
                    Log.d(TAG, "Sala selecionada para envio: " + roomName);
                } else {
                    currentRoomText.setText(R.string.no_room);
                    Log.d(TAG, "Nenhuma sala selecionada");
                }
            }
        });
        
        // Observer para status de conexão
        chatViewModel.getConnectionStatus().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isConnected) {
                updateConnectionStatus(isConnected);
            }
        });
        
        // Observer para Wi-Fi detectado
        chatViewModel.getDetectedWifi().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String detectedSSID) {
                updateDetectedWifi(detectedSSID);
            }
        });
    }
    
    /**
     * Configura os listeners de eventos de UI.
     */
    private void setupListeners() {
        // Listener do botão de enviar
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
        
        // Listener para tecla Enter no campo de texto
        messageInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendMessage();
                    return true;
                }
                return false;
            }
        });
        
        // Listener do botão de configurar sala
        btnConfigureRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openRoomSelector();
            }
        });
    }
    
    /**
     * Abre a tela de seleção de salas.
     */
    private void openRoomSelector() {
        Intent intent = new Intent(this, RoomSelectorActivity.class);
        startActivityForResult(intent, ROOM_SELECTOR_REQUEST_CODE);
    }
    
    /**
     * Envia uma mensagem através do ViewModel.
     */
    private void sendMessage() {
        String messageText = messageInput.getText().toString().trim();
        
        if (messageText.isEmpty()) {
            Toast.makeText(this, R.string.error_empty_message, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Verifica se há uma sala selecionada
        String selectedRoom = chatViewModel.getSelectedRoom().getValue();
        if (selectedRoom == null) {
            Toast.makeText(this, "Selecione uma sala primeiro (botão 🔧)", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Verifica se está conectado
        Boolean isConnected = chatViewModel.getConnectionStatus().getValue();
        if (isConnected == null || !isConnected) {
            Toast.makeText(this, R.string.error_no_connection, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Envia a mensagem
        chatViewModel.sendMessage(messageText);
        
        // Limpa o campo de entrada
        messageInput.setText("");
        
        Log.d(TAG, "Mensagem enviada: " + messageText);
    }
    
    /**
     * Atualiza os indicadores visuais de status de conexão.
     * 
     * @param isConnected true se conectado, false se desconectado
     */
    private void updateConnectionStatus(boolean isConnected) {
        if (isConnected) {
            connectionStatusText.setText(R.string.connection_status_connected);
            
            // Muda o indicador para verde
            GradientDrawable drawable = (GradientDrawable) connectionIndicator.getBackground();
            drawable.setColor(ContextCompat.getColor(this, R.color.status_connected));
            
            Log.d(TAG, "Status: Conectado");
        } else {
            connectionStatusText.setText(R.string.connection_status_disconnected);
            
            // Muda o indicador para vermelho
            GradientDrawable drawable = (GradientDrawable) connectionIndicator.getBackground();
            drawable.setColor(ContextCompat.getColor(this, R.color.status_disconnected));
            
            Log.d(TAG, "Status: Desconectado");
        }
    }
    
    /**
     * Verifica e solicita as permissões necessárias.
     */
    private void checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }
        
        if (!permissionsNeeded.isEmpty()) {
            // Explica ao usuário por que as permissões são necessárias
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                showPermissionRationale(permissionsNeeded);
            } else {
                // Solicita as permissões
                ActivityCompat.requestPermissions(
                        this,
                        permissionsNeeded.toArray(new String[0]),
                        PERMISSION_REQUEST_CODE
                );
            }
        } else {
            // Todas as permissões já foram concedidas
            startWifiProximityService();
        }
    }
    
    /**
     * Mostra um diálogo explicando por que as permissões são necessárias.
     * 
     * @param permissionsNeeded Lista de permissões que precisam ser solicitadas
     */
    private void showPermissionRationale(final List<String> permissionsNeeded) {
        new AlertDialog.Builder(this)
                .setTitle("Permissões Necessárias")
                .setMessage(R.string.permission_location_rationale)
                .setPositiveButton("OK", (dialog, which) -> {
                    ActivityCompat.requestPermissions(
                            MainActivity.this,
                            permissionsNeeded.toArray(new String[0]),
                            PERMISSION_REQUEST_CODE
                    );
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    Toast.makeText(MainActivity.this, 
                            R.string.permission_denied, 
                            Toast.LENGTH_LONG).show();
                })
                .create()
                .show();
    }
    
    /**
     * Callback para resultado de solicitação de permissões.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                Log.d(TAG, "Todas as permissões concedidas");
                startWifiProximityService();
            } else {
                Log.w(TAG, "Permissões negadas");
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show();
            }
        }
    }
    
    /**
     * Inicia o serviço de proximidade Wi-Fi em foreground.
     */
    private void startWifiProximityService() {
        Intent serviceIntent = new Intent(this, WifiProximityService.class);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        Toast.makeText(this, R.string.service_started, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Serviço WifiProximityService iniciado");
    }
    
    /**
     * Callback quando retorna da RoomSelectorActivity.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == ROOM_SELECTOR_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            String roomId = data.getStringExtra("room_id");
            String roomName = data.getStringExtra("room_name");
            
            if (roomId != null && roomName != null) {
                // Seleciona a sala para envio E inscreve (para receber)
                chatViewModel.selectRoomForSending(roomId, roomName, roomManager);
                
                Toast.makeText(this, 
                        "Inscrito em: " + roomName + "\n" +
                        "Você receberá mensagens quando estiver na cobertura Wi-Fi", 
                        Toast.LENGTH_LONG).show();
                
                Log.d(TAG, "Sala selecionada e inscrito: " + roomName);
            }
        }
    }
    
    /**
     * Atualiza o texto do Wi-Fi detectado.
     * 
     * @param detectedSSID SSID detectado ou null
     */
    private void updateDetectedWifi(String detectedSSID) {
        if (detectedSSID != null) {
            detectedWifiText.setText("📍 Na cobertura de: " + detectedSSID);
            Log.d(TAG, "Wi-Fi detectado: " + detectedSSID);
        } else {
            detectedWifiText.setText("📍 Nenhuma rede detectada");
            Log.d(TAG, "Sem Wi-Fi detectado");
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MainActivity onDestroy");
    }
}


