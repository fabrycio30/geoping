package com.geoping.app;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.geoping.app.utils.ApiClient;
import com.geoping.app.utils.AuthManager;
import com.geoping.datacollection.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Activity para coleta de dados Wi-Fi durante criação de sala
 * Interface simplificada com radar animado e contador de amostras
 */
public class RoomDataCollectionActivity extends AppCompatActivity {

    private static final String TAG = "RoomDataCollection";
    private static final int PERMISSION_REQUEST_CODE = 101;
    private static final int SCAN_INTERVAL_MS = 3000; // 3 segundos entre scans
    private static final int MIN_SAMPLES = 30;

    // UI Components
    private TextView textViewRoomName;
    private TextView textViewWifiSsid;
    private TextView textViewCollectionStatus;
    private TextView textViewSampleCount;
    private TextView textViewProgressMessage;
    private Button buttonTrainModel;
    private Button buttonStopCollection;

    // Data
    private String roomName;
    private String wifiSsid;
    private String roomLabel; // Usado como room_label no backend
    private int sampleCount = 0;
    private boolean isCollecting = false;

    // Componentes do sistema
    private WifiManager wifiManager;
    private Handler scanHandler;
    private Runnable scanRunnable;
    
    // BroadcastReceiver para resultados do Wi-Fi scan
    private BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
            if (success) {
                handleScanSuccess();
            } else {
                handleScanFailure();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_data_collection);

        // Receber dados da intent
        roomName = getIntent().getStringExtra("room_name");
        wifiSsid = getIntent().getStringExtra("wifi_ssid");
        roomLabel = wifiSsid; // Usamos o SSID como room_label

