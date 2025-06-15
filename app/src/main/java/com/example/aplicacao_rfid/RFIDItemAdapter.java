package com.example.aplicacao_rfid;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class RFIDItemAdapter extends RecyclerView.Adapter<RFIDItemAdapter.ViewHolder> {
    private List<RFIDItem> items;

    public RFIDItemAdapter(List<RFIDItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_rfiditem_adapter, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RFIDItem item = items.get(position);
        holder.tvItemName.setText(item.getItemName());
        holder.tvLocation.setText(item.getLocationName());
        holder.tvCoordinates.setText(String.format(Locale.getDefault(),
                "Lat: %.6f, Lon: %.6f", item.getLatitude(), item.getLongitude()));
        holder.tvTagId.setText("Tag: " + item.getTagId());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateItems(List<RFIDItem> newItems) {
        items = newItems;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemName, tvLocation, tvCoordinates, tvTagId;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvCoordinates = itemView.findViewById(R.id.tvCoordinates);
            tvTagId = itemView.findViewById(R.id.tvTagId);
        }
    }
}