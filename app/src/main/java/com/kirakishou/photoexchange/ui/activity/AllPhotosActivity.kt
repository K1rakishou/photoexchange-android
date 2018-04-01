package com.kirakishou.photoexchange.ui.activity

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.TabLayout
import android.support.v4.app.ActivityCompat
import android.support.v4.view.ViewPager
import android.widget.ImageButton
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.di.module.AllPhotosActivityModule
import com.kirakishou.photoexchange.helper.location.MyLocationManager
import com.kirakishou.photoexchange.helper.location.RxLocationManager
import com.kirakishou.photoexchange.helper.permission.PermissionManager
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.PhotoUploadingEvent
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import com.kirakishou.photoexchange.mvp.view.AllPhotosActivityView
import com.kirakishou.photoexchange.mvp.viewmodel.AllPhotosActivityViewModel
import com.kirakishou.photoexchange.service.UploadPhotoService
import com.kirakishou.photoexchange.ui.adapter.MyPhotosAdapter
import com.kirakishou.photoexchange.ui.callback.PhotoUploadingCallback
import com.kirakishou.photoexchange.ui.dialog.GpsRationaleDialog
import com.kirakishou.photoexchange.ui.widget.FragmentTabsPager
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class AllPhotosActivity : BaseActivity(), AllPhotosActivityView, TabLayout.OnTabSelectedListener,
    ViewPager.OnPageChangeListener, PhotoUploadingCallback {

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

    private val tag = "[${this::class.java.simpleName}] "
    private var service: UploadPhotoService? = null

    val activityComponent by lazy {
        (application as PhotoExchangeApplication).applicationComponent
            .plus(AllPhotosActivityModule(this))
    }

    private val GPS_LOCATION_OBTAINING_MAX_TIMEOUT_SECONDS = 15L
    private val adapter = FragmentTabsPager(supportFragmentManager)
    private val locationManager by lazy { MyLocationManager(applicationContext) }

    override fun getContentView(): Int = R.layout.activity_all_photos

    override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
        initRx()
        initViews()
        checkPermissions()
    }

    private fun initRx() {
        compositeDisposable += viewModel.stopUploadingProcessSubject
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { stop -> if (stop) service?.stopUploadingProcess() else service?.resumeUploadingProcess() }
            .subscribe()

        compositeDisposable += viewModel.adapterButtonClickSubject
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { _ -> startUploadingService() }
            .subscribe()
    }

    override fun onActivityDestroy() {
        service?.let { srvc ->
            srvc.detachCallback()
            unbindService(connection)
            service = null
        }
    }

    private fun initViews() {
        initTabs()

        ivCloseActivityButton.setOnClickListener {
            finish()
        }

        takePhotoButton.setOnClickListener {
            finish()
        }
    }

    private fun checkPermissions() {
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
                    showGpsRationaleDialog()
                    return@askForPermission
                }
            }

            onPermissionsCallback(granted)
        }
    }

    private fun onPermissionsCallback(isGranted: Boolean) {
        compositeDisposable += viewModel.startUploadingPhotosService(isGranted)
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess { startUploadingService() }
            .subscribe()
    }

    private fun showGpsRationaleDialog() {
        GpsRationaleDialog().show(this, {
            checkPermissions()
        }, {
            onPermissionsCallback(false)
        })
    }

    override fun getCurrentLocation(): Single<LonLat> {
        return Single.fromCallable {
            if (!locationManager.isGpsEnabled()) {
                return@fromCallable LonLat.empty()
            }

            return@fromCallable RxLocationManager.start(locationManager)
                .timeout(GPS_LOCATION_OBTAINING_MAX_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .onErrorReturnItem(LonLat.empty())
                .blockingFirst()
        }
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

    override fun onUploadingEvent(event: PhotoUploadingEvent) {
        viewModel.forwardUploadPhotoEvent(event)
    }

    override fun showToast(message: String, duration: Int) {
        onShowToast(message, duration)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun startUploadingService() {
        if (service == null) {
            val serviceIntent = Intent(applicationContext, UploadPhotoService::class.java)
            startService(serviceIntent)
            bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
        } else {
            service?.resumeUploadingProcess()
            service?.startPhotosUploading()
        }
    }

    override fun resolveDaggerDependency() {
        activityComponent.inject(this)
    }

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, _service: IBinder) {
            Timber.tag(tag).d("Service connected")

            service = (_service as UploadPhotoService.UploadPhotosBinder).getService()
            service?.attachCallback(WeakReference(this@AllPhotosActivity))
            service?.startPhotosUploading()
        }

        override fun onServiceDisconnected(className: ComponentName) {
            Timber.tag(tag).d("Service disconnected")

            service?.detachCallback()
            unbindService(this)
            service = null
        }
    }
}