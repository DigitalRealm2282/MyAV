package com.digitalrealm.shellsec.ui.home

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ProgressDialog
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.provider.Settings
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.digitalrealm.shellsec.CustomScanActivity
import com.digitalrealm.shellsec.R
import com.digitalrealm.shellsec.ScanActivity
import com.digitalrealm.shellsec.barcode.BarcodeActivity
import com.digitalrealm.shellsec.databinding.FragmentHomeBinding
import com.digitalrealm.shellsec.scanners.ApkScanner
import com.digitalrealm.shellsec.settings.SettingsActivity
import com.farimarwat.supergaugeview.SuperGaugeView
import com.github.angads25.filepicker.model.DialogConfigs
import com.github.angads25.filepicker.model.DialogProperties
import com.github.angads25.filepicker.view.FilePickerDialog
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.io.File


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.

    private val binding get() = _binding!!

    private var withSysApps = false
    private var sharedPreferences: SharedPreferences? = null
    var dialog: FilePickerDialog? = null
    private lateinit var pd : ProgressDialog

//    private var activityResultLauncher: ActivityResultLauncher<Array<String>> =
//        registerForActivityResult(
//            ActivityResultContracts.RequestMultiplePermissions()) {result ->
//            var allAreGranted = true
//            for(b in result.values) {
//                allAreGranted = allAreGranted && b
//            }
//
//            if(allAreGranted) {
//
//            }
//        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSharedPreferences()

