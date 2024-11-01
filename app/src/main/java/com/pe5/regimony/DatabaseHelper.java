package com.pe5.regimony;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "health_data.db";
    private static final int DATABASE_VERSION = 1;

    // Table and columns
    private static final String TABLE_DAILY = "daily_data";
    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_STEPS = "steps";
    private static final String COLUMN_BMI = "bmi";
    private static final String COLUMN_BMI_CATEGORY = "bmi_category";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_DAILY + " (" +
                COLUMN_DATE + " TEXT PRIMARY KEY, " +
                COLUMN_STEPS + " INTEGER, " +
                COLUMN_BMI + " REAL, " +
                COLUMN_BMI_CATEGORY + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DAILY);
        onCreate(db);
    }

    // Getter for TABLE_DAILY
    public String getTableDaily() {
        return TABLE_DAILY;
    }

    // Getter for COLUMN_DATE
    public String getColumnDate() {
        return COLUMN_DATE;
    }

    // Method to get daily data for a specific date
    public Cursor getDailyDataByDate(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_DAILY + " WHERE " + COLUMN_DATE + " = ?";
        return db.rawQuery(query, new String[]{date});
    }

    // Method to insert or update daily data (steps, BMI, and BMI category)
    public void insertOrUpdateDailyData(String date, int steps, double bmi, String bmiCategory) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Check if a row for the current date already exists
        String query = "SELECT * FROM " + TABLE_DAILY + " WHERE " + COLUMN_DATE + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{date});

        ContentValues values = new ContentValues();
        values.put(COLUMN_STEPS, steps);
        values.put(COLUMN_BMI, bmi);
        values.put(COLUMN_BMI_CATEGORY, bmiCategory);

        if (cursor.getCount() > 0) {
            // If the row exists, update it
            db.update(TABLE_DAILY, values, COLUMN_DATE + " = ?", new String[]{date});
        } else {
            // If the row does not exist, insert a new one
            values.put(COLUMN_DATE, date);
            db.insert(TABLE_DAILY, null, values);
        }
        cursor.close();
        db.close();
    }

    // Method to update steps for the previous day at midnight
    public void updateStepsForPreviousDay(String date, int finalSteps) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Check if a row for the previous day already exists
        String query = "SELECT * FROM " + TABLE_DAILY + " WHERE " + COLUMN_DATE + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{date});

        ContentValues values = new ContentValues();
        values.put(COLUMN_STEPS, finalSteps); // Only updating steps

        if (cursor.getCount() > 0) {
            // If the row exists, update the steps for the previous day
            db.update(TABLE_DAILY, values, COLUMN_DATE + " = ?", new String[]{date});
        } else {
            // If the row does not exist, insert a new row with the date and steps
            values.put(COLUMN_DATE, date);
            db.insert(TABLE_DAILY, null, values);
        }
        cursor.close();
        db.close();
    }
}
