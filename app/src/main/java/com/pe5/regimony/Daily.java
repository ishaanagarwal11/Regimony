package com.pe5.regimony;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class Daily extends AppCompatActivity implements SensorEventListener {

    private static final int PERMISSION_REQUEST_ACTIVITY_RECOGNITION = 100;
    private static final int PERMISSION_REQUEST_NOTIFICATION = 101;

    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private TextView stepCountTextView;

    private int totalSteps = 0;
    private int previousTotalSteps = 0;

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "stepCounterPrefs";
    private static final String STEPS_SINCE_BOOT_KEY = "stepsSinceBoot";
    private static final String PREVIOUS_STEPS_KEY = "previousTotalSteps";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily);

        stepCountTextView = findViewById(R.id.stepCountTextView);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Check and request permission for activity recognition
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, PERMISSION_REQUEST_ACTIVITY_RECOGNITION);
            } else {
                checkNotificationPermissionAndStartStepCounter();
            }
        } else {
            checkNotificationPermissionAndStartStepCounter(); // For versions below Android 10
        }

        // Setup BottomNavigationView...
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.navigation_daily);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.navigation_home) {
                Intent intent = new Intent(Daily.this, Home.class);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.navigation_daily) {
                return true; // Already in Daily activity, do nothing
            } else if (id == R.id.navigation_records) {
                Intent intent = new Intent(Daily.this, Records.class);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.navigation_profile) {
                Intent intent = new Intent(Daily.this, Profile.class);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });
    }

    private void checkNotificationPermissionAndStartStepCounter() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_NOTIFICATION);
            } else {
                startStepCounter();
            }
        } else {
            startStepCounter();
        }
    }

    private void startStepCounter() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            if (stepCounterSensor != null) {
                sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI);
            } else {
                Toast.makeText(this, "Step Counter Sensor not available", Toast.LENGTH_SHORT).show();
            }
        }

        // Retrieve the previous steps stored from SharedPreferences
        previousTotalSteps = sharedPreferences.getInt(PREVIOUS_STEPS_KEY, 0);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            int stepsSinceBoot = (int) event.values[0];

            // If it's the first time, initialize previous total steps with current boot steps
            if (previousTotalSteps == 0) {
                previousTotalSteps = stepsSinceBoot;
            }

            // Calculate steps taken today
            int stepsToday = stepsSinceBoot - previousTotalSteps;
            stepCountTextView.setText(String.valueOf(stepsToday));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not required for this implementation.
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);

            // Store current steps in SharedPreferences before leaving the activity
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(PREVIOUS_STEPS_KEY, previousTotalSteps);
            editor.apply();

            // Start the background service to keep counting steps
            Intent serviceIntent = new Intent(this, StepCounterService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI);

            // Stop the background service when activity is resumed
            stopService(new Intent(this, StepCounterService.class));

            // Restore the steps stored from the service while the activity was paused
            previousTotalSteps = sharedPreferences.getInt(PREVIOUS_STEPS_KEY, 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_ACTIVITY_RECOGNITION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkNotificationPermissionAndStartStepCounter();
            } else {
                Toast.makeText(this, "Permission for activity recognition denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == PERMISSION_REQUEST_NOTIFICATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startStepCounter();
            } else {
                Toast.makeText(this, "Permission for notifications denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
