package com.example.cardmasters.utils.lootboxes_utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.cardmasters.MainActivity;
import com.example.cardmasters.R;

public class LootBoxReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "LootboxChannel";

    @Override
    public void onReceive(Context context, Intent intent) {
        // 1. עדכון הנתונים ב-SharedPreferences (הקוד הקודם שלך)
        SharedPreferences prefs = context.getSharedPreferences("GamePrefs", Context.MODE_PRIVATE);
        int currentLootboxes = prefs.getInt("lootbox_count", 0);
        prefs.edit().putInt("lootbox_count", currentLootboxes + 1).apply();

        // 2. שליחת ההתראה למשתמש
        showNotification(context);
    }

    private void showNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // יצירת ערוץ התראות (חובה לאנדרואיד 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Lootbox Rewards",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notifications for daily lootboxes");
            notificationManager.createNotificationChannel(channel);
        }

        // מה יקרה כשלוחצים על ההתראה (פתיחת האפליקציה)
        Intent activityIntent = new Intent(context, MainActivity.class); // שנה ל-Activity הראשי שלך
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, 0, activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // בניית ההתראה
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_lootbox) // כאן תשים את האייקון שלך
                .setContentTitle("תיבת שלל מחכה לך!")
                .setContentText("קיבלת לוטבוקס חדש, כנס עכשיו למשחק!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true) // ההתראה תימחק כלוחצים עליה
                .setContentIntent(contentIntent);

        // הצגת ההתראה (המספר 1 הוא ID ייחודי להתראה)
        notificationManager.notify(1, builder.build());
    }
}
