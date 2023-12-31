package com.digitalrealm.shellsec.barcode

import androidx.core.content.ContextCompat
import com.digitalrealm.shellsec.BaseLensActivity

class BarcodeActivity : BaseLensActivity() {

    override val imageAnalyzer = BarcodeAnalyzer(this,this)

    override fun startScanner() {
        scanBarcode()
    }

    private fun scanBarcode() {
        imageAnalysis.setAnalyzer(
            ContextCompat.getMainExecutor(this),
            imageAnalyzer
        )

    }
}