package com.pe5.regimony;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class StepCounterService extends Service implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private SharedPreferences stepPrefs;
    private int previousTotalSteps;

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        stepPrefs = getSharedPreferences("stepCounterPrefs", MODE_PRIVATE);
        previousTotalSteps = stepPrefs.getInt("previousTotalSteps", 0);

        if (sensorManager != null) {
            stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            if (stepCounterSensor != null) {
                sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }

        startForegroundServiceWithNotification();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            int totalStepsSinceBoot = (int) event.values[0];

            // Calculate steps taken
            if (previousTotalSteps == 0) {
                previousTotalSteps = totalStepsSinceBoot;
            }

            int stepsToday = totalStepsSinceBoot - previousTotalSteps;

            // Save the steps in SharedPreferences
            SharedPreferences.Editor editor = stepPrefs.edit();
            editor.putInt("currentSteps", stepsToday);
            editor.putInt("previousTotalSteps", previousTotalSteps);
            editor.apply();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed for this implementation
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    private void startForegroundServiceWithNotification() {
        String channelId = "StepCounterServiceChannel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            NotificationChannel channel = new NotificationChannel(channelId, "Step Counter Service", NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Step Counter Active")
                .setContentText("Counting your steps in the background.")
                .setSmallIcon(R.drawable.ic_step_counter)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        startForeground(1, notificationBuilder.build());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
