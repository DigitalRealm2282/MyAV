package com.digitalrealm.shellsec

import android.app.ProgressDialog
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Bundle
import android.os.PersistableBundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.digitalrealm.shellsec.adapters.AppsListAdapter
import com.digitalrealm.shellsec.databinding.ActivityResultBinding
import java.util.*

class CustomScanActivity : AppCompatActivity() {
    var recyclerView: RecyclerView? = null
    var layoutManager: RecyclerView.LayoutManager? = null
    var withSysApps = false
    var pd: ProgressDialog? = null
    private lateinit var binding : ActivityResultBinding
    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityResultBinding.inflate(layoutInflater)
        withSysApps = intent.getBooleanExtra("withSysApps", false)
        val actionBar: ActionBar? = this.supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.title = this.getString(R.string.custom_scan)
        }
        setContentView(binding.root)
        layoutManager = LinearLayoutManager(this)
        binding.resultList.layoutManager = layoutManager
        LoadApplications().execute()
    }

    protected override fun onStart() {
        super.onStart()
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
    }

    override fun onRestoreInstanceState(
        savedInstanceState: Bundle?,
        persistentState: PersistableBundle?
    ) {
        super.onRestoreInstanceState(savedInstanceState, persistentState)
    }

    protected override fun onResume() {
        super.onResume()
    }

    protected override fun onPause() {
        super.onPause()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        NavUtils.navigateUpFromSameTask(this)
    }

    internal open inner class LoadApplications : AsyncTask<Void?, Void?, Void?>() {
        protected override fun onPreExecute() {
            pd = ProgressDialog(this@CustomScanActivity)
            pd!!.setMessage("Loading applications...")
            pd!!.show()
            super.onPreExecute()
        }

        protected override fun doInBackground(vararg params: Void?): Void? {
            val appList: MutableList<ApplicationInfo> = packageManager!!.getInstalledApplications(PackageManager.GET_META_DATA)

            apps = ArrayList<ApplicationInfo>()

            if (withSysApps) {

                apps = appList

            } else {
                for (appInfo in appList) {
                    if (appInfo != null && appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
                        if (appInfo.processName != "com.digitalrealm.shellsec")
                            apps!!.add(appInfo)
                    }
                }
            }
            Collections.sort<ApplicationInfo>(
                apps,
                ApplicationInfo.DisplayNameComparator(packageManager)
            )
            return null
        }

        protected override fun onPostExecute(aVoid: Void?) {
            if (pd != null && pd!!.isShowing()) {
                pd!!.dismiss()
            }
            val appsListAdapter = AppsListAdapter(this@CustomScanActivity, apps)
            binding.resultList.adapter = appsListAdapter
            super.onPostExecute(aVoid)
        }
    }

    companion object {
        var apps: MutableList<ApplicationInfo>? = null
    }
}