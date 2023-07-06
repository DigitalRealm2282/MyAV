package com.digitalrealm.shellsec.adapters

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.text.SpannableString
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cn.nekocode.badge.BadgeDrawable
import com.digitalrealm.shellsec.R
import com.digitalrealm.shellsec.scanners.AppScanner

class AppsListAdapter(var context: Context, appsList: List<ApplicationInfo>?) :
    RecyclerView.Adapter<RecyclerView.ViewHolder?>() {
    private val apps: List<ApplicationInfo>?
    private val packageManager: PackageManager

    init {
        apps = appsList
        packageManager = context.packageManager
    }

    internal inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var appIcon: ImageView
        var appLabel: TextView
        var packageName: TextView
        var uninstallButton: ImageView

        init {
            appIcon = itemView.findViewById(R.id.itemIcon)
            appLabel = itemView.findViewById<TextView>(R.id.itemLabel)
            packageName = itemView.findViewById<TextView>(R.id.itemSecondaryLabel)
            uninstallButton = itemView.findViewById(R.id.uninstallButton)
        }

        fun bindAppInfo(applicationInfo: ApplicationInfo) {
            appLabel.setText(applicationInfo.loadLabel(packageManager))
            appIcon.setImageDrawable(applicationInfo.loadIcon(packageManager))
            uninstallButton.visibility = View.INVISIBLE
            if (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 1) {
                val drawable2 = BadgeDrawable.Builder()
                    .type(BadgeDrawable.TYPE_ONLY_ONE_TEXT)
                    .badgeColor(-0xcc9967)
                    .text1(context.applicationContext.getString(R.string.system_app))
                    .build()
                val spannableString = SpannableString(
                    TextUtils.concat(
                        applicationInfo.packageName,
                        "  ",
                        drawable2.toSpannable()
                    )
                )
                packageName.setText(spannableString)
            } else {
                packageName.setText(applicationInfo.packageName)
            }
            itemView.setOnClickListener(View.OnClickListener { v: View? ->
                val scanner = AppScanner(context, applicationInfo.packageName, "custom_scan")
                scanner.execute()
            })
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val itemView: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_result_list_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return apps?.size?:0
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val vh = holder as ViewHolder
        vh.bindAppInfo(apps!![position])
    }


}