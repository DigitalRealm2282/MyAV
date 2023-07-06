package com.digitalrealm.shellsec.scanners

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import androidx.annotation.RequiresApi
import com.digitalrealm.shellsec.AppDetails
import com.digitalrealm.shellsec.R
import com.digitalrealm.shellsec.utils.AppConstants
import com.digitalrealm.shellsec.utils.Sha256HashExtractor.getSha256Hash
import net.dongliu.apk.parser.ApkFile
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.StringReader
import java.lang.ref.WeakReference
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class ApkScanner(context: Context, path: String) : AsyncTask<Void?, String?, Void?>() {
    private val contextRef: WeakReference<Context>
    var pd: ProgressDialog? = null
    private var tflite: Interpreter? = null
    private var p_jArray: JSONArray? = null
    private var i_jArray: JSONArray? = null
    private val filePath: String
    private var packageName: String? = null
    private var appName: String? = null
    private var prediction: String? = null
    private var predictionScore = 0f
    private var appPermissionsList = ArrayList<String>()
    private var sha256Hash: String? = null

    init {
        contextRef = WeakReference(context)
        filePath = path
    }

    override fun onPreExecute() {
        pd = ProgressDialog(contextRef.get())
        pd!!.setMessage("Scanning...")
        pd!!.show()
        super.onPreExecute()
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
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
        try {
            var flag = false
            val apkFile = ApkFile(File(filePath))
            val manifestXml = apkFile.manifestXml
            val apkMeta = apkFile.apkMeta
            packageName = apkMeta.packageName
            appName = apkMeta.label
            appPermissionsList = getListOfPermissions(manifestXml)
            val appIntentsList = getListOfIntents(manifestXml)
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
            val hash = getSha256Hash(filePath)
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
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    override fun onPostExecute(aVoid: Void?) {
        val intent = Intent(contextRef.get(), AppDetails::class.java)
        intent.putExtra("appName", appName)
        intent.putExtra("packageName", packageName)
        intent.putExtra("result", prediction)
        intent.putExtra("prediction", predictionScore)
        intent.putExtra("scan_mode", "apk_scan")
        intent.putStringArrayListExtra("permissionList", appPermissionsList)
        intent.putExtra(AppConstants.SHA_256_HASH, sha256Hash)
        if (pd != null && pd!!.isShowing) {
            pd!!.dismiss()
        }
        contextRef.get()!!.startActivity(intent)
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
        private fun getListOfPermissions(manifestXml: String): ArrayList<String> {
            val arr = ArrayList<String>()
            try {
                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = true
                val xpp = factory.newPullParser()
                xpp.setInput(StringReader(manifestXml))
                var eventType = xpp.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && "uses-permission" == xpp.name) {
                        for (i in 0 until xpp.attributeCount) {
                            if (xpp.getAttributeName(i) == "name") {
                                arr.add(xpp.getAttributeValue(i))
                            }
                        }
                    }
                    eventType = xpp.next()
                }
            } catch (exception: XmlPullParserException) {
                exception.printStackTrace()
            } catch (exception: IOException) {
                exception.printStackTrace()
            }
            return arr
        }

        private fun getListOfIntents(manifestXml: String): ArrayList<String> {
            val arr = ArrayList<String>()
            try {
                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = true
                val xpp = factory.newPullParser()
                xpp.setInput(StringReader(manifestXml))
                var eventType = xpp.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && "action" == xpp.name) {
                        for (i in 0 until xpp.attributeCount) {
                            if (xpp.getAttributeName(i) == "name") {
                                arr.add(xpp.getAttributeValue(i))
                            }
                        }
                    }
                    eventType = xpp.next()
                }
            } catch (exception: XmlPullParserException) {
                exception.printStackTrace()
            } catch (exception: IOException) {
                exception.printStackTrace()
            }
            return arr
        }
    }
}