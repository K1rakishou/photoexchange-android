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
import com.kirakishou.photoexchange.di.module.AllPhotosActivityModule
import com.kirakishou.photoexchange.helper.extension.debounceClicks
import com.kirakishou.photoexchange.helper.extension.seconds
import com.kirakishou.photoexchange.helper.location.MyLocationManager
import com.kirakishou.photoexchange.helper.location.RxLocationManager
import com.kirakishou.photoexchange.helper.permission.PermissionManager
import com.kirakishou.photoexchange.mvp.model.PhotoFindEvent
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.PhotoUploadEvent
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import com.kirakishou.photoexchange.mvp.view.AllPhotosActivityView
import com.kirakishou.photoexchange.mvp.viewmodel.AllPhotosActivityViewModel
import com.kirakishou.photoexchange.service.FindPhotoAnswerServiceConnection
import com.kirakishou.photoexchange.service.FindPhotoAnswerService
import com.kirakishou.photoexchange.service.UploadPhotoService
import com.kirakishou.photoexchange.service.UploadPhotoServiceConnection
import com.kirakishou.photoexchange.ui.adapter.MyPhotosAdapter
import com.kirakishou.photoexchange.ui.callback.FindPhotoAnswerCallback
import com.kirakishou.photoexchange.ui.callback.PhotoUploadingCallback
import com.kirakishou.photoexchange.ui.dialog.GpsRationaleDialog
import com.kirakishou.photoexchange.ui.viewstate.AllPhotosActivityViewState
import com.kirakishou.photoexchange.ui.viewstate.AllPhotosActivityViewStateEvent
import com.kirakishou.photoexchange.ui.viewstate.MyPhotosFragmentViewStateEvent
import com.kirakishou.photoexchange.ui.viewstate.ReceivedPhotosFragmentViewStateEvent
import com.kirakishou.photoexchange.ui.widget.FragmentTabsPager
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.exceptions.CompositeException
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class AllPhotosActivity : BaseActivity(), AllPhotosActivityView, TabLayout.OnTabSelectedListener,
    ViewPager.OnPageChangeListener, PhotoUploadingCallback, FindPhotoAnswerCallback, PopupMenu.OnMenuItemClickListener {

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
    lateinit var viewModel: AllPhotosActivityViewModel

    @Inject
    lateinit var permissionManager: PermissionManager

    val activityComponent by lazy {
        (application as PhotoExchangeApplication).applicationComponent
            .plus(AllPhotosActivityModule(this))
    }

    private val tag = "AllPhotosActivity"
    private val GPS_DELAY_MS = 1.seconds()
    private val GPS_LOCATION_OBTAINING_MAX_TIMEOUT_MS = 15.seconds()
    private val FRAGMENT_SCROLL_DELAY_MS = 250L
    private val MY_PHOTOS_TAB_INDEX = 0
    private val RECEIVED_PHOTO_TAB_INDEX = 1

    private var findServiceConnection = FindPhotoAnswerServiceConnection(this)
    private var uploadServiceConnection = UploadPhotoServiceConnection(this)

    private val adapter = FragmentTabsPager(supportFragmentManager)
    private val locationManager by lazy { MyLocationManager(applicationContext) }
    private var savedInstanceState: Bundle? = null
    private var viewState = AllPhotosActivityViewState()

    override fun getContentView(): Int = R.layout.activity_all_photos

    override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
        this.savedInstanceState = savedInstanceState
    }

    override fun onInitRx() {
        compositeDisposable += viewModel.startPhotoUploadingServiceSubject
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .filter { !uploadServiceConnection.isConnected() }
            .doOnNext { startUploadingService() }
            .doOnError { Timber.e(it) }
            .subscribe()

        compositeDisposable += viewModel.startFindPhotoAnswerServiceSubject
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .filter { !findServiceConnection.isConnected() }
            .doOnNext { startFindingService() }
            .doOnError { Timber.e(it) }
            .subscribe()

        compositeDisposable += RxView.clicks(ivCloseActivityButton)
            .subscribeOn(AndroidSchedulers.mainThread())
            .debounceClicks()
            .doOnNext { finish() }
            .subscribe()

        compositeDisposable += RxView.clicks(takePhotoButton)
            .subscribeOn(AndroidSchedulers.mainThread())
            .debounceClicks()
            .doOnNext { finish() }
            .subscribe()

        compositeDisposable += RxView.clicks(menuButton)
            .subscribeOn(AndroidSchedulers.mainThread())
            .debounceClicks()
            .doOnNext { createMenu() }
            .subscribe()

        compositeDisposable += viewModel.allPhotosActivityViewStateSubject
            .subscribeOn(AndroidSchedulers.mainThread())
            .doOnNext { onViewStateChanged(it) }
            .subscribe()
    }

    override fun onActivityStart() {
        viewModel.setView(this)
    }

    override fun onResume() {
        super.onResume()
        checkPermissions(this.savedInstanceState)
    }

    override fun onActivityStop() {
        findServiceConnection.onFindingServiceDisconnected()
        uploadServiceConnection.onUploadingServiceDisconnected()

        viewModel.clearView()
    }

    override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)

        viewState.lastOpenedTab = viewPager.currentItem
        viewState.saveToBundle(outState)
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

    private fun onPermissionsCallback(savedInstanceState: Bundle?, isGranted: Boolean) {
        initViews()
        restoreMyPhotosFragmentFromViewState(savedInstanceState)

        viewModel.checkShouldStartPhotoUploadingService(isGranted)
    }

    private fun restoreMyPhotosFragmentFromViewState(savedInstanceState: Bundle?) {
        viewState = AllPhotosActivityViewState().also {
            it.loadFromBundle(savedInstanceState)
        }

        if (viewState.lastOpenedTab != 0) {
            viewPager.currentItem = viewState.lastOpenedTab
        }
    }

    private fun initViews() {
        initTabs()
    }

    override fun getCurrentLocation(): Single<LonLat> {
        val resultSingle = Single.fromCallable {
            if (!locationManager.isGpsEnabled()) {
                return@fromCallable LonLat.empty()
            }

            return@fromCallable RxLocationManager.start(locationManager)
                .observeOn(Schedulers.io())
                .timeout(GPS_LOCATION_OBTAINING_MAX_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .onErrorReturnItem(LonLat.empty())
                .blockingFirst()
        }

        return resultSingle
            .delay(GPS_DELAY_MS, TimeUnit.MILLISECONDS)
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

            else -> throw IllegalArgumentException("Unknown menu item id ${item.itemId}")
        }

        return true
    }

    private fun onViewStateChanged(viewStateEvent: AllPhotosActivityViewStateEvent) {
        when (viewStateEvent) {
            else -> throw IllegalArgumentException("Unknown MyPhotosFragmentViewStateEvent $viewStateEvent")
        }
    }

    override fun onUploadPhotosEvent(event: PhotoUploadEvent) {
        viewModel.forwardUploadPhotoEvent(event)

        when (event) {
            is PhotoUploadEvent.OnUploaded -> {
                showPhotoUploadedSnackbar()
            }

            is PhotoUploadEvent.OnFailedToUpload -> {
                showUploadPhotoErrorMessage(event.errorCode)
            }

            is PhotoUploadEvent.OnUnknownError -> {
                showUnknownErrorMessage(event.error)
            }

            is PhotoUploadEvent.OnEnd -> {
                viewModel.checkShouldStartFindPhotoAnswersService()
            }
        }
    }

    override fun onPhotoFindEvent(event: PhotoFindEvent) {
        viewModel.forwardPhotoFindEvent(event)

        when (event) {
            is PhotoFindEvent.OnPhotoAnswerFound -> {
                viewModel.forwardUploadPhotoEvent(PhotoUploadEvent.OnFoundPhotoAnswer(event.photoId))
                showPhotoAnswerFoundSnackbar()
            }
        }
    }

    override fun handleMyPhotoFragmentAdapterButtonClicks(adapterButtonsClickEvent: MyPhotosAdapter.MyPhotosAdapterButtonClickEvent): Observable<Boolean> {
        return when (adapterButtonsClickEvent) {
            is MyPhotosAdapter.MyPhotosAdapterButtonClickEvent.DeleteButtonClick -> {
                Observable.fromCallable { viewModel.deletePhotoById(adapterButtonsClickEvent.photo.id).blockingAwait() }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext {
                        viewModel.myPhotosFragmentViewStateSubject.onNext(MyPhotosFragmentViewStateEvent.RemovePhotoById(adapterButtonsClickEvent.photo.id))
                    }
                    .map { false }
            }

            is MyPhotosAdapter.MyPhotosAdapterButtonClickEvent.RetryButtonClick -> {
                Observable.fromCallable { viewModel.changePhotoState(adapterButtonsClickEvent.photo.id, PhotoState.PHOTO_QUEUED_UP).blockingAwait() }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext {
                        val photo = adapterButtonsClickEvent.photo
                        photo.photoState = PhotoState.PHOTO_QUEUED_UP

                        viewModel.myPhotosFragmentViewStateSubject.onNext(MyPhotosFragmentViewStateEvent.RemovePhotoById(photo.id))
                        viewModel.myPhotosFragmentViewStateSubject.onNext(MyPhotosFragmentViewStateEvent.AddPhoto(photo))
                    }.map { true }
            }
        }
    }

    private fun showPhotoUploadedSnackbar() {
        Snackbar.make(rootLayout, "A photo has been uploaded", Snackbar.LENGTH_LONG)
            .setAction("VIEW", {
                if (viewPager.currentItem != MY_PHOTOS_TAB_INDEX) {
                    viewPager.currentItem = MY_PHOTOS_TAB_INDEX
                }

                compositeDisposable += Single.timer(FRAGMENT_SCROLL_DELAY_MS, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSuccess { viewModel.myPhotosFragmentViewStateSubject.onNext(MyPhotosFragmentViewStateEvent.ScrollToTop()) }
                    .subscribe()
            }).show()
    }

    private fun showPhotoAnswerFoundSnackbar() {
        Snackbar.make(rootLayout, "A photo has been received", Snackbar.LENGTH_LONG)
            .setAction("VIEW", {
                if (viewPager.currentItem != RECEIVED_PHOTO_TAB_INDEX) {
                    viewPager.currentItem = RECEIVED_PHOTO_TAB_INDEX
                }

                compositeDisposable += Single.timer(FRAGMENT_SCROLL_DELAY_MS, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSuccess { viewModel.receivedPhotosFragmentViewStateSubject.onNext(ReceivedPhotosFragmentViewStateEvent.ScrollToTop()) }
                    .subscribe()
            }).show()
    }

    override fun showToast(message: String, duration: Int) {
        onShowToast(message, duration)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun startFindingService() {
        Timber.tag(tag).d("startFindingService")

        val serviceIntent = Intent(applicationContext, FindPhotoAnswerService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, findServiceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun startUploadingService() {
        Timber.tag(tag).d("startUploadingService")

        val serviceIntent = Intent(applicationContext, UploadPhotoService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, uploadServiceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun showUploadPhotoErrorMessage(errorCode: ErrorCode) {
        val errorMessage = when (errorCode) {
            is ErrorCode.UploadPhotoErrors.Remote.Ok,
            is ErrorCode.GetPhotoAnswersErrors.Remote.Ok,
            is ErrorCode.GalleryPhotosErrors.Remote.Ok,
            is ErrorCode.FavouritePhotoErrors.Remote.Ok,
            is ErrorCode.ReportPhotoErrors.Remote.Ok -> null

            is ErrorCode.UploadPhotoErrors.Remote.UnknownError,
            is ErrorCode.GetPhotoAnswersErrors.Remote.UnknownError,
            is ErrorCode.GalleryPhotosErrors.Remote.UnknownError,
            is ErrorCode.FavouritePhotoErrors.Remote.UnknownError,
            is ErrorCode.ReportPhotoErrors.Remote.UnknownError -> "Unknown error"

            is ErrorCode.UploadPhotoErrors.Remote.BadRequest,
            is ErrorCode.GetPhotoAnswersErrors.Remote.BadRequest,
            is ErrorCode.GalleryPhotosErrors.Remote.BadRequest,
            is ErrorCode.FavouritePhotoErrors.Remote.BadRequest,
            is ErrorCode.ReportPhotoErrors.Remote.BadRequest -> "Bad request error"

            is ErrorCode.UploadPhotoErrors.Local.Timeout,
            is ErrorCode.GetPhotoAnswersErrors.Local.Timeout,
            is ErrorCode.GalleryPhotosErrors.Local.Timeout,
            is ErrorCode.FavouritePhotoErrors.Local.Timeout,
            is ErrorCode.ReportPhotoErrors.Local.Timeout -> "Operation timeout error"

            is ErrorCode.UploadPhotoErrors.Remote.DatabaseError,
            is ErrorCode.GetPhotoAnswersErrors.Remote.DatabaseError -> "Server database error"

            is ErrorCode.UploadPhotoErrors.Local.BadServerResponse,
            is ErrorCode.GetPhotoAnswersErrors.Local.BadServerResponse,
            is ErrorCode.GalleryPhotosErrors.Local.BadServerResponse,
            is ErrorCode.ReportPhotoErrors.Local.BadServerResponse,
            is ErrorCode.FavouritePhotoErrors.Local.BadServerResponse -> "Bad server response error"

            is ErrorCode.UploadPhotoErrors.Local.NoPhotoFileOnDisk -> "No photo file on disk error"
            is ErrorCode.GetPhotoAnswersErrors.Remote.NoPhotosInRequest -> "No photos were selected error"
            is ErrorCode.GetPhotoAnswersErrors.Remote.TooManyPhotosRequested -> "Too many photos requested error"
            is ErrorCode.GetPhotoAnswersErrors.Remote.NoPhotosToSendBack -> "No photos to send back"
            is ErrorCode.GetPhotoAnswersErrors.Remote.NotEnoughPhotosUploaded -> "Upload more photos first"
            is ErrorCode.FavouritePhotoErrors.Remote.AlreadyFavourited -> "You have already added this photo to favourites"
            is ErrorCode.ReportPhotoErrors.Remote.AlreadyReported -> "You have already reported this photo"

            is ErrorCode.TakePhotoErrors.UnknownError,
            is ErrorCode.TakePhotoErrors.Ok,
            is ErrorCode.TakePhotoErrors.CameraIsNotAvailable,
            is ErrorCode.TakePhotoErrors.CameraIsNotStartedException,
            is ErrorCode.TakePhotoErrors.TimeoutException,
            is ErrorCode.TakePhotoErrors.DatabaseError,
            is ErrorCode.TakePhotoErrors.CouldNotTakePhoto -> null
        }

        errorMessage?.let { msg ->
            showToast(msg, Toast.LENGTH_SHORT)
        }
    }

    private fun showUnknownErrorMessage(error: Throwable) {
        when (error) {
            is CompositeException -> {
                for (exception in error.exceptions) {
                    Timber.e(error)
                    showToast(error.message, Toast.LENGTH_SHORT)
                }
            }

            else -> {
                Timber.e(error)
                showToast(error.message ?: "Unknown error (exception)", Toast.LENGTH_SHORT)
            }
        }
    }

    override fun resolveDaggerDependency() {
        activityComponent.inject(this)
    }
}