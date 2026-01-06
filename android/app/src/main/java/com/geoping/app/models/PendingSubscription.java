package com.geoping.app.models;

import java.io.Serializable;

public class PendingSubscription implements Serializable {
    private int id; // subscription_id
    private String subscribedAt;
    private int userId;
    private String username;
    private String email;

    public PendingSubscription() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getSubscribedAt() { return subscribedAt; }
    public void setSubscribedAt(String subscribedAt) { this.subscribedAt = subscribedAt; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}


