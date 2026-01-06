package com.geoping.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.geoping.datacollection.R;
import com.geoping.app.models.Room;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter para lista de salas
 */
public class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.RoomViewHolder> {

    private List<Room> rooms;
    private OnRoomClickListener listener;
    private boolean isCreator; // Se true, mostra botoes de gerenciamento

    public interface OnRoomClickListener {
        void onEnterRoom(Room room);
        void onManageRoom(Room room);
    }

    public RoomAdapter(boolean isCreator, OnRoomClickListener listener) {
        this.rooms = new ArrayList<>();
        this.isCreator = isCreator;
        this.listener = listener;
    }

    public void setRooms(List<Room> rooms) {
        this.rooms = rooms;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_room, parent, false);
        return new RoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
        Room room = rooms.get(position);
        holder.bind(room);
    }

    @Override
    public int getItemCount() {
        return rooms.size();
    }

    class RoomViewHolder extends RecyclerView.ViewHolder {
        TextView textViewRoomName;
        TextView textViewSsid;
        TextView textViewModelStatus;
        TextView textViewSubscribers;
        TextView textViewPending;
        LinearLayout layoutActions;
        Button buttonEnterChat;
        Button buttonManage;

        RoomViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewRoomName = itemView.findViewById(R.id.textViewRoomName);
            textViewSsid = itemView.findViewById(R.id.textViewSsid);
            textViewModelStatus = itemView.findViewById(R.id.textViewModelStatus);
            textViewSubscribers = itemView.findViewById(R.id.textViewSubscribers);
            textViewPending = itemView.findViewById(R.id.textViewPending);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            buttonEnterChat = itemView.findViewById(R.id.buttonEnterChat);
            buttonManage = itemView.findViewById(R.id.buttonManage);
        }

        void bind(Room room) {
            textViewRoomName.setText(room.getRoomName());
            textViewSsid.setText("Wi-Fi: " + room.getWifiSsid());

            // Status do modelo
            if (room.isModelTrained()) {
                textViewModelStatus.setText("Modelo: Treinado");
                textViewModelStatus.setTextColor(0xFF27AE60); // Verde
            } else {
                textViewModelStatus.setText("Modelo: Nao treinado");
                textViewModelStatus.setTextColor(0xFFE74C3C); // Vermelho
            }

            // Numero de inscritos
            textViewSubscribers.setText(room.getSubscriberCount() + " inscritos");

            // Solicitacoes pendentes (apenas para criadores)
            if (isCreator && room.getPendingCount() > 0) {
                textViewPending.setVisibility(View.VISIBLE);
                textViewPending.setText(room.getPendingCount() + " pendentes");
            } else {
                textViewPending.setVisibility(View.GONE);
            }

            // Botoes de acao
            layoutActions.setVisibility(View.VISIBLE);

            // Botao Entrar
            if (room.isModelTrained() && !room.isBlocked()) {
                buttonEnterChat.setEnabled(true);
                buttonEnterChat.setAlpha(1.0f);
            } else {
                buttonEnterChat.setEnabled(false);
                buttonEnterChat.setAlpha(0.5f);
            }

            buttonEnterChat.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEnterRoom(room);
                }
            });

            // Botao Gerenciar (apenas criadores)
            if (isCreator) {
                buttonManage.setVisibility(View.VISIBLE);
                buttonManage.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onManageRoom(room);
                    }
                });
            } else {
                buttonManage.setVisibility(View.GONE);
            }
        }
    }
}

