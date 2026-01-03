package com.geoping.datacollection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Activity para exibir os resultados do treinamento do modelo
 */
public class TrainingResultsActivity extends AppCompatActivity {

    private TextView textViewRoomLabel;
    private TextView textViewTrainingSummary;
    private TextView textViewInterpretation;
    private TextView textViewTechnicalHeader;
    private TextView textViewTechnicalDetails;
    private ImageView imageViewTrainingHistory;
    private ImageView imageViewReconstructionErrors;
    private ProgressBar progressBarTrainingHistory;
    private ProgressBar progressBarReconstructionErrors;
    private TextView textViewErrorTrainingHistory;
    private TextView textViewErrorReconstructionErrors;
    private Button buttonBack;

    private String roomLabel;
    private String serverUrl;
    private String trainingInfoJson;
    private boolean technicalDetailsExpanded = false;
    private OkHttpClient httpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training_results);

        // Obter dados da Intent
        roomLabel = getIntent().getStringExtra("room_label");
        serverUrl = getIntent().getStringExtra("server_url");
        trainingInfoJson = getIntent().getStringExtra("training_info");

        // Inicializar componentes
        initializeComponents();

        // Preencher informações do treinamento
        populateTrainingInfo();

        // Carregar imagens
        loadTrainingImages();
    }

    private void initializeComponents() {
        textViewRoomLabel = findViewById(R.id.textViewRoomLabel);
        textViewTrainingSummary = findViewById(R.id.textViewTrainingSummary);
        textViewInterpretation = findViewById(R.id.textViewInterpretation);
        textViewTechnicalHeader = findViewById(R.id.textViewTechnicalHeader);
        textViewTechnicalDetails = findViewById(R.id.textViewTechnicalDetails);
        imageViewTrainingHistory = findViewById(R.id.imageViewTrainingHistory);
        imageViewReconstructionErrors = findViewById(R.id.imageViewReconstructionErrors);
        progressBarTrainingHistory = findViewById(R.id.progressBarTrainingHistory);
        progressBarReconstructionErrors = findViewById(R.id.progressBarReconstructionErrors);
        textViewErrorTrainingHistory = findViewById(R.id.textViewErrorTrainingHistory);
        textViewErrorReconstructionErrors = findViewById(R.id.textViewErrorReconstructionErrors);
        buttonBack = findViewById(R.id.buttonBack);

        textViewRoomLabel.setText("Sala: " + roomLabel);
        
        httpClient = new OkHttpClient();

        buttonBack.setOnClickListener(v -> finish());
        
        // Listener para expandir/colapsar detalhes técnicos
        textViewTechnicalHeader.setOnClickListener(v -> toggleTechnicalDetails());
    }
    
    private void toggleTechnicalDetails() {
        technicalDetailsExpanded = !technicalDetailsExpanded;
        
        if (technicalDetailsExpanded) {
            textViewTechnicalDetails.setVisibility(View.VISIBLE);
            textViewTechnicalHeader.setText("Detalhes Tecnicos ▲");
        } else {
            textViewTechnicalDetails.setVisibility(View.GONE);
            textViewTechnicalHeader.setText("Detalhes Tecnicos ▼");
        }
    }
    
    private void populateTrainingInfo() {
        if (trainingInfoJson == null || trainingInfoJson.isEmpty()) {
            textViewTrainingSummary.setText("Informacoes de treinamento nao disponiveis");
            textViewTechnicalDetails.setText("Detalhes tecnicos nao disponiveis");
            return;
        }
        
        try {
            JSONObject trainingInfo = new JSONObject(trainingInfoJson);
            
            // SEÇÃO 1: Resumo do Treinamento (BÁSICO)
            StringBuilder summary = new StringBuilder();
            summary.append("• Amostras utilizadas: ").append(trainingInfo.optInt("samples_used", 0)).append(" scans\n");
            summary.append("• Redes Wi-Fi unicas: ").append(trainingInfo.optInt("unique_bssids", 0)).append(" BSSIDs\n");
            summary.append("• Limiar de decisao: ").append(String.format(Locale.US, "%.6f", trainingInfo.optDouble("threshold", 0))).append("\n");
            summary.append("• Metodo do limiar: ").append(trainingInfo.optString("threshold_method", "N/A")).append("\n");
            
            textViewTrainingSummary.setText(summary.toString());
            
            // SEÇÃO 2: Interpretação (já definida no XML, mas vamos personalizar)
            double threshold = trainingInfo.optDouble("threshold", 0);
            String interpretation = String.format(Locale.US,
                "O modelo aprendeu a assinatura da sala usando One-Class Classification (Autoencoder).\n\n" +
                "Como funciona a classificacao:\n" +
                "• Erro de reconstrucao < %.6f: usuario DENTRO da sala\n" +
                "• Erro de reconstrucao > %.6f: usuario FORA da sala\n\n" +
                "Metodo do limiar: %s\n" +
                "Multiplicador: %.1f × IQR\n\n" +
                "Metodo robusto contra outliers e nao requer dados de outras salas (One-Class).",
                threshold, threshold, 
                trainingInfo.optString("threshold_method", "N/A"),
                trainingInfo.optDouble("threshold_multiplier", 0));
            
            textViewInterpretation.setText(interpretation);
            
            // SEÇÃO 3: Detalhes Técnicos (AVANÇADO)
            StringBuilder technical = new StringBuilder();
            
            technical.append("ARQUITETURA DO MODELO\n");
            technical.append("─────────────────────\n");
            technical.append("• Input: ").append(trainingInfo.optInt("unique_bssids", 0)).append(" features (BSSIDs)\n");
            
            // Hidden layers
            if (trainingInfo.has("hidden_layers")) {
                JSONArray layers = trainingInfo.optJSONArray("hidden_layers");
                if (layers != null && layers.length() > 0) {
                    StringBuilder layersStr = new StringBuilder();
                    for (int i = 0; i < layers.length(); i++) {
                        if (i > 0) layersStr.append(" → ");
                        layersStr.append(layers.optInt(i));
                    }
                    technical.append("• Encoder: ").append(layersStr.toString()).append(" neuronios\n");
                }
            }
            
            technical.append("• Espaco Latente (bottleneck): ").append(trainingInfo.optInt("latent_dim", 0)).append(" dimensoes\n");
            
            // Decoder (espelho do encoder)
            if (trainingInfo.has("hidden_layers")) {
                JSONArray layers = trainingInfo.optJSONArray("hidden_layers");
                if (layers != null && layers.length() > 0) {
                    StringBuilder layersStr = new StringBuilder();
                    for (int i = layers.length() - 1; i >= 0; i--) {
                        if (i < layers.length() - 1) layersStr.append(" → ");
                        layersStr.append(layers.optInt(i));
                    }
                    technical.append("• Decoder: ").append(layersStr.toString()).append(" neuronios\n");
                }
            }
            
            technical.append("• Output: ").append(trainingInfo.optInt("unique_bssids", 0)).append(" features (reconstrucao)\n\n");
            
            technical.append("HIPERPARAMETROS\n");
            technical.append("───────────────\n");
            technical.append("• Epocas: ").append(trainingInfo.optInt("epochs", 0)).append("\n");
            technical.append("• Batch size: ").append(trainingInfo.optInt("batch_size", 0)).append("\n");
            technical.append("• Validation split: ").append(String.format(Locale.US, "%.0f%%", trainingInfo.optDouble("validation_split", 0) * 100)).append("\n");
            technical.append("• Funcao de ativacao: ").append(trainingInfo.optString("activation", "N/A")).append("\n");
            technical.append("• Otimizador: ").append(trainingInfo.optString("optimizer", "N/A")).append("\n");
            technical.append("• Funcao de perda: ").append(trainingInfo.optString("loss_function", "N/A").toUpperCase()).append(" (Mean Squared Error)\n\n");
            
            technical.append("CALCULO DO LIMIAR\n");
            technical.append("─────────────────\n");
            technical.append("• Metodo: ").append(trainingInfo.optString("threshold_method", "N/A")).append(" (Interquartile Range)\n");
            technical.append("• Multiplicador: ").append(trainingInfo.optDouble("threshold_multiplier", 0)).append(" × IQR\n");
            technical.append("• Formula: Q3 + ").append(trainingInfo.optDouble("threshold_multiplier", 0)).append(" × (Q3 - Q1)\n");
            technical.append("• Limiar resultante: ").append(String.format(Locale.US, "%.6f", threshold)).append("\n\n");
            
            technical.append("METADATA\n");
            technical.append("────────\n");
            String trainingDate = trainingInfo.optString("training_date", "");
            if (!trainingDate.isEmpty()) {
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                    SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US);
                    Date date = inputFormat.parse(trainingDate.substring(0, 19));
                    technical.append("• Data do treinamento: ").append(outputFormat.format(date)).append("\n");
                } catch (Exception e) {
                    technical.append("• Data do treinamento: ").append(trainingDate).append("\n");
                }
            }
            
            textViewTechnicalDetails.setText(technical.toString());
            
        } catch (JSONException e) {
            textViewTrainingSummary.setText("Erro ao processar informacoes: " + e.getMessage());
            textViewTechnicalDetails.setText("Erro ao processar detalhes tecnicos");
        }
    }

    private void loadTrainingImages() {
        // Carregar gráfico de histórico de treinamento
        String trainingHistoryUrl = serverUrl + "/api/training-results/" + roomLabel + "/training_history.png";
        loadImage(trainingHistoryUrl, imageViewTrainingHistory, progressBarTrainingHistory, 
                  textViewErrorTrainingHistory, "Histórico de Treinamento");

        // Carregar gráfico de erros de reconstrução
        String reconstructionErrorsUrl = serverUrl + "/api/training-results/" + roomLabel + "/reconstruction_errors.png";
        loadImage(reconstructionErrorsUrl, imageViewReconstructionErrors, progressBarReconstructionErrors,
                  textViewErrorReconstructionErrors, "Erros de Reconstrução");
    }

    private void loadImage(String url, ImageView imageView, ProgressBar progressBar, 
                          TextView errorTextView, String imageName) {
        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    errorTextView.setVisibility(View.VISIBLE);
                    errorTextView.setText("Erro ao carregar " + imageName + ": " + e.getMessage());
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    // Carregar imagem do stream
                    InputStream inputStream = response.body().byteStream();
                    final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                    runOnUiThread(() -> {
                        if (bitmap != null) {
                            progressBar.setVisibility(View.GONE);
                            imageView.setVisibility(View.VISIBLE);
                            imageView.setImageBitmap(bitmap);
                        } else {
                            progressBar.setVisibility(View.GONE);
                            errorTextView.setVisibility(View.VISIBLE);
                            errorTextView.setText("Erro ao decodificar " + imageName);
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        errorTextView.setVisibility(View.VISIBLE);
                        errorTextView.setText("HTTP " + response.code() + ": " + imageName + " não encontrado");
                    });
                }
            }
        });
    }
}

