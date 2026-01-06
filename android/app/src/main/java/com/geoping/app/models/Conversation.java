package com.geoping.app.models;

import java.io.Serializable;

/**
 * Modelo de Conversa
 */
public class Conversation implements Serializable {
    private static final long serialVersionUID = 1L;

    private String conversationId;
    private String title;
    private int creatorId;
    private String creatorUsername;
    private String createdAt;
    private int messageCount;

    public Conversation() {}

    public Conversation(String conversationId, String title, int creatorId, String creatorUsername, String createdAt, int messageCount) {
        this.conversationId = conversationId;
        this.title = title;
        this.creatorId = creatorId;
        this.creatorUsername = creatorUsername;
        this.createdAt = createdAt;
        this.messageCount = messageCount;
    }

    // Getters
    public String getConversationId() { return conversationId; }
    public String getTitle() { return title; }
    public int getCreatorId() { return creatorId; }
    public String getCreatorUsername() { return creatorUsername; }
    public String getCreatedAt() { return createdAt; }
    public int getMessageCount() { return messageCount; }

    // Setters
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public void setTitle(String title) { this.title = title; }
    public void setCreatorId(int creatorId) { this.creatorId = creatorId; }
    public void setCreatorUsername(String creatorUsername) { this.creatorUsername = creatorUsername; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setMessageCount(int messageCount) { this.messageCount = messageCount; }

    @Override
    public String toString() {
        return "Conversation{" +
                "conversationId='" + conversationId + '\'' +
                ", title='" + title + '\'' +
                ", creatorUsername='" + creatorUsername + '\'' +
                ", messageCount=" + messageCount +
                '}';
    }
}

