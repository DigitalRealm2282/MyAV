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
package com.digitalrealm.shellsec.scanners

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import android.content.res.XmlResourceParser
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import com.digitalrealm.shellsec.AppDetails
import org.tensorflow.lite.Interpreter
import com.digitalrealm.shellsec.R
import com.digitalrealm.shellsec.utils.AppConstants
import com.digitalrealm.shellsec.utils.Sha256HashExtractor
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.FileInputStream
import java.io.IOException
import java.lang.ref.WeakReference
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*

open class AppScanner(context: Context, pkgName: String, scan_mode: String) :
    AsyncTask<Void?, String?, Void?>() {
    private val contextRef: WeakReference<Context>
    var pd: ProgressDialog? = null
    private var tflite: Interpreter? = null
    private var p_jArray: JSONArray? = null
    private var i_jArray: JSONArray? = null
    private val packageName: String
    private var appName: String? = null
    private var prediction: String? = null
    private val scan_mode: String
    private var skipScan = false
    private var predictionScore = 0f
    private val withSysApps: Boolean
    private var appPermissionsList = ArrayList<String>()
    private var sha256Hash: String? = null

    init {
        contextRef = WeakReference(context)
        packageName = pkgName
        withSysApps = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean("includeSystemApps", false)
        this.scan_mode = scan_mode
    }

    protected override fun onPreExecute() {
        if (scan_mode.equals("custom_scan", ignoreCase = true)) {
            pd = ProgressDialog(contextRef.get())
            pd!!.setMessage("Scanning...")
            pd!!.show()
        }
        super.onPreExecute()
    }

    @Deprecated("Deprecated in Java")
    override fun doInBackground(vararg params: Void?): Void? {
        val inputVal = FloatArray(2000)
        try {
            tflite = Interpreter(loadModelFile(), null)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        try {
            // Loading the features.json from assets folder.
            val obj = JSONObject(loadJSONFromAsset())
            p_jArray =
                obj.getJSONArray("permissions") // This array stores permissions from features.json file
            i_jArray =
                obj.getJSONArray("intents") // This array  stores intents from features.json file
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        val pm: PackageManager = contextRef.get()!!.packageManager
        try {
            if (pm.getPackageInfo(
                    packageName,
                    0
                ).applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 1 && !withSysApps
            ) {
                skipScan = true
                return null
            } else {
                var flag =
                    false // flag is true if and only if the app under scan contains at least one permission or intent-filter defined in features.json
                appName = pm.getPackageInfo(packageName, 0).applicationInfo.loadLabel(pm).toString()
                appPermissionsList = getListOfPermissions(
                    contextRef.get()!!.createPackageContext(packageName, 0)
                )
                val appIntentsList = getListOfIntents(
                    contextRef.get()!!.createPackageContext(packageName, 0)
                )
                var str: String
                if (appPermissionsList.size == 0 && appIntentsList.size == 0) {
                    prediction = contextRef.get()!!.getString(R.string.unknown)
                    return null
                }
                for (i in 0 until p_jArray!!.length()) {
                    str = p_jArray!!.optString(i)
                    if (appPermissionsList.contains(str)) {
                        inputVal[i] = 1f
                        flag = true
                    } else {
                        inputVal[i] = 0f
                    }
                }
                for (i in 0 until i_jArray!!.length()) {
                    str = i_jArray!!.optString(i)
                    if (appIntentsList.contains(str)) {
                        inputVal[i + 489] = 1f
                        flag = true
                    } else {
                        inputVal[i + 489] = 0f
                    }
                }
                if (!flag) {
                    prediction = contextRef.get()!!.getString(R.string.unknown)
                    return null
                }
                val hash: String? = Sha256HashExtractor.getSha256Hash(
                    pm.getPackageInfo(
                        packageName,
                        0
                    ).applicationInfo.publicSourceDir
                )
                if (hash != null) {
                    sha256Hash = hash
                }
                val outputVal = Array(1) { FloatArray(1) }

                // Run the model
                tflite!!.run(inputVal, outputVal)
                val inferredValue = outputVal[0][0]
                predictionScore = inferredValue
                prediction = if (inferredValue > 0.75) {
                    contextRef.get()!!.getString(R.string.malware)
                } else if (inferredValue > 0.5) {
                    contextRef.get()!!.getString(R.string.risky)
                } else {
                    contextRef.get()!!.getString(R.string.safe)
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return null
    }

    @SuppressLint("MissingPermission")
    override fun onPostExecute(result: Void?) {
        if (!skipScan) {
            val intent = Intent(contextRef.get(), AppDetails::class.java)
            intent.putExtra("appName", appName)
            intent.putExtra("packageName", packageName)
            intent.putExtra("result", prediction)
            intent.putExtra("prediction", predictionScore)
            intent.putExtra("scan_mode", scan_mode)
            intent.putStringArrayListExtra("permissionList", appPermissionsList)
            intent.putExtra(AppConstants.SHA_256_HASH, sha256Hash)
            if (scan_mode.equals("custom_scan", ignoreCase = true)) {
                if (pd != null && pd!!.isShowing) {
                    pd!!.dismiss()
                }
                contextRef.get()!!.startActivity(intent)
            } else if (scan_mode.equals("realtime_scan", ignoreCase = true)) {
                val notificationManager: NotificationManagerCompat =
                    NotificationManagerCompat.from(contextRef.get()!!)
                val CHANNEL_ID = "channel_100"
                val notificationBuilder: NotificationCompat.Builder =
                    NotificationCompat.Builder(contextRef.get()!!, CHANNEL_ID)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val CHANNEL_NAME = "PROJECT MATRIS"
                    val mChannel = NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                    )
                    notificationManager.createNotificationChannel(mChannel)
                }
                var NOTIFICATION_ID = 100
                val contentIntent: PendingIntent = PendingIntent.getActivity(
                    contextRef.get(), 1, intent, PendingIntent.FLAG_CANCEL_CURRENT or
                            PendingIntent.FLAG_IMMUTABLE
                )
                if (prediction.equals(
                        contextRef.get()!!.getString(R.string.safe),
                        ignoreCase = true
                    )
                ) {
                    notificationBuilder.setSmallIcon(R.drawable.ic_notification)
                        .setAutoCancel(true)
                        .setContentTitle("Shell security")
                        .setContentIntent(contentIntent)
                        .setContentText(
                            appName + contextRef.get()!!.getString(R.string.`is`) + prediction
                        )
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                    notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
                } else {
                    NOTIFICATION_ID = Date().time.toInt()
                    val uninstallIntent = Intent(Intent.ACTION_DELETE)
                    uninstallIntent.setData(Uri.parse("package:$packageName"))
                    val uninstallPendingIntent: PendingIntent = PendingIntent.getActivity(
                        contextRef.get(),
                        NOTIFICATION_ID + 1,
                        uninstallIntent,
                        PendingIntent.FLAG_ONE_SHOT
                    )
                    val contentIntent1: PendingIntent = PendingIntent.getActivity(
                        contextRef.get(),
                        NOTIFICATION_ID + 2,
                        intent,
                        PendingIntent.FLAG_CANCEL_CURRENT
                    )
                    notificationBuilder.setSmallIcon(R.drawable.ic_notification)
                        .setAutoCancel(true)
                        .setVibrate(longArrayOf(250, 250, 250, 250))
                        .setContentTitle("Shell security")
                        .setContentText(
                            appName + contextRef.get()!!.getString(R.string.`is`) + prediction
                        )
                        .setContentIntent(contentIntent1)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .addAction(
                            R.drawable.ic_delete_notification,
                            contextRef.get()!!.getString(R.string.uninstall).uppercase(
                                Locale.getDefault()
                            ),
                            uninstallPendingIntent
                        )
                    notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor: AssetFileDescriptor =
            contextRef.get()!!.assets.openFd("saved_model.tflite")
        val inputStream: FileInputStream = FileInputStream(fileDescriptor.getFileDescriptor())
        val fileChannel = inputStream.channel
        val startOffset: Long = fileDescriptor.getStartOffset()
        val declaredLength: Long = fileDescriptor.getDeclaredLength()
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Load the JSON file from assets folder
     *
     * @return String containing contents of JSON file
     *
     *
     * Borrowed from: https://stackoverflow.com/questions/19945411 (GrIsHu)
     */
    private fun loadJSONFromAsset(): String? {
        val json: String
        json = try {
            val `is` = contextRef.get()!!.assets.open("features.json")
            val size = `is`.available()
            val buffer = ByteArray(size)
            `is`.read(buffer)
            `is`.close()
            String(buffer, Charsets.UTF_8)
        } catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }
        return json
    }

    companion object {
        /**
         * Get the list of permissions used by the application
         *
         *
         * Borrowed from: https://stackoverflow.com/questions/18236801 (Yousha Aleayoub)
         */
        private fun getListOfPermissions(context: Context): ArrayList<String> {
            val arr = ArrayList<String>()
            try {
                val am: AssetManager = context.createPackageContext(context.packageName, 0).assets
                val addAssetPath: Method =
                    am.javaClass.getMethod("addAssetPath", String::class.java)
                val cookie = addAssetPath.invoke(
                    am,
                    context.packageManager.getApplicationInfo(context.packageName, 0).sourceDir
                ) as Int
                val xmlParser: XmlResourceParser =
                    am.openXmlResourceParser(cookie, "AndroidManifest.xml")
                var eventType: Int = xmlParser.next()
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && "uses-permission" == xmlParser.getName()) {
                        for (i in 0 until xmlParser.getAttributeCount()) {
                            if (xmlParser.getAttributeName(i) == "name") {
                                arr.add(xmlParser.getAttributeValue(i))
                            }
                        }
                    }
                    eventType = xmlParser.next()
                }
                xmlParser.close()
            } catch (exception: XmlPullParserException) {
                exception.printStackTrace()
            } catch (exception: PackageManager.NameNotFoundException) {
                exception.printStackTrace()
            } catch (exception: IOException) {
                exception.printStackTrace()
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
            }
            return arr
        }

        private fun getListOfIntents(context: Context): ArrayList<String> {
            val arr = ArrayList<String>()
            try {
                val am: AssetManager = context.createPackageContext(context.packageName, 0).assets
                val addAssetPath: Method =
                    am.javaClass.getMethod("addAssetPath", String::class.java)
                val cookie = addAssetPath.invoke(
                    am,
                    context.packageManager.getApplicationInfo(context.packageName, 0).sourceDir
                ) as Int
                val xmlParser: XmlResourceParser =
                    am.openXmlResourceParser(cookie, "AndroidManifest.xml")
                var eventType: Int = xmlParser.next()
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && "action" == xmlParser.getName()) {
                        for (i in 0 until xmlParser.getAttributeCount()) {
                            if (xmlParser.getAttributeName(i) == "name") {
                                arr.add(xmlParser.getAttributeValue(i))
                            }
                        }
                    }
                    eventType = xmlParser.next()
                }
                xmlParser.close()
            } catch (exception: XmlPullParserException) {
                exception.printStackTrace()
            } catch (exception: PackageManager.NameNotFoundException) {
                exception.printStackTrace()
            } catch (exception: IOException) {
                exception.printStackTrace()
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
            }
            return arr
        }
    }
}