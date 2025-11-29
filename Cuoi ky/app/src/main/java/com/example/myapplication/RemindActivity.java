package com.example.myapplication;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class RemindActivity extends AppCompatActivity {

    private static final String TAG = "RemindActivity";
    private Button btnStandardMode;
    private Button btnIntervalMode;
    private Button btnCustomMode;
    private TextView selectedModeTextView;
    private String selectedModeName;
    private SharedPreferences sharedPreferences;
    private AlarmManager alarmManager;
    private static final int REQUEST_POST_NOTIFICATIONS = 1;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.remind);

        sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Switch switchReminder = findViewById(R.id.switchReminder);
        switchReminder.setChecked(sharedPreferences.getBoolean("switchReminder", false));

        switchReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("switchReminder", isChecked);
            editor.apply();

            Intent serviceIntent = new Intent(this, ForegroundService.class);
            if (isChecked) {
                // Start the service
                ContextCompat.startForegroundService(this, serviceIntent);
            } else {
                // Stop the service
                stopService(serviceIntent);
            }
        });
        btnStandardMode = findViewById(R.id.btnStandardMode);
        btnIntervalMode = findViewById(R.id.btnIntervalMode);
        btnCustomMode = findViewById(R.id.btnCustomMode);
        selectedModeTextView = findViewById(R.id.selectedModeTextView);

        btnStandardMode.setOnClickListener(v -> {
            selectMode(btnStandardMode, "Standard Mode");
            logCurrentMode();
        });
        btnIntervalMode.setOnClickListener(v -> {
            selectMode(btnIntervalMode, "Interval Mode");
            logCurrentMode();
        });
        btnCustomMode.setOnClickListener(v -> {
            selectMode(btnCustomMode, "Custom Mode");
            logCurrentMode();
        });

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> onBackPressed());

        // Load the saved mode
        selectedModeName = sharedPreferences.getString("selectedModeName", "Standard Mode");
        updateModeUI(selectedModeName);
        loadModeLayout(selectedModeName);

        // Check and request SCHEDULE_EXACT_ALARM permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            requestScheduleExactAlarmPermission();
        }
        // Request POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, REQUEST_POST_NOTIFICATIONS);
            }
        }
        // Log the current mode on startup
        logCurrentMode();

        // Schedule a test notification at 23:00
