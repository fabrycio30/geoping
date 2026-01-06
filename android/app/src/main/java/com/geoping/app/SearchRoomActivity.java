package com.geoping.app;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.geoping.datacollection.R;
import com.geoping.app.adapters.SearchResultAdapter;
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
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * SearchRoomActivity - Buscar e solicitar assinatura em salas
 */
public class SearchRoomActivity extends AppCompatActivity implements SearchResultAdapter.OnSubscribeClickListener {

    private EditText editTextSearch;
    private Button buttonSearch;
    private Button buttonClose;
    private RecyclerView recyclerViewResults;
    private TextView textViewEmpty;
    private ProgressBar progressBar;

    private AuthManager authManager;
    private ApiClient apiClient;
    private SearchResultAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_room);

        authManager = new AuthManager(this);
        apiClient = new ApiClient(this);

        initializeComponents();
        setupListeners();
    }

    private void initializeComponents() {
        editTextSearch = findViewById(R.id.editTextSearch);
        buttonSearch = findViewById(R.id.buttonSearch);
        buttonClose = findViewById(R.id.buttonClose);
        recyclerViewResults = findViewById(R.id.recyclerViewResults);
        textViewEmpty = findViewById(R.id.textViewEmpty);
        progressBar = findViewById(R.id.progressBar);

        recyclerViewResults.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SearchResultAdapter(this);
        recyclerViewResults.setAdapter(adapter);
    }

    private void setupListeners() {
        buttonSearch.setOnClickListener(v -> performSearch());
        buttonClose.setOnClickListener(v -> finish());

        editTextSearch.setOnEditorActionListener((v, actionId, event) -> {
            performSearch();
            return true;
        });
    }

    private void performSearch() {
        String query = editTextSearch.getText().toString().trim();

        if (query.isEmpty()) {
            Toast.makeText(this, "Digite algo para buscar", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        String url = apiClient.buildUrl("/api/rooms/search?query=" + query);
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
                    Toast.makeText(SearchRoomActivity.this,
                            "Erro de conexao: " + e.getMessage(),
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
                            List<Room> rooms = parseSearchResults(roomsArray);

                            if (rooms.isEmpty()) {
                                showEmptyState("Nenhuma sala encontrada");
                            } else {
                                adapter.setRooms(rooms);
                                hideEmptyState();
                            }
                        }

                    } catch (JSONException e) {
                        Toast.makeText(SearchRoomActivity.this,
                                "Erro ao processar resposta: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private List<Room> parseSearchResults(JSONArray jsonArray) throws JSONException {
        List<Room> rooms = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject json = jsonArray.getJSONObject(i);
            Room room = new Room();
            room.setId(json.getInt("id"));
            room.setRoomId(json.getString("room_id"));
            room.setRoomName(json.getString("room_name"));
            room.setWifiSsid(json.getString("wifi_ssid"));
            room.setModelTrained(json.getBoolean("model_trained"));
            room.setCreatorUsername(json.optString("creator_username", "Desconhecido"));
            room.setSubscriberCount(json.optInt("subscriber_count", 0));
            rooms.add(room);
        }
        return rooms;
    }

    @Override
    public void onSubscribeClick(Room room) {
        Toast.makeText(this, "Solicitando assinatura...", Toast.LENGTH_SHORT).show();

        try {
            JSONObject json = new JSONObject();
            json.put("room_id", room.getRoomId());

            String url = apiClient.buildUrl("/api/rooms/subscribe");
            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", authManager.getAuthorizationHeader())
                    .post(body)
                    .build();

            apiClient.getHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(SearchRoomActivity.this,
                                "Erro de conexao: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    final String responseBody = response.body() != null ? response.body().string() : "";

                    runOnUiThread(() -> {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);

                            if (jsonResponse.getBoolean("success")) {
                                Toast.makeText(SearchRoomActivity.this,
                                        "Solicitacao enviada! Aguarde aprovacao do criador.",
                                        Toast.LENGTH_LONG).show();
                            } else {
                                String error = jsonResponse.optString("error", "Erro desconhecido");
                                Toast.makeText(SearchRoomActivity.this,
                                        "Erro: " + error,
                                        Toast.LENGTH_SHORT).show();
                            }

                        } catch (JSONException e) {
                            Toast.makeText(SearchRoomActivity.this,
                                    "Erro ao processar resposta: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });

        } catch (JSONException e) {
            Toast.makeText(this, "Erro ao criar requisicao: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void showEmptyState(String message) {
        textViewEmpty.setText(message);
        textViewEmpty.setVisibility(View.VISIBLE);
        recyclerViewResults.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        textViewEmpty.setVisibility(View.GONE);
        recyclerViewResults.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        buttonSearch.setEnabled(!loading);
        editTextSearch.setEnabled(!loading);
    }
}

