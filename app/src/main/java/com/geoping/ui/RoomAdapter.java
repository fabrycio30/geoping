package com.geoping.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.geoping.R;
import com.geoping.model.Room;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter para exibir lista de salas no RecyclerView.
 */
public class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.RoomViewHolder> {
    
    private List<Room> rooms;
    private OnRoomClickListener listener;
    
    public interface OnRoomClickListener {
        void onRoomClick(Room room);
        void onRoomDelete(Room room);
    }
    
    public RoomAdapter(OnRoomClickListener listener) {
        this.rooms = new ArrayList<>();
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
        holder.bind(room, listener);
    }
    
    @Override
    public int getItemCount() {
        return rooms.size();
    }
    
    static class RoomViewHolder extends RecyclerView.ViewHolder {
        TextView roomName;
        TextView roomWifi;
        Button btnDelete;
        
        RoomViewHolder(@NonNull View itemView) {
            super(itemView);
            roomName = itemView.findViewById(R.id.roomName);
            roomWifi = itemView.findViewById(R.id.roomWifi);
            btnDelete = itemView.findViewById(R.id.btnDeleteRoom);
        }
        
        void bind(final Room room, final OnRoomClickListener listener) {
            roomName.setText(room.getRoomName());
            
            if (room.isVirtual()) {
                roomWifi.setText("Sala virtual (sem Wi-Fi)");
                roomWifi.setTextColor(itemView.getContext()
                        .getResources().getColor(R.color.text_hint));
            } else {
                roomWifi.setText("Wi-Fi: " + room.getWifiSSID());
                roomWifi.setTextColor(itemView.getContext()
                        .getResources().getColor(R.color.text_secondary));
            }
            
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onRoomClick(room);
                    }
                }
            });
            
            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onRoomDelete(room);
                    }
                }
            });
        }
    }
}

