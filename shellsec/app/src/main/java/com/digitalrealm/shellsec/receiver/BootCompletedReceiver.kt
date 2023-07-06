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
package com.digitalrealm.shellsec.receiver

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.preference.PreferenceManager
import com.digitalrealm.shellsec.services.RealTimeService

class BootCompletedReceiver : BroadcastReceiver() {
    var sharedPreferences: SharedPreferences? = null
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.getAction() != null) {
            if (intent.getAction() == "android.intent.action.BOOT_COMPLETED" || intent.getAction() == "android.intent.action.LOCKED_BOOT_COMPLETED") {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (!isServiceRunning(RealTimeService::class.java, context)) {
                        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
                        if (sharedPreferences!!.getBoolean("realTime", true)) {
                            context.startForegroundService(
                                Intent(
                                    context,
                                    RealTimeService::class.java
                                )
                            )
                        }

                        if (sharedPreferences!!.getBoolean("camnot",true)){
                            context.startForegroundService(Intent(context,CameraEventReciever::class.java))
                        }
                    }
                }
            }
        }
    }

    private fun isServiceRunning(serviceClass: Class<*>, context: Context): Boolean {
        val manager: ActivityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}