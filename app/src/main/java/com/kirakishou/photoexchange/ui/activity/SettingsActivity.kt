package com.kirakishou.photoexchange.ui.activity

import android.content.Intent
import android.os.Bundle
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.di.module.SettingsActivityModule

class SettingsActivity : BaseActivity() {

    override fun getContentView(): Int = R.layout.activity_settings

    override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
    }

    override fun onInitRx() {
    }

    override fun onActivityStart() {
    }

    override fun onActivityStop() {
    }

    override fun resolveDaggerDependency() {
        (application as PhotoExchangeApplication).applicationComponent
            .plus(SettingsActivityModule(this))
            .inject(this)
    }
}
