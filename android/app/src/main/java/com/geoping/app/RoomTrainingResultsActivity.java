package com.geoping.app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.geoping.datacollection.R;

/**
 * Activity para exibir resultado do treinamento de forma simples
 * Focada no usuario final, sem detalhes tecnicos complexos
 */
public class RoomTrainingResultsActivity extends AppCompatActivity {

    private TextView textViewRoomName;
    private TextView textViewWifiSsid;
    private TextView textViewSampleCount;
    private Button buttonFinish;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_training_results);

        // Receber dados da intent
        String roomName = getIntent().getStringExtra("room_name");
        String wifiSsid = getIntent().getStringExtra("wifi_ssid");
        int sampleCount = getIntent().getIntExtra("sample_count", 0);

        initializeViews();
        displayResults(roomName, wifiSsid, sampleCount);
    }

    private void initializeViews() {
        textViewRoomName = findViewById(R.id.textViewRoomName);
        textViewWifiSsid = findViewById(R.id.textViewWifiSsid);
        textViewSampleCount = findViewById(R.id.textViewSampleCount);
        buttonFinish = findViewById(R.id.buttonFinish);

        buttonFinish.setOnClickListener(v -> finish());
    }

    private void displayResults(String roomName, String wifiSsid, int sampleCount) {
        textViewRoomName.setText(roomName);
        textViewWifiSsid.setText("Wi-Fi: " + wifiSsid);
        textViewSampleCount.setText(String.valueOf(sampleCount));
    }

    @Override
    public void onBackPressed() {
        // Desabilitar volta com botao back - usuario deve usar botao CONCLUIR
        // Para evitar confusao na navegacao
    }
}

