package com.kirakishou.photoexchange.ui.activity

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.view.MenuItem
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.base.BaseActivity
import com.kirakishou.photoexchange.base.BaseActivityWithoutViewModel
import com.kirakishou.photoexchange.mvvm.model.event.SendPhotoEvent
import com.kirakishou.photoexchange.mvvm.model.event.SendPhotoEventStatus
import com.kirakishou.photoexchange.mvvm.viewmodel.AllPhotoViewActivityViewModel
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class AllPhotoViewActivity : BaseActivityWithoutViewModel(),
        BottomNavigationView.OnNavigationItemSelectedListener{

    @BindView(R.id.bottom_nav_menu)
    lateinit var bottomNavigationMenu: BottomNavigationView

    override fun getContentView(): Int = R.layout.activity_all_photo_view

    override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
    }

    override fun onActivityDestroy() {
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.received_photos -> {

            }

            R.id.sent_photos -> {

            }
        }

        return true
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: SendPhotoEvent) {
        when (event.status) {
            SendPhotoEventStatus.SUCCESS -> {

            }

            SendPhotoEventStatus.FAIL -> {

            }
        }
    }

    override fun resolveDaggerDependency() {

    }
}
