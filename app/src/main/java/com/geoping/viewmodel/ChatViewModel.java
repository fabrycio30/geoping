package com.geoping.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.geoping.model.ChatMessage;
import com.geoping.services.SocketManager;
import com.geoping.services.WifiProximityService;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel do Chat - Gerencia o estado da interface de chat.
 * 
 * Esta classe segue o padrão MVVM e atua como intermediária entre a UI (MainActivity)
 * e as camadas de serviço (SocketManager e WifiProximityService).
 * 
 * Responsabilidades:
 * - Gerenciar a lista de mensagens
 * - Observar mudanças na sala atual
 * - Coordenar envio de mensagens
 * - Manter o estado da UI após rotações e mudanças de configuração
 */
public class ChatViewModel extends ViewModel {
    
    private static final String TAG = "ChatViewModel";
    
    // Nome do usuário (pode ser configurado ou gerado)
    private String username;
    
    // LiveData para lista de mensagens
    private MutableLiveData<List<ChatMessage>> messagesLiveData;
    
    // LiveData para sala atual
    private MutableLiveData<String> currentRoomLiveData;
    
    // LiveData para status de conexão
    private LiveData<Boolean> connectionStatusLiveData;
    
    // LiveData privado para receber novas mensagens do SocketManager
    private MutableLiveData<ChatMessage> newMessageLiveData;
    
    // Lista interna de mensagens
    private List<ChatMessage> messagesList;
    
    // Instância do SocketManager
    private SocketManager socketManager;
    
    // Observer para novas mensagens
    private Observer<ChatMessage> newMessageObserver;
    
    // Observer para mudanças de sala
    private Observer<String> roomChangeObserver;
    
    /**
     * Construtor do ViewModel.
     * Inicializa todas as estruturas de dados e LiveData.
     */
    public ChatViewModel() {
        Log.d(TAG, "ChatViewModel criado");
        
        // Inicializa a lista de mensagens
        messagesList = new ArrayList<>();
        messagesLiveData = new MutableLiveData<>(messagesList);
        
        // Obtém a instância do SocketManager
        socketManager = SocketManager.getInstance();
        
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
        
        // Observa mudanças na sala atual (do WifiProximityService)
        currentRoomLiveData = WifiProximityService.getCurrentRoomLiveData();
        
        roomChangeObserver = new Observer<String>() {
            @Override
            public void onChanged(String room) {
                Log.d(TAG, "Mudança de sala detectada: " + room);
                
                if (room != null) {
                    // Entrou em uma nova sala - adiciona mensagem informativa
                    ChatMessage systemMessage = new ChatMessage(
                            "Sistema",
                            "Você entrou na sala: " + room,
                            room
                    );
                    addMessage(systemMessage);
                } else {
                    // Saiu da sala - adiciona mensagem informativa
                    ChatMessage systemMessage = new ChatMessage(
                            "Sistema",
                            "Você saiu da sala",
                            ""
                    );
                    addMessage(systemMessage);
                }
            }
        };
        currentRoomLiveData.observeForever(roomChangeObserver);
        
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
     * Retorna o LiveData contendo a sala atual.
     * A UI deve observar este LiveData para saber em qual sala está.
     * 
     * @return LiveData com ID da sala atual ou null
     */
    public LiveData<String> getCurrentRoom() {
        return currentRoomLiveData;
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
     * Envia uma mensagem para a sala atual.
     * 
     * @param messageText Texto da mensagem a ser enviada
     */
    public void sendMessage(String messageText) {
        // Valida se há texto
        if (messageText == null || messageText.trim().isEmpty()) {
            Log.w(TAG, "Tentativa de enviar mensagem vazia");
            return;
        }
        
        // Valida se está em uma sala
        String room = currentRoomLiveData.getValue();
        if (room == null) {
            Log.w(TAG, "Tentativa de enviar mensagem sem estar em uma sala");
            return;
        }
        
        // Valida se está conectado
        if (!socketManager.isConnected()) {
            Log.w(TAG, "Tentativa de enviar mensagem sem conexão ativa");
            return;
        }
        
        // Envia a mensagem via SocketManager
        socketManager.sendMessage(messageText, room, username);
        
        // Adiciona a própria mensagem à lista (feedback imediato)
        ChatMessage ownMessage = new ChatMessage(username, messageText, room);
        ownMessage.setOwnMessage(true);
        addMessage(ownMessage);
        
        Log.d(TAG, "Mensagem enviada: " + messageText);
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
        
        if (roomChangeObserver != null) {
            currentRoomLiveData.removeObserver(roomChangeObserver);
        }
        
        // Nota: Não desconectamos o socket aqui pois ele pode ser compartilhado
        // com outros componentes. O ciclo de vida do socket é gerenciado separadamente.
    }
}

