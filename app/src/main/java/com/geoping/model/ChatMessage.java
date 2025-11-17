package com.geoping.model;

/**
 * Modelo de dados para representar uma mensagem de chat.
 * 
 * Esta classe encapsula todas as informações necessárias sobre uma mensagem
 * trocada através do Socket.IO, incluindo o remetente, conteúdo e timestamp.
 */
public class ChatMessage {
    
    private String username;      // Nome do usuário que enviou a mensagem
    private String message;        // Conteúdo da mensagem
    private String room;           // Sala (cerca digital) onde a mensagem foi enviada
    private long timestamp;        // Timestamp da mensagem em milissegundos
    private boolean isOwnMessage;  // Flag para identificar mensagens do próprio usuário
    
    /**
     * Construtor completo para criar uma nova mensagem de chat.
     * 
     * @param username Nome do usuário remetente
     * @param message Conteúdo da mensagem
     * @param room Identificador da sala
     * @param timestamp Timestamp em milissegundos
     */
    public ChatMessage(String username, String message, String room, long timestamp) {
        this.username = username;
        this.message = message;
        this.room = room;
        this.timestamp = timestamp;
        this.isOwnMessage = false;
    }
    
    /**
     * Construtor simplificado que gera timestamp automaticamente.
     * 
     * @param username Nome do usuário remetente
     * @param message Conteúdo da mensagem
     * @param room Identificador da sala
     */
    public ChatMessage(String username, String message, String room) {
        this(username, message, room, System.currentTimeMillis());
    }
    
    // Getters e Setters
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getRoom() {
        return room;
    }
    
    public void setRoom(String room) {
        this.room = room;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isOwnMessage() {
        return isOwnMessage;
    }
    
    public void setOwnMessage(boolean ownMessage) {
        isOwnMessage = ownMessage;
    }
    
    /**
     * Retorna uma representação em string formatada da mensagem.
     * Útil para debugging.
     */
    @Override
    public String toString() {
        return "ChatMessage{" +
                "username='" + username + '\'' +
                ", message='" + message + '\'' +
                ", room='" + room + '\'' +
                ", timestamp=" + timestamp +
                ", isOwnMessage=" + isOwnMessage +
                '}';
    }
}

