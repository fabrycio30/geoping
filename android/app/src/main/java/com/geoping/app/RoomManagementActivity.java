package com.geoping.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.geoping.app.models.Room;
import com.geoping.app.utils.ApiClient;
import com.geoping.app.utils.AuthManager;
import com.geoping.datacollection.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Activity para gerenciar uma sala criada pelo usuario
 */
public class RoomManagementActivity extends AppCompatActivity {

    private static final String TAG = "RoomManagement";

    private TextView textViewRoomName;
    private TextView textViewWifiSsid;
    private TextView textViewAccessCode;
    private TextView textViewModelStatus;
    private TextView textViewSubscriberCount;
    private TextView textViewModelDate;
    private TextView textViewModelThreshold;
    private androidx.cardview.widget.CardView cardModelDetails;
    private androidx.recyclerview.widget.RecyclerView recyclerViewSubscribers;
    
    private Button buttonViewPendingSubscriptions;
    private Button buttonRetrainModel;
    private Button buttonDeleteRoom;

    private Room room;
    private ApiClient apiClient;
    private AuthManager authManager;
    private com.geoping.app.adapters.SubscriberAdapter subscriberAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_management);

        apiClient = new ApiClient(this);
        authManager = new AuthManager(this);

        // Receber dados da sala via intent
        room = (Room) getIntent().getSerializableExtra("room");
        if (room == null) {
            Toast.makeText(this, "Erro: Dados da sala nao encontrados", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initializeViews();
        displayRoomInfo();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDetailedRoomData();
    }

    private void initializeViews() {
        textViewRoomName = findViewById(R.id.textViewRoomName);
        textViewWifiSsid = findViewById(R.id.textViewWifiSsid);
        textViewAccessCode = findViewById(R.id.textViewAccessCode);
        textViewModelStatus = findViewById(R.id.textViewModelStatus);
        textViewSubscriberCount = findViewById(R.id.textViewSubscriberCount);
        
        textViewModelDate = findViewById(R.id.textViewModelDate);
        textViewModelThreshold = findViewById(R.id.textViewModelThreshold);
        cardModelDetails = findViewById(R.id.cardModelDetails);
        recyclerViewSubscribers = findViewById(R.id.recyclerViewSubscribers);
        
        recyclerViewSubscribers.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        subscriberAdapter = new com.geoping.app.adapters.SubscriberAdapter(new java.util.ArrayList<>());
        recyclerViewSubscribers.setAdapter(subscriberAdapter);

        buttonViewPendingSubscriptions = findViewById(R.id.buttonViewPendingSubscriptions);
        buttonRetrainModel = findViewById(R.id.buttonRetrainModel);
        buttonDeleteRoom = findViewById(R.id.buttonDeleteRoom);
    }


    private void displayRoomInfo() {
        textViewRoomName.setText(room.getRoomName());
        textViewWifiSsid.setText("Wi-Fi: " + room.getWifiSsid());
        textViewAccessCode.setText(room.getAccessCode());
        
        if (room.isModelTrained()) {
            textViewModelStatus.setText("Sim");
            textViewModelStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            textViewModelStatus.setText("Nao");
            textViewModelStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }

        textViewSubscriberCount.setText(String.valueOf(room.getSubscriberCount()));
    }

    private void refreshDetailedRoomData() {
        String url = apiClient.buildUrl("/api/rooms/" + room.getRoomId() + "/details");
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", authManager.getAuthorizationHeader())
                .build();

        apiClient.getHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Erro ao atualizar dados detalhados: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful() || responseBody == null) return;

                    String responseData = responseBody.string();
                    runOnUiThread(() -> {
                        try {
                            JSONObject json = new JSONObject(responseData);
                            JSONObject roomJson = json.getJSONObject("room");
                            JSONArray subscribersJson = json.getJSONArray("subscribers");
                            JSONObject modelInfo = json.optJSONObject("model_info");

                            // Atualizar dados básicos
                            room.setSubscriberCount(roomJson.optInt("subscribers_count", 0));
                            room.setModelTrained(roomJson.optBoolean("model_trained", false));
                            displayRoomInfo();

                            // Atualizar lista de inscritos
                            java.util.List<JSONObject> subscribersList = new java.util.ArrayList<>();
                            for (int i = 0; i < subscribersJson.length(); i++) {
                                subscribersList.add(subscribersJson.getJSONObject(i));
                            }
                            subscriberAdapter.updateData(subscribersList);

                            // Atualizar detalhes do modelo
                            if (modelInfo != null) {
                                cardModelDetails.setVisibility(android.view.View.VISIBLE);
                                String date = modelInfo.optString("training_date", "-");
                                double threshold = modelInfo.optDouble("threshold", 0.0);
                                
                                // Formatar data se possível
                                try {
                                    // Assumindo ISO 8601, simples substring para data
                                    if (date.length() > 10) date = date.substring(0, 10) + " " + date.substring(11, 16);
                                } catch (Exception e) {}

                                textViewModelDate.setText("Data Treinamento: " + date);
                                textViewModelThreshold.setText(String.format("Limiar de Decisão: %.6f", threshold));
                            } else {
                                cardModelDetails.setVisibility(android.view.View.GONE);
                            }

                        } catch (JSONException e) {
                            Log.e(TAG, "Erro ao processar dados detalhados: " + e.getMessage());
                        }
                    });
                }
            }
        });
    }

    // refreshRoomData original pode ser removido ou mantido como fallback, mas o details substitui


    private void setupListeners() {
        buttonViewPendingSubscriptions.setOnClickListener(v -> {
            Intent intent = new Intent(this, PendingRequestsActivity.class);
            intent.putExtra("room_id", room.getRoomId());
            startActivity(intent);
        });

        buttonRetrainModel.setOnClickListener(v -> {
            showRetrainConfirmation();
        });

        buttonDeleteRoom.setOnClickListener(v -> {
            showDeleteConfirmation();
        });
    }

    private void showRetrainConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Re-treinar Modelo")
                .setMessage("Deseja iniciar o processo de re-treinamento? Voce precisara coletar novas amostras.")
                .setPositiveButton("Sim, Re-treinar", (dialog, which) -> {
                    // Abrir RoomDataCollectionActivity para nova coleta
                    Intent intent = new Intent(this, RoomDataCollectionActivity.class);
                    intent.putExtra("room_name", room.getRoomName());
                    intent.putExtra("wifi_ssid", room.getWifiSsid());
                    startActivity(intent);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Deletar Sala")
                .setMessage("ATENCAO: Esta acao nao pode ser desfeita!\n\nTodos os dados, conversas e inscricoes serao permanentemente removidos.\n\nDeseja realmente deletar a sala \"" + room.getRoomName() + "\"?")
                .setPositiveButton("Sim, Deletar", (dialog, which) -> {
                    deleteRoom();
                })
                .setNegativeButton("Cancelar", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteRoom() {
        String url = apiClient.buildUrl("/api/rooms/" + room.getRoomId());

        Request request = new Request.Builder()
                .url(url)
                .delete()
                .addHeader("Authorization", authManager.getAuthorizationHeader())
                .build();

        apiClient.getHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Erro ao deletar sala: " + e.getMessage());
                    Toast.makeText(RoomManagementActivity.this,
                            "Erro de conexao: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String responseData = responseBody != null ? responseBody.string() : "";

                    runOnUiThread(() -> {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseData);

                            if (response.isSuccessful() && jsonResponse.getBoolean("success")) {
                                Toast.makeText(RoomManagementActivity.this,
                                        "Sala deletada com sucesso!",
                                        Toast.LENGTH_SHORT).show();
                                
                                // Voltar para MainActivity e atualizar lista
                                setResult(RESULT_OK);
                                finish();

                            } else {
                                String error = jsonResponse.optString("error", "Erro desconhecido");
                                Toast.makeText(RoomManagementActivity.this,
                                        "Erro: " + error,
                                        Toast.LENGTH_LONG).show();
                            }

                        } catch (JSONException e) {
                            Toast.makeText(RoomManagementActivity.this,
                                    "Erro ao processar resposta: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }
}

