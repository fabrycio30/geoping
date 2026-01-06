package com.geoping.app.models;

import java.io.Serializable;

/**
 * Modelo de Sala
 */
public class Room implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private String roomId;
    private String roomName;
    private String wifiSsid;
    private String accessCode;
    private int creatorId;
    private boolean modelTrained;
    private String createdAt;
    private String subscriptionStatus; // Para assinaturas: pending, approved, rejected
    private boolean isBlocked;
    private String creatorUsername;
    private int subscriberCount;
    private int pendingCount;

    // Construtores
    public Room() {}

    public Room(String roomId, String roomName, String wifiSsid) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.wifiSsid = wifiSsid;
    }

    // Getters e Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getWifiSsid() {
        return wifiSsid;
    }

    public void setWifiSsid(String wifiSsid) {
        this.wifiSsid = wifiSsid;
    }

    public String getAccessCode() {
        return accessCode;
    }

    public void setAccessCode(String accessCode) {
        this.accessCode = accessCode;
    }

    public int getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(int creatorId) {
        this.creatorId = creatorId;
    }

    public boolean isModelTrained() {
        return modelTrained;
    }

    public void setModelTrained(boolean modelTrained) {
        this.modelTrained = modelTrained;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getSubscriptionStatus() {
        return subscriptionStatus;
    }

    public void setSubscriptionStatus(String subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public void setBlocked(boolean blocked) {
        isBlocked = blocked;
    }

    public String getCreatorUsername() {
        return creatorUsername;
    }

    public void setCreatorUsername(String creatorUsername) {
        this.creatorUsername = creatorUsername;
    }

    public int getSubscriberCount() {
        return subscriberCount;
    }

    public void setSubscriberCount(int subscriberCount) {
        this.subscriberCount = subscriberCount;
    }

    public int getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(int pendingCount) {
        this.pendingCount = pendingCount;
    }

    @Override
    public String toString() {
        return "Room{" +
                "roomName='" + roomName + '\'' +
                ", wifiSsid='" + wifiSsid + '\'' +
                ", modelTrained=" + modelTrained +
                '}';
    }
}

