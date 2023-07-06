package com.digitalrealm.shellsec.adapters

import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.digitalrealm.shellsec.R

open class ScanResultAdapter(private val context: Context, private val arrayList: ArrayList<String>) :
    RecyclerView.Adapter<ScanResultAdapter.ViewHolder?>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_app_result, parent, false)
        return ViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return if (arrayList.isEmpty()) 0 else arrayList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.name.setText(arrayList[position])
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var name: TextView

        init {
            name = itemView.findViewById<TextView>(R.id.name)
            itemView.setOnClickListener {
                val permissionDialog = PermissionDialog()
                permissionDialog.showDialog(context, arrayList[getAdapterPosition()])
            }
        }
    }
}

internal class PermissionDialog {
    fun showDialog(context: Context, permission: String?) {
        val packageManager: PackageManager = context.packageManager
        val builder = AlertDialog.Builder(context)
        var title: CharSequence? = null
        var desc: CharSequence
        try {
            val permissionInfo: PermissionInfo =
                packageManager.getPermissionInfo(permission!!, PackageManager.GET_META_DATA)
            if (permissionInfo.group != null) {
                title = packageManager.getPermissionGroupInfo(permissionInfo.group!!, 0)
                    .loadLabel(packageManager)
                builder.setIcon(
                    packageManager.getPermissionGroupInfo(permissionInfo.group!!, 0)
                        .loadIcon(packageManager)
                )
            }
            desc = permissionInfo.loadDescription(packageManager).toString()
            if (desc == null) {
                desc = permissionInfo.nonLocalizedDescription.toString()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            title = context.getString(R.string.permission_info)
            desc = context.getString(R.string.no_description_found)
        }
        if (title == null) {
            title = context.getString(R.string.permission_info)
        }
        if (desc == null) {
            desc = context.getString(R.string.no_description_found)
        }
        builder.setMessage(desc)
        builder.setTitle(title)
        builder.create().show()
    }
}