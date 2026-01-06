package com.geoping.app.models;

import java.io.Serializable;

/**
 * Modelo de Mensagem
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int conversationId;
    private int senderId;
    private String senderUsername;
    private String content;
    private String sentAt;
    private boolean isMine; // Se a mensagem é do usuário logado

    public Message() {}

    public Message(int id, int conversationId, int senderId, String senderUsername, String content, String sentAt, boolean isMine) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.content = content;
        this.sentAt = sentAt;
        this.isMine = isMine;
    }

    // Getters
    public int getId() { return id; }
    public int getConversationId() { return conversationId; }
    public int getSenderId() { return senderId; }
    public String getSenderUsername() { return senderUsername; }
    public String getContent() { return content; }
    public String getSentAt() { return sentAt; }
    public boolean isMine() { return isMine; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setConversationId(int conversationId) { this.conversationId = conversationId; }
    public void setSenderId(int senderId) { this.senderId = senderId; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }
    public void setContent(String content) { this.content = content; }
    public void setSentAt(String sentAt) { this.sentAt = sentAt; }
    public void setMine(boolean mine) { isMine = mine; }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", senderUsername='" + senderUsername + '\'' +
                ", content='" + content + '\'' +
                ", sentAt='" + sentAt + '\'' +
                ", isMine=" + isMine +
                '}';
    }
}

