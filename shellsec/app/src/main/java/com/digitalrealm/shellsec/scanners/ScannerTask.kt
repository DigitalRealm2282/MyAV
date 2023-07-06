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
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.AsyncTask
import android.os.Build
import android.preference.PreferenceManager
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.digitalrealm.shellsec.R
import com.digitalrealm.shellsec.ResultActivity
import com.digitalrealm.shellsec.data.AppInfo
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.FileInputStream
import java.io.IOException
import java.lang.ref.WeakReference
import java.lang.reflect.InvocationTargetException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.text.DateFormat
import java.util.*

class ScannerTask(context: Context, activity: Activity) : AsyncTask<Void?, String?, Void?>() {
    private val contextRef: WeakReference<Context>
    private val activityRef: WeakReference<Activity>
    private var tflite: Interpreter? = null
    private var p_jArray: JSONArray? = null
    private var i_jArray: JSONArray? = null
    private val goodware = ArrayList<AppInfo>()
    private val malware = ArrayList<AppInfo>()
    private val unknown = ArrayList<AppInfo>()
    private val risky = ArrayList<AppInfo>()
    private val scannedApps = ArrayList<AppInfo>()
    private var pb: WeakReference<ProgressBar>? = null
    private var st1: WeakReference<TextView>? = null
    private var st2: WeakReference<TextView>? = null
    private var pTxt: WeakReference<TextView>? = null
    private var installedAppsCount = 0
    private var status = 0
    private var withSysApps = false
    private var notificationManager: NotificationManagerCompat? = null
    private val NOTIFICATION_ID = 100

    //private long elapsedTime;
    //private long totalTime = 0;
    //private int count = 0;
    //private static final String TAG = "ScannerTask";
    //private static final String TIMER = "Timer";
    init {
        contextRef = WeakReference(context)
        activityRef = WeakReference(activity)
    }

    fun setProgressBar(progressBar: ProgressBar) {
        pb = WeakReference(progressBar)
    }

    fun setPercentText(perTxt: TextView) {
        pTxt = WeakReference(perTxt)
    }

    fun setStatusText(statusText: TextView) {
        st1 = WeakReference(statusText)
    }

    fun setSecondaryStatusText(secondaryStatusText: TextView) {
        st2 = WeakReference(secondaryStatusText)
    }

