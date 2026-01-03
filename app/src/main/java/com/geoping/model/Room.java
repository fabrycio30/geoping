package com.geoping.model;

/**
 * Modelo de dados para representar uma Sala (Cerca Digital).
 * 
 * Uma sala pode estar associada a um SSID Wi-Fi específico ou ser uma sala virtual.
 */
public class Room {
    
    private String roomId;           // Identificador único da sala
    private String roomName;         // Nome amigável da sala
    private String wifiSSID;         // SSID Wi-Fi associado (pode ser null)
    private long createdAt;          // Timestamp de criação
    private boolean isVirtual;       // True se não tem Wi-Fi associado
    
    /**
     * Construtor completo.
     * 
     * @param roomId ID único da sala
     * @param roomName Nome da sala
     * @param wifiSSID SSID Wi-Fi (null para sala virtual)
     */
    public Room(String roomId, String roomName, String wifiSSID) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.wifiSSID = wifiSSID;
        this.createdAt = System.currentTimeMillis();
        this.isVirtual = (wifiSSID == null || wifiSSID.trim().isEmpty());
    }
    
    /**
     * Construtor simplificado (gera ID automaticamente).
     * 
     * @param roomName Nome da sala
     * @param wifiSSID SSID Wi-Fi (null para sala virtual)
     */
    public Room(String roomName, String wifiSSID) {
        this(generateId(roomName), roomName, wifiSSID);
    }
    
    /**
     * Gera um ID único baseado no nome da sala.
     * 
     * @param roomName Nome da sala
     * @return ID único
     */
    private static String generateId(String roomName) {
        return roomName.toLowerCase()
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-z0-9_]", "");
    }
    
    // Getters e Setters
    
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
    
    public String getWifiSSID() {
        return wifiSSID;
    }
    
    public void setWifiSSID(String wifiSSID) {
        this.wifiSSID = wifiSSID;
        this.isVirtual = (wifiSSID == null || wifiSSID.trim().isEmpty());
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public boolean isVirtual() {
        return isVirtual;
    }
    
    /**
     * Verifica se esta sala está associada a um SSID específico.
     * 
     * @param ssid SSID para verificar
     * @return true se o SSID corresponde
     */
    public boolean matchesSSID(String ssid) {
        if (isVirtual || ssid == null) {
            return false;
        }
        return wifiSSID.equals(ssid);
    }
    
    @Override
    public String toString() {
        return "Room{" +
                "roomId='" + roomId + '\'' +
                ", roomName='" + roomName + '\'' +
                ", wifiSSID='" + wifiSSID + '\'' +
                ", isVirtual=" + isVirtual +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Room room = (Room) o;
        return roomId.equals(room.roomId);
    }
    
    @Override
    public int hashCode() {
        return roomId.hashCode();
    }
}

