package com.digitalrealm.shellsec.services

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast


class DeviceAdminSampleReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "Sample Device Admin has been enabled.", Toast.LENGTH_SHORT).show();

        super.onEnabled(context, intent)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "Sample Device Admin has been disabled.", Toast.LENGTH_SHORT).show();

        super.onDisabled(context, intent)
    }
}