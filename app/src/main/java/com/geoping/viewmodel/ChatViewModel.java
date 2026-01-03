package com.geoping.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.geoping.model.ChatMessage;
import com.geoping.services.RoomManager;
import com.geoping.services.SocketManager;
import com.geoping.services.WifiProximityService;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel do Chat - Gerencia o estado da interface de chat.
 * 
 * NOVO COMPORTAMENTO HÍBRIDO:
 * - Professor: pode criar sala e enviar mensagens de qualquer lugar
 * - Aluno: se inscreve na sala (persistente) mas só RECEBE se na cobertura Wi-Fi
 * - WifiProximityService controla entrada/saída automática do Socket.IO
 * - ViewModel apenas gerencia inscrições e envio
 * 
 * Responsabilidades:
 * - Gerenciar a lista de mensagens
 * - Gerenciar sala selecionada (para envio)
 * - Observar Wi-Fi detectado (informativo)
 * - Coordenar envio de mensagens
 * - Manter o estado da UI após rotações
 */
public class ChatViewModel extends ViewModel {
    
    private static final String TAG = "ChatViewModel";
    
    // Nome do usuário (pode ser configurado ou gerado)
    private String username;
    
    // LiveData para lista de mensagens
    private MutableLiveData<List<ChatMessage>> messagesLiveData;
    
    // LiveData para sala SELECIONADA (para ENVIAR mensagens)
    private MutableLiveData<String> selectedRoomLiveData;
    
    // LiveData para Wi-Fi detectado (apenas informativo)
    private MutableLiveData<String> detectedWifiLiveData;
    
    // LiveData para status de conexão
    private LiveData<Boolean> connectionStatusLiveData;
    
    // LiveData privado para receber novas mensagens do SocketManager
    private MutableLiveData<ChatMessage> newMessageLiveData;
    
    // Lista interna de mensagens
    private List<ChatMessage> messagesList;
    
    // Instâncias dos gerenciadores
    private SocketManager socketManager;
    private RoomManager roomManager;
    
    // Observers
    private Observer<ChatMessage> newMessageObserver;
    private Observer<String> wifiChangeObserver;
    
    /**
     * Construtor do ViewModel.
     * Inicializa todas as estruturas de dados e LiveData.
     */
    public ChatViewModel() {
        Log.d(TAG, "ChatViewModel criado");
        
        // Inicializa a lista de mensagens
        messagesList = new ArrayList<>();
        messagesLiveData = new MutableLiveData<>(messagesList);
        
        // Inicializa LiveData
        selectedRoomLiveData = new MutableLiveData<>(null);
        detectedWifiLiveData = WifiProximityService.getDetectedWifiLiveData();
        
        // Obtém as instâncias dos gerenciadores
        socketManager = SocketManager.getInstance();
        // roomManager será inicializado quando necessário (precisa de Context)
        
        // Conecta ao servidor Socket.IO
        socketManager.connect();
        
        // Inicializa LiveData para novas mensagens
        newMessageLiveData = new MutableLiveData<>();
        
        // Configura listener para mensagens
        socketManager.listenForMessages(newMessageLiveData);
        
        // Observa novas mensagens e adiciona à lista
        newMessageObserver = new Observer<ChatMessage>() {
            @Override
            public void onChanged(ChatMessage chatMessage) {
                if (chatMessage != null) {
                    addMessage(chatMessage);
                }
            }
        };
        newMessageLiveData.observeForever(newMessageObserver);
        
        // Observa Wi-Fi detectado (apenas para log)
        wifiChangeObserver = new Observer<String>() {
            @Override
            public void onChanged(String detectedWifi) {
                if (detectedWifi != null) {
                    Log.i(TAG, "📡 Wi-Fi detectado: " + detectedWifi + 
                            " (entrada automática nas salas inscritas)");
                } else {
                    Log.i(TAG, "📡 Fora da cobertura Wi-Fi (saída automática das salas)");
                }
            }
        };
        detectedWifiLiveData.observeForever(wifiChangeObserver);
        
        // Obtém status de conexão do SocketManager
        connectionStatusLiveData = socketManager.getConnectionStatusLiveData();
        
        // Gera nome de usuário padrão (pode ser customizado)
        username = "Usuario_" + System.currentTimeMillis() % 10000;
        
        Log.d(TAG, "ChatViewModel inicializado com usuário: " + username);
    }
    
    /**
     * Retorna o LiveData contendo a lista de mensagens.
     * A UI deve observar este LiveData para atualizar o RecyclerView.
     * 
     * @return LiveData com lista de mensagens
     */
    public LiveData<List<ChatMessage>> getMessages() {
        return messagesLiveData;
    }
    
    /**
     * Retorna o LiveData contendo a sala SELECIONADA (para envio).
     * A UI deve observar este LiveData para saber qual sala está configurada.
     * 
     * @return LiveData com ID da sala selecionada ou null
     */
    public LiveData<String> getSelectedRoom() {
        return selectedRoomLiveData;
    }
    
    /**
     * Retorna o LiveData contendo o Wi-Fi detectado.
     * Apenas informativo - NÃO controla salas Socket.IO.
     * 
     * @return LiveData com SSID detectado ou null
     */
    public LiveData<String> getDetectedWifi() {
        return detectedWifiLiveData;
    }
    
    /**
     * Retorna o LiveData do status de conexão com o servidor.
     * 
     * @return LiveData<Boolean> true se conectado, false se desconectado
     */
    public LiveData<Boolean> getConnectionStatus() {
        return connectionStatusLiveData;
    }
    
