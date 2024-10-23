package com.pe5.regimony;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class Daily extends AppCompatActivity implements SensorEventListener {

    private static final int PERMISSION_REQUEST_ACTIVITY_RECOGNITION = 100;
    private static final int PERMISSION_REQUEST_NOTIFICATION = 101;
    private static final int PERMISSION_REQUEST_SCHEDULE_ALARM = 102;

    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private TextView stepCountTextView;
    private Button simulateStepButton, resetStepsButton;

    private DatabaseHelper dbHelper;

    // Variables for BMI calculation
    private TextView bmiResultTextView, bmiCategoryTextView;
    private EditText weightInput, heightInput;
    private Button calculateBmiButton, addBmiButton;

    private int totalSteps = 0;
    private int previousTotalSteps = 0;

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "stepCounterPrefs";
    private static final String PREVIOUS_STEPS_KEY = "previousTotalSteps";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily);

        dbHelper = new DatabaseHelper(this);

        stepCountTextView = findViewById(R.id.stepCountTextView);
        simulateStepButton = findViewById(R.id.simulateStepButton);
        resetStepsButton = findViewById(R.id.resetStepsButton);

        // Initialize views for BMI calculation
        weightInput = findViewById(R.id.weightInput);
        heightInput = findViewById(R.id.heightInput);
        bmiResultTextView = findViewById(R.id.bmiResultTextView);
        bmiCategoryTextView = findViewById(R.id.bmiCategoryTextView);
        calculateBmiButton = findViewById(R.id.calculateBmiButton);
        addBmiButton = findViewById(R.id.addBmiButton);

        bmiResultTextView.setVisibility(View.GONE);
        bmiCategoryTextView.setVisibility(View.GONE);

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
            checkNotificationPermissionAndStartStepCounter();
        }

        // Schedule the alarm for midnight reset
        checkAlarmPermission();

        // Setup BottomNavigationView
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

        // Add button functionality
        simulateStepButton.setOnClickListener(v -> simulateStep());
        resetStepsButton.setOnClickListener(v -> triggerMidnightReset());

        // Set up BMI calculation button logic
        calculateBmiButton.setOnClickListener(v -> calculateAndDisplayBMI());

        // Add BMI and step data to the database
        addBmiButton.setOnClickListener(v -> saveToDatabase());
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

    private void checkAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!getSystemService(AlarmManager.class).canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            } else {
                scheduleMidnightReset();
            }
        } else {
            scheduleMidnightReset();
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
        totalSteps = sharedPreferences.getInt("currentSteps", 0); // Get current steps as well
        stepCountTextView.setText(String.valueOf(totalSteps));  // Set the step count from SharedPreferences
    }

    private void scheduleMidnightReset() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        Intent intent = new Intent(this, MidnightResetReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis() + AlarmManager.INTERVAL_DAY, pendingIntent);
        }
    }


    private void simulateStep() {
        totalSteps++;
        stepCountTextView.setText(String.valueOf(totalSteps));

        // Save simulated steps to SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("currentSteps", totalSteps);
        editor.apply();
    }

    private void triggerMidnightReset() {
        // Retrieve steps from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("stepCounterPrefs", Context.MODE_PRIVATE);
        int stepsToday = sharedPreferences.getInt("currentSteps", 0);  // Get current steps from SharedPreferences

        // Log the current steps
        Log.d("Daily", "triggerMidnightReset: Steps today: " + stepsToday);

        // Insert steps into the database for the current day (simulate yesterday's steps)
        DatabaseHelper db = new DatabaseHelper(this);
        String yesterdayDate = getYesterdayDate();  // Treat this as yesterday's date for simulation
        db.updateStepsForPreviousDay(yesterdayDate, stepsToday);

        // Send notification about the steps today (simulating notification for steps taken yesterday)
        sendStepNotification(this, stepsToday);

        // Reset the steps in SharedPreferences for the new day
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("previousTotalSteps", 0);  // Reset previousTotalSteps to 0
        editor.putInt("currentSteps", 0);        // Reset currentSteps to 0
        editor.apply();

        // Reset the TextView to 0 to reflect the reset in the UI
        stepCountTextView.setText(String.valueOf(0));

        // Reset the totalSteps variable in the current activity
        totalSteps = 0;

        Log.d("Daily", "triggerMidnightReset: Steps have been reset to 0 for the new day.");
    }


    // This method will get yesterday's date in the required format
    private String getYesterdayDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -1);  // Move one day back to get yesterday's date
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(calendar.getTime());
    }


    // This method will send a notification with the step count
    private void sendStepNotification(Context context, int stepsYesterday) {
        String channelId = "MidnightResetNotificationChannel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "Midnight Reset", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, channelId)
                .setContentTitle("Simulated Steps")
                .setContentText("You took " + stepsYesterday + " steps in the simulation.")
                .setSmallIcon(R.drawable.ic_step_counter)  // Ensure this drawable exists
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        notificationManagerCompat.notify(1, notificationBuilder.build());

        Log.d("Daily", "Notification sent for simulated steps: " + stepsYesterday);
    }




    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            int stepsSinceBoot = (int) event.values[0];

            if (previousTotalSteps == 0) {
                previousTotalSteps = stepsSinceBoot;
            }

            int stepsToday = stepsSinceBoot - previousTotalSteps;
            totalSteps = stepsToday;
            stepCountTextView.setText(String.valueOf(stepsToday));

            // Save the steps to SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("currentSteps", totalSteps);
            editor.putInt(PREVIOUS_STEPS_KEY, previousTotalSteps);
            editor.apply();
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

            // Save current steps in SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("currentSteps", totalSteps);
            editor.putInt(PREVIOUS_STEPS_KEY, previousTotalSteps);
            editor.apply();

            Log.d("StepCounter", "Steps saved: " + totalSteps);

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

            // Restore steps from SharedPreferences
            previousTotalSteps = sharedPreferences.getInt(PREVIOUS_STEPS_KEY, 0);
            totalSteps = sharedPreferences.getInt("currentSteps", 0);

            // Update the TextView with current steps
            stepCountTextView.setText(String.valueOf(totalSteps));
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

    private void calculateAndDisplayBMI() {
        String weightText = weightInput.getText().toString();
        String heightText = heightInput.getText().toString();

        if (!weightText.isEmpty() && !heightText.isEmpty()) {
            double weight = Double.parseDouble(weightText);
            double height = Double.parseDouble(heightText);

            if (height > 0) {
                double bmi = weight / ((height / 100) * (height / 100));
                bmiResultTextView.setText("" + String.format(Locale.getDefault(), "%.2f", bmi));

                String bmiCategory = getBMICategory(bmi);
                bmiCategoryTextView.setText("" + bmiCategory);

                bmiResultTextView.setVisibility(View.VISIBLE);
                bmiCategoryTextView.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(this, "Height must be greater than 0", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Please enter weight and height", Toast.LENGTH_SHORT).show();
        }
    }

    private String getBMICategory(double bmi) {
        if (bmi < 18.5) {
            return "Underweight";
        } else if (bmi >= 18.5 && bmi < 25) {  // Corrected the upper bound for "Normal"
            return "Normal";
        } else if (bmi >= 25 && bmi < 30) {
            return "Overweight";
        } else {
            return "Obese";
        }

    }

    private void saveToDatabase() {
        String weightText = weightInput.getText().toString();
        String heightText = heightInput.getText().toString();

        if (!weightText.isEmpty() && !heightText.isEmpty()) {
            double weight = Double.parseDouble(weightText);
            double height = Double.parseDouble(heightText);

            if (height > 0) {
                double bmi = weight / ((height / 100) * (height / 100));
                String bmiCategory = getBMICategory(bmi);

                int currentSteps = getCurrentStepsFromService();
                String currentDate = getCurrentDate();

                dbHelper.insertOrUpdateDailyData(currentDate, currentSteps, bmi, bmiCategory);
                Toast.makeText(this, "Data saved successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Height must be greater than 0", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Please enter weight and height", Toast.LENGTH_SHORT).show();
        }
    }

    private int getCurrentStepsFromService() {
        return totalSteps;
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(Calendar.getInstance().getTime());
    }
}
