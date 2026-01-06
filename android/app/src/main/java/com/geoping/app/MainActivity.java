package com.geoping.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.geoping.datacollection.R;
import com.geoping.app.adapters.RoomAdapter;
import com.geoping.app.models.Room;
import com.geoping.app.utils.ApiClient;
import com.geoping.app.utils.AuthManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

/**
 * MainActivity - Tela principal com lista de salas
 */
public class MainActivity extends AppCompatActivity implements RoomAdapter.OnRoomClickListener {

    private AuthManager authManager;
    private ApiClient apiClient;

    private TextView textViewUsername;
    private Button buttonLogout;
    private Button buttonCreateRoom;
    private Button buttonSearchRoom;
    private Button buttonTabMyRooms;
    private Button buttonTabSubscriptions;
    private RecyclerView recyclerViewRooms;
    private TextView textViewEmpty;
    private ProgressBar progressBar;

    private RoomAdapter roomAdapter;
    private boolean showingMyRooms = true; // true = minhas salas, false = assinaturas

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        authManager = new AuthManager(this);
        apiClient = new ApiClient(this);

        // Verificar autenticacao
        if (!authManager.isAuthenticated()) {
            goToLogin();
            return;
        }

        initializeComponents();
        setupListeners();
        loadMyRooms();
    }

    private void initializeComponents() {
        textViewUsername = findViewById(R.id.textViewUsername);
        buttonLogout = findViewById(R.id.buttonLogout);
        buttonCreateRoom = findViewById(R.id.buttonCreateRoom);
        buttonSearchRoom = findViewById(R.id.buttonSearchRoom);
        buttonTabMyRooms = findViewById(R.id.buttonTabMyRooms);
        buttonTabSubscriptions = findViewById(R.id.buttonTabSubscriptions);
        recyclerViewRooms = findViewById(R.id.recyclerViewRooms);
        textViewEmpty = findViewById(R.id.textViewEmpty);
        progressBar = findViewById(R.id.progressBar);

        textViewUsername.setText(authManager.getUsername());

        // Configurar RecyclerView
        recyclerViewRooms.setLayoutManager(new LinearLayoutManager(this));
        roomAdapter = new RoomAdapter(true, this); // true = sou criador
        recyclerViewRooms.setAdapter(roomAdapter);
    }

    private void setupListeners() {
        buttonLogout.setOnClickListener(v -> logout());

        buttonCreateRoom.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateRoomActivity.class);
            startActivity(intent);
        });

        buttonSearchRoom.setOnClickListener(v -> {
            Intent intent = new Intent(this, SearchRoomActivity.class);
            startActivity(intent);
        });

        buttonTabMyRooms.setOnClickListener(v -> {
            showingMyRooms = true;
            updateTabStyles();
            loadMyRooms();
        });

        buttonTabSubscriptions.setOnClickListener(v -> {
            showingMyRooms = false;
            updateTabStyles();
            loadSubscriptions();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recarregar ao voltar para a tela
        if (showingMyRooms) {
            loadMyRooms();
        } else {
            loadSubscriptions();
        }
    }

    private void updateTabStyles() {
        if (showingMyRooms) {
            buttonTabMyRooms.setBackgroundTintList(
                    getResources().getColorStateList(android.R.color.holo_blue_dark, null));
            buttonTabSubscriptions.setBackgroundTintList(
                    getResources().getColorStateList(android.R.color.darker_gray, null));
        } else {
            buttonTabMyRooms.setBackgroundTintList(
                    getResources().getColorStateList(android.R.color.darker_gray, null));
            buttonTabSubscriptions.setBackgroundTintList(
                    getResources().getColorStateList(android.R.color.holo_blue_dark, null));
        }
    }

    private void loadMyRooms() {
        setLoading(true);

        String url = apiClient.buildUrl("/api/rooms/my-rooms");
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", authManager.getAuthorizationHeader())
                .get()
                .build();

        apiClient.getHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(MainActivity.this,
                            "Erro ao carregar salas: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseBody = response.body() != null ? response.body().string() : "";

                runOnUiThread(() -> {
                    setLoading(false);

                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);

                        if (jsonResponse.getBoolean("success")) {
                            JSONArray roomsArray = jsonResponse.getJSONArray("rooms");
                            List<Room> rooms = parseRooms(roomsArray);

                            if (rooms.isEmpty()) {
                                showEmptyState("Voce ainda nao criou nenhuma sala");
                            } else {
                                roomAdapter = new RoomAdapter(true, MainActivity.this);
                                roomAdapter.setRooms(rooms);
                                recyclerViewRooms.setAdapter(roomAdapter);
                                hideEmptyState();
                            }
                        }

                    } catch (JSONException e) {
                        Toast.makeText(MainActivity.this,
                                "Erro ao processar resposta: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void loadSubscriptions() {
        setLoading(true);

        String url = apiClient.buildUrl("/api/rooms/my-subscriptions");
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", authManager.getAuthorizationHeader())
                .get()
                .build();

        apiClient.getHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(MainActivity.this,
                            "Erro ao carregar assinaturas: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseBody = response.body() != null ? response.body().string() : "";

                runOnUiThread(() -> {
                    setLoading(false);

                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);

                        if (jsonResponse.getBoolean("success")) {
                            JSONArray subscriptionsArray = jsonResponse.getJSONArray("subscriptions");
                            List<Room> rooms = parseSubscriptions(subscriptionsArray);

                            if (rooms.isEmpty()) {
                                showEmptyState("Voce ainda nao esta inscrito em nenhuma sala");
                            } else {
                                roomAdapter = new RoomAdapter(false, MainActivity.this);
                                roomAdapter.setRooms(rooms);
                                recyclerViewRooms.setAdapter(roomAdapter);
                                hideEmptyState();
                            }
                        }

                    } catch (JSONException e) {
                        Toast.makeText(MainActivity.this,
                                "Erro ao processar resposta: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private List<Room> parseRooms(JSONArray jsonArray) throws JSONException {
        List<Room> rooms = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject json = jsonArray.getJSONObject(i);
            Room room = new Room();
            room.setId(json.getInt("id"));
            room.setRoomId(json.getString("room_id"));
            room.setRoomName(json.getString("room_name"));
            room.setWifiSsid(json.getString("wifi_ssid"));
            room.setAccessCode(json.optString("access_code"));
            room.setModelTrained(json.getBoolean("model_trained"));
            room.setSubscriberCount(json.optInt("subscriber_count", 0));
            room.setPendingCount(json.optInt("pending_count", 0));
            rooms.add(room);
        }
        return rooms;
    }

    private List<Room> parseSubscriptions(JSONArray jsonArray) throws JSONException {
        List<Room> rooms = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject json = jsonArray.getJSONObject(i);
            Room room = new Room();
            room.setId(json.getInt("id"));
            room.setRoomId(json.getString("room_id"));
            room.setRoomName(json.getString("room_name"));
            room.setWifiSsid(json.getString("wifi_ssid"));
            room.setModelTrained(json.getBoolean("model_trained"));
            room.setSubscriptionStatus(json.getString("subscription_status"));
            room.setBlocked(json.optBoolean("is_blocked", false));
            room.setCreatorUsername(json.optString("creator_username"));
            rooms.add(room);
        }
        return rooms;
    }

    private void showEmptyState(String message) {
        textViewEmpty.setText(message);
        textViewEmpty.setVisibility(View.VISIBLE);
        recyclerViewRooms.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        textViewEmpty.setVisibility(View.GONE);
        recyclerViewRooms.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        recyclerViewRooms.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onEnterRoom(Room room) {
        // Iniciar PresenceService
        Intent serviceIntent = new Intent(this, com.geoping.app.services.PresenceService.class);
        serviceIntent.putExtra("room_id", room.getRoomId());
        serviceIntent.putExtra("room_name", room.getRoomName());
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        // Abrir ChatActivity
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("room", room);
        startActivity(intent);
    }

    @Override
    public void onManageRoom(Room room) {
        // Abrir tela de gerenciamento
        Intent intent = new Intent(this, RoomManagementActivity.class);
        intent.putExtra("room", room);
        startActivity(intent);
    }

    private void logout() {
        authManager.logout();
        goToLogin();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

