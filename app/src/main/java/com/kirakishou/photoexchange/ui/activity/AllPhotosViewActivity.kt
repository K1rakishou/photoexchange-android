package com.kirakishou.photoexchange.ui.activity

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.view.View
import android.view.animation.AnticipateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import butterknife.BindView
import com.jakewharton.rxbinding2.view.RxView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.base.BaseActivity
import com.kirakishou.photoexchange.di.component.DaggerAllPhotoViewActivityComponent
import com.kirakishou.photoexchange.di.module.AllPhotoViewActivityModule
import com.kirakishou.photoexchange.helper.EventAccumulator
import com.kirakishou.photoexchange.helper.extension.mySetListener
import com.kirakishou.photoexchange.helper.preference.AppSharedPreference
import com.kirakishou.photoexchange.helper.preference.UserInfoPreference
import com.kirakishou.photoexchange.helper.service.FindPhotoAnswerService
import com.kirakishou.photoexchange.helper.service.UploadPhotoService
import com.kirakishou.photoexchange.helper.util.NetUtils
import com.kirakishou.photoexchange.mwvm.model.event.*
import com.kirakishou.photoexchange.mwvm.model.event.status.PhotoReceivedEventStatus
import com.kirakishou.photoexchange.mwvm.model.event.status.SendPhotoEventStatus
import com.kirakishou.photoexchange.mwvm.model.state.LookingForPhotoState
import com.kirakishou.photoexchange.mwvm.model.state.PhotoUploadingState
import com.kirakishou.photoexchange.mwvm.viewmodel.AllPhotosViewActivityViewModel
import com.kirakishou.photoexchange.mwvm.viewmodel.factory.AllPhotosViewActivityViewModelFactory
import com.kirakishou.photoexchange.ui.fragment.QueuedUpPhotosListFragment
import com.kirakishou.photoexchange.ui.fragment.ReceivedPhotosListFragment
import com.kirakishou.photoexchange.ui.fragment.UploadedPhotosListFragment
import com.kirakishou.photoexchange.ui.widget.FragmentTabsPager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
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

    @Inject
    lateinit var eventAccumulator: EventAccumulator

    private val tag = "[${this::class.java.simpleName}]: "
    private val QUEUED_UP_PHOTOS_FRAGMENT_TAB_INDEX = 0
    private val UPLOADED_PHOTOS_FRAGMENT_TAB_INDEX = 1
    private val RECEIVED_PHOTOS_FRAGMENT_TAB_INDEX = 2
    private val JOB_START_DELAY = 30_000L

    //fragmentClass, isActiveListener
    private val fragmentsEventListeners = ConcurrentHashMap<Class<*>, Boolean>()
    private val adapter = FragmentTabsPager(supportFragmentManager)
    private val userInfoPreference by lazy { appSharedPreference.prepare<UserInfoPreference>() }

    init {
        fragmentsEventListeners += Pair(QueuedUpPhotosListFragment::class.java, false)
        fragmentsEventListeners += Pair(UploadedPhotosListFragment::class.java, false)
        fragmentsEventListeners += Pair(ReceivedPhotosListFragment::class.java, false)
    }

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
            Timber.tag(tag).d("No extras. Nothing to handle")
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
                .subscribe({ runTakePhotoActivity() })

        compositeDisposable += RxView.clicks(takePhotoButton)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ runTakePhotoActivity() })

        compositeDisposable += getViewModel().outputs.onBeginReceivingEventsObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ clazz -> onBeginReceivingEvents(clazz) }, this::onUnknownError)

        compositeDisposable += getViewModel().outputs.onStopReceivingEventsObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ clazz -> onStopReceivingEvents(clazz) }, this::onUnknownError)

        compositeDisposable += getViewModel().outputs.onStartPhotosUploadingObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .filter { !UploadPhotoService.isAlreadyRunning(this) }
                .subscribe({ schedulePhotoUploadWithDelay() }, this::onUnknownError)

        compositeDisposable += getViewModel().outputs.onStartLookingForPhotosObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .filter { !FindPhotoAnswerService.isAlreadyRunning(this) }
                .subscribe({ scheduleLookingForPhotoAnswer() }, this::onUnknownError)

        compositeDisposable += getViewModel().errors.onUnknownErrorObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onUnknownError)
    }

    private fun runTakePhotoActivity() {
        val intent = Intent(this, TakePhotoActivity::class.java)
        startActivity(intent)

        finish()
    }

    private fun initTabs(intent: Intent) {
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_title_queued_up)))
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_title_uploaded)))
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_title_received)))
        tabLayout.tabGravity = TabLayout.GRAVITY_FILL

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

    private fun schedulePhotoUploadingASAP() {
        if (UploadPhotoService.isAlreadyRunning(this)) {
            Timber.tag(tag).d("UploadPhotoService is already running. Do nothing")
            return
        }

        if (NetUtils.isWifiConnected(this)) {
            Timber.tag(tag).d("schedulePhotoUploadingASAP() Wi-Fi is connected. Scheduling upload job immediate")
            UploadPhotoService.scheduleJob(this)
        } else {
            Timber.tag(tag).d("schedulePhotoUploadingASAP() Wi-Fi is not connected. Scheduling upload job upon Wi-Fi connection available")
            UploadPhotoService.scheduleJobWhenWiFiAvailable(this)
        }
    }

    private fun schedulePhotoUploadWithDelay() {
        if (NetUtils.isWifiConnected(this)) {
            Timber.tag(tag).d("schedulePhotoUploadWithDelay() Wi-Fi is connected. Scheduling upload job with half minute delay")
            UploadPhotoService.scheduleJob(this, JOB_START_DELAY)
        } else {
            Timber.tag(tag).d("schedulePhotoUploadWithDelay() Wi-Fi is not connected. Scheduling upload job upon Wi-Fi connection available")
            UploadPhotoService.scheduleJobWhenWiFiAvailable(this, JOB_START_DELAY)
        }
    }

    fun scheduleLookingForPhotoAnswer() {
        if (FindPhotoAnswerService.isAlreadyRunning(this)) {
            Timber.tag(tag).d("Service has already started. Do nothing.")
            return
        }

        Timber.tag(tag).d("scheduleLookingForPhotoAnswer() Schedule Look for photo answer job immediately")

        FindPhotoAnswerService.scheduleImmediateJob(userInfoPreference.getUserId(), this)
        getViewModel().inputs.showLookingForPhotoIndicator()
    }

    private fun showNewPhotoReceivedNotification() {
        Snackbar.make(takePhotoButton, getString(R.string.new_photo_received), Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.snackbar_button_title_show), {
                    selectReceivedPhotosTab()
                    getViewModel().inputs.scrollToTop()
                })
                .show()
    }

    private fun onBeginReceivingEvents(clazz: Class<*>) {
        fragmentsEventListeners[clazz] = true
        sendAllEvents(clazz)
    }

    private fun onStopReceivingEvents(clazz: Class<*>) {
        fragmentsEventListeners[clazz] = false
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: BaseEvent) {
        when (event) {
            is PhotoUploadedEvent -> {
                rememberOrSendEvent(QueuedUpPhotosListFragment::class.java, event)
                rememberOrSendEvent(UploadedPhotosListFragment::class.java, event)
            }

            is PhotoReceivedEvent -> {
                rememberOrSendEvent(ReceivedPhotosListFragment::class.java, event)
            }
        }
    }

    private fun rememberOrSendEvent(clazz: Class<*>, event: BaseEvent) {
        if (!fragmentsEventListeners[clazz]!!) {
            eventAccumulator.rememberEvent(clazz, event)
        } else {
            sendEvent(clazz, event)
        }
    }

    private fun sendAllEvents(clazz: Class<*>) {
        while (eventAccumulator.hasEvent(clazz)) {
            val event = eventAccumulator.getEvent(clazz)
            sendEvent(clazz, event)
        }
    }

    private fun sendEvent(clazz: Class<*>, event: BaseEvent) {
        if (event is PhotoUploadedEvent) {
            handlePhotoUploadedEvent(clazz, event)
        } else if (event is PhotoReceivedEvent) {
            handlePhotoReceivedEvent(clazz, event)
        }
    }

    private fun handlePhotoUploadedEvent(clazz: Class<*>, event: PhotoUploadedEvent) {
        when (event.status) {
            SendPhotoEventStatus.START -> {
                Timber.tag(tag).d("handlePhotoUploadedEvent($clazz) SendPhotoEventStatus.START")
                getViewModel().inputs.updatePhotoUploadingState(clazz, PhotoUploadingState.StartPhotoUploading())
            }
            SendPhotoEventStatus.PHOTO_UPLOADED -> {
                Timber.tag(tag).d("handlePhotoUploadedEvent($clazz) SendPhotoEventStatus.PHOTO_UPLOADED")
                getViewModel().inputs.updatePhotoUploadingState(clazz, PhotoUploadingState.PhotoUploaded(event.photo!!))
            }
            SendPhotoEventStatus.FAIL -> {
                Timber.tag(tag).d("handlePhotoUploadedEvent($clazz) SendPhotoEventStatus.FAIL")
                getViewModel().inputs.updatePhotoUploadingState(clazz, PhotoUploadingState.FailedToUploadPhoto(event.photo!!))
            }
            SendPhotoEventStatus.DONE -> {
                Timber.tag(tag).d("handlePhotoUploadedEvent($clazz) SendPhotoEventStatus.DONE")
                getViewModel().inputs.updatePhotoUploadingState(clazz, PhotoUploadingState.AllPhotosUploaded())

                //FIXME: this function is being called twice because this event type is being sent to two fragments
                scheduleLookingForPhotoAnswer()
            }
            else -> throw IllegalArgumentException("Unknown event status: ${event.status}")
        }
    }

    private fun handlePhotoReceivedEvent(clazz: Class<*>, event: PhotoReceivedEvent) {
        when (event.status) {
            PhotoReceivedEventStatus.SUCCESS_ALL_RECEIVED -> {
                Timber.tag(tag).d("handlePhotoReceivedEvent($clazz) PhotoReceivedEventStatus.SUCCESS_ALL_RECEIVED")
                checkNotNull(event.photoAnswer)

                getViewModel().inputs.updateLookingForPhotoState(clazz,
                        LookingForPhotoState.PhotoFound(event.photoAnswer!!, event.allFound))

                showNewPhotoReceivedNotification()
            }
            PhotoReceivedEventStatus.SUCCESS_NOT_ALL_RECEIVED -> {
                Timber.tag(tag).d("handlePhotoReceivedEvent($clazz) PhotoReceivedEventStatus.SUCCESS_NOT_ALL_RECEIVED")
                checkNotNull(event.photoAnswer)

                getViewModel().inputs.updateLookingForPhotoState(clazz,
                        LookingForPhotoState.PhotoFound(event.photoAnswer!!, event.allFound))

                showNewPhotoReceivedNotification()
            }
            PhotoReceivedEventStatus.FAIL -> {
                Timber.tag(tag).d("handlePhotoReceivedEvent($clazz) PhotoReceivedEventStatus.FAIL")
                getViewModel().inputs.updateLookingForPhotoState(clazz,
                        LookingForPhotoState.LocalRepositoryError())
            }
            PhotoReceivedEventStatus.NO_PHOTOS_ON_SERVER -> {
                Timber.tag(tag).d("handlePhotoReceivedEvent($clazz) PhotoReceivedEventStatus.NO_PHOTOS_ON_SERVER")
                getViewModel().inputs.updateLookingForPhotoState(clazz,
                        LookingForPhotoState.ServerHasNoPhotos())
            }
            PhotoReceivedEventStatus.UPLOAD_MORE_PHOTOS -> {
                Timber.tag(tag).d("handlePhotoReceivedEvent($clazz) PhotoReceivedEventStatus.UPLOAD_MORE_PHOTOS")
                getViewModel().inputs.updateLookingForPhotoState(clazz,
                        LookingForPhotoState.UploadMorePhotos())
            }
            else -> throw IllegalArgumentException("Unknown event status: ${event.status}")
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
