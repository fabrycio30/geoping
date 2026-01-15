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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.geoping.datacollection.R;
import com.geoping.datacollection.DataCollectionActivity;
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

public class LoginActivity extends AppCompatActivity {

    private EditText editTextUsername;
    private EditText editTextPassword;
    private EditText editTextServerUrl; // Novo campo
    private Button buttonLogin;
    private TextView textViewRegister;
    private TextView textViewDevMode;
    private ProgressBar progressBar;

    private AuthManager authManager;
    private ApiClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authManager = new AuthManager(this);
        apiClient = new ApiClient(this); // Carrega URL salva automaticamente

        initializeComponents();
        
        // Preencher URL salva
        editTextServerUrl.setText(apiClient.getServerUrl());

        // Se ja esta autenticado, ir direto para tela principal
        if (authManager.isAuthenticated()) {
            goToMainActivity();
            return;
        }

        setupListeners();
    }

    private void initializeComponents() {
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextServerUrl = findViewById(R.id.editTextServerUrl); // Init
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewRegister = findViewById(R.id.textViewRegister);
        textViewDevMode = findViewById(R.id.textViewDevMode);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        buttonLogin.setOnClickListener(v -> attemptLogin());

        textViewRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        textViewDevMode.setOnClickListener(v -> showDevModeDialog());

        // Logo com contador para menu dev (7 toques)
        TextView logo = findViewById(R.id.textViewLogo);
        final int[] clickCount = {0};
        logo.setOnClickListener(v -> {
            clickCount[0]++;
            if (clickCount[0] >= 7) {
                clickCount[0] = 0;
                showDevModeDialog();
            }
        });
    }

    private void attemptLogin() {
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String serverUrl = editTextServerUrl.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty() || serverUrl.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Atualizar e Salvar URL do servidor
        apiClient.setServerUrl(serverUrl);
        ApiClient.setBaseUrl(serverUrl); // Atualiza estático também para garantir

        setLoading(true);

        try {
            // Criar JSON payload
            JSONObject json = new JSONObject();
            json.put("username", username);
            json.put("password", password);

            // Fazer requisicao
            String url = apiClient.buildUrl("/api/auth/login");
            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            apiClient.getHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this,
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
                                // Salvar dados de autenticacao
                                JSONObject user = jsonResponse.getJSONObject("user");
                                String token = jsonResponse.getString("token");

                                authManager.saveAuth(
                                        token,
                                        user.getInt("id"),
                                        user.getString("username"),
                                        user.getString("email")
                                );

                                Toast.makeText(LoginActivity.this,
                                        "Login realizado com sucesso!",
                                        Toast.LENGTH_SHORT).show();

                                goToMainActivity();

                            } else {
                                String error = jsonResponse.optString("error", "Erro desconhecido");
                                Toast.makeText(LoginActivity.this,
                                        "Erro: " + error,
                                        Toast.LENGTH_LONG).show();
                            }

                        } catch (JSONException e) {
                            Toast.makeText(LoginActivity.this,
                                    "Erro ao processar resposta: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });

        } catch (JSONException e) {
            setLoading(false);
            Toast.makeText(this, "Erro ao criar requisicao: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void showDevModeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Modo Teste/Analise");
        builder.setMessage("Digite a chave de acesso:");

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String password = input.getText().toString();
            if ("chave-dev-analise".equals(password)) {
                // Abrir DataCollectionActivity (app de teste)
                Intent intent = new Intent(LoginActivity.this, DataCollectionActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(LoginActivity.this,
                        "Chave incorreta", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        buttonLogin.setEnabled(!loading);
        editTextUsername.setEnabled(!loading);
        editTextPassword.setEnabled(!loading);
    }

    private void goToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

