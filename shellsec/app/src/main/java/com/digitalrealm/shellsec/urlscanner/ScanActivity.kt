package com.digitalrealm.shellsec.urlscanner

import android.content.ComponentName
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.digitalrealm.shellsec.R
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.pixplicity.easyprefs.library.Prefs
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ScanActivity : AppCompatActivity() {
    private var urlToCheck: String? = null
    private var retrofit: Retrofit? = null
    private var myAPI: ScanURLAPI? = null
    private var progressDialog: MaterialDialog? = null
    private var browserIntent: Intent? = null
    private var browserCompName: ComponentName? = null
    private var virusTotalUrl = ""
    private var blockWithoutDiag = false
    private var ignoreNotInDbWarning = false
    private var whitelistArray: Set<String>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_url)
        urlToCheck = intent.dataString
        browserIntent = Intent()
        Prefs.Builder()
            .setContext(this)
            .setMode(ContextWrapper.MODE_PRIVATE)
            .setPrefsName(packageName)
            .setUseDefaultSharedPreference(true)
            .build()
        val packageManager: PackageManager = this.packageManager
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("http://www.google.com")
        val list: List<ResolveInfo> = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.GET_META_DATA
            )
        } else {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.MATCH_ALL
            )
        }
        whitelistArray = Prefs.getOrderedStringSet("whitelistArray", emptySet<String>())
        for (info in list) {
            if (info.activityInfo.applicationInfo.loadLabel(packageManager)
                    .toString() == Prefs.getString("browserName", "Chrome")
            ) {
                browserCompName = ComponentName(info.activityInfo.packageName, info.activityInfo.name)
                browserIntent!!.component = browserCompName
            }
        }
        for (url in whitelistArray!!) {
            if (urlToCheck!!.contains(url)) {
                launchURLInBrowser()
            }
        }
        if (Prefs.getBoolean("suppressWarnings", false)) {
            launchURLInBrowser()
        }
        if (Prefs.getBoolean("blockWithoutDialog", false)) {
            blockWithoutDiag = true
        }
        if (Prefs.getBoolean("blockWithDialog", false)) {
            blockWithoutDiag = false
        }
        if (Prefs.getBoolean("ignoreNotInDbWarn", false)) {
            ignoreNotInDbWarning = true
        }
        if (progressDialog?.isShowing == false)
            showProgressDialog()
        scanIDAndExecuteScan
    }

    fun launchURLInBrowser() {
        try {
            browserIntent!!.action = Intent.ACTION_VIEW
            browserIntent!!.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            browserIntent!!.component = browserCompName
            browserIntent!!.data = Uri.parse(urlToCheck)
            if (browserCompName!!.packageName.contains("shell")){
                val webView = findViewById<WebView>(R.id.webview)
                webView.loadUrl(urlToCheck!!)
            }else{
                startActivity(browserIntent)
                finish()
            }
        }catch (ex:NullPointerException){Log.e("ScanNull",ex.message.toString())}


    }

    private val scanIDAndExecuteScan: Unit
        get() {
            val gson: Gson = GsonBuilder()
                .setLenient()
                .create()
            retrofit = Retrofit.Builder()
                .baseUrl(ScanURLAPI.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
            myAPI = retrofit!!.create(ScanURLAPI::class.java)
            val scanID = myAPI!!.getScanResultID(ScanRequestBody(urlToCheck!!))
            scanID!!.enqueue(object : Callback<String?> {
                override fun onResponse(call: Call<String?>, response: Response<String?>) {
                    if (response.isSuccessful) {
                        Log.i("TAG", response.body()!!)
                        performScan(response.body())
                    } else {
                        hideProgressDialog()
                        showScanUnavailableDialog()
                    }
                }

                override fun onFailure(call: Call<String?>, t: Throwable) {
                    t.printStackTrace()
                    Log.i("TAG", "Failed to get scan ID")
                    hideProgressDialog()
                    showScanUnavailableDialog()
                }
            })
        }

    fun performScan(id: String?) {
        val scanResults = myAPI!!.getScanResults(id)
        scanResults!!.enqueue(object : Callback<Any?> {
            override fun onResponse(call: Call<Any?>, response: Response<Any?>) {
                if (response.isSuccessful) {
                    hideProgressDialog()
                    if (response.body() is ArrayList<*>) {
                        val ls = response.body() as ArrayList<*>?
                        Log.i("TAG", response.body().toString())
                        if (ls!![2] as Double > 0.0) {
                            virusTotalUrl = ls[1] as String
                            if (!blockWithoutDiag) {
                                showWarningDialog()
                            } else {
                                Toast.makeText(
                                    this@ScanActivity,
                                    getString(R.string.no_dialog_warning_toast),
                                    Toast.LENGTH_SHORT
                                ).show()
                                Handler().postDelayed({ finish() }, Toast.LENGTH_SHORT.toLong())
                            }
                        } else {
                            Toast.makeText(
                                this@ScanActivity,
                                getString(R.string.safe),
                                Toast.LENGTH_SHORT
                            ).show()
                            launchURLInBrowser()
                        }
                    } else if (response.body() is String) {
                        if (!ignoreNotInDbWarning) {
                            showURLBeingAnalyzedDialog()
                        } else {
                            launchURLInBrowser()
                        }
                    } else {
                        launchURLInBrowser()
                    }
                } else {
                    hideProgressDialog()
                    showScanUnavailableDialog()
                }
            }

            override fun onFailure(call: Call<Any?>, t: Throwable) {
                t.printStackTrace()
                Log.i("TAG", "Failed to get scan result")
                hideProgressDialog()
                showScanUnavailableDialog()
            }
        })
    }

    fun showProgressDialog() {
        progressDialog = MaterialDialog(this)
            .title((R.string.progress_dialog_title))
            .message((R.string.progress_dialog_text))
            .noAutoDismiss()
            .cancelable(false)

        progressDialog!!.show()
    }

    fun hideProgressDialog() {
        progressDialog?.dismiss()
    }

    fun showScanUnavailableDialog() {
        val suDialog: MaterialDialog = MaterialDialog(this)
            .title((R.string.scan_unavail_dialog_title))
            .message((R.string.scan_unavail_dialog_text))
            .positiveButton ((R.string.scan_unavail_dialog_positive_button)){
                it.dismiss()
                launchURLInBrowser()}
            .negativeButton((R.string.scan_unavail_dialog_negative_button)){
                it.dismiss()
                finish()
            }
            .cancelable(false)
            .noAutoDismiss()

        suDialog.show()
    }

    fun showURLBeingAnalyzedDialog() {
        val urlAnalyzedDialog: MaterialDialog = MaterialDialog(this)
            .title((R.string.url_not_in_db_dialog_title))
            .message((R.string.url_not_in_db_dialog_text))
            .positiveButton((R.string.url_not_in_db_dialog_positive_button)){                it.dismiss()
                launchURLInBrowser()}
            .negativeButton((R.string.url_not_in_db_dialog_negative_button)){                it.dismiss()
                finish()}
            .neutralButton(R.string.url_not_in_db_neutral_text){
                Prefs.putBoolean("ignoreNotInDbWarn", true)
                it.dismiss()
                launchURLInBrowser()
            }
            .noAutoDismiss()
            .cancelable(false)
        urlAnalyzedDialog.show()
    }

    override fun onDestroy() {
        hideProgressDialog()
        super.onDestroy()
    }

    fun showWarningDialog() {
        try{
            val wDialog: MaterialDialog = MaterialDialog(this)
                .title((R.string.danger_dialog_title))
                .message((R.string.danger_dialog_text))
                .positiveButton((R.string.danger_dialog_positive_text)){
                    it.dismiss()
                    finish()
                }
                .cancelable(false)
                .noAutoDismiss()
                .negativeButton((R.string.danger_dialog_negative_button)){
                    it.dismiss()
                    launchURLInBrowser()
                }
                .neutralButton((R.string.danger_dialog_neutral_button)){

                    it.dismiss()
                    browserIntent!!.setAction(Intent.ACTION_VIEW)
                    browserIntent!!.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    browserIntent!!.setComponent(browserCompName)
                    browserIntent!!.setData(Uri.parse(virusTotalUrl))
                    startActivity(browserIntent)
                    finish()
                }


            wDialog.show()
        }catch (ex:Exception){Log.e("scan error",ex.message.toString())}

    }
}