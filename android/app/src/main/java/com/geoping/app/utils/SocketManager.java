package com.geoping.app.utils;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Gerenciador de conexão Socket.io
 * Singleton para manter uma única conexão ativa
 */
public class SocketManager {
    private static final String TAG = "SocketManager";
    private static SocketManager instance;
    
    private Socket socket;
    private boolean isConnected = false;
    private String currentRoomId;
    private String savedAuthToken; // Token salvo para reconexão

    private SocketManager() {}

    public static synchronized SocketManager getInstance() {
        if (instance == null) {
            instance = new SocketManager();
        }
        return instance;
    }

    /**
     * Conectar ao servidor Socket.io
     */
    public void connect(String serverUrl, String authToken) {
        this.savedAuthToken = authToken; // Salvar token

        if (socket != null && socket.connected()) {
            Log.d(TAG, "Já conectado, re-autenticando...");
            if (authToken != null) {
                socket.emit("authenticate", authToken);
            }
            return;
        }
        
        // Se socket existe mas está desconectado, ou é nulo
        if (socket != null) {
            try {
                socket.disconnect();
                socket.close(); // Forçar limpeza
            } catch (Exception e) {
                Log.e(TAG, "Erro ao fechar socket anterior: " + e.getMessage());
            }
            socket = null;
        }

        try {
            IO.Options options = IO.Options.builder()
                    .setForceNew(true) // Forçar nova conexão
                    .setReconnection(true)
                    .setReconnectionAttempts(5) // Limitar tentativas para evitar loop infinito
                    .setReconnectionDelay(1000)
                    .setTimeout(10000)
                    .build();

            socket = IO.socket(serverUrl, options);

            socket.on(Socket.EVENT_CONNECT, args -> {
                isConnected = true;
                Log.d(TAG, "Socket.io conectado");
                
                // Autenticar com o token salvo
                if (savedAuthToken != null) {
                    socket.emit("authenticate", savedAuthToken);
                } else {
                    Log.e(TAG, "Token de autenticação não encontrado!");
                }
            });

            socket.on(Socket.EVENT_DISCONNECT, args -> {
                isConnected = false;
                Log.d(TAG, "Socket.io desconectado");
            });

            socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
                Log.e(TAG, "Erro de conexão: " + args[0]);
            });

            socket.on("authenticated", args -> {
                Log.d(TAG, "Autenticado: " + args[0]);
            });

            socket.connect();

        } catch (URISyntaxException e) {
            Log.e(TAG, "Erro ao criar socket: " + e.getMessage());
        }
    }

    /**
     * Entrar em uma sala
     */
    public void joinRoom(String roomId) {
        if (socket == null || !socket.connected()) {
            Log.e(TAG, "Socket não conectado");
            return;
        }

        currentRoomId = roomId;
        socket.emit("join_room", roomId);
        Log.d(TAG, "Entrando na sala: " + roomId);
    }

    /**
     * Sair de uma sala
     */
    public void leaveRoom(String roomId) {
        if (socket == null || !socket.connected()) {
            return;
        }

        socket.emit("leave_room", roomId);
        currentRoomId = null;
        Log.d(TAG, "Saindo da sala: " + roomId);
    }

    /**
     * Adicionar listener para novas mensagens
     */
    public void onNewMessage(Emitter.Listener listener) {
        if (socket != null) {
            socket.on("new_message", listener);
        }
    }

    /**
     * Adicionar listener para novas conversas
     */
    public void onNewConversation(Emitter.Listener listener) {
        if (socket != null) {
            socket.on("new_conversation", listener);
        }
    }

    /**
     * Remover listener
     */
    public void off(String event, Emitter.Listener listener) {
        if (socket != null) {
            socket.off(event, listener);
        }
    }

    /**
     * Desconectar
     */
    public void disconnect() {
        if (socket != null) {
            socket.disconnect();
            socket = null;
            isConnected = false;
            currentRoomId = null;
            Log.d(TAG, "Desconectado");
        }
    }

    public boolean isConnected() {
        return isConnected && socket != null && socket.connected();
    }

    public String getCurrentRoomId() {
        return currentRoomId;
    }
}

