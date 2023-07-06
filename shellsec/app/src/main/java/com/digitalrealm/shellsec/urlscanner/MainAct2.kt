package com.digitalrealm.shellsec.urlscanner

import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.appcompat.widget.AppCompatSpinner
import com.afollestad.materialdialogs.MaterialDialog
import com.digitalrealm.shellsec.R
import com.pixplicity.easyprefs.library.Prefs
import android.R as RBasic

class MainAct2 : AppCompatActivity() {
    private var radioButton1: AppCompatRadioButton? = null
    private var radioButton2: AppCompatRadioButton? = null
    private var radioButton3: AppCompatRadioButton? = null
    private var checkBox1: AppCompatCheckBox? = null
    private var spinner: AppCompatSpinner? = null
    private var button: AppCompatButton? = null
    private var spinnerArray: MutableList<String>? = null
    private var adapter: ArrayAdapter<String>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.act_main2)
        if (supportActionBar != null) {
            supportActionBar!!.elevation = 0.0f
        }
        Prefs.Builder()
            .setContext(this)
            .setMode(ContextWrapper.MODE_PRIVATE)
            .setPrefsName(packageName)
            .setUseDefaultSharedPreference(true)
            .build()
        if (Prefs.getBoolean("firstRun", true)) {
            showFirstRunDialog()
        }
        if (!Prefs.getBoolean("firstRun", true)) {
            if (!checkIfDefault()) {
                showNotDefaultBrowserDialog()
            }
        }
        spinner = findViewById<AppCompatSpinner>(R.id.spinner1)
        radioButton1 = findViewById<AppCompatRadioButton>(R.id.radioButton1)
        radioButton2 = findViewById<AppCompatRadioButton>(R.id.radioButton2)
        radioButton3 = findViewById<AppCompatRadioButton>(R.id.radioButton3)
        checkBox1 = findViewById<AppCompatCheckBox>(R.id.checkBox1)
        button = findViewById<AppCompatButton>(R.id.button2)
        spinnerArray = ArrayList()
        loadPreferences()
        button!!.setOnClickListener(View.OnClickListener {
            savePreferences()
            Toast.makeText(this@MainAct2,R.string.saved,Toast.LENGTH_SHORT).show()
        })
    }

    fun showFirstRunDialog() {
        val frDialog: MaterialDialog = MaterialDialog(this)
            .title((R.string.welcome_app_dialog_title))
            .message((R.string.welcome_app_text))
            .cancelable(false)
            .positiveButton((R.string.welcome_app_dialog_positive_text)){
                it.dismiss()
                Prefs.putBoolean("firstRun", false)
            }
            .cancelOnTouchOutside(false)
            .noAutoDismiss()

        frDialog.show()
    }

    fun showNotDefaultBrowserDialog() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            val notDefDialog: MaterialDialog = MaterialDialog(this)
                .title((R.string.browser_not_default_dialog_title))
                .message((R.string.browser_not_default_dialog_text))
                .cancelable(false)
                .positiveButton((R.string.browser_not_default_dialog_positive_button)){
                    it.dismiss()
                }
                .noAutoDismiss()
                .cancelOnTouchOutside(false)

            notDefDialog.show()
        } else {
            val notDefDialog: MaterialDialog = MaterialDialog(this)
                .title((R.string.browser_not_default_dialog_title))
                .message((R.string.browser_not_default_dialog_text))
                .positiveButton(R.string.browser_not_default_dialog_positive_button){it.dismiss()}
                .negativeButton ((R.string.browser_not_default_dialog_negative_text)){ it.dismiss()
                    startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                }
                .cancelable(false)
                .noAutoDismiss()

            notDefDialog.show()
        }
    }

    fun loadPreferences() {
        radioButton1!!.isChecked = Prefs.getBoolean("blockWithDialog", true)
        radioButton2!!.isChecked = Prefs.getBoolean("blockWithoutDialog", false)
        radioButton3!!.isChecked = Prefs.getBoolean("suppressWarnings", false)
        checkBox1!!.isChecked = Prefs.getBoolean("ignoreNotInDbWarn", false)
        val bwName: String = Prefs.getString("browserName", "Chrome")
        loadInstalledBrowsers()
        if (bwName != "") {
            spinner!!.setSelection(adapter!!.getPosition(bwName))
        }
    }

    fun savePreferences() {
        Prefs.putBoolean("blockWithDialog", radioButton1!!.isChecked)
        Prefs.putBoolean("blockWithoutDialog", radioButton2!!.isChecked)
        Prefs.putBoolean("suppressWarnings", radioButton3!!.isChecked)
        Prefs.putBoolean("ignoreNotInDbWarn", checkBox1!!.isChecked)
        Prefs.putString("browserName", spinner!!.selectedItem.toString())
    }

    fun loadInstalledBrowsers() {
        val packageManager: PackageManager = this.packageManager
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("http://www.google.com")
        val list: List<ResolveInfo> = packageManager.queryIntentActivities(
                intent,
                PackageManager.MATCH_ALL)
        for (info in list) {
            spinnerArray!!.add(
                info.activityInfo.applicationInfo.loadLabel(packageManager).toString()
            )
        }
        adapter = ArrayAdapter<String>(
            this@MainAct2, RBasic.layout.simple_spinner_item, spinnerArray!!
        )
        adapter!!.remove("ScanLinks")
        adapter!!.setDropDownViewResource(RBasic.layout.simple_spinner_dropdown_item)
        spinner!!.adapter = adapter
    }

    fun checkIfDefault(): Boolean {
        val browserIntent = Intent(Intent.ACTION_VIEW)
        browserIntent.data = Uri.parse("http://www.google.com")
        val defaultResolution: ResolveInfo = this.getPackageManager()
            .resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY)!!
        if (defaultResolution != null) {
            val activity: ActivityInfo = defaultResolution.activityInfo
            if (activity.name != "com.android.internal.app.ResolverActivity") {
                if (activity.applicationInfo.packageName == "com.digitalrealm.shellsec") {
                    return true
                }
            }
        }
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            menu.getItem(1).isVisible = false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.about_app -> startActivity(Intent(this, AboutActivity::class.java))
            R.id.show_default_apps -> startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
            R.id.show_setup_instructions -> showFirstRunDialog()
            R.id.show_whitelist -> startActivity(Intent(this, WhitelistActivity::class.java))
        }
        return true
    }
}