    /**
     * Seleciona uma sala para ENVIAR mensagens.
     * NÃO controla entrada/saída do Socket.IO (isso é automático baseado em Wi-Fi).
     * Também inscreve o usuário na sala (para receber quando na cobertura).
     * 
     * @param roomId ID da sala
     * @param roomName Nome da sala (para log)
     * @param roomManager Instância do RoomManager (passado pela UI)
     */
    public void selectRoomForSending(String roomId, String roomName, RoomManager roomManager) {
        Log.i(TAG, "📤 Sala selecionada para envio: " + roomName);
        
        // Define como sala selecionada
        selectedRoomLiveData.postValue(roomId);
        
        // Inscreve na sala (para receber quando na cobertura)
        roomManager.subscribeToRoom(roomId);
        
        // Mensagem do sistema
        ChatMessage systemMessage = new ChatMessage(
                "Sistema",
                "Inscrito em: " + roomName + "\n" +
                "Você receberá mensagens quando estiver na cobertura Wi-Fi",
                roomId
        );
        addMessage(systemMessage);
        
        Log.i(TAG, "✅ Inscrito na sala: " + roomName);
    }
    
    /**
     * Limpa a seleção de sala para envio.
     * NÃO desinscreve o usuário - ele continua recebendo quando na cobertura.
     */
    public void clearRoomSelection() {
        Log.d(TAG, "Limpando seleção de sala para envio");
        selectedRoomLiveData.postValue(null);
    }
    
    /**
     * Desinscreve de uma sala.
     * O usuário para de receber mensagens desta sala.
     * 
     * @param roomId ID da sala
     * @param roomName Nome da sala
     * @param roomManager Instância do RoomManager
     */
    public void unsubscribeFromRoom(String roomId, String roomName, RoomManager roomManager) {
        Log.i(TAG, "Desinscrevendo de sala: " + roomName);
        
        roomManager.unsubscribeFromRoom(roomId);
        
        // Se era a sala selecionada, limpa
        if (roomId.equals(selectedRoomLiveData.getValue())) {
            selectedRoomLiveData.postValue(null);
        }
        
        // Mensagem do sistema
        ChatMessage systemMessage = new ChatMessage(
                "Sistema",
                "Você se desinscreveu de: " + roomName,
                ""
        );
        addMessage(systemMessage);
        
        Log.i(TAG, "❌ Desinscrito da sala: " + roomName);
    }
    
    /**
     * Envia uma mensagem para a sala selecionada.
     * 
     * IMPORTANTE:
     * - Professor pode enviar de QUALQUER LUGAR
     * - Alunos só RECEBEM se estiverem na cobertura + inscritos
     * 
     * @param messageText Texto da mensagem
     * @return true se enviou com sucesso, false caso contrário
     */
    public boolean sendMessage(String messageText) {
        // Valida se há sala selecionada
        String selectedRoom = selectedRoomLiveData.getValue();
        if (selectedRoom == null) {
            Log.w(TAG, "❌ Não é possível enviar: nenhuma sala selecionada");
            return false;
        }
        
        // Valida se há texto
        if (messageText == null || messageText.trim().isEmpty()) {
            Log.w(TAG, "❌ Mensagem vazia não será enviada");
            return false;
        }
        
        // Valida se está conectado
        if (!socketManager.isConnected()) {
            Log.w(TAG, "❌ Não é possível enviar: sem conexão com servidor");
            return false;
        }
        
        // Envia a mensagem via SocketManager
        // ORDEM CORRETA: message, room, username
        socketManager.sendMessage(messageText.trim(), selectedRoom, username);
        
        Log.d(TAG, "📤 Mensagem enviada para sala: " + selectedRoom + 
                " (de qualquer lugar, mas só quem está na cobertura recebe)");
        
        return true;
    }
    
    /**
     * Adiciona uma nova mensagem à lista e notifica observadores.
     * 
     * @param message Mensagem a ser adicionada
     */
    private void addMessage(ChatMessage message) {
        messagesList.add(message);
        
        // Cria nova lista para garantir que o LiveData detecte a mudança
        List<ChatMessage> updatedList = new ArrayList<>(messagesList);
        messagesLiveData.postValue(updatedList);
        
        Log.d(TAG, "Mensagem adicionada: " + message.toString());
    }
    
    /**
     * Define o nome do usuário.
     * 
     * @param username Novo nome de usuário
     */
    public void setUsername(String username) {
        if (username != null && !username.trim().isEmpty()) {
            this.username = username;
            Log.d(TAG, "Nome de usuário definido como: " + username);
        }
    }
    
    /**
     * Retorna o nome do usuário atual.
     * 
     * @return Nome do usuário
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * Limpa todas as mensagens do chat.
     */
    public void clearMessages() {
        messagesList.clear();
        messagesLiveData.postValue(new ArrayList<>());
        Log.d(TAG, "Mensagens limpas");
    }
    
    /**
     * Chamado quando o ViewModel está sendo destruído.
     * Remove observers para evitar memory leaks.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        
        Log.d(TAG, "ChatViewModel sendo destruído");
        
        // Remove observers
        if (newMessageObserver != null) {
            newMessageLiveData.removeObserver(newMessageObserver);
        }
        
        if (wifiChangeObserver != null) {
            detectedWifiLiveData.removeObserver(wifiChangeObserver);
        }
        
        // Nota: Não desconectamos o socket aqui pois ele pode ser compartilhado
        // com outros componentes. O ciclo de vida do socket é gerenciado separadamente.
    }
}
