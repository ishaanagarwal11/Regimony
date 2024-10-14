package com.pe5.regimony;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class Profile extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Setup BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Set the selected item to profile
        bottomNavigationView.setSelectedItemId(R.id.navigation_profile);

        // Handle bottom navigation item clicks
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.navigation_home) {
                Intent intent = new Intent(Profile.this, Home.class); // Navigate to Home activity
                startActivity(intent);
                finish();  // Close current Daily activity
                return true;
            } else if (id == R.id.navigation_daily) {
                Intent intent = new Intent(Profile.this, Daily.class); // Navigate to Daily activity
                startActivity(intent);
                finish();  // Close current Daily activity
                return true;
            } else if (id == R.id.navigation_records) {
                Intent intent = new Intent(Profile.this, Records.class); // Navigate to Records activity
                startActivity(intent);
                finish();  // Close current Daily activity
                return true;
            } else if (id == R.id.navigation_profile) {
                // Already in Profile activity, do nothing
                return true;
            }

            return false;
        });
    }
}
