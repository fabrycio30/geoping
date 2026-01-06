package com.geoping.datacollection;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Activity responsável pela coleta de dados de Wi-Fi para treinamento do modelo de localização indoor.
 * 
 * Funcionalidades:
 * - Escaneamento periódico de redes Wi-Fi
 * - Coleta de BSSID, SSID  e RSSI de todas as redes visíveis
 * - Envio automático dos dados para o servidor backend
 * - Interface visual para monitoramento do processo
 * 
 * Baseado no paper de Nikola et al. (2025) - ZeroTouch adaptado para topologia invertida
 */
public class DataCollectionActivity extends AppCompatActivity {

    // Constantes
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int DEFAULT_SCAN_INTERVAL_MS = 3000; // 3 segundos
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // UI Components
    private EditText editTextRoomLabel;
    private EditText editTextServerUrl;
    private EditText editTextScanInterval;
    private Button buttonStartCollection;
    private Button buttonStopCollection;
    private Button buttonTrainModel;
    private TextView textViewStatus;
    private TextView textViewScanCount;
    private TextView textViewLogs;
    private android.widget.ScrollView scrollViewLogs;

    // Wi-Fi e Coleta
    private WifiManager wifiManager;
    private Handler scanHandler;
    private Runnable scanRunnable;
    private boolean isCollecting = false;
    private int scanCount = 0;
    private int currentScanInterval = DEFAULT_SCAN_INTERVAL_MS;

    // Networking
    private OkHttpClient httpClient;

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
        
        try {
            setContentView(R.layout.activity_data_collection);

            // Inicializar componentes
            initializeComponents();
            setupListeners();

            // Verificar e solicitar permissões
            if (!checkPermissions()) {
                requestPermissions();
            } else {
                addLog(" Permissões já concedidas");
            }
        } catch (Exception e) {
            // Tratar erro de inicialização
            Toast.makeText(this, "Erro ao inicializar: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            finish();
        }
    }

    /**
     * Inicializa todos os componentes da Activity
     */
    private void initializeComponents() {
        // UI Elements
        editTextRoomLabel = findViewById(R.id.editTextRoomLabel);
        editTextServerUrl = findViewById(R.id.editTextServerUrl);
        editTextScanInterval = findViewById(R.id.editTextScanInterval);
        buttonStartCollection = findViewById(R.id.buttonStartCollection);
        buttonStopCollection = findViewById(R.id.buttonStopCollection);
        buttonTrainModel = findViewById(R.id.buttonTrainModel);
        textViewStatus = findViewById(R.id.textViewStatus);
        textViewScanCount = findViewById(R.id.textViewScanCount);
        textViewLogs = findViewById(R.id.textViewLogs);
        scrollViewLogs = findViewById(R.id.scrollViewLogs);

        // Wi-Fi Manager com tratamento de erro
        try {
            wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null) {
                Toast.makeText(this, "Erro: WifiManager não disponível neste dispositivo", Toast.LENGTH_LONG).show();
                addLog("ERRO: WifiManager não disponível");
            }
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao inicializar Wi-Fi: " + e.getMessage(), Toast.LENGTH_LONG).show();
            addLog("ERRO ao inicializar WifiManager: " + e.getMessage());
        }

        // HTTP Client
        httpClient = new OkHttpClient();

        // Handler para scans periódicos
        scanHandler = new Handler();

