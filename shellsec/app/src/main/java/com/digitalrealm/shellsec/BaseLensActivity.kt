package com.digitalrealm.shellsec

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.digitalrealm.shellsec.barcode.BarcodeAnalyzer
import com.digitalrealm.shellsec.databinding.ActivityBaseLensBinding
import com.digitalrealm.shellsec.urlscanner.MainAct2
import com.digitalrealm.shellsec.urlscanner.WhitelistActivity

abstract class BaseLensActivity : AppCompatActivity(), BarcodeAnalyzer.SampleInterface {

    companion object {
        @JvmStatic
        val CAMERA_PERM_CODE = 422
    }

    override fun getResult(value: String) {
        binding.tvOutput.text = value
//        binding.scanbtn.setOnClickListener {
//            if (binding.tvOutput.text.contains("http") or binding.tvOutput.text.contains(".com") or binding.tvOutput.text.contains("www."))
//                startActivity(Intent(this@BaseLensActivity,com.digitalrealm.shellsec.urlscanner.ScanActivity::class.java))
//            else
//                binding.tvOutput.text = "No Link Detected"
//        }
        binding.menubtn.rippleColor = this.resources.getColorStateList(R.color.black)
        binding.menubtn.setOnClickListener {
            startActivity(Intent(this@BaseLensActivity,MainAct2::class.java))

//            startActivity(Intent(this, WhitelistActivity::class.java))
        }
    }

    abstract val imageAnalyzer: ImageAnalysis.Analyzer
    protected lateinit var imageAnalysis: ImageAnalysis

    private fun askCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
            CAMERA_PERM_CODE
        )
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(
            Runnable {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(binding.previewBarcode.surfaceProvider)
                    }

                imageAnalysis = ImageAnalysis.Builder()
                    .build()

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
                } catch (ex: Exception) {
                    Log.e("CAM", "Error bindind camera", ex)
                }

            },
            ContextCompat.getMainExecutor(this)

        )
    }

    abstract fun startScanner()

    private lateinit var binding: ActivityBaseLensBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBaseLensBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val tb = intent.getStringExtra("tb")
        binding.nameTv.text = tb

//        binding.menubtn.background = ResourcesCompat.getDrawable(resources.getColor(R.color.red))
        binding.menubtn.rippleColor = this.resources.getColorStateList(R.color.red)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        askCameraPermission()

//        val bottomSheetFragment = BottomSheetFragment()
        binding.btnStartScanner.setOnClickListener {
            binding.btnStartScanner.isVisible = false
            startScanner()

//            bottomSheetFragment.show(supportFragmentManager,"BottomSheetDialog")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == CAMERA_PERM_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                AlertDialog.Builder(this)
                    .setTitle("Permission Error")
                    .setMessage("Camera Permission not provided")
                    .setPositiveButton("OK") { _, _ -> finish() }
                    .setCancelable(false)
                    .show()
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


}