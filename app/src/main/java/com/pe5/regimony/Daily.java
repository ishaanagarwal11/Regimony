package com.pe5.regimony;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class Daily extends AppCompatActivity {

    private static final String CHANNEL_ID = "StepCounterServiceChannel";
    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private DatabaseHelper dbHelper;

    private TextView stepCountTextView, bmiResultTextView, bmiCategoryTextView;
    private EditText weightInput, heightInput;
    private Button calculateBmiButton, addBmiButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily);

        dbHelper = new DatabaseHelper(this);

        // Initialize views
        stepCountTextView = findViewById(R.id.stepCountTextView);
        bmiResultTextView = findViewById(R.id.bmiResultTextView);
        bmiCategoryTextView = findViewById(R.id.bmiCategoryTextView);
        weightInput = findViewById(R.id.weightInput);
        heightInput = findViewById(R.id.heightInput);
        calculateBmiButton = findViewById(R.id.calculateBmiButton);
        addBmiButton = findViewById(R.id.addBmiButton);

        // Initialize and set up bottom navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Set the Daily menu item as selected
        bottomNavigationView.setSelectedItemId(R.id.navigation_daily);

        // Handle bottom navigation item clicks
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

        // Check for exact alarm permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                // Prompt the user to grant exact alarm permission
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }

        // Start the foreground service to track steps in the background
        startStepCounterService();

        // Set up an alarm to reset steps at midnight
        setMidnightAlarm(this);

        // Set up BMI calculation button logic
        calculateBmiButton.setOnClickListener(v -> calculateAndDisplayBMI());

        // Add BMI and step data to the database
        addBmiButton.setOnClickListener(v -> saveToDatabase());
    }

    // Method to calculate and display BMI
    private void calculateAndDisplayBMI() {
        String weightText = weightInput.getText().toString();
        String heightText = heightInput.getText().toString();

        if (!weightText.isEmpty() && !heightText.isEmpty()) {
            double weight = Double.parseDouble(weightText);
            double height = Double.parseDouble(heightText);

            if (height > 0) {
                double bmi = weight / ((height / 100) * (height / 100));
                bmiResultTextView.setText("BMI: " + String.format(Locale.getDefault(), "%.2f", bmi));

                String bmiCategory = getBMICategory(bmi);
                bmiCategoryTextView.setText("Category: " + bmiCategory);
            } else {
                Toast.makeText(this, "Height must be greater than 0", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Please enter weight and height", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to get BMI category based on BMI value
    private String getBMICategory(double bmi) {
        if (bmi < 18.5) {
            return "Underweight";
        } else if (bmi >= 18.5 && bmi < 24.9) {
            return "Normal";
        } else if (bmi >= 25 && bmi < 29.9) {
            return "Overweight";
        } else {
            return "Obese";
        }
    }

    // Method to save steps, BMI, and category to the database
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

    // Get current step count from SharedPreferences updated by the service
    private int getCurrentStepsFromService() {
        SharedPreferences prefs = getSharedPreferences("stepCounterPrefs", Context.MODE_PRIVATE);
        return prefs.getInt("currentSteps", 0); // Return 0 if no value is found
    }

    // Method to get current date in "yyyy-MM-dd" format
    private String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date currentDate = Calendar.getInstance().getTime();
        return dateFormat.format(currentDate);
    }

    // Start the step counter service in the background as a Foreground Service
    private void startStepCounterService() {
        Intent serviceIntent = new Intent(this, StepCounterService.class);

        // For API 26+, use startForegroundService if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    // Alarm Setup for midnight reset
    public void setMidnightAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, StepResetReceiver.class);
        PendingIntent pendingIntent;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        // For Android 12 and above, ensure exact alarm permission is granted
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            } else {
                // Handle case where permission is not granted
                Toast.makeText(context, "Exact alarm permission not granted", Toast.LENGTH_SHORT).show();
            }
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        } else {
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
        }
    }

    // Foreground Service for Step Counting
    public static class StepCounterService extends Service implements SensorEventListener {
        private SensorManager sensorManager;
        private Sensor stepCounterSensor;

        @Override
        public void onCreate() {
            super.onCreate();

            // Step counter sensor initialization
            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            if (sensorManager != null) {
                stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
                if (stepCounterSensor != null) {
                    sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
                } else {
                    Toast.makeText(this, "Step Counter sensor not available!", Toast.LENGTH_SHORT).show();
                }
            }

            // Start foreground service with notification
            startForegroundServiceWithNotification();
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            return START_STICKY;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (sensorManager != null) {
                sensorManager.unregisterListener(this);
            }
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
                int stepCount = (int) event.values[0];
                SharedPreferences prefs = getSharedPreferences("stepCounterPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("currentSteps", stepCount);
                editor.apply();

                Log.d("StepCounterService", "Step count updated: " + stepCount);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        private void startForegroundServiceWithNotification() {
            createNotificationChannel();

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Step Counter Active")
                    .setContentText("Counting your steps in the background.")
                    .setSmallIcon(R.drawable.ic_step_counter)  // Replace with your own icon
                    .setPriority(NotificationCompat.PRIORITY_LOW);

            startForeground(1, notificationBuilder.build());
        }

        private void createNotificationChannel() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel serviceChannel = new NotificationChannel(
                        CHANNEL_ID,
                        "Step Counter Service Channel",
                        NotificationManager.IMPORTANCE_LOW
                );

                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) {
                    manager.createNotificationChannel(serviceChannel);
                }
            }
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }

    // Step Reset Receiver for resetting steps at midnight
    public static class StepResetReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            DatabaseHelper dbHelper = new DatabaseHelper(context);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar calendar = Calendar.getInstance();

            // Get the previous day's date
            calendar.add(Calendar.DATE, -1);
            String previousDate = dateFormat.format(calendar.getTime());

            // Get final step count from SharedPreferences
            SharedPreferences prefs = context.getSharedPreferences("stepCounterPrefs", Context.MODE_PRIVATE);
            int finalSteps = prefs.getInt("currentSteps", 0);

            // Update the steps for the previous day
            dbHelper.updateStepsForPreviousDay(previousDate, finalSteps);

            // Reset the step counter for the new day
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("stepsAtMidnight", finalSteps);
            editor.apply();

            Toast.makeText(context, "Steps reset for new day", Toast.LENGTH_SHORT).show();
        }
    }
}
