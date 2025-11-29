package com.example.myapplication;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

import javax.mail.MessagingException;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String TAG = "NotificationReceiver";
    private static final String CHANNEL_ID = "reminder_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        String reminderLabel = intent.getStringExtra("reminderLabel");
        Log.d(TAG, "Received reminder: " + reminderLabel);

        // Create notification channel
        createNotificationChannel(context);

        // Check for POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "POST_NOTIFICATIONS permission not granted");
            return; // Permission not granted, do not show notification
        }

        // Get SharedPreferences
        SharedPreferences sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE);

        // Check if switchReminder is enabled
        boolean isReminderEnabled = sharedPreferences.getBoolean("switchReminder", false);
        if (!isReminderEnabled) {
            Log.d(TAG, "Reminder is disabled, skipping notification");
            return; // Reminder is disabled, do not show notification
        }

        // Check if stopWhenGoalAchieved is enabled
        boolean isStopWhenGoalAchievedEnabled = sharedPreferences.getBoolean("stopWhenGoalAchieved", false);
        if (isStopWhenGoalAchievedEnabled) {
            DatabaseHelper dbHelper = new DatabaseHelper(context);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT SUM(intake_amount) AS totalIntake FROM water_intake", null);
            int totalIntake = 0;
            if (cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex("totalIntake");
                if (columnIndex != -1) {
                    totalIntake = cursor.getInt(columnIndex);
                }
            }
            cursor.close();

            Cursor userCursor = db.query(DatabaseHelper.TABLE_USER, null, null, null, null, null, null);
            int dailyGoal = 0;
            if (userCursor.moveToFirst()) {
                int columnIndex = userCursor.getColumnIndex(DatabaseHelper.COLUMN_DAILY_GOAL);
                if (columnIndex != -1) {
                    dailyGoal = userCursor.getInt(columnIndex);
                }
            }
            userCursor.close();

            if (totalIntake >= dailyGoal) {
                Log.d(TAG, "Daily goal achieved, skipping notification");
                return; // Daily goal achieved, do not show notification
            }
        }

        // Get the current mode
        String currentMode = sharedPreferences.getString("selectedModeName", "Standard Mode");

        // Get bedtime and wakeup time from SharedPreferences
        String bedtime = sharedPreferences.getString("bedtime", "23:00");
        String wakeup = sharedPreferences.getString("wakeup", "08:00");

        String[] bedtimeParts = bedtime.split(":");
        int bedtimeHour = Integer.parseInt(bedtimeParts[0]);
        int bedtimeMinute = Integer.parseInt(bedtimeParts[1]);

        String[] wakeupParts = wakeup.split(":");
        int wakeupHour = Integer.parseInt(wakeupParts[0]);
        int wakeupMinute = Integer.parseInt(wakeupParts[1]);

        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        // Skip notification during sleep period only if in Interval Mode
        if ("Interval Mode".equals(currentMode) && isWithinSleepPeriod(hour, minute, bedtimeHour, bedtimeMinute, wakeupHour, wakeupMinute)) {
            Log.d(TAG, "Within sleep period, skipping notification");
            return; // Skip notification during sleep period
        }

        // Create notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // Ensure this icon exists in res/drawable
                .setContentTitle("Water Reminder")
                .setContentText("It's time to drink water: " + reminderLabel)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        // Show notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(0, builder.build());
        Log.d(TAG, "Notification shown");

        // Get the signed-in Google account
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account != null) {
            String userEmail = account.getEmail();
            if (userEmail != null) {
                // Send email in background thread
                Executors.newSingleThreadExecutor().execute(() -> {
                    EmailSender emailSender = new EmailSender();
                    try {
                        String emailContent = generateEmailContent(context);
                        emailSender.sendEmail(userEmail, "Water Reminder", emailContent);
                        Log.d(TAG, "Email sent to: " + userEmail);
                    } catch (MessagingException e) {
                        Log.e(TAG, "Failed to send email", e);
                    }
                });
            } else {
                Log.e(TAG, "User email is null");
            }
        } else {
            Log.e(TAG, "No Google account signed in");
        }
    }
    private String generateEmailContent(Context context) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account == null) {
            Log.e("generateEmailContent", "No Google account found");
            return "No Google account found.";
        }
        String userId = account.getId();
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = sdf.format(calendar.getTime());
        // Add title to the email content
        StringBuilder emailContent = new StringBuilder("Water App Tracking Report\n\n");

        // Get daily intake
        List<Integer> dailyIntake = dbHelper.getWaterIntakeByDay(todayDate, userId);
        emailContent.append("Daily Water Intake:\n");
        for (int hour = 0; hour < 24; hour++) {
            emailContent.append(String.format(Locale.getDefault(), "%02d:00 - %d ml\n", hour, dailyIntake.get(hour)));
        }

        // Get weekly intake
        List<Integer> weeklyIntake = dbHelper.getWaterIntakeByCustomWeek(todayDate, userId);
        emailContent.append("\nWeekly Water Intake:\n");
        for (int week = 0; week < 4; week++) {
            emailContent.append(String.format(Locale.getDefault(), "Day %d - %d ml\n", week + 1, weeklyIntake.get(week)));
        }

        // Get monthly intake
        sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        String currentMonth = sdf.format(calendar.getTime());
        List<Integer> monthlyIntake = dbHelper.getWaterIntakeByMonth(currentMonth, userId);
        emailContent.append("\nMonthly Water Intake:\n");
        for (int day = 0; day < monthlyIntake.size(); day++) {
            emailContent.append(String.format(Locale.getDefault(), "Day %d - %d ml\n", day + 1, monthlyIntake.get(day)));
        }

        return emailContent.toString();
    }

    private boolean isWithinSleepPeriod(int hour, int minute, int bedtimeHour, int bedtimeMinute, int wakeupHour, int wakeupMinute) {
        if (bedtimeHour < wakeupHour) {
            return (hour > bedtimeHour || (hour == bedtimeHour && minute >= bedtimeMinute)) &&
                    (hour < wakeupHour || (hour == wakeupHour && minute < wakeupMinute));
        } else {
            return (hour > bedtimeHour || (hour == bedtimeHour && minute >= bedtimeMinute)) ||
                    (hour < wakeupHour || (hour == wakeupHour && minute < wakeupMinute));
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Reminder Channel";
            String description = "Channel for water reminders";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
            Log.d(TAG, "Notification channel created");
        }
    }
}