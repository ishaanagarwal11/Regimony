package com.pe5.regimony;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.Calendar;
import android.database.Cursor;
import android.widget.Toast;

public class Records extends AppCompatActivity {

    private CalendarView calendarView;
    private TextView txtSteps, txtBmi, txtBmiCategory;
    private Calendar currentCalendar;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_records);

        calendarView = findViewById(R.id.calendarView);
        txtSteps = findViewById(R.id.txt_steps);
        txtBmi = findViewById(R.id.txt_bmi);
        txtBmiCategory = findViewById(R.id.txt_bmi_category);

        databaseHelper = new DatabaseHelper(this);

        currentCalendar = Calendar.getInstance();

        // Hide the TextViews initially
        txtSteps.setVisibility(View.GONE);
        txtBmi.setVisibility(View.GONE);
        txtBmiCategory.setVisibility(View.GONE);

        // Handle calendar date change
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            String selectedDate = formatDate(year, month, dayOfMonth);
            showDataForDate(selectedDate);
            Toast.makeText(this, "Selected Date: " + selectedDate, Toast.LENGTH_SHORT).show();
        });

        // Initialize the view with current date's data
        showDataForDate(getCurrentDate());

        // Setup BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Set the selected item to records
        bottomNavigationView.setSelectedItemId(R.id.navigation_records);

        // Handle bottom navigation item clicks
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.navigation_home) {
                Intent intent = new Intent(Records.this, Home.class); // Navigate to Home activity
                startActivity(intent);
                finish();  // Close current Records activity
                return true;
            } else if (id == R.id.navigation_daily) {
                Intent intent = new Intent(Records.this, Daily.class); // Navigate to Daily activity
                startActivity(intent);
                finish();  // Close current Records activity
                return true;
            } else if (id == R.id.navigation_records) {
                // Already in Records activity, do nothing
                return true;
            } else if (id == R.id.navigation_profile) {
                Intent intent = new Intent(Records.this, Profile.class); // Navigate to Profile activity
                startActivity(intent);
                finish();  // Close current Records activity
                return true;
            }

            return false;
        });
    }

    private String getCurrentDate() {
        int year = currentCalendar.get(Calendar.YEAR);
        int month = currentCalendar.get(Calendar.MONTH); // month is zero-indexed
        int day = currentCalendar.get(Calendar.DAY_OF_MONTH);
        return formatDate(year, month, day);
    }

    private String formatDate(int year, int month, int dayOfMonth) {
        // Ensure month and day have leading zeros
        return String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
    }

    private void showDataForDate(String date) {
        // Show the selected date for debugging
        Toast.makeText(this, "Selected Date: " + date, Toast.LENGTH_SHORT).show();

        // Fetch data from the database for the selected date
        Cursor cursor = databaseHelper.getDailyDataByDate(date);

        // Check if cursor returns any data
        if (cursor != null && cursor.moveToFirst()) {
            // If data is found, log or toast the steps
            int steps = cursor.getInt(cursor.getColumnIndex("steps"));
            double bmi = cursor.getDouble(cursor.getColumnIndex("bmi"));
            String bmiCategory = cursor.getString(cursor.getColumnIndex("bmi_category"));

            // Only display the TextViews if the data is available
            if (steps > 0) {
                txtSteps.setText("Steps: " + steps);
                txtSteps.setVisibility(View.VISIBLE);
            } else {
                txtSteps.setVisibility(View.GONE);
            }

            if (bmi > 0) {
                txtBmi.setText("BMI: " + bmi);
                txtBmi.setVisibility(View.VISIBLE);
            } else {
                txtBmi.setVisibility(View.GONE);
            }

            if (bmiCategory != null && !bmiCategory.equals("N/A")) {
                txtBmiCategory.setText("Category: " + bmiCategory);
                txtBmiCategory.setVisibility(View.VISIBLE);
            } else {
                txtBmiCategory.setVisibility(View.GONE);
            }

            // Toast message to confirm data was found
            Toast.makeText(this, "Data found for date: " + date, Toast.LENGTH_SHORT).show();
        } else {
            // If no data is found, show this message
            Toast.makeText(this, "No data found for date: " + date, Toast.LENGTH_SHORT).show();

            // If no data is found, hide the TextViews
            txtSteps.setVisibility(View.GONE);
            txtBmi.setVisibility(View.GONE);
            txtBmiCategory.setVisibility(View.GONE);
        }

        if (cursor != null) {
            cursor.close();
        }
    }
}
