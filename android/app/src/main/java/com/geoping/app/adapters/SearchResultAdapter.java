package com.geoping.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.geoping.datacollection.R;
import com.geoping.app.models.Room;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter para resultados de busca de salas
 */
public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.SearchResultViewHolder> {

    private List<Room> rooms;
    private OnSubscribeClickListener listener;

    public interface OnSubscribeClickListener {
        void onSubscribeClick(Room room);
    }

    public SearchResultAdapter(OnSubscribeClickListener listener) {
        this.rooms = new ArrayList<>();
        this.listener = listener;
    }

    public void setRooms(List<Room> rooms) {
        this.rooms = rooms;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SearchResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new SearchResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchResultViewHolder holder, int position) {
        Room room = rooms.get(position);
        holder.bind(room);
    }

    @Override
    public int getItemCount() {
        return rooms.size();
    }

    class SearchResultViewHolder extends RecyclerView.ViewHolder {
        TextView textViewRoomName;
        TextView textViewSsid;
        TextView textViewCreator;
        TextView textViewSubscribers;
        TextView textViewModelStatus;
        Button buttonSubscribe;

        SearchResultViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewRoomName = itemView.findViewById(R.id.textViewRoomName);
            textViewSsid = itemView.findViewById(R.id.textViewSsid);
            textViewCreator = itemView.findViewById(R.id.textViewCreator);
            textViewSubscribers = itemView.findViewById(R.id.textViewSubscribers);
            textViewModelStatus = itemView.findViewById(R.id.textViewModelStatus);
            buttonSubscribe = itemView.findViewById(R.id.buttonSubscribe);
        }

        void bind(Room room) {
            textViewRoomName.setText(room.getRoomName());
            textViewSsid.setText("Wi-Fi: " + room.getWifiSsid());
            textViewCreator.setText("Criada por: " + room.getCreatorUsername());
            textViewSubscribers.setText(room.getSubscriberCount() + " inscritos");

            if (room.isModelTrained()) {
                textViewModelStatus.setText("Modelo: OK");
                textViewModelStatus.setTextColor(0xFF27AE60);
            } else {
                textViewModelStatus.setText("Modelo: Pendente");
                textViewModelStatus.setTextColor(0xFFE74C3C);
            }

            buttonSubscribe.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSubscribeClick(room);
                }
            });
        }
    }
}

