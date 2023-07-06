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

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.digitalrealm.shellsec.adapters.AppsAdapter
import com.digitalrealm.shellsec.data.AppInfo

class ResultActivity : AppCompatActivity() {
    var layoutManager: RecyclerView.LayoutManager? = null
    var appsAdapter: AppsAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val actionBar = this.supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.title = this.getString(R.string.report)
        }
        setContentView(R.layout.activity_result)
        val resultList = findViewById<RecyclerView>(R.id.resultList)
        layoutManager = LinearLayoutManager(this)
        resultList.layoutManager = layoutManager
        appsAdapter = AppsAdapter(this, apps!!)
        resultList.adapter = appsAdapter
    }

    private fun sendMessage() {
        //Log.d("sender", "Broadcasting message");
        val intent = Intent("uninstall")
        intent.putExtra("uninstall", "yes")
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
    }

    override fun onRestoreInstanceState(
        savedInstanceState: Bundle?,
        persistentState: PersistableBundle?
    ) {
        super.onRestoreInstanceState(savedInstanceState, persistentState)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        NavUtils.navigateUpFromSameTask(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                //Log.d("TAG", "onActivityResult: user accepted the (un)install");
                sendMessage()
            }
        }
    }

    companion object {
        var apps: ArrayList<AppInfo>? = null
    }
}