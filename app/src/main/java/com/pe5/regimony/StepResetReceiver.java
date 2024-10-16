package com.pe5.regimony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class StepResetReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        // Debugging message
        Toast.makeText(context, "StepResetReceiver triggered!", Toast.LENGTH_SHORT).show();
        Log.d("StepResetReceiver", "StepResetReceiver triggered!");

        SharedPreferences prefs = context.getSharedPreferences("stepCounterPrefs", Context.MODE_PRIVATE);
        int stepsToday = prefs.getInt("currentSteps", 0);  // Get today's steps before resetting

        // Save the current steps before resetting
        int stepsBeforeReset = stepsToday;

        // Get today's and yesterday's date in "yyyy-MM-dd" format
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -1);  // Subtract 1 to get yesterday's date
        String yesterdayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());

        // Update the database with yesterday's steps (before resetting to zero)
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        dbHelper.updateStepsForPreviousDay(yesterdayDate, stepsBeforeReset);

        // Show notification with total steps of yesterday before reset
        showMidnightNotification(context, stepsBeforeReset);

        // Log the values before resetting
        Log.d("StepResetReceiver", "Before reset: currentSteps=" + prefs.getInt("currentSteps", 0));
        Log.d("StepResetReceiver", "Before reset: previousTotalSteps=" + prefs.getInt("previousTotalSteps", 0));

// Reset step counter
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("currentSteps", 0);  // Reset steps to 0 for the new day
        editor.putInt("previousTotalSteps", stepsBeforeReset);  // Save today's steps
        editor.putBoolean("hasReset", true);
        editor.apply();



// Log the values after resetting
        Log.d("StepResetReceiver", "After reset: currentSteps=" + prefs.getInt("currentSteps", 0));

        // Notify the UI to update the step count to 0
        notifyUIReset(context);

        // Optional toast for debugging or user notification
        Toast.makeText(context, "Steps reset to 0 for a new day.", Toast.LENGTH_SHORT).show();
    }

    private void showMidnightNotification(Context context, int totalSteps) {
        String channelId = "midnight_steps_channel";
        String channelName = "Daily Steps Summary";

        // Create notification channel for Android O+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_step_counter)  // Use appropriate icon
                .setContentTitle("Daily Step Summary")
                .setContentText("Total steps taken yesterday: " + totalSteps)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(1, builder.build());
    }

    private void notifyUIReset(Context context) {
        // Send a broadcast to notify the UI to reset the step count to 0
        Intent resetIntent = new Intent("RESET_STEP_COUNT");
        LocalBroadcastManager.getInstance(context).sendBroadcast(resetIntent);
    }
}
