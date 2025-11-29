package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.RecordViewHolder> {

    private List<Record> records;
    private OnRecordLongClickListener longClickListener;

    public RecordAdapter(List<Record> records, OnRecordLongClickListener longClickListener) {
        this.records = records;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.record_item, parent, false);
        return new RecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
        Record record = records.get(position);
        holder.tvRecordId.setText("ID: " + record.getId());
        holder.tvRecordAmount.setText("Water: " + record.getAmount());
        holder.tvRecordTimestamp.setText("Time: " + record.getTimestamp());

        holder.itemView.setOnLongClickListener(v -> {
            longClickListener.onRecordLongClick(record, position);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    static class RecordViewHolder extends RecyclerView.ViewHolder {
        TextView tvRecordId;
        TextView tvRecordAmount;
        TextView tvRecordTimestamp;

        public RecordViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRecordId = itemView.findViewById(R.id.tvRecordId);
            tvRecordAmount = itemView.findViewById(R.id.tvRecordAmount);
            tvRecordTimestamp = itemView.findViewById(R.id.tvRecordTimestamp);
        }
    }

    public interface OnRecordLongClickListener {
        void onRecordLongClick(Record record, int position);
    }
}