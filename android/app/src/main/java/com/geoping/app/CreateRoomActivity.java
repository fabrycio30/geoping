package com.geoping.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.geoping.datacollection.R;
import com.geoping.app.utils.ApiClient;
import com.geoping.app.utils.AuthManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * CreateRoomActivity - Criar nova sala e treinar modelo
 * Processo:
 * 1. Criar sala no backend (gera room_id e access_code)
 * 2. Abrir RoomDataCollectionActivity para coletar dados
 * 3. RoomDataCollectionActivity treina o modelo
 * 4. Exibir RoomTrainingResultsActivity com resultado
 */
public class CreateRoomActivity extends AppCompatActivity {

    private EditText editTextRoomName;
    private EditText editTextWifiSsid;
    private Button buttonCreateAndTrain;
    private Button buttonCancel;
    private ProgressBar progressBar;
    private TextView textViewStatus;

    private AuthManager authManager;
    private ApiClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_room);

        authManager = new AuthManager(this);
        apiClient = new ApiClient(this);

        initializeComponents();
        setupListeners();
    }

    private void initializeComponents() {
        editTextRoomName = findViewById(R.id.editTextRoomName);
        editTextWifiSsid = findViewById(R.id.editTextWifiSsid);
        buttonCreateAndTrain = findViewById(R.id.buttonCreateAndTrain);
        buttonCancel = findViewById(R.id.buttonCancel);
        progressBar = findViewById(R.id.progressBar);
        textViewStatus = findViewById(R.id.textViewStatus);
    }

    private void setupListeners() {
        buttonCreateAndTrain.setOnClickListener(v -> attemptCreateRoom());
        buttonCancel.setOnClickListener(v -> finish());
    }

    private void attemptCreateRoom() {
        String roomName = editTextRoomName.getText().toString().trim();
        String wifiSsid = editTextWifiSsid.getText().toString().trim();

        if (roomName.isEmpty() || wifiSsid.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true, "Criando sala...");

        try {
            JSONObject json = new JSONObject();
            json.put("room_name", roomName);
            json.put("wifi_ssid", wifiSsid);

            String url = apiClient.buildUrl("/api/rooms/create");
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
                        setLoading(false, "");
                        Toast.makeText(CreateRoomActivity.this,
                                "Erro de conexao: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    final String responseBody = response.body() != null ? response.body().string() : "";

                    runOnUiThread(() -> {
                        setLoading(false, "");

                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);

                            if (jsonResponse.getBoolean("success")) {
                                JSONObject room = jsonResponse.getJSONObject("room");
                                String roomId = room.getString("room_id");
                                String accessCode = room.getString("access_code");

                                Toast.makeText(CreateRoomActivity.this,
                                        "Sala criada! Codigo: " + accessCode,
                                        Toast.LENGTH_LONG).show();

                                // Abrir DataCollectionActivity para coletar dados e treinar
                                openDataCollectionForTraining(roomId, roomName);

                            } else {
                                String error = jsonResponse.optString("error", "Erro desconhecido");
                                Toast.makeText(CreateRoomActivity.this,
                                        "Erro: " + error,
                                        Toast.LENGTH_LONG).show();
                            }

                        } catch (JSONException e) {
                            Toast.makeText(CreateRoomActivity.this,
                                    "Erro ao processar resposta: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });

        } catch (JSONException e) {
            setLoading(false, "");
            Toast.makeText(this, "Erro ao criar requisicao: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void openDataCollectionForTraining(String roomId, String roomName) {
        // Abrir RoomDataCollectionActivity para coletar 30+ amostras e treinar modelo
        String wifiSsid = editTextWifiSsid.getText().toString().trim();
        
        Intent intent = new Intent(this, RoomDataCollectionActivity.class);
        intent.putExtra("room_name", roomName);
        intent.putExtra("wifi_ssid", wifiSsid);
        startActivity(intent);
        
        // Fechar esta activity e voltar para MainActivity
        finish();
    }

    private void setLoading(boolean loading, String status) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        textViewStatus.setText(status);
        textViewStatus.setVisibility(loading && !status.isEmpty() ? View.VISIBLE : View.GONE);
        buttonCreateAndTrain.setEnabled(!loading);
        buttonCancel.setEnabled(!loading);
        editTextRoomName.setEnabled(!loading);
        editTextWifiSsid.setEnabled(!loading);
    }
}

