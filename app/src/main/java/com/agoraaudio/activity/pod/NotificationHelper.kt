package com.agoraaudio.activity.pod

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.agoraaudio.R

private const val CHANNEL_ID = "Channel"
private const val CHANNEL_NAME = "channelName"
private const val CHANNEL_DESCRIPTION = "channelDescription"

class NotificationHelper(private val context: Context) {

  private val notificationBuilder: NotificationCompat.Builder by lazy {
    NotificationCompat.Builder(context, CHANNEL_ID)
      .setContentTitle(context.getString(R.string.app_name))
      .setSound(null)
      .setContentIntent(contentIntent)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)
  }

  private val notificationManager by lazy {
    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  }

  private val contentIntent by lazy {
    PendingIntent.getActivity(
      context,
      0,
      Intent(context, PodsActivity::class.java),
      PendingIntent.FLAG_UPDATE_CURRENT
    )
  }

  fun getNotification(): Notification {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      notificationManager.createNotificationChannel(createChannel())
    }

    return notificationBuilder.build()
  }

  fun updateNotification(notificationText: String? = null) {
    notificationText?.let { notificationBuilder.setContentText(it) }
    notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
  }

  @RequiresApi(Build.VERSION_CODES.O)
  private fun createChannel() =
    NotificationChannel(
      CHANNEL_ID,
      CHANNEL_NAME,
      NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
      description = CHANNEL_DESCRIPTION
      setSound(null, null)
    }

  companion object {
    const val NOTIFICATION_ID = 99
  }
}