package com.digitalrealm.shellsec.urlscanner

import android.content.ContextWrapper
import android.R as RBasic
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.InputCallback
import com.afollestad.materialdialogs.input.input
import com.digitalrealm.shellsec.R
import com.pixplicity.easyprefs.library.Prefs

class WhitelistActivity : AppCompatActivity() {
    private var listView: RecyclerView? = null
    private var whitelistArray: MutableSet<String>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_whitelist)
        if (supportActionBar != null) {
            supportActionBar!!.elevation = 0.0f
            supportActionBar!!.setDisplayShowHomeEnabled(true)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
        Prefs.Builder()
            .setContext(this)
            .setMode(ContextWrapper.MODE_PRIVATE)
            .setPrefsName(getPackageName())
            .setUseDefaultSharedPreference(true)
            .build()
        whitelistArray = Prefs.getOrderedStringSet("whitelistArray", LinkedHashSet<String>())
        listView = findViewById<RecyclerView>(R.id.listView1)
        val manager: RecyclerView.LayoutManager = LinearLayoutManager(this)
        listView!!.layoutManager = manager
        listView!!.setHasFixedSize(true)
        listView!!.adapter = RecyclerAdapter(whitelistArray!!.toTypedArray())

    }

    private fun showAddToWhitelistDialog() {
        MaterialDialog(this).show {
            this.input("ADD URL") { materialDialog, input ->
                if (!whitelistArray!!.contains(input.toString().trim { it <= ' ' })) {
                    whitelistArray!!.add(input.toString().trim { it <= ' ' })
                    listView!!.getAdapter()!!.notifyDataSetChanged()
                    Toast.makeText(
                        this@WhitelistActivity,
                        getString(R.string.add_to_whitelist_dialog_toast_success),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@WhitelistActivity,
                        getString(R.string.add_to_whitelist_dialog_toast_failure),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                Prefs.putOrderedStringSet("whitelistArray", whitelistArray)
                this.dismiss()
            }

                .title((R.string.add_to_whitelist_dialog_title))
                .message((R.string.add_to_whitelist_dialog_text))

        }
    }

    private fun showRemoveFromWhiteListDialog() {
        MaterialDialog(this).show {
            this.input("Remove url", waitForPositiveButton = true) { materialDialog, input ->
                if (whitelistArray!!.contains(input.toString().trim { it <= ' ' })) {
                    whitelistArray!!.remove(input.toString().trim { it <= ' ' })
                    listView!!.getAdapter()!!.notifyDataSetChanged()
                    Toast.makeText(
                        this@WhitelistActivity,
                        getString(R.string.remove_from_whitelist_dialog_toast_success),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@WhitelistActivity,
                        getString(R.string.remove_from_whitelist_dialog_toast_failure),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                Prefs.putOrderedStringSet("whitelistArray", whitelistArray)
                materialDialog.dismiss()
            }

                .title((R.string.remove_from_whitelist_dialog_title))
                .message((R.string.remove_from_whitelist_dialog_text))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.whitelist_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.add_to_whitelist -> showAddToWhitelistDialog()
            R.id.remove_from_whitelist -> showRemoveFromWhiteListDialog()
            RBasic.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }
}