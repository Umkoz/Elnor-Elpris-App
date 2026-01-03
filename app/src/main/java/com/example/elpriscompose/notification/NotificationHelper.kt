package com.example.elpriscompose.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.elpriscompose.MainActivity
import com.example.elpriscompose.R

/**
 * Helper class for creating and showing price alert notifications.
 */
class NotificationHelper(private val context: Context) {

    companion object {
        private const val TAG = "NotificationHelper"
        const val CHANNEL_ID = "price_alerts"
        const val CHANNEL_NAME = "Prisvarningar"
        const val CHANNEL_DESCRIPTION = "Notifikationer när elpriset når dina gränsvärden"

        const val NOTIFICATION_ID_LOW_PRICE = 1001
        const val NOTIFICATION_ID_HIGH_PRICE = 1002
        const val NOTIFICATION_ID_TEST = 1003

        /**
         * Creates the notification channel. Call this from Application or MainActivity.
         */
        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val importance = NotificationManager.IMPORTANCE_HIGH // Changed from DEFAULT
                val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                    description = CHANNEL_DESCRIPTION
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 250, 250, 250)
                    setSound(
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                }

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "Notification channel created: $CHANNEL_ID")
            }
        }
    }

    init {
        createChannel(context)
    }

    /**
     * Shows a test notification to verify everything works
     */
    fun showTestNotification() {
        Log.d(TAG, "Showing test notification")

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bolt)
            .setContentTitle("✅ Notifikationer fungerar!")
            .setContentText("Du kommer få varningar när priset passerar dina gränser.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_TEST, notification)
            Log.d(TAG, "Test notification sent successfully")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for test notification", e)
        }
    }

    /**
     * Shows a notification when price is below the low threshold (good time to charge!)
     */
    fun showLowPriceNotification(currentPrice: Double, threshold: Double, region: String) {
        Log.d(TAG, "Showing low price notification: $currentPrice <= $threshold")

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val priceFormatted = String.format("%.1f", currentPrice)
        val thresholdFormatted = String.format("%.0f", threshold)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bolt)
            .setContentTitle("⚡ Lågt elpris - dags att ladda!")
            .setContentText("$priceFormatted öre/kWh i $region (under $thresholdFormatted öre)")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Elpriset är nu $priceFormatted öre/kWh i $region.\n\nDetta är under din gräns på $thresholdFormatted öre - perfekt tillfälle att ladda elbilen eller köra tunga apparater!"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_LOW_PRICE, notification)
            Log.d(TAG, "Low price notification sent")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for low price notification", e)
        }
    }

    /**
     * Shows a notification when price is above the high threshold (avoid using power!)
     */
    fun showHighPriceNotification(currentPrice: Double, threshold: Double, region: String) {
        Log.d(TAG, "Showing high price notification: $currentPrice >= $threshold")

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val priceFormatted = String.format("%.1f", currentPrice)
        val thresholdFormatted = String.format("%.0f", threshold)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bolt)
            .setContentTitle("⚠️ Högt elpris")
            .setContentText("$priceFormatted öre/kWh i $region (över $thresholdFormatted öre)")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Elpriset är nu $priceFormatted öre/kWh i $region.\n\nDetta är över din gräns på $thresholdFormatted öre - försök undvika tunga apparater just nu."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_HIGH_PRICE, notification)
            Log.d(TAG, "High price notification sent")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for high price notification", e)
        }
    }

    /**
     * Cancels all price alert notifications
     */
    fun cancelAllNotifications() {
        NotificationManagerCompat.from(context).cancelAll()
    }
}