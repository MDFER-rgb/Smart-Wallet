package com.example.smartwallet;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * Handles creation + sending notifications safely on all Android versions.
 */
public class NotificationHelper {

    public static final String CHANNEL_ID = "price_alerts_channel";

    // ---------------------------------------------------------
    // 🔥 Create Notification Channel (Android 8+)
    // ---------------------------------------------------------
    public static void createNotificationChannel(Context ctx) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Price Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );

            channel.setDescription("Notifications for crypto price alerts");

            NotificationManager manager = ctx.getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    // ---------------------------------------------------------
    // 🔥 Send Notification (Handles Android 13+ permission)
    // ---------------------------------------------------------
    public static void sendNotification(Context ctx, int id, String title, String body) {

        // Android 13 permission check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    ctx,
                    android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
                // Permission not granted → Do NOT crash
                return;
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)      // make sure this icon exists
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(ctx);
        managerCompat.notify(id, builder.build());
    }
}
