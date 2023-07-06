package com.digitalrealm.shellsec.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.widget.Toast
import com.digitalrealm.shellsec.services.appService

class CameraEventReciever : BroadcastReceiver() {
    private var sharedPreferences:SharedPreferences ?= null
    override fun onReceive(context: Context, intent: Intent) {
        setupSharedPreferences(context)

        val cam =sharedPreferences!!.getBoolean("camnot",true)
        if (!cam) {
            Toast.makeText(context,"cam option disabled",Toast.LENGTH_SHORT).show()
        } else  {
            Toast.makeText(context,"cam option enabled",Toast.LENGTH_SHORT).show()
            val not = Intent(context, appService::class.java)
            not.action = "camnotification"
            context.startService(not)
        }
    }

    private fun setupSharedPreferences(context: Context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    }
}