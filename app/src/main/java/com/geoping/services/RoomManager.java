package com.geoping.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.geoping.model.Room;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Gerenciador de Salas.
 * 
 * Responsável por criar, salvar e recuperar salas do armazenamento local.
 * Usa SharedPreferences para persistência simples.
 */
public class RoomManager {
    
    private static final String TAG = "RoomManager";
    private static final String PREFS_NAME = "GeoPingRooms";
    private static final String KEY_ROOMS = "rooms_list";
    private static final String KEY_SELECTED_ROOM = "selected_room";
    private static final String KEY_SUBSCRIBED_ROOMS = "subscribed_rooms";
    
    private static RoomManager instance;
    private SharedPreferences prefs;
    private List<Room> roomsList;
    private String selectedRoomId;
    private List<String> subscribedRoomIds;
    
    /**
     * Construtor privado (Singleton).
     * 
     * @param context Contexto da aplicação
     */
    private RoomManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        roomsList = new ArrayList<>();
        subscribedRoomIds = new ArrayList<>();
        loadRooms();
        loadSubscriptions();
        selectedRoomId = prefs.getString(KEY_SELECTED_ROOM, null);
    }
    
    /**
     * Obtém a instância única do RoomManager.
     * 
     * @param context Contexto da aplicação
     * @return Instância singleton
     */
    public static synchronized RoomManager getInstance(Context context) {
        if (instance == null) {
            instance = new RoomManager(context);
        }
        return instance;
    }
    
    /**
     * Cria uma nova sala e a salva.
     * 
     * @param roomName Nome da sala
     * @param wifiSSID SSID Wi-Fi (null para sala virtual)
     * @return Sala criada
     */
    public Room createRoom(String roomName, String wifiSSID) {
        Room room = new Room(roomName, wifiSSID);
        
        // Verifica se já existe
        for (Room existing : roomsList) {
            if (existing.getRoomId().equals(room.getRoomId())) {
                Log.w(TAG, "Sala já existe: " + roomName);
                return existing;
            }
        }
        
        roomsList.add(room);
        saveRooms();
        
        Log.d(TAG, "Sala criada: " + room);
        return room;
    }
    
    /**
     * Remove uma sala.
     * 
     * @param roomId ID da sala
     * @return true se removida com sucesso
     */
    public boolean removeRoom(String roomId) {
        for (int i = 0; i < roomsList.size(); i++) {
            if (roomsList.get(i).getRoomId().equals(roomId)) {
                roomsList.remove(i);
                saveRooms();
                Log.d(TAG, "Sala removida: " + roomId);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Obtém todas as salas.
     * 
     * @return Lista de salas
     */
    public List<Room> getAllRooms() {
        return new ArrayList<>(roomsList);
    }
    
    /**
     * Obtém uma sala pelo ID.
     * 
     * @param roomId ID da sala
     * @return Sala encontrada ou null
     */
    public Room getRoomById(String roomId) {
        for (Room room : roomsList) {
            if (room.getRoomId().equals(roomId)) {
                return room;
            }
        }
        return null;
    }
    
    /**
     * Busca sala por SSID Wi-Fi.
     * 
     * @param ssid SSID para buscar
     * @return Sala encontrada ou null
     */
    public Room getRoomBySSID(String ssid) {
        for (Room room : roomsList) {
            if (room.matchesSSID(ssid)) {
                return room;
            }
        }
        return null;
    }
    
    /**
     * Define a sala selecionada atualmente.
     * 
     * @param roomId ID da sala
     */
    public void setSelectedRoom(String roomId) {
        this.selectedRoomId = roomId;
        prefs.edit().putString(KEY_SELECTED_ROOM, roomId).apply();
        Log.d(TAG, "Sala selecionada: " + roomId);
    }
    
    /**
     * Obtém a sala selecionada atualmente.
     * 
     * @return Sala selecionada ou null
     */
    public Room getSelectedRoom() {
        if (selectedRoomId == null) {
            return null;
        }
        return getRoomById(selectedRoomId);
    }
    
    /**
     * Limpa a seleção de sala.
     */
    public void clearSelectedRoom() {
        this.selectedRoomId = null;
        prefs.edit().remove(KEY_SELECTED_ROOM).apply();
        Log.d(TAG, "Seleção de sala limpa");
    }
    
    /**
     * Salva as salas no SharedPreferences.
     */
    private void saveRooms() {
        try {
            JSONArray jsonArray = new JSONArray();
            
            for (Room room : roomsList) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", room.getRoomId());
                jsonObject.put("name", room.getRoomName());
                jsonObject.put("ssid", room.getWifiSSID());
                jsonObject.put("created", room.getCreatedAt());
                jsonArray.put(jsonObject);
            }
            
            prefs.edit().putString(KEY_ROOMS, jsonArray.toString()).apply();
            Log.d(TAG, "Salas salvas: " + roomsList.size());
            
        } catch (JSONException e) {
            Log.e(TAG, "Erro ao salvar salas", e);
        }
    }
    
    /**
     * Carrega as salas do SharedPreferences.
     */
    private void loadRooms() {
        roomsList.clear();
        
        String json = prefs.getString(KEY_ROOMS, null);
        if (json == null) {
            // Cria salas padrão na primeira execução
            createDefaultRooms();
            return;
        }
        
        try {
            JSONArray jsonArray = new JSONArray(json);
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                
                String id = jsonObject.getString("id");
                String name = jsonObject.getString("name");
                String ssid = jsonObject.optString("ssid", null);
                long created = jsonObject.getLong("created");
                
                Room room = new Room(id, name, ssid);
                room.setCreatedAt(created);
                roomsList.add(room);
            }
            
            Log.d(TAG, "Salas carregadas: " + roomsList.size());
            
        } catch (JSONException e) {
            Log.e(TAG, "Erro ao carregar salas", e);
            createDefaultRooms();
        }
    }
    
    /**
     * Cria salas padrão para demonstração.
     */
    private void createDefaultRooms() {
        Log.d(TAG, "Criando salas padrão");
        
        // Sala com Wi-Fi (exemplo do usuário)
        createRoom("lab LESERC", "ALMEIDA 2.4G");
        
        // Salas virtuais (sem Wi-Fi)
        createRoom("Biblioteca", null);
        createRoom("Auditório", null);
    }
    
    /**
     * Inscreve (subscribe) em uma sala.
     * O usuário ficará inscrito mesmo quando sair da cobertura.
     * 
     * @param roomId ID da sala
     */
    public void subscribeToRoom(String roomId) {
        if (!subscribedRoomIds.contains(roomId)) {
            subscribedRoomIds.add(roomId);
            saveSubscriptions();
            Log.d(TAG, "Inscrito na sala: " + roomId);
        }
    }
    
    /**
     * Desinscreve (unsubscribe) de uma sala.
     * 
     * @param roomId ID da sala
     */
    public void unsubscribeFromRoom(String roomId) {
        subscribedRoomIds.remove(roomId);
        saveSubscriptions();
        Log.d(TAG, "Desinscrito da sala: " + roomId);
    }
    
    /**
     * Verifica se está inscrito em uma sala.
     * 
     * @param roomId ID da sala
     * @return true se inscrito
     */
    public boolean isSubscribedTo(String roomId) {
        return subscribedRoomIds.contains(roomId);
    }
    
    /**
     * Obtém lista de IDs das salas inscritas.
     * 
     * @return Lista de IDs
     */
    public List<String> getSubscribedRoomIds() {
        return new ArrayList<>(subscribedRoomIds);
    }
    
    /**
     * Salva as inscrições no SharedPreferences.
     */
    private void saveSubscriptions() {
        try {
            JSONArray jsonArray = new JSONArray();
            for (String roomId : subscribedRoomIds) {
                jsonArray.put(roomId);
            }
            prefs.edit().putString(KEY_SUBSCRIBED_ROOMS, jsonArray.toString()).apply();
            Log.d(TAG, "Inscrições salvas: " + subscribedRoomIds.size());
        } catch (Exception e) {
            Log.e(TAG, "Erro ao salvar inscrições", e);
        }
    }
    
    /**
     * Carrega as inscrições do SharedPreferences.
     */
    private void loadSubscriptions() {
        subscribedRoomIds.clear();
        
        String json = prefs.getString(KEY_SUBSCRIBED_ROOMS, null);
        if (json == null) {
            return;
        }
        
        try {
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                subscribedRoomIds.add(jsonArray.getString(i));
            }
            Log.d(TAG, "Inscrições carregadas: " + subscribedRoomIds.size());
        } catch (Exception e) {
            Log.e(TAG, "Erro ao carregar inscrições", e);
        }
    }
}