//        scheduleTestNotification();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "POST_NOTIFICATIONS permission granted");
            } else {
                Log.d(TAG, "POST_NOTIFICATIONS permission not granted");
                Toast.makeText(this, "Notification permission is required to receive reminders", Toast.LENGTH_LONG).show();
            }
        }
    }
    private void requestScheduleExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            startActivityForResult(intent, 1);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(this, "Exact alarm permission not granted", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void selectMode(Button selectedButton, String modeName) {
    btnStandardMode.setBackgroundColor(ContextCompat.getColor(this, R.color.light_blue_dark));
    btnIntervalMode.setBackgroundColor(ContextCompat.getColor(this, R.color.light_blue_dark));
    btnCustomMode.setBackgroundColor(ContextCompat.getColor(this, R.color.light_blue_dark));

    selectedButton.setBackgroundColor(ContextCompat.getColor(this, R.color.dark_blue));
    selectedModeTextView.setText(modeName);
    selectedModeName = modeName;

    // Save the selected mode
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putString("selectedModeName", selectedModeName);
    editor.apply();

    // Clear the database
    AlarmDatabaseHelper dbHelper = new AlarmDatabaseHelper(this);
    dbHelper.clearDatabase();

    // Load the selected mode layout and add new data
    loadModeLayout(modeName);
}
    private void updateButtonAppearance(Button button, boolean isEnabled) {
        if (isEnabled) {
            button.setBackgroundResource(R.drawable.rounded_button_blue);
        } else {
            button.setBackgroundResource(R.drawable.rounded_button);
        }
    }

    private void updateModeUI(String modeName) {
        if (modeName.equals("Standard Mode")) {
            selectMode(btnStandardMode, modeName);
        } else if (modeName.equals("Interval Mode")) {
            selectMode(btnIntervalMode, modeName);
        } else if (modeName.equals("Custom Mode")) {
            selectMode(btnCustomMode, modeName);
        }
    }
    private void showTimePickerDialog(Button timeButton) {
        String[] timeParts = timeButton.getText().toString().split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minuteOfHour) -> {
            String newTime = String.format("%02d:%02d", hourOfDay, minuteOfHour);
            timeButton.setText(newTime);

            // Save the new time in SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("button_time_" + timeButton.getId(), newTime);
            editor.apply();

            // Reschedule the notification
            scheduleNotification(newTime, "Custom Mode Reminder", timeButton.getId());
        }, hour, minute, true);

        timePickerDialog.show();
    }
    private void loadModeLayout(String modeName) {
        AlarmDatabaseHelper dbHelper = new AlarmDatabaseHelper(this);
        dbHelper.clearDatabaseAlarm();
        FrameLayout container = findViewById(R.id.modeContainer);
        container.removeAllViews();

        int layoutId;
        switch (modeName) {
            case "Standard Mode":
                dbHelper.clearDatabaseAlarm();
                layoutId = R.layout.layout_standard_mode;
                break;
            case "Interval Mode":
                dbHelper.clearDatabaseAlarm();
                layoutId = R.layout.layout_interval_mode;
                break;
            case "Custom Mode":
                dbHelper.clearDatabaseAlarm();
                layoutId = R.layout.layout_custom_mode;
                break;
            default:
                layoutId = R.layout.layout_standard_mode;
        }

        getLayoutInflater().inflate(layoutId, container, true);

        if (modeName.equals("Interval Mode")) {
            setupIntervalMode();
        } else if (modeName.equals("Custom Mode")) {
            setupCustomMode();
        } else if (modeName.equals("Standard Mode")) {
            updateStandardReminders(container);
        }
    }

    private void setupIntervalMode() {
        // Set up interval mode specific settings
        TextView intervalTextView = findViewById(R.id.intervalTextView);
        TextView bedtimeTextView = findViewById(R.id.bedtimeTextView);
        TextView wakeupTextView = findViewById(R.id.wakeupTextView);

        // Load saved interval settings from SharedPreferences
        String interval = sharedPreferences.getString("interval", "1min");
        String bedtime = sharedPreferences.getString("bedtime", "23:00");
        String wakeup = sharedPreferences.getString("wakeup", "08:00");

        // Update the TextView elements with the saved values
        intervalTextView.setText(interval);
        bedtimeTextView.setText(bedtime);
        wakeupTextView.setText(wakeup);

        // Set up click listeners to edit interval settings
        findViewById(R.id.editInterval).setOnClickListener(v -> showEditIntervalDialog(intervalTextView));
        findViewById(R.id.editBedtime).setOnClickListener(v -> showEditTimeDialog(bedtimeTextView, "bedtime"));
        findViewById(R.id.editWakeup).setOnClickListener(v -> showEditTimeDialog(wakeupTextView, "wakeup"));

        // Schedule the interval notifications
        scheduleIntervalNotifications(interval, bedtime, wakeup);
    }

    private void scheduleIntervalNotifications(String interval, String bedtime, String wakeup) {
        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("reminderLabel", "Interval Mode Reminder");
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, flags);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // Parse the interval value
        long intervalMillis;
        if (interval.endsWith("min")) {
            intervalMillis = Integer.parseInt(interval.replace("min", "").trim()) * 60 * 1000;
        } else {
            intervalMillis = 1 * 60 * 1000; // Default to 1 minute if parsing fails
        }

        // Set the initial trigger time to the next interval
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.MINUTE, 1);

        long triggerTime = calendar.getTimeInMillis();

        // Schedule the repeating alarm
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, triggerTime, intervalMillis, pendingIntent);
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

    private void showEditIntervalDialog(TextView intervalTextView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Interval");

        // Create a NumberPicker
        final NumberPicker numberPicker = new NumberPicker(this);
        numberPicker.setMinValue(1); // Minimum value (e.g., 1 minute)
        numberPicker.setMaxValue(60); // Maximum value (e.g., 60 minutes)

        // Extract the numeric part of the interval string
        String intervalText = intervalTextView.getText().toString().trim();
        int currentValue = 1; // Default value
        if (intervalText.endsWith("min")) {
            try {
                currentValue = Integer.parseInt(intervalText.replace("min", "").trim());
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid interval format: " + intervalText, e);
            }
        }
        numberPicker.setValue(currentValue);

        numberPicker.setFormatter(value -> value + "min"); // Format values as "Xmin"

        builder.setView(numberPicker);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String newInterval = numberPicker.getValue() + "min"; // Get selected value
            intervalTextView.setText(newInterval);

            // Save the new interval in SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("interval", newInterval);
            editor.apply();

            // Reschedule the interval notifications
            String bedtime = sharedPreferences.getString("bedtime", "23:00");
            String wakeup = sharedPreferences.getString("wakeup", "08:00");
            scheduleIntervalNotifications(newInterval, bedtime, wakeup);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }
    private void showEditTimeDialog(TextView timeTextView, String key) {
        String[] timeParts = timeTextView.getText().toString().split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minuteOfHour) -> {
            String newTime = String.format("%02d:%02d", hourOfDay, minuteOfHour);
            timeTextView.setText(newTime);

            // Save the new time in SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(key, newTime);
            editor.apply();

            // Reschedule the interval notifications
            String interval = sharedPreferences.getString("interval", "1min");
            String bedtime = sharedPreferences.getString("bedtime", "23:00");
            String wakeup = sharedPreferences.getString("wakeup", "08:00");
            scheduleIntervalNotifications(interval, bedtime, wakeup);
        }, hour, minute, true);

        timePickerDialog.show();
    }

    // RemindActivity.java
    // RemindActivity.java
    // RemindActivity.java
    private void setupCustomMode() {
        GridLayout customTimeGrid = findViewById(R.id.customTimeGrid);
        Button btnAddTime = findViewById(R.id.btnAddTime);

        // Clear all existing buttons
        customTimeGrid.removeAllViews();

        btnAddTime.setOnClickListener(v -> {
            addNewTimeButton(customTimeGrid, "08:00");
        });

        // Load saved buttons from SharedPreferences
        int buttonCount = sharedPreferences.getInt("button_count", 0);
        for (int i = 0; i < buttonCount; i++) {
            String time = sharedPreferences.getString("button_time_" + i, "08:00");
            boolean isEnabled = sharedPreferences.getBoolean("button_enabled_" + i, true);
            addNewTimeButton(customTimeGrid, time, i, isEnabled);
        }
    }

    private void addNewTimeButton(GridLayout customTimeGrid, String time) {
        int buttonId = customTimeGrid.getChildCount(); // Ensure unique ID for each button
        addNewTimeButton(customTimeGrid, time, buttonId, true);
    }

    private void addNewTimeButton(GridLayout customTimeGrid, String time, int buttonId, boolean isEnabled) {
        Button newTimeButton = new Button(this);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        int widthInDp = 80;
        int heightInDp = 48;
        float scale = getResources().getDisplayMetrics().density;
        params.width = (int) (widthInDp * scale + 0.5f);
        params.height = (int) (heightInDp * scale + 0.5f);
        newTimeButton.setLayoutParams(params);
        newTimeButton.setText(time);
        newTimeButton.setId(buttonId); // Set unique ID
        newTimeButton.setBackgroundResource(R.drawable.rounded_button);
        newTimeButton.setTextColor(getResources().getColor(android.R.color.black));
        newTimeButton.setTextAppearance(this, R.style.TimeChip_Enabled);
        updateButtonAppearance(newTimeButton, isEnabled);
        newTimeButton.setOnClickListener(buttonView -> {
            boolean newState = !sharedPreferences.getBoolean("button_enabled_" + newTimeButton.getId(), true);
            updateButtonAppearance(newTimeButton, newState);
            String status = newState ? "enabled" : "disabled";
            Toast.makeText(RemindActivity.this, "Button " + newTimeButton.getText() + " is " + status, Toast.LENGTH_SHORT).show();

            // Save the button state
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("button_enabled_" + newTimeButton.getId(), newState);
            editor.apply();

            // Update notifications and database
            if (newState) {
                scheduleNotification(newTimeButton.getText().toString(), "Custom Mode Reminder", newTimeButton.getId());
            } else {
                cancelNotification(newTimeButton.getId());
                removeAlarmFromDatabase(newTimeButton.getId());
            }
        });

        newTimeButton.setOnLongClickListener(buttonView -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Options")
                    .setItems(new CharSequence[]{"Change Time", "Delete"}, (dialog, which) -> {
                        if (which == 0) {
                            showTimePickerDialog(newTimeButton);
                        } else if (which == 1) {
                            customTimeGrid.removeView(newTimeButton);
                            // Remove button details from SharedPreferences
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.remove("button_time_" + newTimeButton.getId());
                            editor.remove("button_enabled_" + newTimeButton.getId());
                            editor.putInt("button_count", customTimeGrid.getChildCount());
                            editor.apply();
                            // Remove alarm from database
                            removeAlarmFromDatabase(newTimeButton.getId());
                        }
                    })
                    .show();
            return true;
        });

        customTimeGrid.addView(newTimeButton);

        // Save the new button details
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("button_time_" + buttonId, newTimeButton.getText().toString());
        editor.putBoolean("button_enabled_" + buttonId, isEnabled);
        editor.putInt("button_count", customTimeGrid.getChildCount());
        editor.apply();
    }
    private void removeAlarmFromDatabase(int requestCode) {
        AlarmDatabaseHelper dbHelper = new AlarmDatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(AlarmDatabaseHelper.TABLE_ALARMS, AlarmDatabaseHelper.COLUMN_REQUEST_CODE + "=?", new String[]{String.valueOf(requestCode)});
        db.close();
    }
    private void cancelNotification(int requestCode) {
        Intent intent = new Intent(this, NotificationReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent, flags);
        alarmManager.cancel(pendingIntent);
        Log.d(TAG, "Notification canceled for request code: " + requestCode);
    }

    private void updateStandardReminders(View rootView) {
        // List of row IDs
        int[] labelIds = {
                R.id.reminderWakeUp, R.id.reminderBeforeBreakfast, R.id.reminderAfterBreakfast, R.id.reminderBeforeLunch,
                R.id.reminderAfterLunch, R.id.reminderBeforeDinner, R.id.reminderAfterDinner, R.id.reminderBeforeSleep
        };

        // Default data
        String[] defaultLabels = {
                "After Wake-up", "Before Breakfast", "After Breakfast", "Before Lunch",
                "After Lunch", "Before Dinner", "After Dinner", "Before Sleep"
        };
        // Default times
        String[] defaultTimes = {
                "09:00", "09:30", "10:00", "11:00",
                "13:00", "18:00", "23:29", "23:30"
        };
        boolean[] defaultToggles = {
                true, false, false, true,
                true, true, true, true
        };

        for (int i = 0; i < labelIds.length; i++) {
            final int index = i; // Use a final variable inside the loop
            View reminderView = rootView.findViewById(labelIds[index]);
            TextView label = reminderView.findViewById(R.id.tvLabel);
            TextView time = reminderView.findViewById(R.id.tvTime);
            Switch toggle = reminderView.findViewById(R.id.switchEnable);
            ImageView btnEdit = reminderView.findViewById(R.id.btnEdit);

            // Load data from SharedPreferences or use default data
            String labelText = sharedPreferences.getString("label_" + index, defaultLabels[index]);
            String timeText = sharedPreferences.getString("time_" + index, defaultTimes[index]);
            boolean toggleState = sharedPreferences.getBoolean("toggle_" + index, defaultToggles[index]);

            if (label != null) label.setText(labelText);
            if (time != null) time.setText(timeText);
            if (toggle != null) {
                toggle.setChecked(toggleState);
                toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("toggle_" + index, isChecked);
                    editor.apply();

                    // Update notifications and database
                    if (isChecked) {
                        scheduleNotification(timeText, labelText, index);
                    } else {
                        cancelNotification(index);
                        removeAlarmFromDatabase(index);
                    }
                });
            }

            if (btnEdit != null) {
                btnEdit.setOnClickListener(v -> {
                    // Show dialog to edit label and time
                    showEditDialog(index, label, time);
                });
            }

            if (toggleState && !timeText.isEmpty()) {
                Log.d(TAG, "Scheduling notification for " + timeText + " with label: " + labelText);
                scheduleNotification(timeText, labelText, index);
            }
        }
    }

    private void showEditDialog(int index, TextView label, TextView time) {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_reminder, null);
            EditText editLabel = dialogView.findViewById(R.id.editLabel);
            TextView editTime = dialogView.findViewById(R.id.editTime); // Change to TextView for time

            editLabel.setText(label.getText());
            editTime.setText(time.getText());

            // Set up TimePickerDialog for time input
            editTime.setOnClickListener(v -> {
                String[] timeParts = time.getText().toString().split(":");
                int hour = Integer.parseInt(timeParts[0]);
                int minute = Integer.parseInt(timeParts[1]);

                TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                    (view, selectedHour, selectedMinute) -> {
                        String formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute);
                        editTime.setText(formattedTime);
                    }, hour, minute, true);
                timePickerDialog.show();
            });

            new AlertDialog.Builder(this)
                    .setTitle("Edit Reminder")
                    .setView(dialogView)
                    .setPositiveButton("Save", (dialog, which) -> {
                        String newLabel = editLabel.getText().toString();
                        String newTime = editTime.getText().toString();

                        label.setText(newLabel);
                        time.setText(newTime);

                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("label_" + index, newLabel);
                        editor.putString("time_" + index, newTime);
                        editor.apply();

                        // Reload the reminders to update the scheduled notifications
                        updateStandardReminders(findViewById(R.id.modeContainer));
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    private String getCurrentMode() {
        return sharedPreferences.getString("selectedModeName", "Standard Mode");
    }
    private void logCurrentMode() {
        String currentMode = getCurrentMode();
        Log.d(TAG, "Current mode: " + currentMode);
        Toast.makeText(this, "Current mode: " + currentMode, Toast.LENGTH_SHORT).show();
    }
    private void scheduleNotification(String time, String label, int requestCode) {
        String currentMode = getCurrentMode();
        String notificationMessage;

        switch (currentMode) {
            case "Custom Mode":
                notificationMessage = "Custom Mode: " + label;
                break;
            case "Interval Mode":
                notificationMessage = "Interval Mode: " + label;
                break;
            case "Standard Mode":
            default:
                notificationMessage = "Standard Mode: " + label;
                break;
        }

        Log.d(TAG, "Scheduling notification for " + time + " with message: " + notificationMessage);
        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("reminderLabel", notificationMessage);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent, flags);

        // Parse the time and set the alarm
        String[] timeParts = time.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        long triggerTime = calendar.getTimeInMillis();
        if (calendar.before(Calendar.getInstance())) {
            triggerTime += AlarmManager.INTERVAL_DAY; // Schedule for the next day if the time has already passed
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                } else {
                    View rootView = findViewById(android.R.id.content);
                    Snackbar.make(rootView, "Cannot schedule exact alarms. Please grant the required permission.", Snackbar.LENGTH_LONG).show();
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
            Log.d(TAG, "Notification scheduled for " + time);

            // Save the alarm details into SQLite database
            saveAlarmToDatabase(time, label, requestCode);
        } catch (SecurityException e) {
            View rootView = findViewById(android.R.id.content);
            Snackbar.make(rootView, "Failed to schedule exact alarm: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
            Log.e(TAG, "Failed to schedule exact alarm: " + e.getMessage());
        }
    }

    private void saveAlarmToDatabase(String time, String label, int requestCode) {
        AlarmDatabaseHelper dbHelper = new AlarmDatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(AlarmDatabaseHelper.COLUMN_TIME, time);
        values.put(AlarmDatabaseHelper.COLUMN_LABEL, label);
        values.put(AlarmDatabaseHelper.COLUMN_REQUEST_CODE, requestCode);
        values.put(AlarmDatabaseHelper.COLUMN_CREATION_DATE, new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date())); // Save creation date

        long newRowId = db.insert(AlarmDatabaseHelper.TABLE_ALARMS, null, values);
        if (newRowId != -1) {
            Log.d(TAG, "Alarm saved to database with ID: " + newRowId);
        } else {
            Log.e(TAG, "Failed to save alarm to database");
        }
        dbHelper.reassignSequentialIds();
        dbHelper.removeDuplicateAlarms();
        db.close();
    }

    private void scheduleTestNotification() {
        scheduleNotification("23:07", "Test Reminder at 23:07", 999);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed(); // Call the super method
        Intent intent = new Intent(RemindActivity.this, MainActivity.class);
        intent.putExtra("loadMeLayout", true);
        intent.putExtra("selectedModeName", selectedModeName);
        startActivity(intent);
        finish();
    }
}