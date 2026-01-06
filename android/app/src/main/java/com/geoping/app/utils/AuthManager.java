package com.geoping.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Gerenciador de autenticacao
 * Armazena token JWT e dados do usuario
 * Singleton para acesso global
 */
public class AuthManager {
    private static final String PREFS_NAME = "GeoPingAuth";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";

    private static AuthManager instance;
    private final SharedPreferences prefs;

    public AuthManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Obter instancia singleton do AuthManager
     */
    public static synchronized AuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new AuthManager(context);
        }
        return instance;
    }

    /**
     * Salvar dados de autenticacao apos login/register
     */
    public void saveAuth(String token, int userId, String username, String email) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_TOKEN, token);
        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_EMAIL, email);
        editor.apply();
    }

    /**
     * Obter token JWT
     */
    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    /**
     * Obter ID do usuario
     */
    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, -1);
    }

    /**
     * Obter username
     */
    public String getUsername() {
        return prefs.getString(KEY_USERNAME, null);
    }

    /**
     * Obter email
     */
    public String getEmail() {
        return prefs.getString(KEY_EMAIL, null);
    }

    /**
     * Verificar se usuario esta autenticado
     */
    public boolean isAuthenticated() {
        return getToken() != null;
    }

    /**
     * Fazer logout (limpar dados)
     */
    public void logout() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }

    /**
     * Obter header Authorization para requisicoes HTTP
     */
    public String getAuthorizationHeader() {
        String token = getToken();
        return token != null ? "Bearer " + token : null;
    }
}

