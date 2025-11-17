package com.geoping.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

/**
 * Serviço de Proximidade Wi-Fi - O "Radar" das Cercas Digitais.
 * 
 * Este serviço roda em background escaneando continuamente redes Wi-Fi disponíveis
 * para detectar quando o dispositivo entra ou sai de uma "cerca digital" (geofence).
 * 
 * Implementa lógica de histerese para evitar entrada/saída repetitiva da sala
 * quando o sinal está na borda do limite.
 * 
 * Funciona como Foreground Service para manter execução contínua no Android 8.0+.
 */
public class WifiProximityService extends Service {
    
    private static final String TAG = "WifiProximityService";
    
    // Configuração da cerca digital alvo
    private static final String TARGET_SSID = "ALMEIDA 2.4G";  // Nome da rede Wi-Fi alvo
    private static final int THRESHOLD_ENTER = -75;      // Limiar de entrada (dBm)
    private static final int THRESHOLD_EXIT = -85;       // Limiar de saída (dBm)
    
    // Intervalo de escaneamento (em milissegundos)
    private static final long SCAN_INTERVAL = 5000;      // 5 segundos
    
    // Notificação para Foreground Service
    private static final String CHANNEL_ID = "WifiProximityChannel";
    private static final int NOTIFICATION_ID = 1;
    
    // Gerenciadores Android
    private WifiManager wifiManager;
    private Handler scanHandler;
    
    // Estado atual do serviço
    private String currentActiveRoom = null;             // Sala ativa atual (null = nenhuma)
    private boolean isScanning = false;                  // Flag de escaneamento ativo
    
    // LiveData para comunicar mudanças de sala para a UI
    private static MutableLiveData<String> currentRoomLiveData = new MutableLiveData<>(null);
    
    // BroadcastReceiver para resultados do escaneamento Wi-Fi
    private BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
            
