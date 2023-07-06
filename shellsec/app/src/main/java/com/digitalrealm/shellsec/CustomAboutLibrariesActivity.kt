package com.digitalrealm.shellsec

import android.os.Bundle
import com.digitalrealm.shellsec.helper.ThemeToggleHelper.setIntent
import com.mikepenz.aboutlibraries.LibsBuilder
import com.mikepenz.aboutlibraries.ui.LibsActivity

class CustomAboutLibrariesActivity : LibsActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setIntent(
            LibsBuilder()
                .withActivityTitle(getResources().getString(R.string.open_source_libraries))
                .withAutoDetect(true)
                .withAboutIconShown(true)
                .withLicenseShown(true)
                .intent(this)
        )
        super.onCreate(savedInstanceState)
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }
}