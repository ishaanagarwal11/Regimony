package com.pe5.regimony;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class Records extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_records);

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
                Intent intent = new Intent(Records.this, Daily.class); // Navigate to Records activity
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
}
