package com.geoping.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.geoping.R;
import com.geoping.model.Room;
import com.geoping.services.RoomManager;

import java.util.List;

/**
 * Activity para selecionar ou criar salas.
 * 
 * Permite ao usuário:
 * - Ver lista de salas disponíveis
 * - Criar nova sala (com ou sem Wi-Fi)
 * - Selecionar uma sala para entrar
 * - Deletar salas
 */
public class RoomSelectorActivity extends AppCompatActivity {
    
    private RecyclerView roomsRecyclerView;
    private RoomAdapter roomAdapter;
    private RoomManager roomManager;
    private Button btnCreateRoom;
    private Button btnCancel;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_selector);
        
        // Inicializa o RoomManager
        roomManager = RoomManager.getInstance(this);
        
        // Inicializa as views
        initViews();
        
        // Configura o RecyclerView
        setupRecyclerView();
        
        // Carrega as salas
        loadRooms();
        
        // Configura listeners
        setupListeners();
    }
    
    /**
     * Inicializa as views.
     */
    private void initViews() {
        roomsRecyclerView = findViewById(R.id.roomsRecyclerView);
        btnCreateRoom = findViewById(R.id.btnCreateRoom);
        btnCancel = findViewById(R.id.btnCancel);
    }
    
    /**
     * Configura o RecyclerView.
     */
    private void setupRecyclerView() {
        roomAdapter = new RoomAdapter(new RoomAdapter.OnRoomClickListener() {
            @Override
            public void onRoomClick(Room room) {
                selectRoom(room);
            }
            
            @Override
            public void onRoomDelete(Room room) {
                showDeleteConfirmation(room);
            }
        });
        
        roomsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        roomsRecyclerView.setAdapter(roomAdapter);
    }
    
    /**
     * Carrega e exibe as salas.
     */
    private void loadRooms() {
        List<Room> rooms = roomManager.getAllRooms();
        roomAdapter.setRooms(rooms);
    }
    
    /**
     * Configura os listeners.
     */
    private void setupListeners() {
        btnCreateRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCreateRoomDialog();
            }
        });
        
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
    
    /**
     * Mostra diálogo para criar nova sala.
     */
    private void showCreateRoomDialog() {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_create_room, null);
        
        final EditText editRoomName = dialogView.findViewById(R.id.editRoomName);
        final EditText editWifiSSID = dialogView.findViewById(R.id.editWifiSSID);
        
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        
        Button btnCreateDialog = dialogView.findViewById(R.id.btnCreateDialog);
        Button btnCancelDialog = dialogView.findViewById(R.id.btnCancelDialog);
        
        btnCreateDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String roomName = editRoomName.getText().toString().trim();
                String wifiSSID = editWifiSSID.getText().toString().trim();
                
                if (roomName.isEmpty()) {
                    Toast.makeText(RoomSelectorActivity.this,
                            "Digite o nome da sala",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Se o SSID estiver vazio, passa null (sala virtual)
                String ssid = wifiSSID.isEmpty() ? null : wifiSSID;
                
                // Cria a sala
                Room room = roomManager.createRoom(roomName, ssid);
                
                // Recarrega a lista
                loadRooms();
                
                // Fecha o diálogo
                dialog.dismiss();
                
                // Mensagem de sucesso
                String message = room.isVirtual() ?
                        "Sala virtual criada: " + roomName :
                        "Sala criada: " + roomName + " (" + ssid + ")";
                Toast.makeText(RoomSelectorActivity.this,
                        message,
                        Toast.LENGTH_SHORT).show();
            }
        });
        
        btnCancelDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        
        dialog.show();
    }
    
    /**
     * Seleciona uma sala e volta para MainActivity.
     */
    private void selectRoom(Room room) {
        // Define a sala selecionada
        roomManager.setSelectedRoom(room.getRoomId());
        
        // Retorna o resultado
        Intent resultIntent = new Intent();
        resultIntent.putExtra("room_id", room.getRoomId());
        resultIntent.putExtra("room_name", room.getRoomName());
        setResult(RESULT_OK, resultIntent);
        
        // Mensagem de sucesso
        Toast.makeText(this,
                "Sala selecionada: " + room.getRoomName(),
                Toast.LENGTH_SHORT).show();
        
        // Fecha a activity
        finish();
    }
    
    /**
     * Mostra confirmação antes de deletar sala.
     */
    private void showDeleteConfirmation(final Room room) {
        new AlertDialog.Builder(this)
                .setTitle("Deletar Sala")
                .setMessage("Deseja realmente deletar a sala \"" + room.getRoomName() + "\"?")
                .setPositiveButton("Deletar", (dialog, which) -> {
                    deleteRoom(room);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
    
    /**
     * Deleta uma sala.
     */
    private void deleteRoom(Room room) {
        boolean success = roomManager.removeRoom(room.getRoomId());
        
        if (success) {
            Toast.makeText(this,
                    "Sala deletada: " + room.getRoomName(),
                    Toast.LENGTH_SHORT).show();
            loadRooms();
        } else {
            Toast.makeText(this,
                    "Erro ao deletar sala",
                    Toast.LENGTH_SHORT).show();
        }
    }
}

