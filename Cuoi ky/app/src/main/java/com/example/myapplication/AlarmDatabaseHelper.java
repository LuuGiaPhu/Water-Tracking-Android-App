package com.example.myapplication;

        import android.content.ContentValues;
        import android.content.Context;
        import android.database.Cursor;
        import android.database.sqlite.SQLiteDatabase;
        import android.database.sqlite.SQLiteOpenHelper;
        import android.util.Log;

        import java.text.ParseException;
        import java.text.SimpleDateFormat;
        import java.util.Calendar;
        import java.util.Date;
        import java.util.Locale;

public class AlarmDatabaseHelper extends SQLiteOpenHelper {

            private static final String DATABASE_NAME = "alarms.db";
            private static final int DATABASE_VERSION = 2; // Incremented version

            public static final String TABLE_ALARMS = "alarms";
            public static final String COLUMN_ID = "_id";
            public static final String COLUMN_TIME = "time";
            public static final String COLUMN_LABEL = "label";
            public static final String COLUMN_REQUEST_CODE = "request_code";
            public static final String COLUMN_CREATION_DATE = "creation_date"; // New column

            private static final String TABLE_CREATE =
                    "CREATE TABLE " + TABLE_ALARMS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_TIME + " TEXT, " +
                    COLUMN_LABEL + " TEXT, " +
                    COLUMN_REQUEST_CODE + " INTEGER, " +
                    COLUMN_CREATION_DATE + " TEXT);"; // New column

            public AlarmDatabaseHelper(Context context) {
                super(context, DATABASE_NAME, null, DATABASE_VERSION);
            }

            @Override
            public void onCreate(SQLiteDatabase db) {
                db.execSQL(TABLE_CREATE);
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                if (oldVersion < 2) {
                    db.execSQL("ALTER TABLE " + TABLE_ALARMS + " ADD COLUMN " + COLUMN_CREATION_DATE + " TEXT;");
                }
            }
            public void clearDatabase() {
                SQLiteDatabase db = this.getWritableDatabase();
                db.execSQL("DELETE FROM " + TABLE_ALARMS);
                db.close();
            }
            public String getNextReminder() {
                SQLiteDatabase db = this.getReadableDatabase();
                String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                String nextReminderTime = null;

                // Query to get the next reminder time closest to the current time
                String query = "SELECT " + COLUMN_TIME + " FROM " + TABLE_ALARMS + " WHERE " + COLUMN_TIME + " > ? ORDER BY " + COLUMN_TIME + " ASC LIMIT 1";
                Cursor cursor = db.rawQuery(query, new String[]{currentTime});

                if (cursor.moveToFirst()) {
                    nextReminderTime = cursor.getString(cursor.getColumnIndex(COLUMN_TIME));
                } else {
                    // If no future reminders, get the earliest reminder for the next day
                    query = "SELECT " + COLUMN_TIME + " FROM " + TABLE_ALARMS + " ORDER BY " + COLUMN_TIME + " ASC LIMIT 1";
                    cursor = db.rawQuery(query, null);
                    if (cursor.moveToFirst()) {
                        nextReminderTime = cursor.getString(cursor.getColumnIndex(COLUMN_TIME));
                    }
                }

                cursor.close();
                db.close();
                return nextReminderTime;
            }

    private boolean isReminderTimeValid(String reminderTime, String creationDate) {
        if (reminderTime == null || creationDate == null) {
            return false; // Invalid if either reminderTime or creationDate is null
        }

        // Parse the reminder time
        String[] timeParts = reminderTime.split(":");
        int reminderHour = Integer.parseInt(timeParts[0]);
        int reminderMinute = Integer.parseInt(timeParts[1]);

        Calendar reminderCalendar = Calendar.getInstance();
        reminderCalendar.set(Calendar.HOUR_OF_DAY, reminderHour);
        reminderCalendar.set(Calendar.MINUTE, reminderMinute);
        reminderCalendar.set(Calendar.SECOND, 0);

        // Parse the creation date
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Calendar creationCalendar = Calendar.getInstance();
            creationCalendar.setTime(sdf.parse(creationDate));

            // Check if the current time is before the reminder time
            return Calendar.getInstance().before(reminderCalendar);
        } catch (ParseException e) {
            e.printStackTrace();
            return false; // Invalid if parsing fails
        }
    }
    // AlarmDatabaseHelper.java
    public void logAllAlarms() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_ALARMS, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID));
                String time = cursor.getString(cursor.getColumnIndex(COLUMN_TIME));
                String label = cursor.getString(cursor.getColumnIndex(COLUMN_LABEL));
                int requestCode = cursor.getInt(cursor.getColumnIndex(COLUMN_REQUEST_CODE));
                String creationDate = cursor.getString(cursor.getColumnIndex(COLUMN_CREATION_DATE));
                Log.d("AlarmDatabaseHelper", "Alarm ID: " + id + ", Time: " + time + ", Label: " + label + ", Request Code: " + requestCode + ", Creation Date: " + creationDate);
            }
            cursor.close();
        }
    }
    public void removeDuplicateAlarms() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            // Query to find all duplicate times
            String query = "SELECT " + COLUMN_TIME + ", MIN(" + COLUMN_ID + ") as min_id FROM " + TABLE_ALARMS + " GROUP BY " + COLUMN_TIME;
            Cursor cursor = db.rawQuery(query, null);

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String time = cursor.getString(cursor.getColumnIndex(COLUMN_TIME));
                    int minId = cursor.getInt(cursor.getColumnIndex("min_id"));

                    // Delete all rows with the same time except the one with the minimum ID
                    db.delete(TABLE_ALARMS, COLUMN_TIME + " = ? AND " + COLUMN_ID + " != ?", new String[]{time, String.valueOf(minId)});
                }
                cursor.close();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }
    // AlarmDatabaseHelper.java
    public void reassignSequentialIds() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            // Retrieve all records ordered by the current ID
            Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_ALARMS + " ORDER BY " + COLUMN_ID, null);
            if (cursor != null) {
                int newId = 1;
                while (cursor.moveToNext()) {
                    int oldId = cursor.getInt(cursor.getColumnIndex(COLUMN_ID));
                    ContentValues values = new ContentValues();
                    values.put(COLUMN_ID, newId);
                    values.put(COLUMN_TIME, cursor.getString(cursor.getColumnIndex(COLUMN_TIME)));
                    values.put(COLUMN_LABEL, cursor.getString(cursor.getColumnIndex(COLUMN_LABEL)));
                    values.put(COLUMN_REQUEST_CODE, cursor.getInt(cursor.getColumnIndex(COLUMN_REQUEST_CODE)));
                    values.put(COLUMN_CREATION_DATE, cursor.getString(cursor.getColumnIndex(COLUMN_CREATION_DATE)));

                    // Update the record with the new ID
                    db.update(TABLE_ALARMS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(oldId)});
                    newId++;
                }
                cursor.close();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
    public void clearDatabaseAlarm() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_ALARMS);
        // Do not close the database here if it is still needed
    }
}