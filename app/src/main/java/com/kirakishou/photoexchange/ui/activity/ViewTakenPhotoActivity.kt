package com.kirakishou.photoexchange.ui.activity

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.base.BaseActivityWithoutViewModel

class ViewTakenPhotoActivity : BaseActivityWithoutViewModel() {

    override fun getContentView(): Int = R.layout.activity_view_taken_photo

    override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {

    }

    override fun onActivityDestroy() {
    }

    override fun resolveDaggerDependency() {
    }

}
