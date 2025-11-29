package com.example.myapplication;

    import android.content.Intent;
    import android.os.Bundle;
    import android.util.Log;
    import android.widget.Button;
    import android.widget.ImageButton;
    import android.widget.ImageView;
    import android.widget.ListView;
    import android.widget.TextView;

    import androidx.annotation.NonNull;
    import androidx.appcompat.app.AppCompatActivity;

    import com.bumptech.glide.Glide;
    import com.google.firebase.database.DataSnapshot;
    import com.google.firebase.database.DatabaseError;
    import com.google.firebase.database.DatabaseReference;
    import com.google.firebase.database.FirebaseDatabase;
    import com.google.firebase.database.ValueEventListener;

    import java.util.ArrayList;
    import java.util.List;

    public class LeaderboardActivity extends AppCompatActivity {
        private ListView leaderboardListView;
        private DatabaseReference databaseReference;
        private List<UserData> userList = new ArrayList<>(); // Declare userList as a class-level variable
        private LeaderboardAdapter adapter; // Declare the adapter as a class-level variable

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_leaderboard);

            // Initialize the return button
            ImageButton btnReturn = findViewById(R.id.btnReturn);
            btnReturn.setOnClickListener(v -> onBackPressed());

            // Initialize the ListView
            leaderboardListView = findViewById(R.id.leaderboardList);

            // Initialize the "View achievements" button
            Button viewAchievementsButton = findViewById(R.id.View_achievements);
            viewAchievementsButton.setOnClickListener(v -> {
                // Navigate to BadgesActivity
                Intent intent = new Intent(LeaderboardActivity.this, BadgesActivity.class);
                startActivity(intent);
            });

            // Initialize Firebase Database reference
            databaseReference = FirebaseDatabase.getInstance().getReference("databases");

            // Initialize the adapter and set it to the ListView
            adapter = new LeaderboardAdapter(this, userList);
            leaderboardListView.setAdapter(adapter);

            // Load leaderboard data on app start
            loadLeaderboardData();

            // Listen for real-time updates
            setupRealTimeUpdates();
        }

        private void loadLeaderboardData() {
            Log.d("LeaderboardActivity", "Fetching leaderboard data...");

            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        userList.clear(); // Clear the existing data

                        for (DataSnapshot userIdSnapshot : snapshot.getChildren()) {
                            DataSnapshot userArraySnapshot = userIdSnapshot.child("user");
                            if (userArraySnapshot.exists()) {
                                for (DataSnapshot userSnapshot : userArraySnapshot.getChildren()) {
                                    String name = userSnapshot.child("name").getValue(String.class);
                                    String avatar = userSnapshot.child("avatar").getValue(String.class);
                                    Long dailyGoal = userSnapshot.child("daily_goal").getValue(Long.class);
                                    Long totalDaysGoalAchieved = userSnapshot.child("TotalDaysGoalAchieved").getValue(Long.class);
                                    Log.d("LeaderboardActivity", "User data: " + name + ", " + avatar + ", " + dailyGoal + ", " + totalDaysGoalAchieved);

                                    if (name == null) name = "Unknown";
                                    if (avatar == null) avatar = ""; // Default to empty string
                                    if (dailyGoal == null) dailyGoal = 2000L;
                                    if (totalDaysGoalAchieved == null) totalDaysGoalAchieved = 0L;

                                    userList.add(new UserData(name, avatar, dailyGoal, totalDaysGoalAchieved));
                                }
                            }
                        }

                        // Notify the adapter of data changes
                        adapter.notifyDataSetChanged();

                        // Update the top three users
                        displayTopThreeUsers(userList);
                    } else {
                        Log.w("LeaderboardActivity", "No data found in the database.");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("LeaderboardActivity", "Failed to retrieve data.", error.toException());
                }
            });
        }

        private void setupRealTimeUpdates() {
            databaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Log.d("LeaderboardActivity", "Real-time data update received.");
                    loadLeaderboardData(); // Reload leaderboard data
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("LeaderboardActivity", "Real-time update failed.", error.toException());
                }
            });
        }

        private void displayTopThreeUsers(List<UserData> userList) {
            if (isDestroyed() || isFinishing()) {
                Log.w("LeaderboardActivity", "Activity is destroyed or finishing. Skipping UI update.");
                return;
            }

            userList.sort((u1, u2) -> Long.compare(u2.getTotalDaysGoalAchieved(), u1.getTotalDaysGoalAchieved()));

            List<UserData> topThreeUsers = userList.subList(0, Math.min(3, userList.size()));

            if (topThreeUsers.size() > 0) {
                UserData firstPlaceUser = topThreeUsers.get(0);
                Glide.with(this)
                        .load(firstPlaceUser.getAvatarUrl())
                        .placeholder(R.drawable.placeholder)
                        .error(R.drawable.error)
                        .into((ImageView) findViewById(R.id.firstPlaceAvatar));
                ((TextView) findViewById(R.id.firstPlaceName)).setText(firstPlaceUser.getName());
                ((TextView) findViewById(R.id.firstPlaceScore)).setText("💧" + firstPlaceUser.getTotalDaysGoalAchieved() + " days");
            }

            if (topThreeUsers.size() > 1) {
                UserData secondPlaceUser = topThreeUsers.get(1);
                Glide.with(this)
                        .load(secondPlaceUser.getAvatarUrl())
                        .placeholder(R.drawable.placeholder)
                        .error(R.drawable.error)
                        .into((ImageView) findViewById(R.id.secondPlaceAvatar));
                ((TextView) findViewById(R.id.secondPlaceName)).setText(secondPlaceUser.getName());
                ((TextView) findViewById(R.id.secondPlaceScore)).setText("💧" + secondPlaceUser.getTotalDaysGoalAchieved() + " days");
            }

            if (topThreeUsers.size() > 2) {
                UserData thirdPlaceUser = topThreeUsers.get(2);
                Glide.with(this)
                        .load(thirdPlaceUser.getAvatarUrl())
                        .placeholder(R.drawable.placeholder)
                        .error(R.drawable.error)
                        .into((ImageView) findViewById(R.id.thirdPlaceAvatar));
                ((TextView) findViewById(R.id.thirdPlaceName)).setText(thirdPlaceUser.getName());
                ((TextView) findViewById(R.id.thirdPlaceScore)).setText("💧" + thirdPlaceUser.getTotalDaysGoalAchieved() + " days");
            }
        }
        @Override
        public void onBackPressed() {
            super.onBackPressed(); // Call the super method
            Intent intent = new Intent(LeaderboardActivity.this, MainActivity.class);
            intent.putExtra("loadMeLayout", true);
            startActivity(intent);
            finish();
        }
    }