        // Valores padrão
        editTextScanInterval.setText("3");
    }

    /**
     * Configura os listeners dos botões
     */
    private void setupListeners() {
        buttonStartCollection.setOnClickListener(v -> startDataCollection());
        buttonStopCollection.setOnClickListener(v -> stopDataCollection());
        buttonTrainModel.setOnClickListener(v -> checkAndTrainModel());
    }

    /**
     * Verifica se todas as permissões necessárias foram concedidas
     * Android 12+ permite localização aproximada OU exata
     */
    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 12+ (API 31+): Aceitar FINE ou COARSE location
            boolean fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                    == PackageManager.PERMISSION_GRANTED;
            boolean coarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) 
                    == PackageManager.PERMISSION_GRANTED;
            
            // Precisa ter pelo menos uma das permissões de localização
            boolean hasLocationPermission = fineLocation || coarseLocation;
            
            // Android 13+ (API 33+) requer permissão adicional
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                boolean nearbyDevices = ContextCompat.checkSelfPermission(this, "android.permission.NEARBY_WIFI_DEVICES")
                        == PackageManager.PERMISSION_GRANTED;
                return hasLocationPermission && nearbyDevices;
            }
            
            return hasLocationPermission;
        }
        return true;
    }

    /**
     * Solicita as permissões necessárias ao usuário
     */
    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Lista de permissões base
            java.util.ArrayList<String> permissions = new java.util.ArrayList<>();
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            permissions.add(Manifest.permission.ACCESS_WIFI_STATE);
            permissions.add(Manifest.permission.CHANGE_WIFI_STATE);
            
            // Android 13+ requer permissão adicional
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add("android.permission.NEARBY_WIFI_DEVICES");
            }
            
            ActivityCompat.requestPermissions(
                this,
                permissions.toArray(new String[0]),
                PERMISSION_REQUEST_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Verificar se pelo menos as permissões essenciais foram concedidas
            boolean hasLocationPermission = false;
            boolean hasWifiPermission = false;
            boolean hasNearbyDevicesPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU; // true se não precisa
            
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    String permission = permissions[i];
                    
                    // Aceitar FINE ou COARSE location
                    if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION) || 
                        permission.equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                        hasLocationPermission = true;
                        addLog(" Permissão de localização concedida: " + 
                               (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION) ? "Exata" : "Aproximada"));
                    }
                    
                    if (permission.equals(Manifest.permission.ACCESS_WIFI_STATE)) {
                        hasWifiPermission = true;
                    }
                    
                    if (permission.equals("android.permission.NEARBY_WIFI_DEVICES")) {
                        hasNearbyDevicesPermission = true;
                        addLog(" Permissão NEARBY_WIFI_DEVICES concedida");
                    }
                }
            }
            
            // Verificar se todas as permissões essenciais foram concedidas
            if (hasLocationPermission && hasNearbyDevicesPermission) {
                addLog(" Todas as permissões necessárias concedidas");
                Toast.makeText(this, "Permissões concedidas com sucesso", Toast.LENGTH_SHORT).show();
            } else {
                addLog(" Permissões insuficientes");
                
                String mensagemErro = "Permissões necessárias:\n";
                if (!hasLocationPermission) mensagemErro += "• Localização (exata ou aproximada)\n";
                if (!hasNearbyDevicesPermission) mensagemErro += "• Dispositivos Wi-Fi próximos\n";
                
                Toast.makeText(this, "Permissões insuficientes", Toast.LENGTH_LONG).show();
                
                // Mostrar diálogo explicativo
                new AlertDialog.Builder(this)
                    .setTitle("Permissões Necessárias")
                    .setMessage(mensagemErro + "\nEste aplicativo precisa dessas permissões para escanear redes Wi-Fi.")
                    .setPositiveButton("Tentar Novamente", (dialog, which) -> {
                        requestPermissions();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
            }
        }
    }

    /**
     * Inicia o processo de coleta de dados
     */
    private void startDataCollection() {
        // Validações
        String roomLabel = editTextRoomLabel.getText().toString().trim();
        String serverUrl = editTextServerUrl.getText().toString().trim();
        String scanIntervalStr = editTextScanInterval.getText().toString().trim();

        if (roomLabel.isEmpty()) {
            Toast.makeText(this, "Por favor, insira o nome da sala", Toast.LENGTH_SHORT).show();
            return;
        }

        if (serverUrl.isEmpty()) {
            Toast.makeText(this, "Por favor, insira a URL do servidor", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!checkPermissions()) {
            Toast.makeText(this, "Permissões não concedidas", Toast.LENGTH_SHORT).show();
            requestPermissions();
            return;
        }

        // Verificar se Wi-Fi está habilitado
        if (!wifiManager.isWifiEnabled()) {
            new AlertDialog.Builder(this)
                .setTitle("Wi-Fi Desabilitado")
                .setMessage("O Wi-Fi precisa estar habilitado para coletar dados. Deseja habilitar?")
                .setPositiveButton("Sim", (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Android 10+ requer que o usuário habilite manualmente
                        Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                        startActivity(intent);
                    } else {
                        wifiManager.setWifiEnabled(true);
                    }
                })
                .setNegativeButton("Não", null)
                .show();
            return;
        }

        // Obter intervalo de scan
        try {
            currentScanInterval = Integer.parseInt(scanIntervalStr) * 1000; // Converter para ms
        } catch (NumberFormatException e) {
            currentScanInterval = DEFAULT_SCAN_INTERVAL_MS;
        }

        // Iniciar coleta
        isCollecting = true;
        scanCount = 0;
        updateScanCount();

        // Atualizar UI
        buttonStartCollection.setEnabled(false);
        buttonStopCollection.setEnabled(true);
        editTextRoomLabel.setEnabled(false);
        editTextServerUrl.setEnabled(false);
        editTextScanInterval.setEnabled(false);
        textViewStatus.setText("Status: Coletando dados...");

        addLog("========================================");
        addLog("Iniciando coleta de dados");
        addLog("Sala: " + roomLabel);
        addLog("Intervalo: " + (currentScanInterval/1000) + "s");
        addLog("Servidor: " + serverUrl);
        addLog("========================================");

        // Registrar receiver para os resultados do scan
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(wifiScanReceiver, intentFilter);

        // Criar runnable para scans periódicos
        scanRunnable = new Runnable() {
            @Override
            public void run() {
                if (isCollecting) {
                    performWifiScan();
                    scanHandler.postDelayed(this, currentScanInterval);
                }
            }
        };

        // Iniciar primeiro scan
        scanHandler.post(scanRunnable);
    }

    /**
     * Para o processo de coleta de dados
     */
    private void stopDataCollection() {
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

        // Atualizar UI
        buttonStartCollection.setEnabled(true);
        buttonStopCollection.setEnabled(false);
        editTextRoomLabel.setEnabled(true);
        editTextServerUrl.setEnabled(true);
        editTextScanInterval.setEnabled(true);
        textViewStatus.setText("Status: Coleta parada");

        addLog("========================================");
        addLog("Coleta finalizada");
        addLog("Total de scans: " + scanCount);
        addLog("========================================");

        Toast.makeText(this, "Coleta finalizada. Total: " + scanCount + " scans", Toast.LENGTH_LONG).show();
        
        // Habilitar botão de treinamento após coleta
        if (scanCount > 0) {
            buttonTrainModel.setEnabled(true);
            addLog("💡 Você pode treinar o modelo agora!");
        }
    }

    /**
     * Executa um scan de Wi-Fi
     */
    private void performWifiScan() {
        if (wifiManager == null) {
            addLog("ERRO: WifiManager não inicializado");
            return;
        }
        
        try {
            boolean scanStarted = wifiManager.startScan();
            if (!scanStarted) {
                addLog("AVISO: Falha ao iniciar scan (throttling?)");
            }
        } catch (SecurityException e) {
            addLog("ERRO: Permissão negada para scan Wi-Fi");
            stopDataCollection();
        } catch (Exception e) {
            addLog("ERRO ao escanear: " + e.getMessage());
        }
    }

    /**
     * Processa os resultados de um scan bem-sucedido
     */
    private void handleScanSuccess() {
        if (wifiManager == null) {
            addLog("ERRO: WifiManager não disponível");
            return;
        }
        
        try {
            // Verificar permissões antes de obter resultados
            if (!checkPermissions()) {
                addLog("ERRO: Permissões não concedidas ao processar scan");
                stopDataCollection();
                return;
            }
            
            List<ScanResult> scanResults = wifiManager.getScanResults();
            
            if (scanResults == null || scanResults.isEmpty()) {
                addLog("Scan #" + (scanCount + 1) + ": Nenhuma rede encontrada");
                return;
            }

            scanCount++;
            updateScanCount();

            addLog("Scan #" + scanCount + ": " + scanResults.size() + " redes detectadas");
            
            // Mostrar detalhes das redes detectadas
            for (ScanResult result : scanResults) {
                String ssid = result.SSID.isEmpty() ? "<hidden>" : result.SSID;
                addLog("  • " + ssid + " | " + result.BSSID + " | " + result.level + " dBm");
            }

            // Construir JSON e enviar para o servidor
            sendDataToServer(scanResults);
        } catch (SecurityException e) {
            addLog("ERRO: Sem permissão para ler resultados Wi-Fi");
            stopDataCollection();
        } catch (Exception e) {
            addLog("ERRO ao processar scan: " + e.getMessage());
        }
    }

    /**
     * Lida com falha no scan
     */
    private void handleScanFailure() {
        addLog("ERRO: Falha no scan de Wi-Fi");
    }

    /**
     * Envia os dados coletados para o servidor backend
     */
    private void sendDataToServer(List<ScanResult> scanResults) {
        try {
            // Obter dados da UI
            String roomLabel = editTextRoomLabel.getText().toString().trim();
            String serverUrl = editTextServerUrl.getText().toString().trim();
            String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

            // Construir JSON do fingerprint Wi-Fi
            JSONArray wifiFingerprint = new JSONArray();
            for (ScanResult result : scanResults) {
                JSONObject network = new JSONObject();
                network.put("bssid", result.BSSID);
                network.put("ssid", result.SSID);
                network.put("rssi", result.level);
                wifiFingerprint.put(network);
            }

            // Construir payload completo
            JSONObject payload = new JSONObject();
            payload.put("room_label", roomLabel);
            payload.put("device_id", deviceId);
            payload.put("wifi_scan_results", wifiFingerprint);

            // Log do payload sendo enviado
            addLog("Enviando para: " + serverUrl + "/api/collect");
            addLog("Payload: " + wifiFingerprint.length() + " redes");

            // Criar request
            RequestBody body = RequestBody.create(payload.toString(), JSON);
            Request request = new Request.Builder()
                .url(serverUrl + "/api/collect")
                .post(body)
                .build();

            // Enviar de forma assíncrona
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> 
                        addLog("ERRO ao enviar: " + e.getMessage())
                    );
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    final String responseBody = response.body() != null ? response.body().string() : "";
                    final int responseCode = response.code();
                    
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            addLog(" Scan #" + scanCount + " enviado com sucesso");
                            addLog("  Resposta: " + responseBody);
                        } else {
                            addLog(" ERRO: HTTP " + responseCode);
                            addLog("  Resposta: " + responseBody);
                        }
                    });
                }
            });

        } catch (JSONException e) {
            addLog("ERRO ao criar JSON: " + e.getMessage());
        }
    }

    /**
     * Atualiza o contador de scans na UI
     */
    private void updateScanCount() {
        textViewScanCount.setText("Scans realizados: " + scanCount);
    }

    /**
     * Adiciona uma mensagem ao log com timestamp e faz auto-scroll
     */
    private void addLog(String message) {
        // Verificar se os componentes estão inicializados
        if (textViewLogs == null || scrollViewLogs == null) {
            return;
        }
        
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            String timestamp = sdf.format(new Date());
            String logMessage = "[" + timestamp + "] " + message + "\n";
            
            textViewLogs.append(logMessage);
            
            // Auto-scroll para o final usando o ScrollView
            // Usar post() para garantir que o scroll aconteça após o texto ser renderizado
            scrollViewLogs.post(() -> {
                scrollViewLogs.fullScroll(android.view.View.FOCUS_DOWN);
            });
        } catch (Exception e) {
            // Silenciosamente ignorar erros de log para não crashar o app
            e.printStackTrace();
        }
    }

    /**
     * Verifica a quantidade de amostras e inicia o treinamento
     */
    private void checkAndTrainModel() {
        String roomLabel = editTextRoomLabel.getText().toString().trim();
        String serverUrl = editTextServerUrl.getText().toString().trim();

        if (roomLabel.isEmpty()) {
            Toast.makeText(this, "Por favor, insira o nome da sala", Toast.LENGTH_SHORT).show();
            return;
        }

        if (serverUrl.isEmpty()) {
            Toast.makeText(this, "Por favor, insira a URL do servidor", Toast.LENGTH_SHORT).show();
            return;
        }

        addLog("========================================");
        addLog("🔍 Verificando quantidade de amostras...");

        // Verificar quantidade de amostras no servidor
        Request request = new Request.Builder()
            .url(serverUrl + "/api/check-samples/" + roomLabel)
            .get()
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    addLog(" ERRO: " + e.getMessage());
                    Toast.makeText(DataCollectionActivity.this, 
                        "Erro ao verificar amostras", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseBody = response.body() != null ? response.body().string() : "";
                
                runOnUiThread(() -> {
                    try {
                        if (response.isSuccessful()) {
                            JSONObject json = new JSONObject(responseBody);
                            int sampleCount = json.getInt("sample_count");
                            int minRequired = json.getInt("min_required");
                            boolean canTrain = json.getBoolean("can_train");

                            addLog(" Amostras encontradas: " + sampleCount + "/" + minRequired);

                            if (canTrain) {
                                addLog(" Quantidade suficiente!");
                                addLog(" Iniciando treinamento...");
                                startTraining(roomLabel, serverUrl);
                            } else {
                                int needed = minRequired - sampleCount;
                                addLog("✗ Insuficiente! Colete mais " + needed + " amostras.");
                                Toast.makeText(DataCollectionActivity.this,
                                    "Colete mais " + needed + " amostras antes de treinar",
                                    Toast.LENGTH_LONG).show();
                            }
                        } else {
                            addLog(" ERRO: HTTP " + response.code());
                            Toast.makeText(DataCollectionActivity.this,
                                "Erro ao verificar amostras", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        addLog(" ERRO ao processar resposta: " + e.getMessage());
                    }
                });
            }
        });
    }

    /**
     * Inicia o treinamento do modelo
     */
    private void startTraining(String roomLabel, String serverUrl) {
        // Desabilitar botões durante o treinamento
        buttonStartCollection.setEnabled(false);
        buttonTrainModel.setEnabled(false);

        addLog("========================================");
        addLog("TREINAMENTO DO MODELO");
        addLog("========================================");

        // Criar request para treinar
        RequestBody body = RequestBody.create("", JSON);
        Request request = new Request.Builder()
            .url(serverUrl + "/api/train/" + roomLabel)
            .post(body)
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    addLog(" ERRO: " + e.getMessage());
                    Toast.makeText(DataCollectionActivity.this,
                        "Erro ao treinar modelo", Toast.LENGTH_SHORT).show();
                    buttonStartCollection.setEnabled(true);
                    buttonTrainModel.setEnabled(true);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                // Ler resposta linha por linha (streaming)
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(response.body().byteStream())
                );

                String line;
                try {
                    while ((line = reader.readLine()) != null) {
                        final String logLine = line;
                        
                        runOnUiThread(() -> {
                            try {
                                JSONObject json = new JSONObject(logLine);
                                String type = json.getString("type");
                                
                                if (type.equals("log")) {
                                    String message = json.getString("message");
                                    addLog(message);
                                    
                                } else if (type.equals("error")) {
                                    String message = json.getString("message");
                                    addLog("[ERRO] " + message);
                                    
                                } else                                 if (type.equals("complete")) {
                                    boolean success = json.getBoolean("success");
                                    
                                    if (success) {
                                        addLog("========================================");
                                        addLog("TREINAMENTO CONCLUÍDO COM SUCESSO!");
                                        addLog("========================================");
                                        
                                        // Extrair informações do treinamento
                                        String trainingInfoJson = json.optJSONObject("training_info") != null 
                                            ? json.getJSONObject("training_info").toString() 
                                            : null;
                                        
                                        // Abrir Activity com os resultados
                                        Intent intent = new Intent(DataCollectionActivity.this, 
                                            TrainingResultsActivity.class);
                                        intent.putExtra("room_label", roomLabel);
                                        intent.putExtra("server_url", serverUrl);
                                        intent.putExtra("training_info", trainingInfoJson);
                                        startActivity(intent);
                                        
                                    } else {
                                        String errorMsg = json.optString("message", "Erro desconhecido");
                                        addLog("========================================");
                                        addLog("TREINAMENTO FALHOU");
                                        addLog("Erro: " + errorMsg);
                                        addLog("========================================");
                                        
                                        Toast.makeText(DataCollectionActivity.this,
                                            "Treinamento falhou: " + errorMsg,
                                            Toast.LENGTH_LONG).show();
                                    }
                                    
                                    // Reabilitar botões
                                    buttonStartCollection.setEnabled(true);
                                    buttonTrainModel.setEnabled(true);
                                }
                                
                            } catch (JSONException e) {
                                addLog(" Linha inválida: " + logLine);
                            }
                        });
                    }
                } catch (IOException e) {
                    runOnUiThread(() -> {
                        addLog("ERRO ao ler resposta: " + e.getMessage());
                        buttonStartCollection.setEnabled(true);
                        buttonTrainModel.setEnabled(true);
                    });
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Parar coleta se ainda estiver ativa
        if (isCollecting) {
            stopDataCollection();
        }
        
        // Limpar recursos
        if (scanHandler != null) {
            scanHandler.removeCallbacksAndMessages(null);
        }
    }
}

