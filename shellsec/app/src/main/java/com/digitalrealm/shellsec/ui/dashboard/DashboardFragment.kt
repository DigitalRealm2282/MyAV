package com.digitalrealm.shellsec.ui.dashboard

import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.crazylegend.kotlinextensions.delegates.int
import com.digitalrealm.shellsec.databinding.FragmentDashboardBinding
import com.digitalrealm.shellsec.services.DeviceAdminSampleReceiver
import com.digitalrealm.shellsec.utils.RamManager
import com.google.android.material.snackbar.Snackbar
import kotlin.reflect.KProperty

class DashboardFragment  : Fragment() {

    private var _binding: FragmentDashboardBinding? = null

    private val binding get() = _binding!!
    var compName: ComponentName? = null
    val RESULT_ENABLE = 11

    private lateinit var sharedPreferences: SharedPreferences

    private var adminComponent: ComponentName? = null
    private var devicePolicyManager: DevicePolicyManager? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        val homeViewModel = ViewModelProvider(this)[DashboardViewModel::class.java]


        _binding = FragmentDashboardBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSharedPreferences()

        binding.seekArc.maxProgress = sharedPreferences.getInt("totRam",0)
        binding.seekArc.progress = sharedPreferences.getInt("totRam",0) - sharedPreferences.getInt("freeRam",0)

        binding.totRam.text = sharedPreferences.getInt("totRam",0).toString()+" MB"
        binding.freeRam.text = sharedPreferences.getInt("freeRam",0).toString()+" MB"

        if (binding.seekArc.progress+1000 >= binding.seekArc.maxProgress)
            binding.seekArc.progressColor = Color.RED

        binding.opt.setOnClickListener {
            binding.progressIndicator.visibility = VISIBLE
            try {
                val manager: ActivityManager = requireActivity().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val runningProcesses: List<ActivityManager.RunningAppProcessInfo> = manager.runningAppProcesses
                for (i in runningProcesses)
                    manager.killBackgroundProcesses(i.processName)
                val rm = RamManager(requireContext())
                rm.killBgProcesses(false)
                sharedPreferences.edit().putInt("totRam",rm.totalRam).apply()
                sharedPreferences.edit().putInt("freeRam",rm.freeRam).apply()
            }catch (ex:Exception){Toast.makeText(requireContext(),"Try Again",Toast.LENGTH_SHORT).show()}

            binding.progressIndicator.visibility = GONE
        }
        binding.lockButton.setOnClickListener { lock() }

//        binding.camSwitch.setOnClickListener {
//
//            try {
//
//                if (binding.camSwitch.isChecked){
//                    val devicePolicyManager = requireContext().getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
////        MainActivity::class.java
//                    compName = ComponentName(requireContext(), DeviceAdminSampleReceiver::class.java)
//                    val isActive = devicePolicyManager.isAdminActive(compName!!)
//
//                    if (isActive) {
//                        Toast.makeText(requireContext(), "admin active", Toast.LENGTH_SHORT).show()
//
//                        devicePolicyManager.setCameraDisabled(compName!!,true)
//                        Toast.makeText(requireContext(), "Camera disabled", Toast.LENGTH_SHORT).show()
//
//                        Log.i("com.digitalrealm.shellsec", "disabling camera...")
//                    } else {
//                        Toast.makeText(requireContext(), "admin not active", Toast.LENGTH_SHORT).show()
//
//                        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
//                        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
//                        intent.putExtra(
//                            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
//                            "Needed to lock Camera"
//                        )
//                        startActivityForResult(intent, RESULT_ENABLE)
//                    }
//                }else{
//                    binding.camSwitch.isChecked = false
//                    devicePolicyManager?.setCameraDisabled(compName!!,false)
//                    Toast.makeText(requireContext(), "Camera enabled", Toast.LENGTH_SHORT).show()
//
//                }
//            }catch (ex:Exception){
//            Snackbar.make(requireContext(),this.requireView(),ex.message.toString(),Snackbar.ANIMATION_MODE_SLIDE).show()
//            }
//
//        }



    }
    private fun setupSharedPreferences() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
//        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    private fun lock() {
        val devicePolicyManager =
            requireContext().getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager?
//        MainActivity::class.java
        compName = ComponentName(requireContext(), DeviceAdminSampleReceiver::class.java)
        val isActive = devicePolicyManager!!.isAdminActive(compName!!)

        if (isActive) {
            Toast.makeText(requireContext(), "admin active", Toast.LENGTH_SHORT).show()

            devicePolicyManager.lockNow()
            Log.i("br.com.sombriks", "locking...")
        } else {
            Toast.makeText(requireContext(), "admin not active", Toast.LENGTH_SHORT).show()

            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
            intent.putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Needed to lock screeen"
            )
            startActivityForResult(intent, RESULT_ENABLE)
        }
    }



}