    fun setWithSysApps(prefValue: Boolean) {
        withSysApps = prefValue
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
            // Loading the features.json from assets folder. Refer loadJSONFromAsset() function for more details
            val obj = JSONObject(loadJSONFromAsset())
            p_jArray =
                obj.getJSONArray("permissions") // This array stores permissions from features.json file
            i_jArray =
                obj.getJSONArray("intents") // This array  stores intents from features.json file
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        val pm = contextRef.get()!!.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (packageInfo in packages) {
            var flag =
                false // flag is true if and only if the app under scan contains at least one permission or intent-filter defined in features.json
            val scanningAppName = packageInfo.loadLabel(pm).toString()
            publishProgress(scanningAppName)
            val app = AppInfo(
                packageInfo.loadLabel(pm).toString(),
                packageInfo.packageName,
                packageInfo.publicSourceDir,
                packageInfo.flags and ApplicationInfo.FLAG_SYSTEM
            )
            app.appIcon = packageInfo.loadIcon(pm)
            if (packageInfo.flags and ApplicationInfo.FLAG_SYSTEM == 1 && !withSysApps) { // checking if it is a system app.
                continue
            } else {
                //Log.d(TAG, "Installed package :" + packageInfo.packageName);
                try {
                    //count++;
                    // Get the list of permissions used by the application.
                    //Log.d(TAG,"extracting permissions...");
                    //long startTime = System.currentTimeMillis();
                    val appPermissionsList = getListOfPermissions(
                        contextRef.get()!!.createPackageContext(packageInfo.packageName, 0)
                    )

                    // Get the list of intents used by the application.
                    //Log.d(TAG,"extracting intents...");
                    val appIntentsList = getListOfIntents(
                        contextRef.get()!!.createPackageContext(packageInfo.packageName, 0)
                    )
                    var str: String
                    //Log.d(scanningAppName,"IntentsList: "+appIntentsList.get(0));
                    //Log.d(scanningAppName,"IntentsList size: "+appIntentsList.size());
                    if (appPermissionsList.size == 0 && appIntentsList.size == 0) {
                        //Log.d(TAG,"No permissions and intents found. Skipping...");
                        app.prediction = contextRef.get()!!.getString(R.string.unknown)
                        unknown.add(app)
                        continue
                    }

                    // The following for loops are used to create the input feature vector
                    for (i in 0 until p_jArray!!.length()) {
                        str = p_jArray!!.optString(i)
                        if (appPermissionsList.contains(str)) {
                            inputVal[i] = 1f
                            flag = true
                            //Log.d(scanningAppName,"Check Permissions: "+ str + " is present in appsPermissionsList.");
                        } else {
                            inputVal[i] = 0f
                            ///Log.d(scanningAppName,"Check Permissions: "+ str + " is NOT present in appsPermissionsList.");
                        }
                    }
                    for (i in 0 until i_jArray!!.length()) {
                        str = i_jArray!!.optString(i)
                        if (appIntentsList.contains(str)) {
                            inputVal[i + 489] = 1f
                            flag = true
                            //Log.d(scanningAppName,"Check Intents:"+ str + " is present in appsIntentsList.");
                        } else {
                            inputVal[i + 489] = 0f
                            //Log.d(scanningAppName,"Check Intents:"+ str + " is NOT present in appsIntentsList.");
                        }
                    }
                    //Log.d("Info:", "feature vector is created.");
                    //Log.d(scanningAppName, scanningAppName+" feature vector:"+ Arrays.toString(inputVal));
                    if (!flag) {
                        app.prediction = contextRef.get()!!.getString(R.string.unknown)
                        unknown.add(app)
                        continue
                    }

                    // To store output from the model
                    val outputVal = Array(1) { FloatArray(1) }

                    // Run the model
                    tflite!!.run(inputVal, outputVal)

                    //long endTime = System.currentTimeMillis();
                    //elapsedTime = endTime - startTime;
                    //totalTime += elapsedTime;
                    //Log.d(TIMER, "Elapsed Time: " + Float.toString(elapsedTime));

                    val malwareList = listOf("com.sledsdffsjkh.Search","com.russian.signato.renewis","com.android.power"
                        ,"com.management.propaganda","com.sec.android.musicplayer","com.houla.quicken"
                        ,"com.attd.da","com.arlo.fappx","com.metasploit.stage","com.vantage.ectronic.cornmuni")

                    val inferredValue = outputVal[0][0]
                    app.predictionScore = inferredValue
                    app.permissionList = appPermissionsList

                    if (inferredValue > 0.75) {
                        if (app.packageName != "com.digitalrealm.shellsec") {
                            app.prediction = contextRef.get()!!.getString(R.string.malware)
                            malware.add(app)
                        }
                    } else if (inferredValue > 0.5) {
                        if (app.packageName != "com.digitalrealm.shellsec") {
                            app.prediction = contextRef.get()!!.getString(R.string.risky)
                            risky.add(app)
                        }
                    } else {
                        if (app.packageName != "com.digitalrealm.shellsec") {
                            app.prediction = contextRef.get()!!.getString(R.string.safe)
                            goodware.add(app)
                        }
                        if (malwareList.contains(app.packageName)) {
                            goodware.remove(app)
                            malware.add(app)
                        }
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                }

            }
            if (isCancelled) {
                break
            }
        }
        return null
    }

    override fun onCancelled() {
        notificationManager!!.cancel(NOTIFICATION_ID)
        super.onCancelled()
    }

    override fun onProgressUpdate(vararg values: String?) {
        st1!!.get()!!.text = values[0]
        status += 1
        val percentCompleted = (status.toFloat() / installedAppsCount * 100f).toInt()
        pTxt!!.get()!!.text = String.format("%s%%", Integer.toString(percentCompleted))
        pb!!.get()!!.progress = status
        st2!!.get()!!.text = String.format(
            "%s of %s",
            status.toString(),
            installedAppsCount.toString()
        )
    }

    override fun onPostExecute(aVoid: Void?) {
        putDateInSharedPreference()
        Collections.sort(malware, AppInfo.appNameComparator)
        Collections.sort(risky, AppInfo.appNameComparator)
        Collections.sort(unknown, AppInfo.appNameComparator)
        Collections.sort(goodware, AppInfo.appNameComparator)
        scannedApps.addAll(malware)
        scannedApps.addAll(risky)
        scannedApps.addAll(unknown)
        scannedApps.addAll(goodware)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(contextRef.get())
        val editor = sharedPreferences.edit()
        editor.apply()
        if (malware.isNotEmpty()){
            editor.putInt("Malwares",malware.size).apply()
        }
        if (risky.isNotEmpty()){
            editor.putInt("Risky",risky.size).apply()
        }
        if (unknown.isNotEmpty()){
            editor.putInt("Unknown",unknown.size).apply()
        }
        //Log.i(TIMER, "Average Time: " + Float.toString(totalTime / count));
        ResultActivity.apps = scannedApps
        val resultScreen = Intent(contextRef.get(), ResultActivity::class.java)
        notificationManager!!.cancel(NOTIFICATION_ID)

        //resultScreen.putParcelableArrayListExtra("appslist", scannedApps);
        activityRef.get()!!.finish()
        contextRef.get()!!.startActivity(resultScreen)
    }


    @SuppressLint("SourceLockedOrientationActivity", "MissingPermission")
    override fun onPreExecute() {
        //Lock screen orientation to prevent UI freeze
        val currentOrientation = activityRef.get()!!.resources.configuration.orientation
        if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) activityRef.get()!!.requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else activityRef.get()!!.requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        installedAppsCount = contextRef.get()!!.packageManager.getInstalledApplications(0).size
        pb!!.get()!!.max = installedAppsCount
        notificationManager = NotificationManagerCompat.from(contextRef.get()!!)
        val CHANNEL_ID = "channel_100"
        val notificationBuilder = NotificationCompat.Builder(contextRef.get()!!, CHANNEL_ID)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val CHANNEL_NAME = "PROJECT MATRIS"
            val mChannel =
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            notificationManager!!.createNotificationChannel(mChannel)
        }
        notificationBuilder.setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setContentTitle("Shell Security")
            .setContentText(contextRef.get()!!.getString(R.string.scanningApplications))
            .setProgress(100, 0, true)
        notificationManager!!.notify(NOTIFICATION_ID, notificationBuilder.build())
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
        val json: String = try {
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

    @Throws(IOException::class)
    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = contextRef.get()!!.assets.openFd("saved_model.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun putDateInSharedPreference() {
        val curDateTime = DateFormat.getDateTimeInstance().format(Date())
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(contextRef.get())
        val editor = sharedPreferences.edit()
        editor.putString("lastScan", curDateTime)
        editor.apply()
    }

    private fun getrandFloat():Float{
        val min = 0.01F
        val max= 0.4F
        return (min + Math.random() * (max - min)).toFloat()
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
                val am = context.createPackageContext(context.packageName, 0).assets
                val addAssetPath = am.javaClass.getMethod("addAssetPath", String::class.java)
                val cookie = addAssetPath.invoke(
                    am,
                    context.packageManager.getApplicationInfo(context.packageName, 0).sourceDir
                ) as Int
                val xmlParser = am.openXmlResourceParser(cookie, "AndroidManifest.xml")
                var eventType = xmlParser.next()
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && "uses-permission" == xmlParser.name) {
                        for (i in 0 until xmlParser.attributeCount) {
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
                val am = context.createPackageContext(context.packageName, 0).assets
                val addAssetPath = am.javaClass.getMethod("addAssetPath", String::class.java)
                val cookie = addAssetPath.invoke(
                    am,
                    context.packageManager.getApplicationInfo(context.packageName, 0).sourceDir
                ) as Int
                val xmlParser = am.openXmlResourceParser(cookie, "AndroidManifest.xml")
                var eventType = xmlParser.next()
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && "action" == xmlParser.name) {
                        for (i in 0 until xmlParser.attributeCount) {
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