// DatabaseHelper.java
    package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

    public class DatabaseHelper extends SQLiteOpenHelper {

        private static final String DATABASE_NAME = "user_info.db";
        private static final int DATABASE_VERSION = 8; // Incremented version
        private static DatabaseHelper instance;
        private SQLiteDatabase database; // Declare the database variable


        public static final String TABLE_USER = "user";
        public static final String COLUMN_ID = "_id";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_GENDER = "gender";
        public static final String COLUMN_HEIGHT = "height";
        public static final String COLUMN_WEIGHT = "weight";
        public static final String COLUMN_DAILY_GOAL = "daily_goal";

        public static final String TABLE_WATER_INTAKE = "water_intake";
        public static final String COLUMN_INTAKE_ID = "_id";
        public static final String COLUMN_INTAKE_AMOUNT = "intake_amount";
        public static final String COLUMN_INTAKE_TIMESTAMP = "intake_timestamp";
        public static final String COLUMN_ID_USER = "id_user";

        // SQL statement to create the user table
        private static final String TABLE_CREATE =
                "CREATE TABLE " + TABLE_USER + " (" +
                COLUMN_ID + " TEXT PRIMARY KEY, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_GENDER + " TEXT, " +
                COLUMN_HEIGHT + " REAL, " +
                COLUMN_WEIGHT + " REAL, " +
                COLUMN_DAILY_GOAL + " INTEGER DEFAULT 2000, " +
                "avatar BLOB, " +
                "TotalDaysGoalAchieved INTEGER DEFAULT 0);";

        private static final String TABLE_CREATE_WATER_INTAKE =
                "CREATE TABLE " + TABLE_WATER_INTAKE + " (" +
                COLUMN_INTAKE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_INTAKE_AMOUNT + " INTEGER, " +
                COLUMN_INTAKE_TIMESTAMP + " TEXT, " +
                "daily_goal INTEGER DEFAULT 2000, " +
                        COLUMN_ID_USER + " TEXT, " +
                        "FOREIGN KEY(" + COLUMN_ID_USER + ") REFERENCES " + TABLE_USER + "(" + COLUMN_ID + "));";
        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(TABLE_CREATE);
            db.execSQL(TABLE_CREATE_WATER_INTAKE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < 5) {
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_WATER_INTAKE);
                onCreate(db);
            } else if (oldVersion < 6) {
                db.execSQL("ALTER TABLE " + TABLE_USER + " ADD COLUMN " + COLUMN_NAME + " TEXT;");
                db.execSQL("ALTER TABLE " + TABLE_USER + " ADD COLUMN avatar BLOB;");
            } else if (oldVersion < 7) {
                db.execSQL("ALTER TABLE " + TABLE_USER + " ADD COLUMN TotalDaysGoalAchieved INTEGER DEFAULT 0;");
            } else if (oldVersion < 8) {
                // Create a new table with the foreign key constraint
                db.execSQL("CREATE TABLE " + TABLE_WATER_INTAKE + "_new (" +
                        COLUMN_INTAKE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_INTAKE_AMOUNT + " INTEGER, " +
                        COLUMN_INTAKE_TIMESTAMP + " TEXT, " +
                        "daily_goal INTEGER DEFAULT 2000, " +
                        "id_user TEXT, " +
                        "FOREIGN KEY(id_user) REFERENCES " + TABLE_USER + "(" + COLUMN_ID + "));");

                // Copy data from the old table to the new table
                db.execSQL("INSERT INTO " + TABLE_WATER_INTAKE + "_new (" +
                        COLUMN_INTAKE_ID + ", " +
                        COLUMN_INTAKE_AMOUNT + ", " +
                        COLUMN_INTAKE_TIMESTAMP + ", " +
                        "daily_goal) " +
                        "SELECT " +
                        COLUMN_INTAKE_ID + ", " +
                        COLUMN_INTAKE_AMOUNT + ", " +
                        COLUMN_INTAKE_TIMESTAMP + ", " +
                        "daily_goal " +
                        "FROM " + TABLE_WATER_INTAKE + ";");

                // Drop the old table
                db.execSQL("DROP TABLE " + TABLE_WATER_INTAKE + ";");

                // Rename the new table to the old table name
                db.execSQL("ALTER TABLE " + TABLE_WATER_INTAKE + "_new RENAME TO " + TABLE_WATER_INTAKE + ";");
            }
        }
        public synchronized SQLiteDatabase getReadableDatabaseInstance() {
            if (database == null || !database.isOpen()) {
                database = super.getReadableDatabase();
            }
            return database;
        }

        public synchronized SQLiteDatabase getWritableDatabaseInstance() {
            if (database == null || !database.isOpen()) {
                database = super.getWritableDatabase();
            }
            return database;
        }

        public synchronized void closeDatabase() {
            if (database != null && database.isOpen()) {
                database.close();
            }
        }

        public void saveWaterIntake(int intakeAmount, String timestamp, int dailyGoal) {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_INTAKE_AMOUNT, intakeAmount);
            values.put(COLUMN_INTAKE_TIMESTAMP, timestamp);
            values.put("daily_goal", dailyGoal);

            Log.d("DatabaseHelper", "Saving water intake: " + intakeAmount + " ml, Timestamp: " + timestamp + ", Daily Goal: " + dailyGoal + " ml");

            db.insert(TABLE_WATER_INTAKE, null, values);
        }

        public int getDailyGoal() {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = null;
            try {
                cursor = db.rawQuery("SELECT " + COLUMN_DAILY_GOAL + " FROM " + TABLE_USER + " LIMIT 1", null);
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(COLUMN_DAILY_GOAL);
                    if (columnIndex >= 0) {
                        return cursor.getInt(columnIndex);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
//                db.close();
            }
            return 2000;
        }

        public void updateDailyGoal(String userId, int dailyGoal) {
            Log.d("DatabaseHelper123456", "Updating daily goal for user: " + userId + " to " + dailyGoal);
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_DAILY_GOAL, dailyGoal);
            db.update(TABLE_USER, values, COLUMN_ID + " = ?", new String[]{userId});

            // Retrieve and print the updated data
            Cursor cursor = db.query(TABLE_USER, null, COLUMN_ID + " = ?", new String[]{userId}, null, null, null);
            if (cursor.moveToFirst()) {
                String gender = cursor.getString(cursor.getColumnIndex(COLUMN_GENDER));
                double height = cursor.getDouble(cursor.getColumnIndex(COLUMN_HEIGHT));
                double weight = cursor.getDouble(cursor.getColumnIndex(COLUMN_WEIGHT));
                int updatedDailyGoal = cursor.getInt(cursor.getColumnIndex(COLUMN_DAILY_GOAL));
                Log.d("DatabaseHelper123456", "Updated user data: Gender: " + gender + ", Height: " + height + " m, Weight: " + weight + " kg, Daily Goal: " + updatedDailyGoal + " ml");
            }
            cursor.close();
        }

        public List<Integer> getTodayWaterIntake() {
            List<Integer> hourlyIntake = new ArrayList<>();
            for (int i = 0; i < 24; i++) {
                hourlyIntake.add(0);
            }

            SQLiteDatabase db = null;
            Cursor cursor = null;
            try {
                db = this.getReadableDatabase();
                String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                cursor = db.rawQuery(
                    "SELECT " + COLUMN_INTAKE_AMOUNT + ", strftime('%H', " + COLUMN_INTAKE_TIMESTAMP + ") as hour " +
                    "FROM " + TABLE_WATER_INTAKE + " WHERE DATE(" + COLUMN_INTAKE_TIMESTAMP + ") = ?",
                    new String[]{todayDate}
                );

                if (cursor != null) {
                    int amountIndex = cursor.getColumnIndex(COLUMN_INTAKE_AMOUNT);
                    int hourIndex = cursor.getColumnIndex("hour");

                    if (amountIndex >= 0 && hourIndex >= 0) {
                        while (cursor.moveToNext()) {
                            int amount = cursor.getInt(amountIndex);
                            int hour = cursor.getInt(hourIndex);
                            hourlyIntake.set(hour, hourlyIntake.get(hour) + amount);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("DatabaseHelper", "Error retrieving today's water intake", e);
            } finally {
                if (cursor != null) cursor.close();
                if (db != null && db.isOpen()) {
//                    db.close();
                }
            }

            return hourlyIntake;
        }

        public List<Integer> getWeekWaterIntake() {
            List<Integer> dailyIntake = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                dailyIntake.add(0);
            }

            SQLiteDatabase db = this.getReadableDatabase();
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
            String startDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
            calendar.add(Calendar.DAY_OF_WEEK, 6);
            String endDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());

            Cursor cursor = db.rawQuery("SELECT " + COLUMN_INTAKE_AMOUNT + ", strftime('%w', " + COLUMN_INTAKE_TIMESTAMP + ") as day FROM " + TABLE_WATER_INTAKE + " WHERE DATE(" + COLUMN_INTAKE_TIMESTAMP + ") BETWEEN ? AND ?", new String[]{startDate, endDate});
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int amountIndex = cursor.getColumnIndex(COLUMN_INTAKE_AMOUNT);
                    int dayIndex = cursor.getColumnIndex("day");
                    if (amountIndex >= 0 && dayIndex >= 0) {
                        int amount = cursor.getInt(amountIndex);
                        int day = cursor.getInt(dayIndex);
                        dailyIntake.set(day, dailyIntake.get(day) + amount);
                    }
                }
                cursor.close();
            }
            return dailyIntake;
        }

        public List<Integer> getMonthWaterIntake() {
            List<Integer> monthlyIntake = new ArrayList<>();
            Calendar calendar = Calendar.getInstance();
            int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
            for (int i = 0; i < daysInMonth; i++) {
                monthlyIntake.add(0);
            }

            SQLiteDatabase db = this.getReadableDatabase();
            String month = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
            Cursor cursor = db.rawQuery("SELECT " + COLUMN_INTAKE_AMOUNT + ", strftime('%d', " + COLUMN_INTAKE_TIMESTAMP + ") as day FROM " + TABLE_WATER_INTAKE + " WHERE strftime('%Y-%m', " + COLUMN_INTAKE_TIMESTAMP + ") = ?", new String[]{month});
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int amountIndex = cursor.getColumnIndex(COLUMN_INTAKE_AMOUNT);
                    int dayIndex = cursor.getColumnIndex("day");
                    if (amountIndex >= 0 && dayIndex >= 0) {
                        int amount = cursor.getInt(amountIndex);
                        int day = cursor.getInt(dayIndex) - 1;
                        monthlyIntake.set(day, monthlyIntake.get(day) + amount);
                    }
                }
                cursor.close();
            }
            return monthlyIntake;
        }

        public List<String> getAllWaterIntakeRecords() {
            List<String> records = new ArrayList<>();
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT " + COLUMN_INTAKE_AMOUNT + ", " + COLUMN_INTAKE_TIMESTAMP + " FROM " + TABLE_WATER_INTAKE, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int amountIndex = cursor.getColumnIndex(COLUMN_INTAKE_AMOUNT);
                    int timestampIndex = cursor.getColumnIndex(COLUMN_INTAKE_TIMESTAMP);
                    if (amountIndex >= 0 && timestampIndex >= 0) {
                        int amount = cursor.getInt(amountIndex);
                        String timestamp = cursor.getString(timestampIndex);
                        records.add(amount + " ml at " + timestamp);
                    }
                }
                cursor.close();
            }
            return records;
        }

        public void updateWaterIntakeRecord(String timestamp, int newAmount) {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_INTAKE_AMOUNT, newAmount);
            db.update(TABLE_WATER_INTAKE, values, COLUMN_INTAKE_TIMESTAMP + " = ?", new String[]{timestamp});
            db.close();
        }

        public void deleteWaterIntakeRecord(String timestamp) {
            SQLiteDatabase db = this.getWritableDatabase();
            db.delete(TABLE_WATER_INTAKE, COLUMN_INTAKE_TIMESTAMP + " = ?", new String[]{timestamp});
            db.close();
        }

        public int getTotalWaterIntake() {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = null;
            int totalIntake = 0;
            try {
                cursor = db.rawQuery("SELECT SUM(" + COLUMN_INTAKE_AMOUNT + ") as total FROM " + TABLE_WATER_INTAKE, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int totalIndex = cursor.getColumnIndex("total");
                    if (totalIndex >= 0) {
                        totalIntake = cursor.getInt(totalIndex);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
//                db.close();
            }
            return totalIntake;
        }

        public int getTotalDaysGoalAchieved() {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = null;
            int totalDays = 0;
            try {
                String query = "SELECT COUNT(DISTINCT DATE(" + COLUMN_INTAKE_TIMESTAMP + ")) as days " +
                        "FROM " + TABLE_WATER_INTAKE + " wi " +
                        "WHERE (SELECT SUM(" + COLUMN_INTAKE_AMOUNT + ") " +
                        "FROM " + TABLE_WATER_INTAKE + " " +
                        "WHERE DATE(" + COLUMN_INTAKE_TIMESTAMP + ") = DATE(wi." + COLUMN_INTAKE_TIMESTAMP + ")) >= " +
                        "(SELECT " + COLUMN_DAILY_GOAL + " FROM " + TABLE_USER + " LIMIT 1)";
                cursor = db.rawQuery(query, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int daysIndex = cursor.getColumnIndex("days");
                    if (daysIndex >= 0) {
                        totalDays = cursor.getInt(daysIndex);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
//                db.close();
            }
            return totalDays;
        }

        public String exportDatabaseToJson() {
            SQLiteDatabase db = this.getReadableDatabase();
            JSONObject json = new JSONObject();

            try {
                Cursor cursor = db.query(TABLE_USER, null, null, null, null, null, null);
                JSONArray userArray = new JSONArray();
                while (cursor.moveToNext()) {
                    JSONObject user = new JSONObject();
                    int idIndex = cursor.getColumnIndex(COLUMN_ID);
                    int nameIndex = cursor.getColumnIndex(COLUMN_NAME);
                    int genderIndex = cursor.getColumnIndex(COLUMN_GENDER);
                    int heightIndex = cursor.getColumnIndex(COLUMN_HEIGHT);
                    int weightIndex = cursor.getColumnIndex(COLUMN_WEIGHT);
                    int dailyGoalIndex = cursor.getColumnIndex(COLUMN_DAILY_GOAL);
                    int avatarIndex = cursor.getColumnIndex("avatar");
                    int totalDaysGoalAchievedIndex = cursor.getColumnIndex("TotalDaysGoalAchieved");
                    if (idIndex >= 0 && nameIndex >= 0 && genderIndex >= 0 && heightIndex >= 0 && weightIndex >= 0 && dailyGoalIndex >= 0 && avatarIndex >= 0 && totalDaysGoalAchievedIndex >= 0) {
                        user.put(COLUMN_ID, cursor.getString(idIndex));
                        user.put(COLUMN_NAME, cursor.getString(nameIndex));
                        user.put(COLUMN_GENDER, cursor.getString(genderIndex));
                        user.put(COLUMN_HEIGHT, cursor.getDouble(heightIndex));
                        user.put(COLUMN_WEIGHT, cursor.getDouble(weightIndex));
                        user.put(COLUMN_DAILY_GOAL, cursor.getInt(dailyGoalIndex));
                        user.put("avatar", cursor.getBlob(avatarIndex));
                        user.put("TotalDaysGoalAchieved", cursor.getInt(totalDaysGoalAchievedIndex));
                        userArray.put(user);
                    }
                }
                cursor.close();
                json.put(TABLE_USER, userArray);

                cursor = db.query(TABLE_WATER_INTAKE, null, null, null, null, null, null);
                JSONArray intakeArray = new JSONArray();
                while (cursor.moveToNext()) {
                    JSONObject intake = new JSONObject();
                    int intakeIdIndex = cursor.getColumnIndex(COLUMN_INTAKE_ID);
                    int intakeAmountIndex = cursor.getColumnIndex(COLUMN_INTAKE_AMOUNT);
                    int intakeTimestampIndex = cursor.getColumnIndex(COLUMN_INTAKE_TIMESTAMP);
                    int userIdIndex = cursor.getColumnIndex("id_user");
                    if (intakeIdIndex >= 0 && intakeAmountIndex >= 0 && intakeTimestampIndex >= 0 && userIdIndex >= 0) {
                        intake.put(COLUMN_INTAKE_ID, cursor.getInt(intakeIdIndex));
                        intake.put(COLUMN_INTAKE_AMOUNT, cursor.getInt(intakeAmountIndex));
                        intake.put(COLUMN_INTAKE_TIMESTAMP, cursor.getString(intakeTimestampIndex));
                        intake.put("id_user", cursor.getString(userIdIndex));
                        intakeArray.put(intake);
                    }
                }
                cursor.close();
                json.put(TABLE_WATER_INTAKE, intakeArray);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            return json.toString();
        }

        public void importDatabaseFromJson(String jsonData) {
            SQLiteDatabase db = null;
            try {
                db = this.getWritableDatabase();
                db.beginTransaction();

                JSONObject json = new JSONObject(jsonData);

                // Xoá dữ liệu cũ
                db.delete(TABLE_USER, null, null);
                db.delete(TABLE_WATER_INTAKE, null, null);
                Log.d("DatabaseHelper", "Importing JSON: " + jsonData);

                // ✅ Import user (Map)
                JSONObject userObject = json.getJSONObject(TABLE_USER); // "user" là một Map
                Iterator<String> keys = userObject.keys();
                while (keys.hasNext()) {
                    String key = keys.next(); // key chính là userId
                    JSONObject user = userObject.getJSONObject(key);

                    ContentValues values = new ContentValues();
                    values.put(COLUMN_ID, key);
                    if (user.has(COLUMN_NAME)) values.put(COLUMN_NAME, user.getString(COLUMN_NAME));
                    if (user.has(COLUMN_GENDER)) values.put(COLUMN_GENDER, user.getString(COLUMN_GENDER));
                    if (user.has(COLUMN_HEIGHT)) values.put(COLUMN_HEIGHT, user.getDouble(COLUMN_HEIGHT));
                    if (user.has(COLUMN_WEIGHT)) values.put(COLUMN_WEIGHT, user.getDouble(COLUMN_WEIGHT));
                    if (user.has(COLUMN_DAILY_GOAL)) values.put(COLUMN_DAILY_GOAL, user.getInt(COLUMN_DAILY_GOAL));
                    if (user.has("avatar")) values.put("avatar", user.getString("avatar"));
                    if (user.has("TotalDaysGoalAchieved")) values.put("TotalDaysGoalAchieved", user.getInt("TotalDaysGoalAchieved"));
                    db.insertWithOnConflict(TABLE_USER, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                }

                // ✅ Import water intake (vẫn là mảng)
                JSONArray intakeArray = json.getJSONArray(TABLE_WATER_INTAKE);
                for (int i = 0; i < intakeArray.length(); i++) {
                    JSONObject intake = intakeArray.getJSONObject(i);
                    ContentValues values = new ContentValues();
                    if (intake.has(COLUMN_INTAKE_ID)) values.put(COLUMN_INTAKE_ID, intake.getInt(COLUMN_INTAKE_ID));
                    if (intake.has(COLUMN_INTAKE_AMOUNT)) values.put(COLUMN_INTAKE_AMOUNT, intake.getInt(COLUMN_INTAKE_AMOUNT));
                    if (intake.has(COLUMN_INTAKE_TIMESTAMP)) values.put(COLUMN_INTAKE_TIMESTAMP, intake.getString(COLUMN_INTAKE_TIMESTAMP));
                    if (intake.has(COLUMN_ID_USER)) values.put(COLUMN_ID_USER, intake.getString(COLUMN_ID_USER));
                    db.insertWithOnConflict(TABLE_WATER_INTAKE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                }

                db.setTransactionSuccessful();
                Log.d("DatabaseHelper", "Import completed successfully.");
            } catch (JSONException e) {
                Log.e("DatabaseHelper", "Error parsing JSON", e);
            } finally {
                if (db != null) {
                    db.endTransaction();
//          db.close();
                }
            }
        }

        public void clearDatabase() {
            SQLiteDatabase db = this.getWritableDatabase();
            db.execSQL("DELETE FROM " + TABLE_USER);
            db.execSQL("DELETE FROM " + TABLE_WATER_INTAKE);
        }

        public void saveUserId(String userId) {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_ID, userId);
            db.insertWithOnConflict(TABLE_USER, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }

        public void importDatabaseFromJson(String jsonData, String userId) {
            SQLiteDatabase db = null;
            try {
                db = this.getWritableDatabase();
                db.beginTransaction(); // Start a transaction

                JSONObject json = new JSONObject(jsonData);
                JSONArray waterIntakeArray = json.getJSONArray("water_intake");

                for (int i = 0; i < waterIntakeArray.length(); i++) {
                    JSONObject intake = waterIntakeArray.getJSONObject(i);
                    ContentValues values = new ContentValues();
                    values.put("_id", intake.getInt("_id"));
                    values.put("intake_amount", intake.getInt("intake_amount"));
                    values.put("intake_timestamp", intake.getString("intake_timestamp"));
                    values.put("id_user", userId); // Associate with the userId

                    // Use insertWithOnConflict to handle unique constraint violation
                    db.insertWithOnConflict("water_intake", null, values, SQLiteDatabase.CONFLICT_IGNORE);
                }

                db.setTransactionSuccessful(); // Mark the transaction as successful
            } catch (JSONException e) {
                Log.e("DatabaseHelper", "Error importing database from JSON", e);
            } finally {
                if (db != null) {
                    db.endTransaction(); // End the transaction
                    db.close(); // Close the database
                }
            }
        }

        public void saveUserInfo(String userId, String name, String gender, double height, double weight, byte[] avatar) {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_ID, userId);
            values.put(COLUMN_NAME, name); // Save name
            values.put(COLUMN_GENDER, gender);
            values.put(COLUMN_HEIGHT, height);
            values.put(COLUMN_WEIGHT, weight);
            values.put("avatar", avatar); // Save avatar

            db.insertWithOnConflict(TABLE_USER, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }
        public User getUserInfo(String userId) {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT " + COLUMN_NAME + ", " + COLUMN_GENDER + ", " + COLUMN_HEIGHT + ", " + COLUMN_WEIGHT + ", avatar FROM " + TABLE_USER + " WHERE " + COLUMN_ID + " = ?", new String[]{userId});
            User user = null;
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME));
                String gender = cursor.getString(cursor.getColumnIndex(COLUMN_GENDER));
                double height = cursor.getDouble(cursor.getColumnIndex(COLUMN_HEIGHT));
                double weight = cursor.getDouble(cursor.getColumnIndex(COLUMN_WEIGHT));
                byte[] avatar = cursor.getBlob(cursor.getColumnIndex("avatar"));
                user = new User(name, gender, height, weight, avatar);
                cursor.close();
            }
            return user;
        }
        public List<Record> getAllRecords() {
            List<Record> records = new ArrayList<>();
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = null;
            try {
                cursor = db.query(TABLE_WATER_INTAKE, null, null, null, null, null, null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        int idIndex = cursor.getColumnIndex(COLUMN_INTAKE_ID);
                        int amountIndex = cursor.getColumnIndex(COLUMN_INTAKE_AMOUNT);
                        int timestampIndex = cursor.getColumnIndex(COLUMN_INTAKE_TIMESTAMP);
                        if (idIndex >= 0 && amountIndex >= 0 && timestampIndex >= 0) {
                            int id = cursor.getInt(idIndex);
                            String amount = cursor.getString(amountIndex);
                            String timestamp = cursor.getString(timestampIndex);
                            records.add(new Record(id, amount, timestamp));
                        }
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
                db.close();
            }
            return records;
        }
        public List<Integer> getWaterIntakeByDay(String date , String userId) {
            List<Integer> hourlyIntake = new ArrayList<>();
            for (int i = 0; i < 24; i++) {
                hourlyIntake.add(0);
            }

            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = null;
            try {
                cursor = db.rawQuery("SELECT " + COLUMN_INTAKE_AMOUNT + ", strftime('%H', " + COLUMN_INTAKE_TIMESTAMP + ") as hour FROM " + TABLE_WATER_INTAKE + " WHERE DATE(" + COLUMN_INTAKE_TIMESTAMP + ") = ?", new String[]{date});
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        int amountIndex = cursor.getColumnIndex(COLUMN_INTAKE_AMOUNT);
                        int hourIndex = cursor.getColumnIndex("hour");
                        if (amountIndex >= 0 && hourIndex >= 0) {
                            int amount = cursor.getInt(amountIndex);
                            int hour = cursor.getInt(hourIndex);
                            hourlyIntake.set(hour, hourlyIntake.get(hour) + amount);
                        }
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
                db.close();
            }
            return hourlyIntake;
        }
        public List<Integer> getWaterIntakeByCustomWeek(String date, String userId) {
            List<Integer> weeklyIntake = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                weeklyIntake.add(0);
            }

            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = null;
            try {
                cursor = db.rawQuery("SELECT " + COLUMN_INTAKE_AMOUNT + ", strftime('%w', " + COLUMN_INTAKE_TIMESTAMP + ") as day FROM " + TABLE_WATER_INTAKE + " WHERE DATE(" + COLUMN_INTAKE_TIMESTAMP + ") BETWEEN DATE(?, '-6 days') AND DATE(?)", new String[]{date, date});
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        int amountIndex = cursor.getColumnIndex(COLUMN_INTAKE_AMOUNT);
                        int dayIndex = cursor.getColumnIndex("day");
                        if (amountIndex >= 0 && dayIndex >= 0) {
                            int amount = cursor.getInt(amountIndex);
                            int day = cursor.getInt(dayIndex);
                            weeklyIntake.set(day, weeklyIntake.get(day) + amount);
                        }
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
                db.close();
            }
            return weeklyIntake;
        }

        public List<Integer> getWaterIntakeByMonth(String month, String userId) {
                List<Integer> monthlyIntake = new ArrayList<>();
                Calendar calendar = Calendar.getInstance();
                String[] dateParts = month.split("-");
                int year = Integer.parseInt(dateParts[0]);
                int monthValue = Integer.parseInt(dateParts[1]);
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, monthValue - 1);
                int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                // Khởi tạo danh sách monthlyIntake với giá trị 0 cho mỗi ngày trong tháng
                for (int i = 0; i < daysInMonth; i++) {
                    monthlyIntake.add(0);
                }

                SQLiteDatabase db = this.getReadableDatabase();
                Cursor cursor = db.rawQuery(
                    "SELECT " + COLUMN_INTAKE_AMOUNT + ", " + COLUMN_INTAKE_TIMESTAMP +
                    " FROM " + TABLE_WATER_INTAKE +
                    " WHERE strftime('%Y-%m', " + COLUMN_INTAKE_TIMESTAMP + ") = ?",
                    new String[]{month}
                );

                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        int amountIndex = cursor.getColumnIndex(COLUMN_INTAKE_AMOUNT);
                        int timestampIndex = cursor.getColumnIndex(COLUMN_INTAKE_TIMESTAMP);
                        if (amountIndex >= 0 && timestampIndex >= 0) {
                            int amount = cursor.getInt(amountIndex);
                            String timestamp = cursor.getString(timestampIndex);
                            String dayString = timestamp.split(" ")[0].split("-")[2];
                            int day = Integer.parseInt(dayString);

                            // Kiểm tra chỉ số hợp lệ trước khi truy cập danh sách
                            if (day > 0 && day <= daysInMonth) {
                                monthlyIntake.set(day - 1, monthlyIntake.get(day - 1) + amount);
                            } else {
                                Log.e("DatabaseHelper", "Invalid day value: " + day);
                            }
                        }
                    }
                    cursor.close();
                }
                return monthlyIntake;
            }
        // DatabaseHelper.java
        public List<Integer> getWaterIntakeByDayForDownload(String userId, String date) {
            Log.d("DatabaseHelper", "getWaterIntakeByDayForDownload called with userId: " + userId + " and date: " + date);
            List<Integer> hourlyIntake = new ArrayList<>();
            for (int i = 0; i < 24; i++) {
                hourlyIntake.add(0);
            }

            // Convert date from dd/MM/yyyy to yyyy-MM-dd
            String[] dateParts = date.split("/");
            if (dateParts.length != 3) {
                Log.e("DatabaseHelper", "Invalid date format: " + date);
                return hourlyIntake;
            }
            String formattedDate = dateParts[2] + "-" + dateParts[1] + "-" + dateParts[0];

            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT " + COLUMN_INTAKE_AMOUNT + ", strftime('%H', " + COLUMN_INTAKE_TIMESTAMP + ") as hour FROM " + TABLE_WATER_INTAKE + " WHERE strftime('%Y-%m-%d', " + COLUMN_INTAKE_TIMESTAMP + ") = ? AND id_user = ?", new String[]{formattedDate, userId});
            Log.d("DatabaseHelper", "Query executed: SELECT " + COLUMN_INTAKE_AMOUNT + ", strftime('%H', " + COLUMN_INTAKE_TIMESTAMP + ") as hour FROM " + TABLE_WATER_INTAKE + " WHERE strftime('%Y-%m-%d', " + COLUMN_INTAKE_TIMESTAMP + ") = ? AND id_user = ?");

            if (cursor != null) {
                Log.d("DatabaseHelper", "Cursor is not null");
                if (cursor.getCount() > 0) {
                    while (cursor.moveToNext()) {
                        int amountIndex = cursor.getColumnIndex(COLUMN_INTAKE_AMOUNT);
                        int hourIndex = cursor.getColumnIndex("hour");
                        if (amountIndex >= 0 && hourIndex >= 0) {
                            int amount = cursor.getInt(amountIndex);
                            int hour = cursor.getInt(hourIndex);
                            hourlyIntake.set(hour, hourlyIntake.get(hour) + amount);
                        }
                    }
                } else {
                    Log.d("DatabaseHelper", "Cursor is empty");
                }
                cursor.close();
            } else {
                Log.d("DatabaseHelper", "Cursor is null");
            }
            return hourlyIntake;
        }
        public List<Integer> getWaterIntakeByWeekForDownload(String userId, String date) {
                    List<Integer> weeklyIntake = new ArrayList<>();
                    for (int i = 0; i < 7; i++) {
                        weeklyIntake.add(0);
                    }

                    // Parse the input date
                    String[] dateParts = date.split("/");
                    String weekPart = dateParts[0];
                    String month = dateParts[1];
                    String year = dateParts[2];

                    // Determine the start and end dates of the week
                    int startDay = 1;
                    int endDay = 7;
                    switch (weekPart) {
                        case "week2":
                            startDay = 8;
                            endDay = 14;
                            break;
                        case "week3":
                            startDay = 15;
                            endDay = 21;
                            break;
                        case "week4":
                            startDay = 22;
                            endDay = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH);
                            break;
                    }

                    String startDate = year + "-" + month + "-" + String.format("%02d", startDay);
                    String endDate = year + "-" + month + "-" + String.format("%02d", endDay);

                    SQLiteDatabase db = this.getReadableDatabase();
                    Cursor cursor = db.rawQuery("SELECT " + COLUMN_INTAKE_AMOUNT + ", strftime('%w', " + COLUMN_INTAKE_TIMESTAMP + ") as day FROM " + TABLE_WATER_INTAKE + " WHERE DATE(" + COLUMN_INTAKE_TIMESTAMP + ") BETWEEN ? AND ? AND id_user = ?", new String[]{startDate, endDate, userId});
                    if (cursor != null) {
                        while (cursor.moveToNext()) {
                            int amountIndex = cursor.getColumnIndex(COLUMN_INTAKE_AMOUNT);
                            int dayIndex = cursor.getColumnIndex("day");
                            if (amountIndex >= 0 && dayIndex >= 0) {
                                int amount = cursor.getInt(amountIndex);
                                int day = cursor.getInt(dayIndex);
                                weeklyIntake.set(day, weeklyIntake.get(day) + amount);
                            }
                        }
                        cursor.close();
                    }
                    return weeklyIntake;
                }

               public List<Integer> getWaterIntakeByMonthForDownload(String userId, String date) {
                   List<Integer> monthlyIntake = new ArrayList<>();
                   Calendar calendar = Calendar.getInstance();
                   String[] dateParts = date.split("/");
                   int month = Integer.parseInt(dateParts[0]);
                   Log.d("DatabaseHelper", "Month: " + month);
                   int year = Integer.parseInt(dateParts[1]);
                   Log.d("DatabaseHelper", "Year: " + year);
                   calendar.set(Calendar.YEAR, year);
                   calendar.set(Calendar.MONTH, month - 1);
                   int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
                   for (int i = 0; i < daysInMonth; i++) {
                       monthlyIntake.add(0);
                   }

                   String formattedMonth = year + "-" + String.format("%02d", month);
                   Log.d("DatabaseHelper", "Formatted Month: " + formattedMonth);
                   SQLiteDatabase db = this.getReadableDatabase();
                   Cursor cursor = db.rawQuery("SELECT " + COLUMN_INTAKE_AMOUNT + ", " + COLUMN_INTAKE_TIMESTAMP + " FROM " + TABLE_WATER_INTAKE + " WHERE strftime('%Y-%m', " + COLUMN_INTAKE_TIMESTAMP + ") = ? AND id_user = ?", new String[]{formattedMonth, userId});
                   if (cursor != null) {
                       Log.d("DatabaseHelper", "Cursor is not null, processing data...");
                       if (cursor.getCount() > 0) {
                           while (cursor.moveToNext()) {
                               int amountIndex = cursor.getColumnIndex(COLUMN_INTAKE_AMOUNT);
                               int timestampIndex = cursor.getColumnIndex(COLUMN_INTAKE_TIMESTAMP);
                               if (amountIndex >= 0 && timestampIndex >= 0) {
                                   int amount = cursor.getInt(amountIndex);
                                   String timestamp = cursor.getString(timestampIndex);
                                   String dayString = timestamp.split(" ")[0].split("-")[2];
                                   int day = Integer.parseInt(dayString);
                                   Log.d("DatabaseHelper", "Retrieved amount: " + amount + " for day: " + day);
                                   if (day > 0 && day <= daysInMonth) {
                                       monthlyIntake.set(day - 1, monthlyIntake.get(day - 1) + amount);
                                       Log.d("DatabaseHelper", "Updated monthly intake for day " + (day - 1) + ": " + monthlyIntake.get(day - 1));
                                   } else {
                                       Log.e("DatabaseHelper", "Invalid day value: " + day);
                                   }
                               }
                           }
                       } else {
                           Log.d("DatabaseHelper", "Cursor is empty");
                       }
                       cursor.close();
                       Log.d("DatabaseHelper", "Cursor processing completed.");
                   } else {
                       Log.d("DatabaseHelper", "Cursor is null.");
                   }
                   return monthlyIntake;
               }
    }
class User {
    private String name;
    private String gender;
    private double height;
    private double weight;
    private byte[] avatar;

    public User(String name, String gender, double height, double weight, byte[] avatar) {
        this.name = name;
        this.gender = gender;
        this.height = height;
        this.weight = weight;
        this.avatar = avatar;
    }

    public String getName() {
        return name;
    }

    public String getGender() {
        return gender;
    }

    public double getHeight() {
        return height;
    }

    public double getWeight() {
        return weight;
    }

    public byte[] getAvatar() {
        return avatar;
    }
}
