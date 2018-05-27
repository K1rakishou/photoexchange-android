package com.kirakishou.photoexchange.ui.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.design.widget.TabLayout
import android.support.v4.app.ActivityCompat
import android.support.v4.view.ViewPager
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import butterknife.BindView
import com.jakewharton.rxbinding2.view.RxView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.di.module.PhotosActivityModule
import com.kirakishou.photoexchange.helper.extension.debounceClicks
import com.kirakishou.photoexchange.helper.intercom.StateEventListener
import com.kirakishou.photoexchange.helper.permission.PermissionManager
import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.mvp.viewmodel.PhotosActivityViewModel
import com.kirakishou.photoexchange.service.ReceivePhotosServiceConnection
import com.kirakishou.photoexchange.service.ReceivePhotosService
import com.kirakishou.photoexchange.service.UploadPhotoService
import com.kirakishou.photoexchange.service.UploadPhotoServiceConnection
import com.kirakishou.photoexchange.ui.adapter.UploadedPhotosAdapter
import com.kirakishou.photoexchange.ui.callback.ReceivePhotosServiceCallback
import com.kirakishou.photoexchange.ui.callback.PhotoUploadingCallback
import com.kirakishou.photoexchange.ui.dialog.GpsRationaleDialog
import com.kirakishou.photoexchange.ui.viewstate.PhotosActivityViewState
import com.kirakishou.photoexchange.helper.intercom.event.PhotosActivityEvent
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.helper.intercom.event.ReceivedPhotosFragmentEvent
import com.kirakishou.photoexchange.ui.widget.FragmentTabsPager
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.exceptions.CompositeException
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class PhotosActivity : BaseActivity(), TabLayout.OnTabSelectedListener,
    ViewPager.OnPageChangeListener, PhotoUploadingCallback, ReceivePhotosServiceCallback,
    PopupMenu.OnMenuItemClickListener, StateEventListener<PhotosActivityEvent> {

    @BindView(R.id.root_layout)
    lateinit var rootLayout: CoordinatorLayout

    @BindView(R.id.iv_close_button)
    lateinit var ivCloseActivityButton: ImageButton

    @BindView(R.id.sliding_tab_layout)
    lateinit var tabLayout: TabLayout

    @BindView(R.id.view_pager)
    lateinit var viewPager: ViewPager

    @BindView(R.id.take_photo_button)
    lateinit var takePhotoButton: FloatingActionButton

    @BindView(R.id.menu_button)
    lateinit var menuButton: ImageView

    @Inject
    lateinit var viewModel: PhotosActivityViewModel

    @Inject
    lateinit var permissionManager: PermissionManager

    val activityComponent by lazy {
        (application as PhotoExchangeApplication).applicationComponent
            .plus(PhotosActivityModule(this))
    }

    private val TAG = "PhotosActivity"
    private val FRAGMENT_SCROLL_DELAY_MS = 250L
    private val PHOTO_DELETE_DELAY = 3000L
    private val UPLOADED_PHOTOS_TAB_INDEX = 0
    private val RECEIVED_PHOTO_TAB_INDEX = 1

    private lateinit var receivePhotosServiceConnection: ReceivePhotosServiceConnection
    private lateinit var uploadPhotosServiceConnection: UploadPhotoServiceConnection

    private val adapter = FragmentTabsPager(supportFragmentManager)
    private var savedInstanceState: Bundle? = null
    private var viewState = PhotosActivityViewState()

    override fun getContentView(): Int = R.layout.activity_all_photos

    override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
        this.savedInstanceState = savedInstanceState

        receivePhotosServiceConnection = ReceivePhotosServiceConnection(this)
        uploadPhotosServiceConnection = UploadPhotoServiceConnection(this)
    }

    override fun onActivityStart() {
        initRx()
        checkPermissions(this.savedInstanceState)
    }

    override fun onActivityStop() {
        receivePhotosServiceConnection.onFindingServiceDisconnected()
        uploadPhotosServiceConnection.onUploadingServiceDisconnected()
    }

    override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)

        viewState.lastOpenedTab = viewPager.currentItem
        viewState.saveToBundle(outState)
    }

    private fun initRx() {
        compositeDisposable += RxView.clicks(ivCloseActivityButton)
            .subscribeOn(AndroidSchedulers.mainThread())
            .debounceClicks()
            .doOnNext { finish() }
            .doOnError { Timber.e(it) }
            .subscribe()

        compositeDisposable += RxView.clicks(takePhotoButton)
            .subscribeOn(AndroidSchedulers.mainThread())
            .debounceClicks()
            .doOnNext { finish() }
            .doOnError { Timber.e(it) }
            .subscribe()

        compositeDisposable += RxView.clicks(menuButton)
            .subscribeOn(AndroidSchedulers.mainThread())
            .debounceClicks()
            .doOnNext { createMenu() }
            .doOnError { Timber.e(it) }
            .subscribe()

        compositeDisposable += viewModel.eventForwarder.getPhotoActivityEventsStream()
            .subscribeOn(AndroidSchedulers.mainThread())
            .doOnNext { onStateEvent(it) }
            .doOnError { Timber.e(it) }
            .subscribe()
    }

    private fun checkPermissions(savedInstanceState: Bundle?) {
        val requestedPermissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        permissionManager.askForPermission(this, requestedPermissions) { permissions, grantResults ->
            val index = permissions.indexOf(Manifest.permission.ACCESS_FINE_LOCATION)
            if (index == -1) {
                throw RuntimeException("Couldn't find Manifest.permission.CAMERA in result permissions")
            }

            var granted = true

            if (grantResults[index] == PackageManager.PERMISSION_DENIED) {
                granted = false

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    showGpsRationaleDialog(savedInstanceState)
                    return@askForPermission
                }
            }

            onPermissionsCallback(savedInstanceState, granted)
        }
    }

    private fun showGpsRationaleDialog(savedInstanceState: Bundle?) {
        GpsRationaleDialog().show(this, {
            checkPermissions(savedInstanceState)
        }, {
            onPermissionsCallback(savedInstanceState, false)
        })
    }

    private fun onPermissionsCallback(savedInstanceState: Bundle?, granted: Boolean) {
        compositeDisposable += Observable.fromCallable {
            initViews()
            restoreUploadedPhotosFragmentFromViewState(savedInstanceState)
        }
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(Schedulers.io())
            .concatMap {
                viewModel.updateGpsPermissionGranted(granted)
                    .toObservable<Unit>()
                    .startWith(Unit)
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                if (UploadPhotoService.isRunning(this)) {
                    bindUploadingService(false)
                }
            }
            .subscribe()
    }

    private fun restoreUploadedPhotosFragmentFromViewState(savedInstanceState: Bundle?) {
        viewState = PhotosActivityViewState().also {
            it.loadFromBundle(savedInstanceState)
        }

        if (viewState.lastOpenedTab != 0) {
            viewPager.currentItem = viewState.lastOpenedTab
        }
    }

    private fun initViews() {
        initTabs()
    }

    private fun initTabs() {
        tabLayout.removeAllTabs()
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_title_uploaded_photos)))
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_title_received_photos)))
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_title_gallery)))
        tabLayout.tabGravity = TabLayout.GRAVITY_FILL

        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = 1

        viewPager.clearOnPageChangeListeners()
        tabLayout.clearOnTabSelectedListeners()

        viewPager.addOnPageChangeListener(this)
        tabLayout.addOnTabSelectedListener(this)
    }

    private fun createMenu() {
        val popupMenu = PopupMenu(this, menuButton)
        popupMenu.setOnMenuItemClickListener(this)
        popupMenu.menu.add(1, R.id.settings_item, 1, resources.getString(R.string.settings_menu_item_text))
        popupMenu.show()
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
        if (viewPager.currentItem != position) {
            viewPager.currentItem = position
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings_item -> {
                runActivity(SettingsActivity::class.java)
            }

            else -> throw IllegalArgumentException("Unknown menu item photoId ${item.itemId}")
        }

        return true
    }

    override fun onStateEvent(event: PhotosActivityEvent) {
        when (event) {
            is PhotosActivityEvent.StartUploadingService -> {
                compositeDisposable += viewModel.checkHasPhotosToUpload()
                    .filter { hasPhotoToUpload -> hasPhotoToUpload }
                    .subscribe({ bindUploadingService(true) })
            }
            is PhotosActivityEvent.StartReceivingService -> {
                compositeDisposable += viewModel.checkHasPhotosToReceive()
                    .filter { hasPhotosToReceive -> hasPhotosToReceive }
                    .subscribe({ bindReceivingService(true) })
            }
            is PhotosActivityEvent.FailedToUploadPhotoButtonClick -> {
                compositeDisposable += handleUploadedPhotosFragmentAdapterButtonClicks(event.clickType)
                    .subscribe({ }, { error ->
                        Timber.tag(TAG).e(error)
                    })
            }
        }
    }

    override fun onUploadPhotosEvent(event: UploadedPhotosFragmentEvent.PhotoUploadEvent) {
        viewModel.eventForwarder.sendUploadedPhotosFragmentEvent(event)

        when (event) {
            is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnFailedToUpload -> {

            }
            is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnUnknownError -> {

            }
            is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadStart -> {

            }
            is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress -> {

            }
            is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnUploaded -> {

            }
            is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnFoundPhotoAnswer -> {

            }
            is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnEnd -> {

            }
        }

        when (event) {
            is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnEnd -> {
                if (ReceivePhotosService.isRunning(this)) {
                    bindReceivingService(false)
                }
            }
        }
    }

    override fun onPhotoFindEvent(event: ReceivedPhotosFragmentEvent.ReceivePhotosEvent) {
        viewModel.eventForwarder.sendReceivedPhotosFragmentEvent(event)

        when (event) {
            is ReceivedPhotosFragmentEvent.ReceivePhotosEvent.OnPhotoReceived -> {
                viewModel.eventForwarder.sendUploadedPhotosFragmentEvent(
                    UploadedPhotosFragmentEvent.PhotoUploadEvent.OnFoundPhotoAnswer(event.takenPhotoName))
                viewModel.eventForwarder.sendReceivedPhotosFragmentEvent(
                    ReceivedPhotosFragmentEvent.ReceivePhotosEvent.OnPhotoReceived(event.receivedPhoto, event.takenPhotoName)
                )
                showPhotoAnswerFoundSnackbar()
            }
        }
    }

    private fun handleUploadedPhotosFragmentAdapterButtonClicks(
        adapterButtonsClick: UploadedPhotosAdapter.UploadedPhotosAdapterButtonClick
    ): Observable<Boolean> {

        return when (adapterButtonsClick) {
            is UploadedPhotosAdapter.UploadedPhotosAdapterButtonClick.DeleteButtonClick -> {
                Observable.fromCallable { showPhotoDeletedSnackbar(adapterButtonsClick.photo) }
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .map { false }
                    .doOnError { Timber.e(it) }
            }

            is UploadedPhotosAdapter.UploadedPhotosAdapterButtonClick.RetryButtonClick -> {
                Observable.fromCallable { viewModel.changePhotoState(adapterButtonsClick.photo.id, PhotoState.PHOTO_QUEUED_UP).blockingAwait() }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext {
                        val photo = adapterButtonsClick.photo
                        photo.photoState = PhotoState.PHOTO_QUEUED_UP

                        viewModel.eventForwarder.sendUploadedPhotosFragmentEvent(
                            UploadedPhotosFragmentEvent.UiEvents.RemovePhoto(photo))
                        viewModel.eventForwarder.sendUploadedPhotosFragmentEvent(
                            UploadedPhotosFragmentEvent.UiEvents.AddPhoto(photo))
                    }.map { true }
                    .doOnError { Timber.e(it) }
            }
        }
    }

    private fun showPhotoDeletedSnackbar(photo: TakenPhoto) {
        val disposable = Single.just(1)
            .observeOn(Schedulers.io())
            .doOnSuccess {
                viewModel.eventForwarder.sendUploadedPhotosFragmentEvent(
                    UploadedPhotosFragmentEvent.UiEvents.RemovePhoto(photo))
            }
            .zipWith(Single.timer(PHOTO_DELETE_DELAY, TimeUnit.MILLISECONDS))
            .flatMap { viewModel.deletePhotoById(photo.id).toSingleDefault(Unit) }
            .doOnError { Timber.e(it) }
            .subscribe()

        compositeDisposable += disposable

        Snackbar.make(rootLayout, getString(R.string.photo_has_been_deleted_snackbar_text), Snackbar.LENGTH_LONG)
            .setDuration(PHOTO_DELETE_DELAY.toInt())
            .setAction(getString(R.string.cancel_snackbar_action_text), {
                viewModel.eventForwarder.sendUploadedPhotosFragmentEvent(
                    UploadedPhotosFragmentEvent.UiEvents.AddPhoto(photo))
                disposable.dispose()
            })
            .show()
    }

    private fun showPhotoAnswerFoundSnackbar() {
        Snackbar.make(rootLayout, getString(R.string.photo_has_been_received_snackbar_text), Snackbar.LENGTH_LONG)
            .setAction(getString(R.string.show_snackbar_action_text), {
                if (viewPager.currentItem != RECEIVED_PHOTO_TAB_INDEX) {
                    viewPager.currentItem = RECEIVED_PHOTO_TAB_INDEX
                }

                compositeDisposable += Single.timer(FRAGMENT_SCROLL_DELAY_MS, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSuccess {
                        viewModel.eventForwarder.sendReceivedPhotosFragmentEvent(
                            ReceivedPhotosFragmentEvent.UiEvents.ScrollToTop())
                    }
                    .doOnError { Timber.e(it) }
                    .subscribe()
            }).show()
    }

    fun showToast(message: String, duration: Int) {
        onShowToast(message, duration)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun bindReceivingService(start: Boolean = true) {
        if (!receivePhotosServiceConnection.isConnected()) {
            Timber.tag(TAG).d("bindReceivingService, start = $start")

            val serviceIntent = Intent(applicationContext, ReceivePhotosService::class.java)
            if (start) {
                startService(serviceIntent)
            }

            bindService(serviceIntent, receivePhotosServiceConnection, Context.BIND_AUTO_CREATE)
        } else {
            Timber.tag(TAG).d("Already connected, force startPhotosReceiving")
            receivePhotosServiceConnection.startPhotosReceiving()
        }
    }

    private fun bindUploadingService(start: Boolean = true) {
        if (!uploadPhotosServiceConnection.isConnected()) {
            Timber.tag(TAG).d("bindUploadingService, start = $start")

            val serviceIntent = Intent(applicationContext, UploadPhotoService::class.java)
            if (start) {
                startService(serviceIntent)
            }

            bindService(serviceIntent, uploadPhotosServiceConnection, Context.BIND_AUTO_CREATE)
        } else {
            Timber.tag(TAG).d("Already connected, force startPhotosUploading")
            uploadPhotosServiceConnection.startPhotosUploading()
        }
    }

    fun showUploadPhotoErrorMessage(errorCode: ErrorCode) {
        showErrorCodeToast(errorCode)
    }

    fun showUnknownErrorMessage(error: Throwable) {
        when (error) {
            is CompositeException -> {
                for (exception in error.exceptions) {
                    Timber.e(error)
                    showToast(error.message, Toast.LENGTH_SHORT)
                }
            }

            else -> {
                Timber.e(error)
                showToast(error.message
                    ?: getString(R.string.unknown_error_exception_text), Toast.LENGTH_SHORT)
            }
        }
    }

    override fun resolveDaggerDependency() {
        activityComponent.inject(this)
    }
}