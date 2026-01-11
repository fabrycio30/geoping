package com.geoping.app.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.geoping.app.MainActivity;
import com.geoping.app.utils.ApiClient;
import com.geoping.app.utils.AuthManager;
import com.geoping.datacollection.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Serviço em foreground para detecção de presença indoor em tempo real.
 * Faz scan Wi-Fi a cada 10 segundos e atualiza status de presença no servidor.
 */
public class PresenceService extends Service {

    private static final String TAG = "PresenceService";
    private static final String CHANNEL_ID = "presence_service_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final int SCAN_INTERVAL_MS = 10000; // 10 segundos

    private WifiManager wifiManager;
    private Handler scanHandler;
    private Runnable scanRunnable;
    private boolean isRunning = false;

    private String currentRoomId;
    private String currentRoomName;
    private boolean isInside = false;
    private double lastConfidence = 0.0;

    // BroadcastReceiver para resultados do Wi-Fi scan
    private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
            if (success) {
                handleScanSuccess();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "PresenceService criado");

        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        scanHandler = new Handler(Looper.getMainLooper());

        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("room_id")) {
            currentRoomId = intent.getStringExtra("room_id");
            currentRoomName = intent.getStringExtra("room_name");

            Log.d(TAG, "Iniciando monitoramento de presença para sala: " + currentRoomName);

            try {
                if (Build.VERSION.SDK_INT >= 34) { // Android 14+
                    startForeground(NOTIFICATION_ID, createNotification("Iniciando..."), 
                                  android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
                } else {
                    startForeground(NOTIFICATION_ID, createNotification("Iniciando..."));
                }
                startPresenceMonitoring();
            } catch (Exception e) {
                Log.e(TAG, "Erro ao iniciar Foreground Service: " + e.getMessage());
                stopSelf(); // Parar serviço se não conseguir iniciar foreground
            }

            return START_STICKY;
        }

        return START_NOT_STICKY;
    }

    private void startPresenceMonitoring() {
        if (isRunning) {
            return;
        }

        isRunning = true;

        // Registrar receiver para os resultados do scan
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(wifiScanReceiver, intentFilter);

        // Criar runnable para scans periódicos
        scanRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    performWifiScan();
                    scanHandler.postDelayed(this, SCAN_INTERVAL_MS);
                }
            }
        };

        // Iniciar primeiro scan
        scanHandler.post(scanRunnable);
    }

    private void stopPresenceMonitoring() {
        isRunning = false;

        if (scanHandler != null && scanRunnable != null) {
            scanHandler.removeCallbacks(scanRunnable);
        }

        try {
            unregisterReceiver(wifiScanReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver já foi desregistrado
        }
    }

    private void performWifiScan() {
        if (wifiManager == null) {
            return;
        }

        try {
            wifiManager.startScan();
        } catch (SecurityException e) {
            Log.e(TAG, "Erro de permissão ao escanear Wi-Fi: " + e.getMessage());
        }
    }

    private void handleScanSuccess() {
        if (wifiManager == null) {
            return;
        }

        try {
            List<ScanResult> scanResults = wifiManager.getScanResults();

            if (scanResults == null || scanResults.isEmpty()) {
                return;
            }

            Log.d(TAG, "Scan Wi-Fi: " + scanResults.size() + " redes detectadas");

            // Enviar para backend
            updatePresence(scanResults);

        } catch (SecurityException e) {
            Log.e(TAG, "Erro ao ler resultados Wi-Fi: " + e.getMessage());
        }
    }

    private void updatePresence(List<ScanResult> scanResults) {
        try {
            // Construir JSON do scan Wi-Fi
            JSONArray wifiScanResults = new JSONArray();
            for (ScanResult result : scanResults) {
                JSONObject network = new JSONObject();
                network.put("bssid", result.BSSID);
                network.put("ssid", result.SSID);
                network.put("rssi", result.level);
                wifiScanResults.put(network);
            }

            JSONObject payload = new JSONObject();
            payload.put("room_id", currentRoomId);
            payload.put("wifi_scan_results", wifiScanResults);

            String url = ApiClient.getBaseUrl() + "/api/presence/update";

            RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Authorization", AuthManager.getInstance(this).getAuthorizationHeader())
                    .build();

            ApiClient.getSharedHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "Erro ao atualizar presença: " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    try (ResponseBody responseBody = response.body()) {
                        if (response.isSuccessful() && responseBody != null) {
                            String responseData = responseBody.string();
                            JSONObject result = new JSONObject(responseData);

                            isInside = result.getBoolean("inside");
                            lastConfidence = result.getDouble("confidence");

                            Log.d(TAG, "Presença atualizada: " + (isInside ? "INSIDE" : "OUTSIDE") + 
                                      " (confiança: " + String.format("%.2f", lastConfidence * 100) + "%)");

                            updateNotification();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Erro ao processar resposta: " + e.getMessage());
                    }
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "Erro ao criar JSON: " + e.getMessage());
        }
    }

    private void updateNotification() {
        String status = isInside 
            ? "✓ Dentro da sala" 
            : "✗ Fora da sala";
        String confidence = String.format("Confiança: %.0f%%", lastConfidence * 100);

        Notification notification = createNotification(status + " | " + confidence);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Detecção de Presença",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Monitoramento contínuo de presença em salas");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification(String contentText) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("GeoPing - " + currentRoomName)
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopPresenceMonitoring();
        Log.d(TAG, "PresenceService destruído");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