        if (roomName == null || wifiSsid == null) {
            Toast.makeText(this, "Erro: Dados da sala não encontrados", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initializeViews();
        setupWifiManager();
        checkPermissionsAndStartCollection();
    }

    private void initializeViews() {
        textViewRoomName = findViewById(R.id.textViewRoomName);
        textViewWifiSsid = findViewById(R.id.textViewWifiSsid);
        textViewCollectionStatus = findViewById(R.id.textViewCollectionStatus);
        textViewSampleCount = findViewById(R.id.textViewSampleCount);
        textViewProgressMessage = findViewById(R.id.textViewProgressMessage);
        buttonTrainModel = findViewById(R.id.buttonTrainModel);
        buttonStopCollection = findViewById(R.id.buttonStopCollection);

        textViewRoomName.setText("Configurando: " + roomName);
        textViewWifiSsid.setText("Wi-Fi: " + wifiSsid);
        updateSampleCount();

        buttonTrainModel.setOnClickListener(v -> startTraining());
        buttonStopCollection.setOnClickListener(v -> showStopConfirmation());
    }

    private void setupWifiManager() {
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        scanHandler = new Handler();
        scanRunnable = new Runnable() {
            @Override
            public void run() {
                if (isCollecting) {
                    performWifiScan();
                    scanHandler.postDelayed(this, SCAN_INTERVAL_MS);
                }
            }
        };
    }

    private void checkPermissionsAndStartCollection() {
        String[] permissions = {
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_WIFI_STATE,
                android.Manifest.permission.CHANGE_WIFI_STATE
        };

        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            startCollection();
        } else {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                startCollection();
            } else {
                Toast.makeText(this, "Permissões necessárias não concedidas", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void startCollection() {
        isCollecting = true;
        sampleCount = 0;
        updateSampleCount();
        
        textViewCollectionStatus.setText("🔄 Coletando amostras...");
        
        Log.d(TAG, "=== COLETA INICIADA ===");
        Log.d(TAG, "Room Name: " + roomName);
        Log.d(TAG, "Wi-Fi SSID: " + wifiSsid);
        Log.d(TAG, "Room Label: " + roomLabel);
        Log.d(TAG, "Server URL: " + ApiClient.getBaseUrl());
        
        // Registrar receiver para os resultados do scan
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(wifiScanReceiver, intentFilter);
        
        // Iniciar primeiro scan
        scanHandler.post(scanRunnable);
    }

    private void performWifiScan() {
        if (wifiManager == null) {
            Log.e(TAG, "ERRO: WifiManager não inicializado");
            return;
        }
        
        try {
            boolean scanStarted = wifiManager.startScan();
            if (!scanStarted) {
                Log.w(TAG, "AVISO: Falha ao iniciar scan (throttling?)");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "ERRO: Permissão negada para scan Wi-Fi");
            runOnUiThread(() -> {
                Toast.makeText(this, "Erro de permissão", Toast.LENGTH_SHORT).show();
                stopCollection();
            });
        } catch (Exception e) {
            Log.e(TAG, "ERRO ao escanear: " + e.getMessage());
        }
    }
    
    private void handleScanSuccess() {
        if (wifiManager == null) {
            Log.e(TAG, "ERRO: WifiManager não disponível");
            return;
        }
        
        try {
            List<ScanResult> scanResults = wifiManager.getScanResults();
            
            if (scanResults == null || scanResults.isEmpty()) {
                Log.w(TAG, "Scan #" + (sampleCount + 1) + ": Nenhuma rede encontrada");
                return;
            }

            Log.d(TAG, "Scan #" + (sampleCount + 1) + ": " + scanResults.size() + " redes detectadas");
            
            // Enviar dados para o servidor
            sendDataToServer(scanResults);
            
        } catch (SecurityException e) {
            Log.e(TAG, "ERRO: Sem permissão para ler resultados Wi-Fi");
            runOnUiThread(() -> stopCollection());
        } catch (Exception e) {
            Log.e(TAG, "ERRO ao processar scan: " + e.getMessage());
        }
    }
    
    private void handleScanFailure() {
        Log.e(TAG, "ERRO: Falha no scan de Wi-Fi");
    }

    private void sendDataToServer(List<ScanResult> scanResults) {
        try {
            String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

            // Construir JSON do fingerprint Wi-Fi (usando o formato que funciona)
            JSONArray wifiScanResults = new JSONArray();
            for (ScanResult result : scanResults) {
                JSONObject network = new JSONObject();
                network.put("bssid", result.BSSID);
                network.put("ssid", result.SSID);
                network.put("rssi", result.level);
                wifiScanResults.put(network);
            }

            // Construir payload completo (usando o formato que funciona)
            JSONObject payload = new JSONObject();
            payload.put("room_label", roomLabel);
            payload.put("device_id", deviceId);
            payload.put("wifi_scan_results", wifiScanResults);

            String url = ApiClient.getBaseUrl() + "/api/collect";
            Log.d(TAG, "Enviando para: " + url);
            Log.d(TAG, "Payload: " + wifiScanResults.length() + " redes");

            RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            ApiClient.getSharedHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "❌ ERRO ao enviar: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    final String responseBody = response.body() != null ? response.body().string() : "";
                    final int responseCode = response.code();
                    
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            sampleCount++;
                            updateSampleCount();
                            Log.d(TAG, "✅ Scan #" + sampleCount + " enviado com sucesso");
                            Log.d(TAG, "  Resposta: " + responseBody);
                        } else {
                            Log.e(TAG, "✗ ERRO: HTTP " + responseCode);
                            Log.e(TAG, "  Resposta: " + responseBody);
                        }
                    });
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "❌ ERRO ao criar JSON: " + e.getMessage());
        }
    }

    private void updateSampleCount() {
        textViewSampleCount.setText("Amostras coletadas: " + sampleCount + " / " + MIN_SAMPLES);
        
        if (sampleCount >= MIN_SAMPLES) {
            buttonTrainModel.setEnabled(true);
            textViewProgressMessage.setText("✅ Amostras suficientes! Você pode treinar o modelo agora.");
            textViewProgressMessage.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            int remaining = MIN_SAMPLES - sampleCount;
            textViewProgressMessage.setText("Colete mais " + remaining + " amostras para treinar o modelo");
        }
    }

    private void showStopConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Parar Coleta")
                .setMessage("Deseja realmente parar a coleta? Você precisará recomeçar se não tiver amostras suficientes.")
                .setPositiveButton("Sim, Parar", (dialog, which) -> {
                    stopCollection();
                    finish();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void stopCollection() {
        isCollecting = false;
        
        // Parar scans
        if (scanHandler != null && scanRunnable != null) {
            scanHandler.removeCallbacks(scanRunnable);
        }
        
        // Desregistrar receiver
        try {
            unregisterReceiver(wifiScanReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver já foi desregistrado
        }
        
        Log.d(TAG, "Coleta parada. Total: " + sampleCount + " amostras");
    }

    private void startTraining() {
        stopCollection();

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Aguarde. Estamos treinando o modelo da sala...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        Request request = new Request.Builder()
                .url(ApiClient.getBaseUrl() + "/api/train/" + roomLabel)
                .post(RequestBody.create("", MediaType.parse("application/json")))
                .build();

        ApiClient.getSharedHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(RoomDataCollectionActivity.this, "Erro ao treinar modelo: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> progressDialog.dismiss());

                if (response.isSuccessful()) {
                    try (ResponseBody responseBody = response.body()) {
                        String responseData = responseBody != null ? responseBody.string() : "";
                        JSONObject jsonResponse = new JSONObject(responseData);

                        runOnUiThread(() -> {
                            Intent intent = new Intent(RoomDataCollectionActivity.this, RoomTrainingResultsActivity.class);
                            intent.putExtra("room_name", roomName);
                            intent.putExtra("wifi_ssid", wifiSsid);
                            intent.putExtra("sample_count", sampleCount);
                            startActivity(intent);
                            finish();
                        });
                    } catch (JSONException e) {
                        runOnUiThread(() -> Toast.makeText(RoomDataCollectionActivity.this, "Erro ao processar resposta", Toast.LENGTH_LONG).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(RoomDataCollectionActivity.this, "Erro no treinamento", Toast.LENGTH_LONG).show());
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCollection();
        
        // Garantir que o receiver seja desregistrado
        try {
            unregisterReceiver(wifiScanReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver já foi desregistrado
        }
    }
}

