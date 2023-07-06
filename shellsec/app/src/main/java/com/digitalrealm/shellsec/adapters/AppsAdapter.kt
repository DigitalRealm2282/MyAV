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
package com.digitalrealm.shellsec.adapters

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.text.SpannableString
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import cn.nekocode.badge.BadgeDrawable
import com.digitalrealm.shellsec.AppDetails
import com.digitalrealm.shellsec.R
import com.digitalrealm.shellsec.data.AppInfo
import com.digitalrealm.shellsec.utils.AppConstants
import java.lang.ref.WeakReference

class AppsAdapter(var context: Context, scannedapps: ArrayList<AppInfo>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder?>() {
    var pos = 0
    private val apps: ArrayList<AppInfo>
    private val contextRef: WeakReference<Context>

    internal inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var appIcon: ImageView
        var appLabel: TextView
        var prediction: TextView
        private var uninstallButton: ImageView

        init {
            appIcon = itemView.findViewById(R.id.itemIcon)
            appLabel = itemView.findViewById<TextView>(R.id.itemLabel)
            prediction = itemView.findViewById<TextView>(R.id.itemSecondaryLabel)
            uninstallButton = itemView.findViewById(R.id.uninstallButton)
        }

        fun bindAppInfo(appInfo: AppInfo) {
            if (appInfo.appIcon == null) {
                appInfo.appIcon = appInfo.loadIcon(contextRef.get()!!)
            }
            appIcon.setImageDrawable(appInfo.appIcon)
            appLabel.text = appInfo.appName
            if (appInfo.prediction.equals(
                    contextRef.get()!!.getString(R.string.malware),
                    ignoreCase = true
                )
            ) {
                prediction.setTextColor(Color.parseColor("#FF0000"))
            } else if (appInfo.prediction.equals(
                    contextRef.get()!!.getString(R.string.safe),
                    ignoreCase = true
                )
            ) {
                prediction.setTextColor(Color.parseColor("#008000"))
            } else if (appInfo.prediction.equals(
                    contextRef.get()!!.getString(R.string.risky),
                    ignoreCase = true
                )
            ) {
                prediction.setTextColor(Color.parseColor("#FFA500"))
            } else {
                prediction.setTextColor(Color.parseColor("#0080FF"))
            }
            if (appInfo.isSystemApp == 1) {
                val drawable2 = BadgeDrawable.Builder()
                    .type(BadgeDrawable.TYPE_ONLY_ONE_TEXT)
                    .badgeColor(-0xcc9967)
                    .text1(contextRef.get()!!.getString(R.string.system_app))
                    .build()
                val spannableString = SpannableString(
                    TextUtils.concat(
                        appInfo.prediction,
                        "  ",
                        drawable2.toSpannable()
                    )
                )
                prediction.text = spannableString
            } else {
                prediction.text = appInfo.prediction
            }
            uninstallButton.setOnClickListener {
                LocalBroadcastManager.getInstance(contextRef.get()!!).registerReceiver(
                    mMessageReceiver,
                    IntentFilter("uninstall")
                )
                val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE)
                intent.setData(Uri.parse("package:" + appInfo.packageName))
                intent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
                 pos = adapterPosition
                (context as Activity).startActivityForResult(intent, 1)
            }
            itemView.setOnClickListener(View.OnClickListener {
                val intent = Intent(contextRef.get(), AppDetails::class.java)
                intent.putExtra("appName", appInfo.appName)
                intent.putExtra("packageName", appInfo.packageName)
                intent.putExtra("result", appInfo.prediction)
                intent.putExtra("prediction", appInfo.predictionScore)
                intent.putExtra("scan_mode", "normal_scan")
                intent.putStringArrayListExtra("permissionList", appInfo.permissionList)
                intent.putExtra(AppConstants.SHA_256_HASH, appInfo.sha256Hash)
                contextRef.get()!!.startActivity(intent)
            })
        }
    }

    init {
        apps = scannedapps
        contextRef = WeakReference(context)
    }
    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            LocalBroadcastManager.getInstance(contextRef.get()!!).unregisterReceiver(this)
            val message: String? = intent.getStringExtra("uninstall")
            if (message == "yes") delete(pos)
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        LocalBroadcastManager.getInstance(parent.context).registerReceiver(
            mMessageReceiver,
            IntentFilter("uninstall")
        )
        val itemView: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_result_list_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return apps.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val vh = holder as ViewHolder
        vh.bindAppInfo(apps[position])
    }

    private fun delete(position: Int) {
        try {
            Log.d("un", position.toString())
            apps.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, apps.size)
            Toast.makeText(
                contextRef.get(),
                contextRef.get()!!.getString(R.string.uninstall_successful),
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: ArrayIndexOutOfBoundsException) {
            e.printStackTrace()
        }
    }
}