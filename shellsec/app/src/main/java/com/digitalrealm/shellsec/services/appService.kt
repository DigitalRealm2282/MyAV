package com.digitalrealm.shellsec.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.digitalrealm.shellsec.MainActivity
import com.digitalrealm.shellsec.R

class appService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // do your jobs here
        val action = intent!!.action
        try {
            if ("camnotification" == action) {
                mNotification("Camera has been used")
            }
        } catch (e: Exception) {
            Toast.makeText(this, "" + e.message, Toast.LENGTH_SHORT).show()
        }

        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d(javaClass.name, "App just got removed from Recents!")
    }

    fun mNotification(msg: String?) {
        val builder: NotificationCompat.Builder = NotificationCompat.Builder(this)
        builder.setSmallIcon(R.drawable.ic_notification)
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 2282, intent, PendingIntent.FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)
        builder.setContentIntent(pendingIntent)
        builder.setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_background))
        builder.setContentTitle("Shell Anti Spy")
        builder.setContentText(msg)
        builder.setVibrate(longArrayOf(1000, 1000))
        val sound = Uri.parse("android.resource://" + applicationContext.packageName + "/raw/bell")
        builder.setSound(sound)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Will display the notification in the notification bar
        notificationManager.notify(2287, builder.build())
    }
}