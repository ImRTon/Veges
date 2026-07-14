package tw.taipei.veges.data.alerts

import android.annotation.SuppressLint
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AndroidNotificationPermissionState @Inject constructor(
    @ApplicationContext private val context: Context,
) : NotificationPermissionState {
    override fun canPostNotifications(): Boolean =
        android.os.Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}

class AndroidLocalNotificationPublisher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionState: NotificationPermissionState,
) : LocalNotificationPublisher {
    override fun ensureChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "蔬果價格提醒",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "符合估算價格門檻時的本機提醒"
            },
        )
    }

    @SuppressLint("MissingPermission")
    override fun publish(notification: PriceAlertNotification): Boolean {
        if (!permissionState.canPostNotifications()) return false
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.body))
            .setAutoCancel(true)
            .setContentIntent(
                android.app.PendingIntent.getActivity(
                    context,
                    notification.notificationId,
                    android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        notification.deepLink.toUri(),
                    ),
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        return runCatching {
            NotificationManagerCompat.from(context).notify(notification.notificationId, builder.build())
            true
        }.getOrDefault(false)
    }

    private companion object {
        const val CHANNEL_ID = "produce-price-alerts"
    }
}
