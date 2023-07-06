package com.digitalrealm.shellsec

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.crazylegend.security.MagiskDetector
import com.digitalrealm.shellsec.databinding.ActivityMainBinding
import com.digitalrealm.shellsec.helper.ThemeToggleHelper.toggleDarkMode
import com.digitalrealm.shellsec.receiver.CameraEventReciever
import com.digitalrealm.shellsec.services.RealTimeService
import com.digitalrealm.shellsec.services.appService
import com.digitalrealm.shellsec.ui.dashboard.DashboardViewModel
import com.github.angads25.filepicker.view.FilePickerDialog
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var binding: ActivityMainBinding

    private var withSysApps = false
    var sharedPreferences: SharedPreferences? = null
    var dialog: FilePickerDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        setupSharedPreferences()

        toggleDarkMode(sharedPreferences!!.getBoolean("darkMode", true))
        withSysApps = sharedPreferences!!.getBoolean("includeSystemApps", false)

    }

    private fun setupSharedPreferences() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences!!.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == "darkMode" && sharedPreferences.getBoolean(key, true)) {
            toggleDarkMode(sharedPreferences.getBoolean("darkMode", true))
            Toast.makeText(
                applicationContext,
                this.getString(R.string.dark_mode_enabled_toast),
                Toast.LENGTH_LONG
            )
                .show()
        } else if (key == "darkMode" && !sharedPreferences.getBoolean(key, true)) {
            toggleDarkMode(sharedPreferences.getBoolean("darkMode", true))
            Toast.makeText(
                applicationContext,
                this.getString(R.string.dark_mode_disabled_toast),
                Toast.LENGTH_LONG
            )
                .show()
        } else if (key == "realTime" && sharedPreferences.getBoolean(key, true)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startService(Intent(this, RealTimeService::class.java))
            }
        } else if (key == "realTime" && !sharedPreferences.getBoolean(key, true)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                stopService(Intent(this, RealTimeService::class.java))
            }
        }else if (key == "pref_biometric_auth" && sharedPreferences.getBoolean(key,true)){
//            checkDeviceHasBiometric()
        }else if (key =="pref_biometric_auth" && !sharedPreferences.getBoolean(key,true)){
            Log.e("Biometric Not supported","Not supported")
        }
        else if (key == "camnot" && sharedPreferences.getBoolean(key,true)){
            startService(Intent(this, appService::class.java))
            registerReceiver(CameraEventReciever(), IntentFilter("camnotification"))
        }else if (key == "camnot" && !sharedPreferences.getBoolean(key,true)){
            unregisterReceiver(CameraEventReciever())
            stopService(Intent(this, appService::class.java))
        }

        recreate()
    }



    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

//        checkPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE,0)
        if (requestCode == FilePickerDialog.EXTERNAL_READ_PERMISSION_GRANT) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (dialog != null) {   //Show dialog if the read permission has been granted.
                    dialog!!.show()
                }
            } else {
                //Permission has not been granted. Notify the user.
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.permission_denied_message),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2296) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    // perform action when allow permission success
                    if (dialog != null) {   //Show dialog if the read permission has been granted.
                        dialog!!.show()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Allow permission for storage access!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
}