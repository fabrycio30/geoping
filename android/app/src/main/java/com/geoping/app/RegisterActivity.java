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

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextUsername;
    private EditText editTextEmail;
    private EditText editTextPassword;
    private EditText editTextConfirmPassword;
    private Button buttonRegister;
    private TextView textViewLogin;
    private ProgressBar progressBar;

    private AuthManager authManager;
    private ApiClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authManager = new AuthManager(this);
        apiClient = new ApiClient(this);

        initializeComponents();
        setupListeners();
    }

    private void initializeComponents() {
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewLogin = findViewById(R.id.textViewLogin);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        buttonRegister.setOnClickListener(v -> attemptRegister());

        textViewLogin.setOnClickListener(v -> {
            finish(); // Voltar para LoginActivity
        });
    }

    private void attemptRegister() {
        String username = editTextUsername.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        // Validacoes
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Senha deve ter pelo menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Senhas nao coincidem", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        try {
            JSONObject json = new JSONObject();
            json.put("username", username);
            json.put("email", email);
            json.put("password", password);

            String url = apiClient.buildUrl("/api/auth/register");
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
                        Toast.makeText(RegisterActivity.this,
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
                                JSONObject user = jsonResponse.getJSONObject("user");
                                String token = jsonResponse.getString("token");

                                authManager.saveAuth(
                                        token,
                                        user.getInt("id"),
                                        user.getString("username"),
                                        user.getString("email")
                                );

                                Toast.makeText(RegisterActivity.this,
                                        "Conta criada com sucesso!",
                                        Toast.LENGTH_SHORT).show();

                                // Ir para MainActivity
                                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();

                            } else {
                                String error = jsonResponse.optString("error", "Erro desconhecido");
                                Toast.makeText(RegisterActivity.this,
                                        "Erro: " + error,
                                        Toast.LENGTH_LONG).show();
                            }

                        } catch (JSONException e) {
                            Toast.makeText(RegisterActivity.this,
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

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        buttonRegister.setEnabled(!loading);
        editTextUsername.setEnabled(!loading);
        editTextEmail.setEnabled(!loading);
        editTextPassword.setEnabled(!loading);
        editTextConfirmPassword.setEnabled(!loading);
    }
}

