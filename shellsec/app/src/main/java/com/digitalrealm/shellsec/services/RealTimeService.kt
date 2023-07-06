/*
 * LibreAV - Anti-malware for Android using machine learning
 * Copyright (C) 2020 Project Matris
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.digitalrealm.shellsec.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.*
import android.app.Service
import android.content.*
import android.os.Build
import android.os.IBinder
import android.preference.PreferenceManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.digitalrealm.shellsec.MainActivity
import com.digitalrealm.shellsec.R
import com.digitalrealm.shellsec.scanners.AppScanner


class RealTimeService : Service() {
    private var receiver: BroadcastReceiver? = null

    override fun onCreate() {
        val intentFilter = IntentFilter(Intent.ACTION_PACKAGE_ADDED)
        intentFilter.addDataScheme("package")
        val notificationManager = NotificationManagerCompat.from(this)
        val CHANNEL_ID = "channel_100"
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val CHANNEL_NAME = "PROJECT MATRIS"
            val mChannel =
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(mChannel)
            notificationBuilder.setSmallIcon(R.drawable.ic_notification)
                .setOngoing(true)
                .setContentTitle("Shell security")
                .setContentText("Shell is defending you")
//            applicationContext.getString(R.string.real_time_scan_on)
            val intent = Intent(this, MainActivity::class.java)
            val contentIntent = PendingIntent.getActivity(
                this, 0, intent, FLAG_UPDATE_CURRENT or
                        FLAG_IMMUTABLE
            )
            notificationBuilder.setContentIntent(contentIntent)
            val notification = notificationBuilder.build()
            val NOTIFICATION_ID = 200
            startForeground(NOTIFICATION_ID, notification)
        }
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
                if (sharedPreferences.getBoolean("realTime", true)) {
                    val packageName: String
                    if (intent.action != null) {
                        if (intent.action == "android.intent.action.PACKAGE_ADDED" && intent.dataString != null) {
                            packageName = intent.dataString!!.replace("package:", "")
                            val scanner = AppScanner(context, packageName, "realtime_scan")
                            scanner.execute()
                        }
                    }
                }
            }
        }
        registerReceiver(receiver, intentFilter)
    }

//    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
//        startForeground()
//        return START_STICKY
//    }

//    override fun startForegroundService(service: Intent?): ComponentName? {
//        val notificationIntent = Intent(this, MainActivity::class.java)
//        val pendingIntent = PendingIntent.getActivity(
//            this, 0,
//            notificationIntent, FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT
//        )
//        startForeground(
//            500, NotificationCompat.Builder(
//                this,
//                CHANNEL_1_ID
//            ) // don't forget create a notification channel first
//                .setOngoing(true)
//                .setSmallIcon(R.drawable.ic_notification)
//                .setContentTitle(getString(R.string.app_name))
//                .setContentText("You are in the shell")
//                .setContentIntent(pendingIntent)
//                .build()
//        )
//        return super.startService(service)
//    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        stopForeground(true)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}