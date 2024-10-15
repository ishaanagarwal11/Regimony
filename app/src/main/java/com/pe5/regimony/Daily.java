package com.pe5.regimony;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class Daily extends AppCompatActivity implements SensorEventListener {

    private static final int PERMISSION_REQUEST_ACTIVITY_RECOGNITION = 100;
    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private TextView stepCountTextView;
    private int previousTotalSteps = 0;

    // Variables for BMI calculation
    private EditText weightInput, heightInput;
    private TextView bmiResultTextView, bmiCategoryTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily);

        // Initialize step count display
        stepCountTextView = findViewById(R.id.stepCountTextView);

        // Initialize views for BMI calculation
        weightInput = findViewById(R.id.weightInput);
        heightInput = findViewById(R.id.heightInput);
        bmiResultTextView = findViewById(R.id.bmiResultTextView);
        bmiCategoryTextView = findViewById(R.id.bmiCategoryTextView);
        Button calculateBmiButton = findViewById(R.id.calculateBmiButton);

        // Handle BMI calculation on button click
        calculateBmiButton.setOnClickListener(v -> calculateBMI());

        // Check and request permission for physical activity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, PERMISSION_REQUEST_ACTIVITY_RECOGNITION);
            } else {
                startStepCounter();
            }
        } else {
            startStepCounter(); // No permission required for versions below Android 10
        }

        // Setup BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.navigation_records);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.navigation_home) {
                Intent intent = new Intent(Daily.this, Home.class); // Navigate to Home activity
                startActivity(intent);
                finish();  // Close current Daily activity
                return true;
            } else if (id == R.id.navigation_daily) {
                // Already in Daily activity, do nothing
                return true;
            } else if (id == R.id.navigation_records) {
                Intent intent = new Intent(Daily.this, Records.class); // Navigate to Records activity
                startActivity(intent);
                finish();  // Close current Daily activity
                return true;
            } else if (id == R.id.navigation_profile) {
                Intent intent = new Intent(Daily.this, Profile.class); // Navigate to Profile activity
                startActivity(intent);
                finish();  // Close current Daily activity
                return true;
            }

            return false;
        });
    }

    // Start the step counter sensor
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
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            // This sensor returns the total number of steps since the device was rebooted
            int totalStepsSinceBoot = (int) event.values[0];

            // If it's the first time tracking steps, set the previous total to the current steps
            if (previousTotalSteps == 0) {
                previousTotalSteps = totalStepsSinceBoot;
            }

            // Calculate the actual steps taken today
            int stepsToday = totalStepsSinceBoot - previousTotalSteps;
            stepCountTextView.setText(String.valueOf(stepsToday));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not required for now.
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_ACTIVITY_RECOGNITION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startStepCounter();
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Method to calculate BMI
    private void calculateBMI() {
        String weightStr = weightInput.getText().toString();
        String heightStr = heightInput.getText().toString();

        // Check if weight and height inputs are not empty
        if (TextUtils.isEmpty(weightStr) || TextUtils.isEmpty(heightStr)) {
            Toast.makeText(Daily.this, "Please enter both weight and height", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Parse the inputs to float
            float weight = Float.parseFloat(weightStr);
            float height = Float.parseFloat(heightStr);

            // Validate inputs
            if (weight <= 0 || height <= 0) {
                Toast.makeText(Daily.this, "Weight and height must be positive numbers", Toast.LENGTH_SHORT).show();
                return;
            }

            // BMI Calculation: BMI = weight (kg) / (height (m) * height (m))
            float bmi = weight / (height * height);

            // Display the BMI result
            bmiResultTextView.setText(String.format("BMI: %.2f", bmi));

            // Determine the BMI category
            String bmiCategory;
            if (bmi < 18.5) {
                bmiCategory = "Underweight";
            } else if (bmi >= 18.5 && bmi <= 24.9) {
                bmiCategory = "Normal weight";
            } else if (bmi >= 25 && bmi <= 29.9) {
                bmiCategory = "Overweight";
            } else {
                bmiCategory = "Obesity";
            }

            // Set the BMI category in the TextView
            bmiCategoryTextView.setText(bmiCategory);

        } catch (NumberFormatException e) {
            // Handle number format exception
            Toast.makeText(Daily.this, "Invalid input", Toast.LENGTH_SHORT).show();
        }
    }
}
