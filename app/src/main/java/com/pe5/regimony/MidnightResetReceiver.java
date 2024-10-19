package com.pe5.regimony;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MidnightResetReceiver extends BroadcastReceiver {

    private static final String TAG = "MidnightResetReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        int stepsYesterday = intent.getIntExtra("stepsToday", 0); // Get steps from Intent

        Log.d(TAG, "onReceive: Steps yesterday: " + stepsYesterday);

        // Insert steps into the database for the previous day
        DatabaseHelper db = new DatabaseHelper(context);
        String yesterdayDate = getYesterdayDate();
        db.updateStepsForPreviousDay(yesterdayDate, stepsYesterday);

        // Send notification about the previous day's steps
        sendStepNotification(context, stepsYesterday);

        // Reset the steps to zero for the new day in SharedPreferences
        SharedPreferences sharedPreferences = context.getSharedPreferences("stepCounterPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("previousTotalSteps", 0);
        editor.putInt("currentSteps", 0);
        editor.apply();

        Log.d(TAG, "onReceive: Steps have been reset to 0.");
    }

    private void sendStepNotification(Context context, int stepsYesterday) {
        String channelId = "MidnightResetNotificationChannel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "Midnight Reset", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, channelId)
                .setContentTitle("Yesterday's Steps")
                .setContentText("You took " + stepsYesterday + " steps yesterday.")
                .setSmallIcon(R.drawable.ic_step_counter)  // Ensure this drawable exists
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        notificationManagerCompat.notify(1, notificationBuilder.build());

        Log.d(TAG, "Notification sent for yesterday's steps: " + stepsYesterday);
    }

    private String getYesterdayDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -1);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(calendar.getTime());
    }
}