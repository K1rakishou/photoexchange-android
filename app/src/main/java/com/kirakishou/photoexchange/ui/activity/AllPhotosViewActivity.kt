package com.kirakishou.photoexchange.ui.activity

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.widget.ImageView
import butterknife.BindView
import com.jakewharton.rxbinding2.view.RxView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.base.BaseActivity
import com.kirakishou.photoexchange.di.component.DaggerAllPhotoViewActivityComponent
import com.kirakishou.photoexchange.di.module.AllPhotoViewActivityModule
import com.kirakishou.photoexchange.helper.preference.AppSharedPreference
import com.kirakishou.photoexchange.helper.preference.UserInfoPreference
import com.kirakishou.photoexchange.helper.service.FindPhotoAnswerService
import com.kirakishou.photoexchange.mwvm.model.event.PhotoReceivedEvent
import com.kirakishou.photoexchange.mwvm.model.event.PhotoReceivedEventStatus
import com.kirakishou.photoexchange.mwvm.model.event.PhotoUploadedEvent
import com.kirakishou.photoexchange.mwvm.model.event.SendPhotoEventStatus
import com.kirakishou.photoexchange.mwvm.viewmodel.AllPhotosViewActivityViewModel
import com.kirakishou.photoexchange.mwvm.viewmodel.factory.AllPhotosViewActivityViewModelFactory
import com.kirakishou.photoexchange.ui.widget.FragmentTabsPager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber
import javax.inject.Inject

class AllPhotosViewActivity : BaseActivity<AllPhotosViewActivityViewModel>(),
        TabLayout.OnTabSelectedListener, ViewPager.OnPageChangeListener {

    @BindView(R.id.iv_close_button)
    lateinit var ivCloseActivityButton: ImageView

    @BindView(R.id.sliding_tab_layout)
    lateinit var tabLayout: TabLayout

    @BindView(R.id.view_pager)
    lateinit var viewPager: ViewPager

    @Inject
    lateinit var viewModelFactory: AllPhotosViewActivityViewModelFactory

    @Inject
    lateinit var eventBus: EventBus

    @Inject
    lateinit var appSharedPreference: AppSharedPreference

    private val adapter = FragmentTabsPager(supportFragmentManager)
    private val userInfoPreference by lazy { appSharedPreference.prepare<UserInfoPreference>() }

    override fun initViewModel(): AllPhotosViewActivityViewModel {
        return ViewModelProviders.of(this, viewModelFactory).get(AllPhotosViewActivityViewModel::class.java)
    }

    override fun getContentView(): Int = R.layout.activity_all_photos_view

    override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
        userInfoPreference.load()
        eventBus.register(this)

        initTabs(intent)
        initRx()
    }

    override fun onActivityDestroy() {
        eventBus.unregister(this)

        tabLayout.clearOnTabSelectedListeners()
        viewPager.clearOnPageChangeListeners()

        PhotoExchangeApplication.refWatcher.watch(this, this::class.simpleName)
    }

    override fun onPause() {
        super.onPause()
        userInfoPreference.save()
    }

    private fun initRx() {
        compositeDisposable += RxView.clicks(ivCloseActivityButton)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ finish() })
    }

    private fun initTabs(intent: Intent) {
        tabLayout.addTab(tabLayout.newTab().setText("Sent"))
        tabLayout.addTab(tabLayout.newTab().setText("Received"))
        tabLayout.tabGravity = TabLayout.GRAVITY_FILL

        adapter.isUploadingPhoto = intent.getBooleanExtra("is_uploading_photo", false)

        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = 1

        viewPager.addOnPageChangeListener(this)
        tabLayout.addOnTabSelectedListener(this)
    }

    override fun onTabReselected(tab: TabLayout.Tab) {
        if (viewPager.currentItem != tab.position) {
            viewPager.currentItem = tab.position
        }
    }

    override fun onTabUnselected(tab: TabLayout.Tab) {
    }

    override fun onTabSelected(tab: TabLayout.Tab) {
        if (viewPager.currentItem != tab.position) {
            viewPager.currentItem = tab.position
        }
    }

    override fun onPageScrollStateChanged(state: Int) {
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        tabLayout.setScrollPosition(position, positionOffset, true)
    }

    override fun onPageSelected(position: Int) {
    }

    fun startFindPhotoAnswerService() {
        FindPhotoAnswerService.scheduleImmediateJob(userInfoPreference.getUserId(), this)
        Timber.d("A job has been scheduled")
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPhotoUploadedEvent(event: PhotoUploadedEvent) {
        val fragment = adapter.sentPhotosFragment
        if (fragment == null) {
            Timber.w("Event received when fragment is null!")
            return
        }

        if (!fragment.isAdded) {
            Timber.w("Fragment is not added in the backstack!")
            return
        }

        if (event.status == SendPhotoEventStatus.SUCCESS) {
            checkNotNull(event.photo)
            val photo = event.photo!!

            fragment.onPhotoUploaded(photo)
            startFindPhotoAnswerService()
        } else {
            fragment.onFailedToUploadPhoto()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPhotoReceivedEvent(event: PhotoReceivedEvent) {
        val fragment = adapter.receivedPhotosFragment
        if (fragment == null) {
            Timber.w("Event received when fragment is null!")
            return
        }

        if (!fragment.isAdded) {
            Timber.w("Fragment is not added in the backstack!")
            return
        }

        when (event.status) {
            PhotoReceivedEventStatus.SUCCESS_ALL_RECEIVED -> {
                Timber.d("SUCCESS_ALL_RECEIVED")
            }
            PhotoReceivedEventStatus.SUCCESS_NOT_ALL_RECEIVED -> {
                Timber.d("SUCCESS_NOT_ALL_RECEIVED rescheduling as immediate")
                FindPhotoAnswerService.scheduleImmediateJob(userInfoPreference.getUserId(), this)
            }
            PhotoReceivedEventStatus.FAIL -> {
                Timber.d("FAIL")
            }
            PhotoReceivedEventStatus.NO_PHOTOS_ON_SERVER -> {
                Timber.d("NO_PHOTOS_ON_SERVER rescheduling as periodic")
                FindPhotoAnswerService.scheduleJobPeriodicJob(userInfoPreference.getUserId(), this)
            }
            PhotoReceivedEventStatus.USER_HAS_NOT_UPLOADED_ANY_PHOTOS -> {
                Timber.d("USER_HAS_NOT_UPLOADED_ANY_PHOTOS")
            }
            PhotoReceivedEventStatus.UPLOAD_MORE_PHOTOS -> {
                Timber.d("UPLOAD_MORE_PHOTOS")
            }
        }
    }

    override fun resolveDaggerDependency() {
        DaggerAllPhotoViewActivityComponent.builder()
                .applicationComponent(PhotoExchangeApplication.applicationComponent)
                .allPhotoViewActivityModule(AllPhotoViewActivityModule(this))
                .build()
                .inject(this)
    }
}
