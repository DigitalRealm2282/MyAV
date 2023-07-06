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
package com.digitalrealm.shellsec.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import java.util.*

class AppInfo : Parcelable {
    var appName: String?
    var packageName: String?
    private var filePath: String?
    var prediction: String? = null
    var appIcon: Drawable? = null
    var isSystemApp = 0
    var predictionScore = 0f
    var permissionList: ArrayList<String>? = null
    var context: Context? = null
    var sha256Hash: String? = null

    constructor(source: Parcel) {
        appName = source.readString()
        packageName = source.readString()
        filePath = source.readString()
        prediction = source.readString()
        isSystemApp = source.readInt()
        predictionScore = source.readFloat()
        permissionList = source.readArrayList(String::class.java.classLoader) as ArrayList<String>?
        sha256Hash = source.readString()
    }

    constructor(appName: String?, packName: String?, sourceDir: String?, isSystemApp: Int) {
        this.appName = appName
        packageName = packName
        filePath = sourceDir
        this.isSystemApp = isSystemApp
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(appName)
        dest.writeString(packageName)
        dest.writeString(filePath)
        dest.writeString(prediction)
        dest.writeInt(isSystemApp)
        dest.writeFloat(predictionScore)
        dest.writeList(permissionList)
        dest.writeString(sha256Hash)
    }

    fun loadIcon(context: Context): Drawable? {
        return try {
            context.packageManager.getPackageArchiveInfo(filePath!!, 0)!!.applicationInfo.loadIcon(
                context.packageManager
            )
        } catch (e: NullPointerException) {
            e.printStackTrace()
            null
        }
    }

    companion object {
        var appNameComparator = Comparator<AppInfo> { o1, o2 ->
            val appName1 = o1.appName!!.lowercase(Locale.getDefault())
            val appName2 = o2.appName!!.lowercase(Locale.getDefault())
            appName1.compareTo(appName2)
        }
        @JvmField
        val CREATOR: Creator<AppInfo> = object : Creator<AppInfo> {
            fun getAppInfo(context: Context, app: ApplicationInfo): AppInfo {
                return AppInfo(
                    app.loadLabel(context.packageManager).toString(),
                    app.packageName,
                    app.publicSourceDir,
                    app.flags and ApplicationInfo.FLAG_SYSTEM
                )
            }

            override fun createFromParcel(source: Parcel): AppInfo {
                return AppInfo(source)
            }

            override fun newArray(size: Int): Array<AppInfo?> {
                return arrayOfNulls(size)
            }
        }
    }
}