package com.example.cardmasters.utils.lootboxes_utils;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Calendar;

public class AlarmUtils {

    private static final int NOTIFICATION_PERMISSION_CODE = 101;

    public static void checkNotificationPermission(Context context, Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                // בקשת ההרשאה מהמשתמש
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    public static void setupLootBoxAlarm(Context context, int hour, int minute) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, LootBoxReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        // אם השעה 9:30 כבר עברה היום, קבע למחר
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        if (alarmManager != null) {
            // שימוש ב-setInexactRepeating חוסך בסוללה ומתאים לתיבות שלל
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent);
        }
    }
}




