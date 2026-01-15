package com.geoping.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import okhttp3.OkHttpClient;

/**
 * Cliente HTTP centralizado
 * Gerencia URL base do servidor
 */
public class ApiClient {
    private static final String PREFS_NAME = "GeoPingApi";
    private static final String KEY_SERVER_URL = "server_url";
    private static final String DEFAULT_SERVER_URL ="http://192.168.100.56:3000"; // "http://192.168.100.56:3000";

    private final SharedPreferences prefs;
    private final OkHttpClient httpClient;
    
    // Instancia estatica compartilhada
    private static OkHttpClient sharedHttpClient = new OkHttpClient();
    private static String baseUrl = DEFAULT_SERVER_URL;

    public ApiClient(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        httpClient = new OkHttpClient();
        // Sincronizar estático com preferência salva ao iniciar
        baseUrl = getServerUrl(); 
    }

    /**
     * Obter OkHttpClient
     */
    public OkHttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * Obter URL base do servidor
     */
    public String getServerUrl() {
        return prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL);
    }

    /**
     * Salvar URL base do servidor
     */
    public void setServerUrl(String url) {
        // Remover barra final se existir
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        prefs.edit().putString(KEY_SERVER_URL, url).apply();
        baseUrl = url; // Sincronizar estático
    }

    /**
     * Construir URL completa
     */
    public String buildUrl(String endpoint) {
        String baseUrl = getServerUrl();
        if (!endpoint.startsWith("/")) {
            endpoint = "/" + endpoint;
        }
        return baseUrl + endpoint;
    }
    
    // ========== METODOS ESTATICOS (para uso sem instancia) ==========
    
    /**
     * Obter URL base estatica
     */
    public static String getBaseUrl() {
        return baseUrl;
    }
    
    /**
     * Definir URL base estatica
     */
    public static void setBaseUrl(String newUrl) {
        if (newUrl.endsWith("/")) {
            newUrl = newUrl.substring(0, newUrl.length() - 1);
        }
        baseUrl = newUrl;
    }
    
    /**
     * Obter HttpClient estatico compartilhado
     */
    public static OkHttpClient getSharedHttpClient() {
        return sharedHttpClient;
    }
}

