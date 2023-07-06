package com.digitalrealm.shellsec.ui.notifications

import android.app.ProgressDialog
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.os.Debug
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import com.crazylegend.kotlinextensions.storage.isDiskEncrypted
import com.crazylegend.lifecycle.viewCoroutineScope
import com.crazylegend.root.RootUtils
import com.crazylegend.security.MagiskDetector
import com.digitalrealm.shellsec.R
import com.digitalrealm.shellsec.databinding.FragmentNotificationsBinding
import com.digitalrealm.shellsec.helper.ThemeToggleHelper.getApplicationInfo
import kotlinx.coroutines.*
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.suspendCoroutine


class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var sharedPreferences: SharedPreferences


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val notificationsViewModel =
            ViewModelProvider(this).get(NotificationsViewModel::class.java)

        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

//        val textView: TextView = binding.textNotifications
//        notificationsViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        setupSharedPreferences()




        val cs =CoroutineScope(viewCoroutineScope.coroutineContext)

//        val cc =CoroutineScope(viewCoroutineScope.coroutineContext).coroutineContext
        val job = Dispatchers.IO

        try {
            val pad = ProgressDialog(requireContext())
            pad.setCancelable(false)
            pad.setTitle("Loading")
            pad.show()
            cs.launch {
                withContext(job){
                    onc()
                    pad.dismiss()
                }
            }

        }catch (ex:Exception){
            Log.i("notfrag",ex.message.toString())}


        if (sharedPreferences.getInt("Malwares",0)!=0){
            val malString = sharedPreferences.getInt("Malwares", 0).toString()

            if (sharedPreferences.getInt("Malwares",0) == 1) {
                binding.malwaretxt1.text = "$malString Malware Found"
            }else{
                binding.malwaretxt1.text = "$malString Malwares Found"
            }
        }else{
            binding.malware.visibility = GONE
        }
        if (sharedPreferences.getInt("Risky",0)!=0){
            val risky = sharedPreferences.getInt("Risky",0).toString()
            if (sharedPreferences.getInt("Risky",0) == 1) {
                binding.malwaretxt1.text = "$risky Risky App Found"
            }else{
                binding.malwaretxt1.text = "$risky Risky Apps Found"
            }
        }else{
            binding.risky.visibility = GONE
        }
        if (sharedPreferences.getInt("Unknown",0)!=0){
            val unk = sharedPreferences.getInt("Unknown",0).toString()
            if (sharedPreferences.getInt("Unknown",0) == 1) {
                binding.malwaretxt1.text = "$unk Unknown App Found"
            }else{
                binding.malwaretxt1.text = "$unk Unknown Apps Found"
            }
        }else{
            binding.unknown.visibility = GONE
        }
        if(Settings.Secure.getInt(requireContext().contentResolver, Settings.Secure.ADB_ENABLED, 0) == 1) {
            // debugging enabled
//            Toast.makeText(requireContext(),"Close ADB Debug options",Toast.LENGTH_SHORT).show()
            binding.errortxt1.text = "Close ADB Debug options"
//            val isDebuggable = 0 != ApplicationInfo.FLAG_DEBUGGABLE.let {
//                getApplicationInfo().flags =
//                    getApplicationInfo().flags and it; getApplicationInfo().flags
//            }
//            if (!Debug.isDebuggerConnected()){
//                //Yes, it is.
//            }
        } else {
            //;debugging does not enabled
            binding.debug.visibility = GONE
        }



        super.onViewCreated(view, savedInstanceState)
    }

    private suspend fun onc(){
        coroutineScope {
            try{
                binding.contentStorage.text = if (isDiskEncrypted) getString(R.string.disk_encrypted) else getString(
                    R.string.disk_not_encrypted)
                binding.titleStorage.text= getString(R.string.disk_status)

                binding.titleRoot.text = getString(R.string.root_status)
                binding.contentRoot.text = if (RootUtils.isDeviceRooted) getString(R.string.device_rooted) else getString(
                    R.string.device_not_rooted)

                binding.titleMag.text = getString(R.string.magisk_status)
                binding.contentMag.text = if (MagiskDetector(requireContext()).checkForMagisk()) getString(
                    R.string.magisk_detected) else getString(R.string.magisk_not_detected)

            }catch (ex:Exception){
                Log.i("notfrag",ex.message.toString())
            }
        }
    }

    private fun setupSharedPreferences() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
//        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}