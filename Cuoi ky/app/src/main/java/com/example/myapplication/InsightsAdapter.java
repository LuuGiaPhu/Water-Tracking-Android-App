package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class InsightsAdapter extends RecyclerView.Adapter<InsightsAdapter.InsightViewHolder> {

    private final Context context;
    private final List<Insight> insights;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(Insight insight);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public InsightsAdapter(Context context, List<Insight> insights) {
        this.context = context;
        this.insights = insights;
    }

    @NonNull
    @Override
    public InsightViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.card_item, parent, false);
        return new InsightViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InsightViewHolder holder, int position) {
        Insight insight = insights.get(position);
        holder.title.setText(insight.getTitle());

        String imageName = insight.getImageName().replace(".png", "");
        int imageResId = context.getResources().getIdentifier(imageName, "drawable", context.getPackageName());
        if (imageResId != 0) {
            holder.image.setImageResource(imageResId);
        } else {
            holder.image.setImageResource(R.drawable.sample); // Fallback image
        }

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(insight);
            } else {
                showDialog(insight);
            }
        });
    }

    private void showDialog(Insight insight) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_content, null);
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        TextView dialogContent = dialogView.findViewById(R.id.dialogContent);

        dialogTitle.setText(insight.getTitle());
        dialogContent.setText(insight.getContent());

        builder.setView(dialogView);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    @Override
    public int getItemCount() {
        return insights.size();
    }

    static class InsightViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        ImageView image;

        public InsightViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            image = itemView.findViewById(R.id.image);
        }
    }
}