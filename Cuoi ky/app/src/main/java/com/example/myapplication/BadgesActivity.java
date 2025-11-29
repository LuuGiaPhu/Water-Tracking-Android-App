package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BadgesActivity extends AppCompatActivity {
    private DatabaseHelper databaseHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_badges); // Link to activity_badges.xml
        databaseHelper = new DatabaseHelper(this);
        ImageView imgBadge1 = findViewById(R.id.imgBadge1);
        // Set a long click listener to show badge info
        imgBadge1.setOnLongClickListener(v -> {
            showBadgeInfoDialog("Badge 1", "Hoàn thành 1 lần uống nước");
            return true; // Indicate the event is handled
        });
        ImageView imgBadge2 = findViewById(R.id.imgBadge2);
        imgBadge2.setOnLongClickListener(v -> {
            showBadgeInfoDialog("Badge 2", "Đạt hạng nhất trong bảng xếp hạng");
            return true; // Indicate the event is handled
        });
        ImageView imgBadge3 = findViewById(R.id.imgBadge3);
        imgBadge3.setOnLongClickListener(v -> {
            showBadgeInfoDialog("Badge 3", "Uống nước đều đặn trong 3 ngày");
            return true; // Indicate the event is handled
        });
        ImageView imgBadge4 = findViewById(R.id.imgBadge4);
        imgBadge4.setOnLongClickListener(v -> {
            showBadgeInfoDialog("Badge 4", "Uống nước đều đặn trong 7 ngày");
            return true; // Indicate the event is handled
        });
        ImageView imgBadge5 = findViewById(R.id.imgBadge5);
        imgBadge5.setOnLongClickListener(v -> {
            showBadgeInfoDialog("Badge 5", "Uống nước đều đặn trong 14 ngày");
            return true; // Indicate the event is handled
        });
        ImageView imgBadge6 = findViewById(R.id.imgBadge6);
        imgBadge6.setOnLongClickListener(v -> {
            showBadgeInfoDialog("Badge 6", "Có nhiều lần uống nước nhất");
            return true; // Indicate the event is handled
        });

        // Apply blur effect by default
        setBlurEffect(imgBadge1, true);
        setBlurEffect(imgBadge2, true);
        setBlurEffect(imgBadge3, true);
        setBlurEffect(imgBadge4, true);
        setBlurEffect(imgBadge5, true);
        setBlurEffect(imgBadge6, true);

        String userId = getUserId();
        if (userId != null) {
            // Check water intake data
            if (checkWaterIntakeData(userId)) {
                setBlurEffect(imgBadge1, false);
            }

            // Check if the user is the highest achiever
            checkIfHighestAchiever(userId, isHighestAchiever -> {
                if (isHighestAchiever) {
                    setBlurEffect(imgBadge2, false);
                }
            });

            // Check if the user has achieved the goal for 3 consecutive days
            if (hasContinuousDataForThreeDays(userId)) {
                setBlurEffect(imgBadge3, false);
            }
            // Check if the user has consumed over 5000 liters
            if (hasUserConsumedOver5000Liters(userId)) {
                setBlurEffect(imgBadge4, false);
            }
            //Check  if height and weight > 0
            if (isHeightAndWeightValid(userId)) {
                setBlurEffect(imgBadge5, false);
            }

            // Check if the user has the most water intake
            checkIfUserHasMostWaterIntake(userId, (isMostWaterIntake, currentUserEntries, maxEntries) -> {
                if (isMostWaterIntake) {
                    setBlurEffect(imgBadge6, false);
                }
            });
        }


        // Find the back button
        ImageView btnBack = findViewById(R.id.btnBack);
        // Find the TextView for points
        TextView tvPoints = findViewById(R.id.tvPoints);

        // Set an OnClickListener to navigate back to the leaderboard activity
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to LeaderboardActivity
                Intent intent = new Intent(BadgesActivity.this, LeaderboardActivity.class);
                startActivity(intent);
                finish(); // Optional: Close the current activity
            }
        });
        // Find the CircularImageView for the avatar
        ImageView imgAvatar = findViewById(R.id.imgAvatar);
        // Find the TextView for the username
        TextView tvUsername = findViewById(R.id.tvUsername);
        // Find the TextView for the email
        TextView tvHandle = findViewById(R.id.tvHandle);

        // Get the currently signed-in Google account
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            // Get the avatar URL from the Google account
//            String userId = account.getId();
            Uri photoUri = account.getPhotoUrl();
            int totalDaysGoalAchieved = getTotalDaysGoalAchieved(userId);
            tvPoints.setText(totalDaysGoalAchieved + " pts ▲");
            Log.d("BadgesActivity", "Photo URI: " + photoUri);
            if (photoUri != null) {
                String avatarUrl = photoUri.toString();
                Log.d("BadgesActivity", "Loading avatar from URL: " + avatarUrl);
                // Load the avatar into the CircularImageView using Glide
                Glide.with(this)
                        .load(avatarUrl)
                        .placeholder(R.drawable.placeholder) // Placeholder image
                        .error(R.drawable.error) // Error image
                        .into(imgAvatar);
            } else {
                Log.d("BadgesActivity", "No avatar URL available, setting default avatar.");
                setDefaultAvatar(imgAvatar, "https://lh3.google.com/u/0/ogw/AF2bZygUXMIqJbUHMhhKpDsfHhvQTO0I6WYBPHwyeqlZLXdgag=s32-c-mo");
            }

            // Get the user's name and set it to the TextView
            String displayName = account.getDisplayName();
            if (displayName != null) {
                tvUsername.setText(displayName);
            } else {
                Log.d("BadgesActivity", "No display name available.");
                tvUsername.setText("Unknown User");
            }

            // Get the user's email and set it to the TextView
            String email = account.getEmail();
            if (email != null) {
                tvHandle.setText(email);
            } else {
                Log.d("BadgesActivity", "No email available.");
                tvHandle.setText("No Email");
            }
        } else {
            Log.d("BadgesActivity", "No Google account signed in, setting default avatar, username, and email.");
            setDefaultAvatar(imgAvatar, "https://lh3.google.com/u/0/ogw/AF2bZygUXMIqJbUHMhhKpDsfHhvQTO0I6WYBPHwyeqlZLXdgag=s32-c-mo");
            tvUsername.setText("Guest");
            tvHandle.setText("No Email");
        }
    }
    private String getUserId() {
        // Replace this with your logic to get the user ID
        return GoogleSignIn.getLastSignedInAccount(this) != null
                ? GoogleSignIn.getLastSignedInAccount(this).getId()
                : null;
    }

    private boolean checkWaterIntakeData(String userId) {
        boolean hasData = false;
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = databaseHelper.getReadableDatabaseInstance();
            cursor = db.rawQuery("SELECT 1 FROM " + DatabaseHelper.TABLE_WATER_INTAKE + " WHERE " + DatabaseHelper.COLUMN_ID_USER + " = ? LIMIT 1", new String[]{userId});
            hasData = cursor != null && cursor.moveToFirst();
        } catch (Exception e) {
            Log.e("BadgesActivity", "Error checking water intake data", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return hasData;
    }

    private void setBlurEffect(ImageView imageView, boolean blur) {
        if (blur) {
            // Apply grayscale effect to simulate blur
            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(0); // Set saturation to 0 for grayscale
            imageView.setColorFilter(new ColorMatrixColorFilter(matrix));
            imageView.setAlpha(0.5f); // Reduce opacity
        } else {
            // Remove grayscale effect
            imageView.setColorFilter(null);
            imageView.setAlpha(1.0f); // Restore full opacity
        }
    }
    // Helper method to set the default avatar
    // Updated setDefaultAvatar method
    private void setDefaultAvatar(ImageView imgAvatar, String defaultAvatarUrl) {
        if (imgAvatar != null) {
            Glide.with(this)
                    .load(defaultAvatarUrl)
                    .placeholder(R.drawable.placeholder) // Placeholder image
                    .error(R.drawable.error) // Error image
                    .into(imgAvatar);
        }
    }
    private int getTotalDaysGoalAchieved(String userId) {
        int totalDaysGoalAchieved = 0;
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = databaseHelper.getReadableDatabaseInstance();
            cursor = db.rawQuery("SELECT TotalDaysGoalAchieved FROM " + DatabaseHelper.TABLE_USER + " WHERE " + DatabaseHelper.COLUMN_ID + " = ?", new String[]{userId});
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex("TotalDaysGoalAchieved");
                if (columnIndex >= 0) {
                    totalDaysGoalAchieved = cursor.getInt(columnIndex);
                }
            }
        } catch (Exception e) {
            Log.e("BadgesActivity", "Error retrieving TotalDaysGoalAchieved", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return totalDaysGoalAchieved;
    }
    private void checkIfHighestAchiever(String userId, HighestAchieverCallback callback) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("databases");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d("BadgesActivity", "DataSnapshot: " + dataSnapshot.toString());
                String highestUserId = null;
                int maxAchieved = -1;

                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    DataSnapshot userNode = userSnapshot.child("user").child("0");
                    String currentUserId = userNode.child("_id").getValue(String.class);
                    Integer currentAchieved = userNode.child("TotalDaysGoalAchieved").getValue(Integer.class);

                    if (currentAchieved != null) {
                        if (currentAchieved > maxAchieved) {
                            maxAchieved = currentAchieved;
                            highestUserId = currentUserId;
                        }
                    }
                }

                boolean isHighest = userId.equals(highestUserId);
                Log.d("BadgesActivity", "Is Highest Achiever: " + isHighest);
                callback.onResult(isHighest);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("BadgesActivity", "Error checking highest achiever", databaseError.toException());
                callback.onResult(false); // Return false in case of error
            }
        });
    }
    public interface HighestAchieverCallback {
        void onResult(boolean isHighestAchiever);
    }
    private void showBadgeInfoDialog(String title, String description) {
        // Inflate the custom dialog layout
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_item_info, null);

        // Set the title and description
        TextView tvBadgeTitle = dialogView.findViewById(R.id.tvBadgeTitle);
        TextView tvBadgeDescription = dialogView.findViewById(R.id.tvBadgeDescription);
        tvBadgeTitle.setText(title);
        tvBadgeDescription.setText(description);

        // Create and show the dialog
        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .show();
    }
    private boolean hasContinuousDataForThreeDays(String userId) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = databaseHelper.getReadableDatabaseInstance();
            // Truy xuất tất cả các bản ghi của user, sắp xếp theo thời gian
            cursor = db.rawQuery(
                "SELECT DISTINCT DATE(" + DatabaseHelper.COLUMN_INTAKE_TIMESTAMP + ") AS intake_date " +
                "FROM " + DatabaseHelper.TABLE_WATER_INTAKE + " " +
                "WHERE " + DatabaseHelper.COLUMN_ID_USER + " = ? " +
                "ORDER BY intake_date ASC",
                new String[]{userId}
            );

            // Lưu các ngày vào danh sách
            List<String> intakeDates = new ArrayList<>();
            while (cursor != null && cursor.moveToNext()) {
                intakeDates.add(cursor.getString(cursor.getColumnIndex("intake_date")));
            }

            // Kiểm tra 3 ngày liên tiếp
            for (int i = 0; i < intakeDates.size() - 2; i++) {
                String day1 = intakeDates.get(i);
                String day2 = intakeDates.get(i + 1);
                String day3 = intakeDates.get(i + 2);

                // So sánh ngày liên tiếp
                if (isNextDay(day1, day2) && isNextDay(day2, day3)) {
                    return true; // Có 3 ngày liên tiếp
                }
            }
        } catch (Exception e) {
            Log.e("BadgesActivity", "Error checking continuous data for 3 days", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return false; // Không có 3 ngày liên tiếp
    }

    // Hàm kiểm tra nếu day2 là ngày kế tiếp của day1
    private boolean isNextDay(String day1, String day2) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date date1 = dateFormat.parse(day1);
            Date date2 = dateFormat.parse(day2);
            if (date1 != null && date2 != null) {
                long diff = date2.getTime() - date1.getTime();
                return diff == 24 * 60 * 60 * 1000; // 1 ngày (24 giờ)
            }
        } catch (Exception e) {
            Log.e("BadgesActivity", "Error parsing dates", e);
        }
        return false;
    }
    private void checkIfUserHasMostWaterIntake(String userId, MostWaterIntakeCallback callback) {
                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("databases");
                databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            Map<String, Integer> userEntryCounts = new HashMap<>();
                            int maxEntries = 0;
                            String userWithMostEntries = null;

                            // Iterate through each user in the database
                            for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                                DataSnapshot waterIntakeSnapshot = userSnapshot.child("water_intake");
                                if (waterIntakeSnapshot.exists()) {
                                    int entryCount = (int) waterIntakeSnapshot.getChildrenCount();
                                    String currentUserId = userSnapshot.getKey();

                                    // Update the count for the current user
                                    userEntryCounts.put(currentUserId, entryCount);

                                    // Track the user with the most entries
                                    if (entryCount > maxEntries) {
                                        maxEntries = entryCount;
                                        userWithMostEntries = currentUserId;
                                    }
                                }
                            }

                            // Get the current user's entry count
                            int currentUserEntries = userEntryCounts.getOrDefault(userId, 0);
                            boolean isMostWaterIntake = userId.equals(userWithMostEntries);

                            // Log results
                            Log.d("WaterIntakeCheck", "User ID: " + userId + ", Entries: " + currentUserEntries);
                            Log.d("WaterIntakeCheck", "User with most entries: " + userWithMostEntries + ", Max Entries: " + maxEntries);

                            // Return the result via callback
                            callback.onResult(isMostWaterIntake, currentUserEntries, maxEntries);
                        } else {
                            Log.w("WaterIntakeCheck", "No data found in databases.");
                            callback.onResult(false, 0, 0);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e("WaterIntakeCheck", "Error checking water intake data", databaseError.toException());
                        callback.onResult(false, 0, 0); // Return false in case of error
                    }
                });
            }


    // Callback interface
    public interface MostWaterIntakeCallback {
        void onResult(boolean isMostWaterIntake, int currentUserEntries, int maxEntries);
    }
    private boolean hasUserConsumedOver5000Liters(String userId) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        boolean hasConsumedOver5000 = false;

        try {
            // Mở cơ sở dữ liệu
            db = databaseHelper.getReadableDatabaseInstance();

            // Truy vấn tổng lượng nước uống của user
            String query = "SELECT SUM(" + DatabaseHelper.COLUMN_INTAKE_AMOUNT + ") AS total_intake " +
                           "FROM " + DatabaseHelper.TABLE_WATER_INTAKE + " " +
                           "WHERE " + DatabaseHelper.COLUMN_ID_USER + " = ?";
            cursor = db.rawQuery(query, new String[]{userId});

            if (cursor != null && cursor.moveToFirst()) {
                // Lấy tổng lượng nước uống
                int totalIntake = cursor.getInt(cursor.getColumnIndex("total_intake"));

                // Kiểm tra nếu tổng lượng nước uống >= 5000 lít
                hasConsumedOver5000 = totalIntake >= 5000;
            }
        } catch (Exception e) {
            Log.e("WaterIntakeCheck", "Error checking total water intake", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return hasConsumedOver5000;
    }
    private boolean isHeightAndWeightValid(String userId) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        boolean isValid = false;

        try {
            // Mở cơ sở dữ liệu
            db = databaseHelper.getReadableDatabaseInstance();

            // Truy vấn cột _id, height, và weight
            String query = "SELECT " + DatabaseHelper.COLUMN_ID + ", " +
                           DatabaseHelper.COLUMN_HEIGHT + ", " +
                           DatabaseHelper.COLUMN_WEIGHT +
                           " FROM " + DatabaseHelper.TABLE_USER +
                           " WHERE " + DatabaseHelper.COLUMN_ID + " = ?";
            cursor = db.rawQuery(query, new String[]{userId});

            if (cursor != null && cursor.moveToFirst()) {
                // Lấy giá trị height và weight
                double height = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_HEIGHT));
                double weight = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_WEIGHT));

                // Kiểm tra nếu height và weight > 0
                isValid = height > 0 && weight > 0;
            }
        } catch (Exception e) {
            Log.e("UserValidation", "Error checking height and weight", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return isValid;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        databaseHelper.closeDatabase();
    }
}