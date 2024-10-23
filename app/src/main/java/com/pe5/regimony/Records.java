package com.pe5.regimony;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.Calendar;
import android.database.Cursor;
import android.widget.Toast;
import com.pe5.regimony.DatabaseHelper;

public class Records extends AppCompatActivity {

    private CalendarView calendarView;
    private TextView txtSteps, txtBmi, txtBmiCategory;
    private Calendar currentCalendar;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_records);

        // Initialize views
        calendarView = findViewById(R.id.calendarView);
        txtSteps = findViewById(R.id.txt_steps);
        txtBmi = findViewById(R.id.txt_bmi);
        txtBmiCategory = findViewById(R.id.txt_bmi_category);

        // Initialize the database helper
        databaseHelper = new DatabaseHelper(this);

        // Set current calendar instance
        currentCalendar = Calendar.getInstance();

        // Hide the TextViews initially
        txtSteps.setVisibility(View.GONE);
        txtBmi.setVisibility(View.GONE);
        txtBmiCategory.setVisibility(View.GONE);

        // Handle calendar date change
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            String selectedDate = formatDate(year, month, dayOfMonth);
            showDataForDate(selectedDate);
        });

        // Initialize the view with current date's data
        showDataForDate(getCurrentDate());

        // Setup BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.navigation_records);

        // Handle bottom navigation item clicks
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.navigation_home) {
                Intent intent = new Intent(Records.this, Home.class);
                startActivity(intent);
                finish(); // Close current Records activity
                return true;
            } else if (id == R.id.navigation_daily) {
                Intent intent = new Intent(Records.this, Daily.class);
                startActivity(intent);
                finish(); // Close current Records activity
                return true;
            } else if (id == R.id.navigation_records) {
                return true; // Already in Records activity, do nothing
            } else if (id == R.id.navigation_profile) {
                Intent intent = new Intent(Records.this, Profile.class);
                startActivity(intent);
                finish(); // Close current Records activity
                return true;
            }

            return false;
        });
    }

    // Get the current date in the desired format
    private String getCurrentDate() {
        int year = currentCalendar.get(Calendar.YEAR);
        int month = currentCalendar.get(Calendar.MONTH); // month is zero-indexed
        int day = currentCalendar.get(Calendar.DAY_OF_MONTH);
        return formatDate(year, month, day);
    }

    // Format the date to a string in the format YYYY-MM-DD
    private String formatDate(int year, int month, int dayOfMonth) {
        return String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
    }

    // Show data for the selected date
    private void showDataForDate(String date) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            // Fetch data from the database for the selected date
            db = databaseHelper.getReadableDatabase(); // Open the database
            cursor = db.rawQuery("SELECT * FROM " + databaseHelper.getTableDaily() + " WHERE " + databaseHelper.getColumnDate() + " = ?", new String[]{date});

            // Check if cursor returns any data
            if (cursor != null && cursor.moveToFirst()) {
                // If data is found, log or toast the steps
                int steps = cursor.getInt(cursor.getColumnIndex("steps"));
                double bmi = cursor.getDouble(cursor.getColumnIndex("bmi"));
                String bmiCategory = cursor.getString(cursor.getColumnIndex("bmi_category"));

                // Only display the TextViews if the data is available
                if (steps > 0) {
                    txtSteps.setText("" + steps);
                    txtSteps.setVisibility(View.VISIBLE);
                } else {
                    txtSteps.setVisibility(View.GONE);
                }

                if (bmi > 0) {
                    txtBmi.setText("" + bmi);
                    txtBmi.setVisibility(View.VISIBLE);
                } else {
                    txtBmi.setVisibility(View.GONE);
                }

                if (bmiCategory != null && !bmiCategory.equals("N/A")) {
                    txtBmiCategory.setText("" + bmiCategory);
                    txtBmiCategory.setVisibility(View.VISIBLE);
                } else {
                    txtBmiCategory.setVisibility(View.GONE);
                }
            } else {
                // If no data is found, show this message
                Toast.makeText(this, "No data found for date: " + date, Toast.LENGTH_SHORT).show();

                // Hide the TextViews if no data is found
                txtSteps.setVisibility(View.GONE);
                txtBmi.setVisibility(View.GONE);
                txtBmiCategory.setVisibility(View.GONE);
            }
        } finally {
            // Close cursor and database in finally block to ensure they are always closed
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }

}
