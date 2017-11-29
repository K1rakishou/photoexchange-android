package com.kirakishou.photoexchange.ui.activity

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
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
import com.kirakishou.photoexchange.helper.service.UploadPhotoService
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

    @BindView(R.id.take_photo_button)
    lateinit var takePhotoButton: FloatingActionButton

    @Inject
    lateinit var viewModelFactory: AllPhotosViewActivityViewModelFactory

    @Inject
    lateinit var eventBus: EventBus

    @Inject
    lateinit var appSharedPreference: AppSharedPreference

    private val QUEUED_UP_PHOTOS_FRAGMENT_TAB_INDEX = 0
    private val UPLOADED_PHOTOS_FRAGMENT_TAB_INDEX = 1
    private val RECEIVED_PHOTOS_FRAGMENT_TAB_INDEX = 2

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
        schedulePhotoUpload()
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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        getNotificationIntent(intent)
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
            getViewModel().inputs.scrollToTop()
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
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_title_queued_up)))
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_title_uploaded)))
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_title_received)))
        tabLayout.tabGravity = TabLayout.GRAVITY_FILL

        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = 2

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

    fun schedulePhotoUpload() {
        UploadPhotoService.scheduleJob(this)
        Timber.d("AllPhotosViewActivity: UploadPhoto has been job scheduled")
    }

    fun startLookingForPhotoAnswerService() {
        FindPhotoAnswerService.scheduleImmediateJob(userInfoPreference.getUserId(), this)
        Timber.d("AllPhotosViewActivity: A job has been scheduled")

        getViewModel().inputs.showLookingForPhotoIndicator()
    }

    fun showNewPhotoReceivedNotification() {
        Snackbar.make(takePhotoButton, getString(R.string.new_photo_received), Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.snackbar_button_title_show), {
                    selectReceivedPhotosTab()
                    getViewModel().inputs.scrollToTop()
                })
                .show()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPhotoUploadedEvent(event: PhotoUploadedEvent) {
        when (event.status) {
            SendPhotoEventStatus.START -> {
                Timber.d("AllPhotosViewActivity: SendPhotoEventStatus.START")
                getViewModel().inputs.startUploadingPhotos()
            }
            SendPhotoEventStatus.PHOTO_UPLOADED -> {
                Timber.d("AllPhotosViewActivity: SendPhotoEventStatus.PHOTO_UPLOADED")

                check(event.photoId != -1L)
                getViewModel().inputs.photoUploaded(event.photoId)
            }
            SendPhotoEventStatus.FAIL -> {
                Timber.d("AllPhotosViewActivity: SendPhotoEventStatus.FAIL")
                getViewModel().inputs.showFailedToUploadPhoto()
            }
            SendPhotoEventStatus.DONE -> {
                Timber.d("AllPhotosViewActivity: SendPhotoEventStatus.DONE")
                getViewModel().inputs.allPhotosUploaded()
                startLookingForPhotoAnswerService()
            }
            else -> IllegalArgumentException("Unknown event status: ${event.status}")
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPhotoReceivedEvent(event: PhotoReceivedEvent) {
        when (event.status) {
            PhotoReceivedEventStatus.SUCCESS_ALL_RECEIVED -> {
                Timber.d("AllPhotosViewActivity: PhotoReceivedEventStatus.SUCCESS_ALL_RECEIVED")
                checkNotNull(event.photoAnswer)
                getViewModel().inputs.showPhotoReceived(event.photoAnswer!!, event.allFound)
            }
            PhotoReceivedEventStatus.SUCCESS_NOT_ALL_RECEIVED -> {
                Timber.d("AllPhotosViewActivity: PhotoReceivedEventStatus.SUCCESS_NOT_ALL_RECEIVED")
                checkNotNull(event.photoAnswer)
                getViewModel().inputs.showPhotoReceived(event.photoAnswer!!, event.allFound)
            }
            PhotoReceivedEventStatus.FAIL -> {
                Timber.d("AllPhotosViewActivity: PhotoReceivedEventStatus.FAIL")
                getViewModel().inputs.showErrorWhileTryingToLookForPhoto()
            }
            PhotoReceivedEventStatus.NO_PHOTOS_ON_SERVER -> {
                Timber.d("AllPhotosViewActivity: PhotoReceivedEventStatus.NO_PHOTOS_ON_SERVER")
                getViewModel().inputs.showNoPhotoOnServer()
            }
            PhotoReceivedEventStatus.UPLOAD_MORE_PHOTOS -> {
                Timber.d("AllPhotosViewActivity: PhotoReceivedEventStatus.UPLOAD_MORE_PHOTOS")
                getViewModel().inputs.showUserNeedsToUploadMorePhotos()
            }
            else -> IllegalArgumentException("Unknown event status: ${event.status}")
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
