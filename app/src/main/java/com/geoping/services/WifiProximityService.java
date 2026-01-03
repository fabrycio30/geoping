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

import com.geoping.R;
import com.geoping.model.Room;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private static final String TARGET_SSID = "F-5.8Ghz";  // Nome da rede Wi-Fi alvo
    private static final int THRESHOLD_ENTER = -75;      // Limiar de entrada (dBm)
    private static final int THRESHOLD_EXIT = -85;       // Limiar de saída (dBm)
    
    // Intervalo de escaneamento (em milissegundos)
    private static final long SCAN_INTERVAL = 2000;      // 2 segundos
    
    // Notificação para Foreground Service
    private static final String CHANNEL_ID = "WifiProximityChannel";
    private static final int NOTIFICATION_ID = 1;
    
    // Gerenciadores Android
    private WifiManager wifiManager;
    private Handler scanHandler;
    private RoomManager roomManager;
    
    // Estado atual do serviço
    private String currentDetectedSSID = null;           // SSID detectado atualmente
    private boolean isScanning = false;                  // Flag de escaneamento ativo
    private Set<String> currentActiveRooms = new HashSet<>();  // Salas Socket.IO ativas
    
    // LiveData para comunicar Wi-Fi detectado (apenas informativo)
    private static MutableLiveData<String> detectedWifiLiveData = new MutableLiveData<>(null);
    
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
        
        // Inicializa o RoomManager
        roomManager = RoomManager.getInstance(getApplicationContext());
        
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
        
        // Sai de todas as salas ativas
        for (String roomId : currentActiveRooms) {
            SocketManager.getInstance().leaveRoom(roomId);
            Log.d(TAG, "Saiu da sala ao destruir serviço: " + roomId);
        }
        currentActiveRooms.clear();
        
        // Limpa o Wi-Fi detectado
        if (currentDetectedSSID != null) {
            currentDetectedSSID = null;
            detectedWifiLiveData.postValue(null);
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
     * COMPORTAMENTO HÍBRIDO:
     * 1. Detecta Wi-Fi e informa
     * 2. Busca salas inscritas com esse SSID
     * 3. Entra/sai automaticamente do Socket.IO baseado em cobertura + inscrição
     */
    private void handleScanResults() {
        List<ScanResult> scanResults = wifiManager.getScanResults();
        
        Log.d(TAG, "Processando " + scanResults.size() + " resultados de scan");
        
        // Procura pela rede alvo nos resultados
        ScanResult targetNetwork = findTargetNetwork(scanResults);
        
        if (targetNetwork != null) {
            int signalLevel = targetNetwork.level;
            Log.d(TAG, "Rede " + TARGET_SSID + " encontrada com sinal: " + signalLevel + " dBm");
            
            // Verifica se o sinal está bom o suficiente
            if (signalLevel > THRESHOLD_ENTER) {
                // Atualiza Wi-Fi detectado
                if (!TARGET_SSID.equals(currentDetectedSSID)) {
                    currentDetectedSSID = TARGET_SSID;
                    detectedWifiLiveData.postValue(TARGET_SSID);
                    Log.i(TAG, "Wi-Fi detectado na cobertura: " + TARGET_SSID);
                }
                
                // NOVO: Entra automaticamente nas salas inscritas
                enterSubscribedRoomsForSSID(TARGET_SSID);
                updateNotification("📍 Na cobertura: " + TARGET_SSID + " (" + signalLevel + " dBm)");
                
            } else if (signalLevel < THRESHOLD_EXIT) {
                // Sinal fraco - saiu da cobertura
                if (TARGET_SSID.equals(currentDetectedSSID)) {
                    // NOVO: Sai automaticamente das salas
                    leaveAllActiveRooms();
                    
                    currentDetectedSSID = null;
                    detectedWifiLiveData.postValue(null);
                    updateNotification("Monitorando cercas digitais...");
                    Log.i(TAG, "Saiu da cobertura de: " + TARGET_SSID);
                }
            }
        } else {
            // Rede não encontrada
            if (currentDetectedSSID != null) {
                Log.d(TAG, "Rede " + TARGET_SSID + " não encontrada no scan");
                
                // NOVO: Sai das salas ativas
                leaveAllActiveRooms();
                
                currentDetectedSSID = null;
                detectedWifiLiveData.postValue(null);
                updateNotification("Monitorando cercas digitais...");
            }
        }
        
        // Agenda o próximo scan
        scheduleScan();
    }
    
    /**
     * Entra automaticamente nas salas inscritas que correspondem ao SSID.
     * 
     * @param ssid SSID detectado
     */
    private void enterSubscribedRoomsForSSID(String ssid) {
        // Busca todas as salas inscritas
        List<String> subscribedIds = roomManager.getSubscribedRoomIds();
        
        for (String roomId : subscribedIds) {
            Room room = roomManager.getRoomById(roomId);
            
            if (room != null && room.matchesSSID(ssid)) {
                // Está inscrito E o SSID corresponde
                if (!currentActiveRooms.contains(roomId)) {
                    // Entra na sala Socket.IO
                    SocketManager.getInstance().joinRoom(roomId);
                    currentActiveRooms.add(roomId);
                    
                    Log.i(TAG, "✅ Entrou automaticamente na sala: " + room.getRoomName() + 
                            " (inscrito + na cobertura)");
                }
            }
        }
    }
    
    /**
     * Sai de todas as salas Socket.IO ativas.
     */
    private void leaveAllActiveRooms() {
        for (String roomId : currentActiveRooms) {
            SocketManager.getInstance().leaveRoom(roomId);
            
            Room room = roomManager.getRoomById(roomId);
            String roomName = room != null ? room.getRoomName() : roomId;
            
            Log.i(TAG, "❌ Saiu automaticamente da sala: " + roomName + 
                    " (fora da cobertura, mas continua inscrito)");
        }
        currentActiveRooms.clear();
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
     * Retorna o LiveData que observa Wi-Fi detectado.
     * 
     * @return LiveData<String> com o SSID detectado ou null
     */
    public static MutableLiveData<String> getDetectedWifiLiveData() {
        return detectedWifiLiveData;
    }
}

