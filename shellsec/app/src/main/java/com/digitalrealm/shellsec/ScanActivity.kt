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
package com.digitalrealm.shellsec

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.digitalrealm.shellsec.scanners.ScannerTask

class ScanActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)
        supportActionBar?.setTitle(this.getString(R.string.scanning))
        val withSysApps: Boolean = intent.getBooleanExtra("withSysApps", false)
        val progressBar: ProgressBar = findViewById<ProgressBar>(R.id.progressBar)
        val percentText: TextView = findViewById<TextView>(R.id.percentText)
        val statusText: TextView = findViewById<TextView>(R.id.statusText)
        val secondarystatusText: TextView = findViewById<TextView>(R.id.secondaryStatusText)
        val stopButton: Button = findViewById<Button>(R.id.stopButton)
        progressBar.progressDrawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        progressBar.progress = 0
        val scanner = ScannerTask(this, this@ScanActivity)
        scanner.setProgressBar(progressBar)
        scanner.setPercentText(percentText)
        scanner.setStatusText(statusText)
        scanner.setSecondaryStatusText(secondarystatusText)
        scanner.setWithSysApps(withSysApps)
        scanner.execute()
        stopButton.setOnClickListener {
            scanner.cancel(true)
            finish()
        }
    }

    override fun onBackPressed() {
        //Back button is disabled while scanning
    }
}