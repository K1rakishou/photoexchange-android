package com.kirakishou.photoexchange.ui.activity

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.*
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.design.widget.TabLayout
import android.support.v4.app.ActivityCompat
import android.support.v4.view.ViewPager
import android.widget.ImageButton
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
import com.kirakishou.photoexchange.service.FindPhotoAnswerService
import com.kirakishou.photoexchange.service.UploadPhotoService
import com.kirakishou.photoexchange.ui.adapter.MyPhotosAdapter
import com.kirakishou.photoexchange.ui.callback.FindPhotoAnswerCallback
import com.kirakishou.photoexchange.ui.callback.PhotoUploadingCallback
import com.kirakishou.photoexchange.ui.dialog.GpsRationaleDialog
import com.kirakishou.photoexchange.ui.viewstate.AllPhotosActivityViewState
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
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class AllPhotosActivity : BaseActivity(), AllPhotosActivityView, TabLayout.OnTabSelectedListener,
    ViewPager.OnPageChangeListener, PhotoUploadingCallback, FindPhotoAnswerCallback {

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

    @Inject
    lateinit var viewModel: AllPhotosActivityViewModel

    @Inject
    lateinit var permissionManager: PermissionManager

    val activityComponent by lazy {
        (application as PhotoExchangeApplication).applicationComponent
            .plus(AllPhotosActivityModule(this))
    }

    private val tag = "[${this::class.java.simpleName}] "
    private val GPS_DELAY_MS = 1.seconds()
    private val GPS_LOCATION_OBTAINING_MAX_TIMEOUT_MS = 15.seconds()
    private val FRAGMENT_SCROLL_DELAY_MS = 250L
    private val MY_PHOTOS_TAB_INDEX = 0
    private val RECEIVED_PHOTO_TAB_INDEX = 1
    private var handler: Handler? = null

    private var uploadPhotoService: UploadPhotoService? = null
    private var findPhotoAnswerService: FindPhotoAnswerService? = null
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
            .doOnNext { startUploadingService() }
            .doOnError { Timber.e(it) }
            .subscribe()

        compositeDisposable += viewModel.startFindPhotoAnswerServiceSubject
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
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
    }

    override fun onActivityStart() {
        if (handler == null) {
            handler = Handler(Looper.getMainLooper())
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions(this.savedInstanceState)
    }

    override fun onActivityStop() {
        if (handler != null) {
            handler!!.removeCallbacksAndMessages(null)
            handler = null
        }

        uploadPhotoService?.let { srvc ->
            srvc.detachCallback()
            unbindService(uploadServiceConnection)
            uploadPhotoService = null
        }

        findPhotoAnswerService?.let { srvc ->
            srvc.detachCallback()
            unbindService(findServiceConnection)
            findPhotoAnswerService = null
        }
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

                handler?.postDelayed({
                    viewModel.myPhotosFragmentViewStateSubject.onNext(MyPhotosFragmentViewStateEvent.ScrollToTop())
                }, FRAGMENT_SCROLL_DELAY_MS)
            }).show()
    }

    private fun showPhotoAnswerFoundSnackbar() {
        Snackbar.make(rootLayout, "A photo has been received", Snackbar.LENGTH_LONG)
            .setAction("VIEW", {
                if (viewPager.currentItem != RECEIVED_PHOTO_TAB_INDEX) {
                    viewPager.currentItem = RECEIVED_PHOTO_TAB_INDEX
                }

                handler?.postDelayed({
                    viewModel.receivedPhotosFragmentViewStateSubject.onNext(ReceivedPhotosFragmentViewStateEvent.ScrollToTop())
                }, FRAGMENT_SCROLL_DELAY_MS)
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
        if (findPhotoAnswerService == null) {
            Timber.e("startFindingService")

            val serviceIntent = Intent(applicationContext, FindPhotoAnswerService::class.java)
            startService(serviceIntent)
            bindService(serviceIntent, findServiceConnection, Context.BIND_AUTO_CREATE)
        } else {
            Timber.e("FindingService is already running")
        }
    }

    private fun startUploadingService() {
        if (uploadPhotoService == null) {
            Timber.e("startUploadingService")

            val serviceIntent = Intent(applicationContext, UploadPhotoService::class.java)
            startService(serviceIntent)
            bindService(serviceIntent, uploadServiceConnection, Context.BIND_AUTO_CREATE)
        } else {
            Timber.e("UploadingService is already running")
        }
    }

    private fun showUploadPhotoErrorMessage(errorCode: ErrorCode.UploadPhotoErrors) {
        val errorMessage = when (errorCode) {
            is ErrorCode.UploadPhotoErrors.Remote.Ok -> null
            is ErrorCode.UploadPhotoErrors.Remote.UnknownError -> "Unknown error"
            is ErrorCode.UploadPhotoErrors.Remote.BadRequest -> "Bad request error"
            is ErrorCode.UploadPhotoErrors.Remote.DatabaseError -> "Server database error"
            is ErrorCode.UploadPhotoErrors.Local.BadServerResponse -> "Bad server response error"
            is ErrorCode.UploadPhotoErrors.Local.NoPhotoFileOnDisk -> "No photo file on disk error"
            is ErrorCode.UploadPhotoErrors.Local.Timeout -> "Timeout error"
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

    private val uploadServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, _service: IBinder) {
            Timber.tag(tag).d("UploadPhotosService Service connected")

            uploadPhotoService = (_service as UploadPhotoService.UploadPhotosBinder).getService()
            uploadPhotoService?.attachCallback(WeakReference(this@AllPhotosActivity))
            uploadPhotoService?.startPhotosUploading()
        }

        override fun onServiceDisconnected(className: ComponentName) {
            Timber.tag(tag).d("UploadPhotosService Service disconnected")

            uploadPhotoService?.detachCallback()
            unbindService(this)
            uploadPhotoService = null
        }
    }

    private val findServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, _service: IBinder) {
            Timber.tag(tag).d("FindPhotoAnswerService Service connected")

            findPhotoAnswerService = (_service as FindPhotoAnswerService.FindPhotoAnswerBinder).getService()
            findPhotoAnswerService?.attachCallback(WeakReference(this@AllPhotosActivity))
            findPhotoAnswerService?.startSearchingForPhotoAnswers()
        }

        override fun onServiceDisconnected(className: ComponentName) {
            Timber.tag(tag).d("FindPhotoAnswerService Service disconnected")

            findPhotoAnswerService?.detachCallback()
            unbindService(this)
            findPhotoAnswerService = null
        }
    }
}