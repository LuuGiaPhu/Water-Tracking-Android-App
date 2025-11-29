package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;

import java.util.List;

public class LeaderboardAdapter extends ArrayAdapter<UserData> {
    private final Context context;
    private final List<UserData> userList;

    public LeaderboardAdapter(@NonNull Context context, @NonNull List<UserData> userList) {
        super(context, R.layout.list_item_leaderboard, userList);
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_leaderboard, parent, false);
        }

        UserData userData = userList.get(position);

        ImageView avatarImageView = convertView.findViewById(R.id.avatarImageView);
        TextView userInfoTextView = convertView.findViewById(R.id.userInfoTextView);

        // Load avatar using Glide
        Glide.with(context)
                .load(userData.getAvatarUrl())
                .placeholder(R.drawable.placeholder) // Placeholder image
                .error(R.drawable.error) // Error image
                .into(avatarImageView);

        // Set user info with rank
        userInfoTextView.setText("Rank: " + (position + 1) + "\nName: " + userData.getName() +
                                 "\nGoal: " + userData.getDailyGoal() + " ml" + "\n Days Achieved: " + userData.getTotalDaysGoalAchieved());

        return convertView;
    }
}