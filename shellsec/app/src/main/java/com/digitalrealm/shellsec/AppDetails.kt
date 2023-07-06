package com.digitalrealm.shellsec

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.*
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.digitalrealm.shellsec.adapters.ScanResultAdapter
import com.digitalrealm.shellsec.databinding.ActivityAppDetailsBinding
import com.digitalrealm.shellsec.helper.ThemeToggleHelper.toggleDarkMode
import com.digitalrealm.shellsec.utils.AppConstants
import java.text.DecimalFormat

class AppDetails : AppCompatActivity() {
    var result: String? = null
    var scan_mode: String? = null
    var appIcon: ImageView? = null
    var appName: TextView? = null
    var resultType: TextView? = null
    var permissionListText: TextView? = null
    var pName: TextView? = null
    var prediction: TextView? = null
    var uninstall: Button? = null
    var arrayList: ArrayList<String>? = null
    var adapter: ScanResultAdapter? = null
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var binding: ActivityAppDetailsBinding
    protected override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityAppDetailsBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        toggleDarkMode(sharedPreferences.getBoolean("darkMode", true))
        val actionBar: ActionBar? = this.supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.title = this.getString(R.string.app_details)
        }
        permissionListText = findViewById<TextView>(R.id.permissions_list_text)
        pName = findViewById<TextView>(R.id.app_package)
        prediction = findViewById<TextView>(R.id.app_prediction_score)
        appIcon = findViewById<ImageView>(R.id.app_icon)
        appName = findViewById<TextView>(R.id.app_name)
        resultType = findViewById<TextView>(R.id.app_prediction_label)
        binding.permissionList.layoutManager = LinearLayoutManager(this)
        uninstall = findViewById<Button>(R.id.uninstallButton)
        val intent: Intent = intent
        arrayList = intent.getStringArrayListExtra("permissionList")
        if (arrayList == null || arrayList!!.isEmpty()) {
            binding.permissionsListText.text = this.getString(R.string.no_permissions_required)
        } else {
            adapter = ScanResultAdapter(this, arrayList as java.util.ArrayList<String>)
            binding.permissionList.adapter = adapter
        }
        binding.appName.text = intent.getExtras()?.getString("appName") ?:""
        result = intent.getExtras()?.getString("result")
        binding.appPredictionLabel.setText(result)
        scan_mode = intent.getExtras()?.getString("scan_mode")
        if (result == this.getString(R.string.malware)) {
            binding.appPredictionLabel.setTextColor(Color.parseColor("#FF0000"))
        } else if (result == this.getString(R.string.safe)) {
            binding.appPredictionLabel.setTextColor(Color.parseColor("#008000"))
        } else if (result == this.getString(R.string.risky)) {
            binding.appPredictionLabel.setTextColor(Color.parseColor("#FFA500"))
        } else {
            binding.appPredictionLabel.setTextColor(Color.parseColor("#0080FF"))
        }
        if (scan_mode.equals("normal_scan", ignoreCase = true)) {
            val sha256Label: TextView = findViewById<TextView>(R.id.sha256_label)
            val sha256Container: HorizontalScrollView =
                findViewById<HorizontalScrollView>(R.id.sha256_container)
            sha256Label.setVisibility(View.INVISIBLE)
            sha256Container.setVisibility(View.INVISIBLE)
        } else {
            val sha256Hash: String? = intent.extras!!.getString(AppConstants.SHA_256_HASH)
            val sha256HashTv: TextView = findViewById<TextView>(R.id.sha256_text)
            sha256HashTv.text = sha256Hash
        }
        if (scan_mode.equals("realtime_scan", ignoreCase = true) || scan_mode.equals(
                "normal_scan",
                ignoreCase = true
            ) || scan_mode.equals("custom_scan", ignoreCase = true)
        ) {
            uninstall!!.visibility = View.VISIBLE
            uninstall!!.setOnClickListener {
                val intent = Intent(Intent.ACTION_DELETE)
                intent.setData(Uri.parse("package:$packageName"))
                startActivityForResult(intent, 1)
            }
        } else {
            uninstall!!.visibility = View.INVISIBLE
        }
        val packageName = intent.extras!!.getString("packageName")
        binding.appPackage.text = packageName
        val df = DecimalFormat("#")
        df.maximumFractionDigits = 6
        binding.appPredictionScore.text = this.getString(R.string.prediction_score) + " " + df.format(
            intent.extras!!.getFloat("prediction").toDouble()
        )
        val pm: PackageManager = this@AppDetails.packageManager
        try {
            appIcon!!.setImageDrawable(
                pm.getPackageInfo(
                    packageName.toString(),
                    0
                ).applicationInfo.loadIcon(pm)
            )
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    private fun isPackageInstalled(packageName: String?, packageManager: PackageManager): Boolean {
        return try {
            if (packageName != null) {
                packageManager.getPackageInfo(packageName, 0)
            }
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    protected override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val pm: PackageManager = this.getPackageManager()
        if (requestCode == 1 && !isPackageInstalled(packageName, pm)) {
            Toast.makeText(
                this@AppDetails,
                this.getString(R.string.uninstall_successful),
                Toast.LENGTH_LONG
            ).show()
            startActivity(Intent(this@AppDetails, MainActivity::class.java))
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        super.onBackPressed()
        //        startActivity(new Intent(this,MainAct2.class));
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
        //startActivity(new Intent(this,MainAct2.class));
    }
}