//        toggleDarkMode(sharedPreferences!!.getBoolean("darkMode", true))
        val lastScan = sharedPreferences!!.getString("lastScan", requireContext().getString(R.string.never))

        pd = ProgressDialog(requireContext())


        binding.textHome.text = requireContext().getString(R.string.last_scan) + " " + lastScan

        binding.scanButton.setOnClickListener {

            val intent = Intent(requireContext(), ScanActivity::class.java)
            intent.putExtra("withSysApps",withSysApps)
            startActivity(intent)

//            hide app icon by
//            <uses-feature
//            android:name="android.software.leanback"
//            android:required="true" />
        }

        binding.customscancard.setOnClickListener {
            startActivity(
                Intent(requireContext(), CustomScanActivity::class.java).putExtra(
                    "withSysApps",
                    withSysApps
                )
            )

        }
        binding.apkscancard.setOnClickListener {

//            checkPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE,0)
//            checkPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,1)
//            if (requireContext().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
//                // Requesting the permission
//                Toast.makeText(requireContext(), "Permission not granted", Toast.LENGTH_SHORT).show()
//
//                requestPermissions( arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 0)
//
//            } else {
//                Toast.makeText(requireContext(), "Permission already granted", Toast.LENGTH_SHORT).show()
//            }
//
//            val permissionLauncher = registerForActivityResult(
//                ActivityResultContracts.RequestPermission()
//            ) { isGranted ->
//                if (isGranted) {
//                    Toast.makeText(requireContext(), "Permission already granted", Toast.LENGTH_SHORT).show()
//                }
//                else {
//                    requestPermissions( arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 0)
//                }
//            }

//            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)

//            val appPerms = arrayOf(
//                Manifest.permission.READ_EXTERNAL_STORAGE
//            )
//            activityResultLauncher.launch(appPerms)
            val properties = DialogProperties()
            properties.selection_mode = DialogConfigs.SINGLE_MODE
            properties.selection_type = DialogConfigs.FILE_SELECT
            properties.root = Environment.getExternalStorageDirectory()
            properties.error_dir = properties.root
            properties.offset = properties.root
            properties.extensions = arrayOf("apk")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.addCategory("android.intent.category.DEFAULT")
                    intent.data = Uri.parse(
                        String.format(
                            "package:%s",
                            requireContext().applicationContext.packageName
                        )
                    )
                    startActivityForResult(intent, 2296)
                } catch (e: Exception) {
                    val intent = Intent()
                    intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    activity?.startActivityForResult(intent, 2296)
                }
            }

            dialog = FilePickerDialog(requireContext(), properties)
            dialog!!.setTitle(requireContext().getString(R.string.select_a_file))
            dialog!!.setDialogSelectionListener { files: Array<String?>? ->
                //files is the array of the paths of files selected by the Application User.
                if (files != null) {
                    val selectedFile = File(files[0]!!)
                    if (selectedFile.exists() && selectedFile.isFile) {
                        val apkScanner = ApkScanner(requireContext(), files[0]!!)
                        apkScanner.execute()
                        //Toast.makeText(context,files[0],Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(
                            requireContext(),
                            requireContext().getString(R.string.file_does_not_exist),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        requireContext().getString(R.string.error_loading_file),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            dialog!!.show()
        }

        binding.sett.setOnClickListener {
            val intent = Intent(requireContext(), SettingsActivity::class.java)
            startActivity(intent)
        }

        binding.wifianalyze.setOnClickListener {

            showBottomSheetDialog()

        }
        binding.killtask.setOnClickListener {

            kill()

        }

        binding.qrcam.setOnClickListener {
            startActivity(Intent(requireContext(), BarcodeActivity::class.java))
        }

    }

    private fun showBottomSheetDialog() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog.setContentView(R.layout.bottomsheet)
        val gauge = bottomSheetDialog.findViewById<SuperGaugeView>(R.id.mygaugeview)

        gauge!!.prepareGauge(requireContext())

        gauge.setGaugeText("Download: ${internetDownSpeed()} MB/S")
        gauge.setProgress(internetDownSpeed().toFloat())


        gauge.setOnClickListener {
            if (gauge.getGaugeText().contains("Download")){
                gauge.setGaugeText("Upload: ${internetUpSpeed()} MB/S")
                gauge.setProgress(internetUpSpeed().toFloat())
            }else{
                gauge.setGaugeText("Download: ${internetDownSpeed()} MB/S")
                gauge.setProgress(internetDownSpeed().toFloat())
            }
        }

        bottomSheetDialog.show()

    }
    private fun internetDownSpeed(): Int {
        val cm: ConnectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo: NetworkInfo = cm.getActiveNetworkInfo()!!
        //should check null because in airplane mode it will be null
        //should check null because in airplane mode it will be null
        val nc: NetworkCapabilities? = cm.getNetworkCapabilities(cm.getActiveNetwork())
        return  nc!!.linkDownstreamBandwidthKbps/1024
    }
    private fun internetUpSpeed(): Int {
        val cm: ConnectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo: NetworkInfo = cm.getActiveNetworkInfo()!!
        //should check null because in airplane mode it will be null
        //should check null because in airplane mode it will be null
        val nc: NetworkCapabilities? = cm.getNetworkCapabilities(cm.getActiveNetwork())
        return  nc!!.linkUpstreamBandwidthKbps/1024
    }
    private fun kill(){
        var progressStatus = 0


        val progressAlertDialog = ProgressDialog(requireContext())
        progressAlertDialog.setCancelable(false)
        progressAlertDialog.setTitle(R.string.app_name)
        progressAlertDialog.setMessage("Searching for malicious tasks")
        progressAlertDialog.show()

        // Get running processes

        val malwareList = listOf("com.sledsdffsjkh.Search","com.russian.signato.renewis","com.android.power"
            ,"com.management.propaganda","com.sec.android.musicplayer","com.houla.quicken"
            ,"com.attd.da","com.arlo.fappx","com.metasploit.stage","com.vantage.ectronic.cornmuni")


        // Get running processes
        val manager: ActivityManager = requireActivity().getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val runningProcesses: List<ActivityManager.RunningAppProcessInfo> = manager.runningAppProcesses
        var i = 1
        if (runningProcesses.isNotEmpty()) {
            // Set data to the list adapter
            //setListAdapter(new ListAdapter(this, runningProcesses));
            for (process in runningProcesses) {
                //Log.i(TAG, process.processName);
                if (process.processName.startsWith("com.metasploit") or process.processName.startsWith(malwareList[0])
                    or process.processName.startsWith(malwareList[1]) or process.processName.startsWith(malwareList[2]) or process.processName.startsWith(malwareList[3])
                    or process.processName.startsWith(malwareList[4]) or process.processName.startsWith(malwareList[5]) or process.processName.startsWith(malwareList[6])
                    or process.processName.startsWith(malwareList[7]) or process.processName.startsWith(malwareList[8]) or process.processName.startsWith(malwareList[9]) ) {

                    manager.killBackgroundProcesses(process.processName)
                    Toast.makeText(
                        requireContext(),
                        "Process Killed : " + process.pkgList,
                        Toast.LENGTH_LONG
                    ).show()
                    val text = "Scan report:<br/><br/>" +
                            "<font color='black'>The Malware name was: </font><font color='red'>" + process.processName + "</font><br/>" +
                            "<font color='black'>The pid was: </font><font color='red'>" + process.pid + "</font><br/>" +
                            "<font color='black'>The uid was: </font><font color='red'>" + process.uid + "</font><br/>" +
                            "<font color='black'>The  pkg hash value was: </font><font color='red'>" + process.pkgList.hashCode() + "</font><br/>" +
                            "<font color='black'>The process hash value was: </font><font color='red'>" + process.hashCode() + "</font><br/>" +
                            "<font color='black'>The pkg to string value was: </font><font color='red'>" + process.pkgList.toString() + "</font><br/><br/><br/>" +
                            "<font color='red' ><b><i>The Malware has been successfully terminated.</b></i></font><br/>"
//                    report.setText(Html.fromHtml(text))
                    Toast.makeText(
                        requireContext(),
                        Html.fromHtml(text),
                        Toast.LENGTH_LONG
                    ).show()


                    //+""+process.toString()+""+process.describeContents()+""+process.pid+""+
                    //getPackageManager().getNameForUid(process.uid));
                    //getPackageName().
                }else{
                    Toast.makeText(requireContext(),"No malicious tasks or Metasploit Detected",Toast.LENGTH_SHORT).show()

                }
            }
        } else {
            // In case there are no processes running (not a chance :))
            Toast.makeText(
                requireContext(),
                "No application is running",
                Toast.LENGTH_LONG
            ).show()
        }

        // Start the lengthy operation in a background thread
        // Start the lengthy operation in a background thread
        Thread {
            while (progressStatus < 100) {
                // Update the progress status
                progressStatus += 1
                progressAlertDialog.progress = progressStatus

                // Try to sleep the thread for 20 milliseconds
                try {
                    Thread.sleep(20)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

//                 Update the progress bar
//                Handler().post(Runnable {
//                    pb.setProgress(progressStatus)
//                    // Show the progress on TextView
//                    tv.setText("$progressStatus %")
//                })
            }
        }.start() // Start the operation


        if (i == 1) {
            i = 0
        } else {
            val text = "Scan report:<br/><br/>" +
                    "<font color='green'><b><i>No threat have been found </b></i></font><br/>"
//            report.setText(Html.fromHtml(text))
            Toast.makeText(
                requireContext(),
                Html.fromHtml(text),
                Toast.LENGTH_LONG
            ).show()

        }
        progressAlertDialog.dismiss()
    }

    private fun setupSharedPreferences() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}