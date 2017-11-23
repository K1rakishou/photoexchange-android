package com.kirakishou.photoexchange.ui.activity

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.support.v7.widget.AppCompatButton
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
import com.kirakishou.photoexchange.ui.fragment.ReceivedPhotosListFragment
import com.kirakishou.photoexchange.ui.fragment.UploadedPhotosListFragment
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

    @BindView(R.id.take_photo_button)
    lateinit var takePhotoButton: FloatingActionButton

    @Inject
    lateinit var viewModelFactory: AllPhotosViewActivityViewModelFactory

    @Inject
    lateinit var eventBus: EventBus

    @Inject
    lateinit var appSharedPreference: AppSharedPreference

    private val UPLOADED_PHOTOS_FRAGMENT_TAB_INDEX = 0
    private val RECEIVED_PHOTOS_FRAGMENT_TAB_INDEX = 1

    private val adapter = FragmentTabsPager(supportFragmentManager)
    private val userInfoPreference by lazy { appSharedPreference.prepare<UserInfoPreference>() }

    override fun initViewModel(): AllPhotosViewActivityViewModel {
        return ViewModelProviders.of(this, viewModelFactory).get(AllPhotosViewActivityViewModel::class.java)
    }

    override fun getContentView(): Int = R.layout.activity_all_photos_view

    override fun onInitRx() {
        initRx()
    }

    override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
        userInfoPreference.load()
        eventBus.register(this)

        initTabs(intent)
        getNotificationIntent(getIntent())
    }

    override fun onActivityDestroy() {
        eventBus.unregister(this)

        tabLayout.clearOnTabSelectedListeners()
        viewPager.clearOnPageChangeListeners()
    }

    override fun onPause() {
        super.onPause()
        userInfoPreference.save()
    }

    private fun getNotificationIntent(intent: Intent?) {
        if (intent == null || intent.extras == null) {
            Timber.d("No extras. Nothing to handle")
            return
        }

        val extras = intent.extras
        val openReceivedPhotosFragment = extras.getBoolean("open_received_photos_fragment", false)
        if (openReceivedPhotosFragment) {
            selectReceivedPhotosTab()
            getViewModel().inputs.receivedPhotosFragmentScrollToTop()
        }
    }

    private fun initRx() {
        compositeDisposable += RxView.clicks(ivCloseActivityButton)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ finish() })

        compositeDisposable += RxView.clicks(takePhotoButton)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ finish() })

        compositeDisposable += getViewModel().errors.onUnknownErrorObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onUnknownError)
    }

    private fun initTabs(intent: Intent) {
        tabLayout.addTab(tabLayout.newTab().setText("Sent"))
        tabLayout.addTab(tabLayout.newTab().setText("Received"))
        tabLayout.tabGravity = TabLayout.GRAVITY_FILL

        adapter.isPhotoUploading = intent.getBooleanExtra("is_photo_uploading", false)

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

    private fun selectReceivedPhotosTab() {
        val tab = tabLayout.getTabAt(RECEIVED_PHOTOS_FRAGMENT_TAB_INDEX)
        tab!!.select()
    }

    fun startLookingForPhotoAnswerService() {
        FindPhotoAnswerService.scheduleImmediateJob(userInfoPreference.getUserId(), this)
        Timber.d("A job has been scheduled")

        getViewModel().inputs.receivedPhotosFragmentShowLookingForPhotoIndicator()
    }

    fun showNewPhotoReceivedNotification() {
        Snackbar.make(takePhotoButton, "New photo has been received", Snackbar.LENGTH_LONG)
                .setAction("SHOW", {
                    selectReceivedPhotosTab()
                    getViewModel().inputs.receivedPhotosFragmentScrollToTop()
                })
                .show()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPhotoUploadedEvent(event: PhotoUploadedEvent) {
        if (event.status == SendPhotoEventStatus.SUCCESS) {
            checkNotNull(event.photo)
            val photo = event.photo!!

            getViewModel().inputs.uploadedPhotosFragmentShowPhotoUploaded(photo)
            startLookingForPhotoAnswerService()
        } else {
            getViewModel().inputs.uploadedPhotosFragmentShowFailedToUploadPhoto()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPhotoReceivedEvent(event: PhotoReceivedEvent) {
        when (event.status) {
            PhotoReceivedEventStatus.SUCCESS_ALL_RECEIVED -> {
                Timber.d("SUCCESS_ALL_RECEIVED")
                getViewModel().inputs.receivedPhotosFragmentShowPhotoReceived(event.photoAnswer!!, event.allFound)
            }
            PhotoReceivedEventStatus.SUCCESS_NOT_ALL_RECEIVED -> {
                Timber.d("SUCCESS_NOT_ALL_RECEIVED")
                getViewModel().inputs.receivedPhotosFragmentShowPhotoReceived(event.photoAnswer!!, event.allFound)
            }
            PhotoReceivedEventStatus.FAIL -> {
                Timber.d("FAIL")
                getViewModel().inputs.receivedPhotosFragmentShowErrorWhileTryingToLookForPhoto()
            }
            PhotoReceivedEventStatus.NO_PHOTOS_ON_SERVER -> {
                Timber.d("NO_PHOTOS_ON_SERVER")
                getViewModel().inputs.receivedPhotosFragmentShowNoPhotoOnServer()
            }
            PhotoReceivedEventStatus.UPLOAD_MORE_PHOTOS -> {
                Timber.d("UPLOAD_MORE_PHOTOS")
                getViewModel().inputs.receivedPhotosFragmentShowUserNeedsToUploadMorePhotos()
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
