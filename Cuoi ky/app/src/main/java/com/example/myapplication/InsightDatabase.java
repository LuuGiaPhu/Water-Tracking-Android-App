package com.example.myapplication;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class InsightDatabase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "insights.db";
    private static final int DATABASE_VERSION = 1;

    // Table and column names
    public static final String TABLE_INSIGHTS = "insights";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TOPIC = "topic";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_IMAGE = "image";
    public static final String COLUMN_CONTENT = "content";

    // SQL to create the table
    private static final String CREATE_TABLE_INSIGHTS = "CREATE TABLE " + TABLE_INSIGHTS + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_TOPIC + " TEXT NOT NULL, " +
            COLUMN_TITLE + " TEXT NOT NULL, " +
            COLUMN_IMAGE + " TEXT, " +
            COLUMN_CONTENT + " TEXT NOT NULL);";

    public InsightDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create the insights table
        db.execSQL(CREATE_TABLE_INSIGHTS);

        // Insert hardcoded data
                db.execSQL("INSERT INTO " + TABLE_INSIGHTS + " (" +
                        COLUMN_TOPIC + ", " +
                        COLUMN_TITLE + ", " +
                        COLUMN_IMAGE + ", " +
                        COLUMN_CONTENT + ") VALUES " +
                        "('Water Drinking', 'Stay Hydrated', 'image_water.png', 'Drinking water is essential for health.'), " +
                        "('Beauty & Skincare', 'Glowing Skin Tips', 'image_skincare.png', 'Follow these tips for glowing skin.'), " +
                        "('Fitness', 'Daily Exercise', 'image_fitness.png', 'Exercise daily to stay fit and healthy.'), " +
                        "('Water Drinking', 'Benefits of Water', 'image_water_benefits.png', 'Water helps maintain the balance of body fluids.'), " +
                        "('Beauty & Skincare', 'Night Skincare Routine', 'image_night_skincare.png', 'A good night routine is key to healthy skin.'), " +
                        "('Fitness', 'Morning Workouts', 'image_morning_workout.png', 'Start your day with a refreshing workout.'), " +
                        "('Water Drinking', 'Water and Energy', 'image_water_energy.png', 'Staying hydrated boosts your energy levels.'), " +
                        "('Beauty & Skincare', 'Natural Remedies', 'image_natural_remedies.png', 'Use natural ingredients for glowing skin.'), " +
                        "('Fitness', 'Stretching Benefits', 'image_stretching.png', 'Stretching improves flexibility and reduces injury risk.'), " +
                        "('Water Drinking', 'Hydration Tips', 'image_hydration_tips.png', 'Drink water regularly to stay hydrated.'), " +
                        "('Beauty & Skincare', 'Sun Protection', 'image_sun_protection.png', 'Always use sunscreen to protect your skin.'), " +
                        "('Fitness', 'Strength Training', 'image_strength_training.png', 'Build muscle and strength with proper training.'), " +
                        "('Water Drinking', 'Water and Skin Health', 'image_water_skin.png', 'Hydration improves skin elasticity.'), " +
                        "('Beauty & Skincare', 'Anti-Aging Tips', 'image_anti_aging.png', 'Follow these tips to reduce signs of aging.'), " +
                        "('Fitness', 'Cardio Workouts', 'image_cardio.png', 'Cardio exercises improve heart health.');");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop the old table if it exists
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INSIGHTS);
        onCreate(db);
    }

    // Method to fetch all insights
    public Cursor getAllInsights() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_INSIGHTS, null);
    }
    public Cursor getInsightsByTopic(String topic) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_INSIGHTS + " WHERE " + COLUMN_TOPIC + " = ?", new String[]{topic});
    }
}