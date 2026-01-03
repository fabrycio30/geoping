package com.geoping.services;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.geoping.model.ChatMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Gerenciador Singleton de conexão Socket.IO.
 * 
 * Esta classe gerencia toda a comunicação em tempo real com o servidor Socket.IO,
 * incluindo conexão, entrada/saída de salas, e envio/recebimento de mensagens.
 * 
 * Padrão Singleton garante que apenas uma instância de conexão existe durante
 * toda a execução do aplicativo.
 */
public class SocketManager {
    
    private static final String TAG = "SocketManager";
    
    // URL do servidor Socket.IO - ALTERE PARA IP/SERVIDOR
    private static final String SERVER_URL = "http://192.168.100.56:3000";
    
    // Instância única (Singleton)
    private static SocketManager instance;
    
    // Socket.IO client
    private Socket socket;
    
    // LiveData para notificar novas mensagens
    private MutableLiveData<ChatMessage> newMessageLiveData;
    
    // LiveData para status de conexão
    private MutableLiveData<Boolean> connectionStatusLiveData;
    
    // Sala atual
    private String currentRoom = null;
    
    /**
     * Construtor privado para implementar Singleton.
     * Inicializa o socket e configura listeners básicos.
     */
    private SocketManager() {
        try {
            // Configurações do Socket.IO
            IO.Options options = new IO.Options();
            options.forceNew = true;
            options.reconnection = true;
            options.reconnectionDelay = 1000;
            options.reconnectionDelayMax = 5000;
            options.reconnectionAttempts = Integer.MAX_VALUE;
            
            socket = IO.socket(SERVER_URL, options);
            
            newMessageLiveData = new MutableLiveData<>();
            connectionStatusLiveData = new MutableLiveData<>(false);
            
            setupSocketListeners();
            
            Log.d(TAG, "SocketManager inicializado com URL: " + SERVER_URL);
            
        } catch (URISyntaxException e) {
            Log.e(TAG, "Erro ao criar socket: URL inválida", e);
            throw new RuntimeException("Falha ao inicializar SocketManager", e);
        }
    }
    
    /**
     * Obtém a instância única do SocketManager.
     * 
     * @return Instância singleton do SocketManager
     */
    public static synchronized SocketManager getInstance() {
        if (instance == null) {
            instance = new SocketManager();
        }
        return instance;
    }
    
    /**
     * Configura os listeners básicos de eventos do Socket.IO.
     */
    private void setupSocketListeners() {
        // Listener para conexão estabelecida
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "Socket conectado com sucesso");
                connectionStatusLiveData.postValue(true);
            }
        });
        
        // Listener para desconexão
        socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "Socket desconectado");
                connectionStatusLiveData.postValue(false);
            }
        });
        
        // Listener para erros de conexão
        socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.e(TAG, "Erro de conexão: " + args[0].toString());
                connectionStatusLiveData.postValue(false);
            }
        });
    }
    
    /**
     * Inicia a conexão com o servidor Socket.IO.
     */
    public void connect() {
        if (!socket.connected()) {
            socket.connect();
            Log.d(TAG, "Iniciando conexão com servidor...");
        } else {
            Log.d(TAG, "Socket já está conectado");
        }
    }
    
    /**
     * Encerra a conexão com o servidor.
     */
    public void disconnect() {
        if (socket.connected()) {
            socket.disconnect();
            Log.d(TAG, "Desconectando do servidor...");
        }
    }
    
    /**
     * Entra em uma sala (cerca digital) específica.
     * 
     * @param roomId Identificador da sala (ex: "GP_Lab")
     */
    public void joinRoom(String roomId) {
        if (socket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("room", roomId);
                
                socket.emit("join_room", data);
                currentRoom = roomId;
                
                Log.d(TAG, "Entrando na sala: " + roomId);
                
            } catch (JSONException e) {
                Log.e(TAG, "Erro ao criar JSON para join_room", e);
            }
        } else {
            Log.w(TAG, "Tentativa de entrar em sala sem conexão ativa");
        }
    }
    
    /**
     * Sai de uma sala específica.
     * 
     * @param roomId Identificador da sala
     */
    public void leaveRoom(String roomId) {
        if (socket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("room", roomId);
                
                socket.emit("leave_room", data);
                
                if (roomId.equals(currentRoom)) {
                    currentRoom = null;
                }
                
                Log.d(TAG, "Saindo da sala: " + roomId);
                
            } catch (JSONException e) {
                Log.e(TAG, "Erro ao criar JSON para leave_room", e);
            }
        }
    }
    
    /**
     * Envia uma mensagem para uma sala específica.
     * 
     * @param message Conteúdo da mensagem
     * @param room Identificador da sala
     * @param username Nome do usuário remetente
     */
    public void sendMessage(String message, String room, String username) {
        if (socket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("message", message);
                data.put("room", room);
                data.put("username", username);
                data.put("timestamp", System.currentTimeMillis());
                
                socket.emit("mensagem", data);
                
                Log.d(TAG, "Mensagem enviada para sala " + room + ": " + message);
                
            } catch (JSONException e) {
                Log.e(TAG, "Erro ao criar JSON para enviar mensagem", e);
            }
        } else {
            Log.w(TAG, "Tentativa de enviar mensagem sem conexão ativa");
        }
    }
    
    /**
     * Configura o listener para receber novas mensagens.
     * As mensagens recebidas são postadas no LiveData fornecido.
     * 
     * @param liveData LiveData que será atualizado com novas mensagens
     */
    public void listenForMessages(final MutableLiveData<ChatMessage> liveData) {
        socket.on("nova_mensagem", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    
                    String username = data.getString("username");
                    String message = data.getString("message");
                    String room = data.getString("room");
                    long timestamp = data.getLong("timestamp");
                    
                    ChatMessage chatMessage = new ChatMessage(username, message, room, timestamp);
                    
                    // Posta a mensagem no LiveData (thread-safe)
                    liveData.postValue(chatMessage);
                    
                    Log.d(TAG, "Nova mensagem recebida de " + username + " na sala " + room);
                    
                } catch (JSONException e) {
                    Log.e(TAG, "Erro ao processar nova mensagem", e);
                }
            }
        });
    }
    
    /**
     * Obtém o LiveData de status de conexão.
     * 
     * @return LiveData<Boolean> true se conectado, false se desconectado
     */
    public MutableLiveData<Boolean> getConnectionStatusLiveData() {
        return connectionStatusLiveData;
    }
    
    /**
     * Retorna a sala atual em que o usuário está.
     * 
     * @return ID da sala atual ou null se não estiver em nenhuma sala
     */
    public String getCurrentRoom() {
        return currentRoom;
    }
    
    /**
     * Verifica se o socket está conectado.
     * 
     * @return true se conectado, false caso contrário
     */
    public boolean isConnected() {
        return socket != null && socket.connected();
    }
}