            if (success) {
                handleScanResults();
            } else {
                Log.w(TAG, "Escaneamento Wi-Fi falhou, tentando novamente...");
                scheduleScan();
            }
        }
    };
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "WifiProximityService criado");
        
        // Inicializa o WifiManager
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        
        // Inicializa o Handler para agendamento de scans
        scanHandler = new Handler(Looper.getMainLooper());
        
        // Registra o BroadcastReceiver para resultados de scan
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(wifiScanReceiver, intentFilter);
        
        // Cria canal de notificação e inicia como Foreground Service
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification("Monitorando cercas digitais..."));
        
        Log.d(TAG, "WifiProximityService iniciado em foreground");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand chamado");
        
        if (!isScanning) {
            startScanning();
        }
        
        // Se o serviço for morto pelo sistema, ele será recriado
        return START_STICKY;
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // Este serviço não suporta binding
        return null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "WifiProximityService destruído");
        
        // Para o escaneamento
        stopScanning();
        
        // Remove callbacks pendentes
        scanHandler.removeCallbacksAndMessages(null);
        
        // Desregistra o BroadcastReceiver
        try {
            unregisterReceiver(wifiScanReceiver);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Receiver já estava desregistrado");
        }
        
        // Se estava em uma sala, sai dela
        if (currentActiveRoom != null) {
            SocketManager.getInstance().leaveRoom(currentActiveRoom);
            currentActiveRoom = null;
            currentRoomLiveData.postValue(null);
        }
    }
    
    /**
     * Inicia o escaneamento periódico de redes Wi-Fi.
     */
    private void startScanning() {
        Log.d(TAG, "Iniciando escaneamento periódico de Wi-Fi");
        isScanning = true;
        scheduleScan();
    }
    
    /**
     * Para o escaneamento periódico.
     */
    private void stopScanning() {
        Log.d(TAG, "Parando escaneamento de Wi-Fi");
        isScanning = false;
        scanHandler.removeCallbacksAndMessages(null);
    }
    
    /**
     * Agenda o próximo escaneamento Wi-Fi.
     */
    private void scheduleScan() {
        if (isScanning) {
            scanHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    performScan();
                }
            }, SCAN_INTERVAL);
        }
    }
    
    /**
     * Executa um escaneamento Wi-Fi.
     */
    private void performScan() {
        if (wifiManager != null) {
            boolean scanStarted = wifiManager.startScan();
            
            if (scanStarted) {
                Log.d(TAG, "Escaneamento Wi-Fi iniciado");
            } else {
                Log.w(TAG, "Falha ao iniciar escaneamento Wi-Fi");
                scheduleScan(); // Tenta novamente
            }
        }
    }
    
    /**
     * Processa os resultados do escaneamento Wi-Fi.
     * 
     * Esta é a lógica central do serviço. Implementa histerese para evitar
     * "flapping" (entrada/saída repetitiva) quando o sinal está no limite.
     */
    private void handleScanResults() {
        List<ScanResult> scanResults = wifiManager.getScanResults();
        
        Log.d(TAG, "Processando " + scanResults.size() + " resultados de scan");
        
        // Procura pela rede alvo nos resultados
        ScanResult targetNetwork = findTargetNetwork(scanResults);
        
        if (targetNetwork != null) {
            int signalLevel = targetNetwork.level;
            Log.d(TAG, "Rede " + TARGET_SSID + " encontrada com sinal: " + signalLevel + " dBm");
            
            // LÓGICA DE ENTRADA: Se não está em nenhuma sala E sinal forte o suficiente
            if (currentActiveRoom == null && signalLevel > THRESHOLD_ENTER) {
                enterRoom(TARGET_SSID);
                updateNotification("Conectado à sala: " + TARGET_SSID);
            }
            
            // LÓGICA DE SAÍDA: Se está na sala E sinal ficou muito fraco
            else if (TARGET_SSID.equals(currentActiveRoom) && signalLevel < THRESHOLD_EXIT) {
                exitRoom(TARGET_SSID);
                updateNotification("Monitorando cercas digitais...");
            }
        } else {
            // Rede não encontrada
            Log.d(TAG, "Rede " + TARGET_SSID + " não encontrada no scan");
            
            // Se estava na sala, sai dela (saiu do alcance)
            if (TARGET_SSID.equals(currentActiveRoom)) {
                exitRoom(TARGET_SSID);
                updateNotification("Monitorando cercas digitais...");
            }
        }
        
        // Agenda o próximo scan
        scheduleScan();
    }
    
    /**
     * Procura a rede alvo na lista de resultados do scan.
     * 
     * @param scanResults Lista de redes Wi-Fi detectadas
     * @return ScanResult da rede alvo ou null se não encontrada
     */
    private ScanResult findTargetNetwork(List<ScanResult> scanResults) {
        for (ScanResult result : scanResults) {
            // Remove aspas do SSID se presentes
            String ssid = result.SSID.replace("\"", "");
            
            if (TARGET_SSID.equals(ssid)) {
                return result;
            }
        }
        return null;
    }
    
    /**
     * Entra em uma sala (cerca digital).
     * 
     * @param roomId Identificador da sala
     */
    private void enterRoom(String roomId) {
        Log.i(TAG, "ENTRANDO na sala: " + roomId);
        
        // Atualiza o estado interno
        currentActiveRoom = roomId;
        
        // Notifica a UI via LiveData
        currentRoomLiveData.postValue(roomId);
        
        // Entra na sala no Socket.IO
        SocketManager.getInstance().joinRoom(roomId);
    }
    
    /**
     * Sai de uma sala (cerca digital).
     * 
     * @param roomId Identificador da sala
     */
    private void exitRoom(String roomId) {
        Log.i(TAG, "SAINDO da sala: " + roomId);
        
        // Sai da sala no Socket.IO
        SocketManager.getInstance().leaveRoom(roomId);
        
        // Atualiza o estado interno
        currentActiveRoom = null;
        
        // Notifica a UI via LiveData
        currentRoomLiveData.postValue(null);
    }
    
    /**
     * Cria o canal de notificação para Foreground Service (Android 8.0+).
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Serviço de Proximidade Wi-Fi",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Monitora cercas digitais via Wi-Fi");
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    /**
     * Cria uma notificação para o Foreground Service.
     * 
     * @param contentText Texto da notificação
     * @return Notification configurada
     */
    private Notification createNotification(String contentText) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("GeoPing")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
    
    /**
     * Atualiza o texto da notificação do Foreground Service.
     * 
     * @param contentText Novo texto da notificação
     */
    private void updateNotification(String contentText) {
        NotificationManager notificationManager = 
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, createNotification(contentText));
        }
    }
    
    /**
     * Retorna o LiveData que observa mudanças na sala atual.
     * 
     * @return LiveData<String> com o ID da sala atual ou null
     */
    public static MutableLiveData<String> getCurrentRoomLiveData() {
        return currentRoomLiveData;
    }
}

