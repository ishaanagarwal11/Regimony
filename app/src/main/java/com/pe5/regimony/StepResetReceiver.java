package com.pe5.regimony;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import com.pe5.regimony.DatabaseHelper;

public class StepResetReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);

        // Get the current date and the previous date
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date currentDate = Calendar.getInstance().getTime();
        String currentDateString = dateFormat.format(currentDate);

        // Get previous day's date
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -1);
        Date previousDate = calendar.getTime();
        String previousDateString = dateFormat.format(previousDate);

        // Get the final step count from storage
        int finalSteps = getFinalStepsForDay(context);

        // Update the steps for the previous day in the database
        dbHelper.updateStepsForPreviousDay(previousDateString, finalSteps);

        // Reset the step counter for the new day
        resetStepCounter(context);

        // Show a toast for confirmation
        Toast.makeText(context, "Steps reset for new day", Toast.LENGTH_SHORT).show();
    }

    // Method to get the final step count for the day
    private int getFinalStepsForDay(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("stepCounterPrefs", Context.MODE_PRIVATE);
        return prefs.getInt("currentSteps", 0); // Use the correct key to retrieve current steps
    }

    // Method to reset step counter
    private void resetStepCounter(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("stepCounterPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Get the current step count from SharedPreferences
        int currentStepCount = prefs.getInt("currentSteps", 0);

        // Set stepsAtMidnight for the new day
        editor.putInt("stepsAtMidnight", currentStepCount);
        editor.apply();
    }
}
