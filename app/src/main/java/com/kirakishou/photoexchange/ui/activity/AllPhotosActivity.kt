package com.kirakishou.photoexchange.ui.activity

import android.Manifest
import android.arch.lifecycle.ViewModelProviders
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
import com.kirakishou.photoexchange.base.BaseActivity
import com.kirakishou.photoexchange.di.component.AllPhotosActivityComponent
import com.kirakishou.photoexchange.di.component.ApplicationComponent
import com.kirakishou.photoexchange.di.module.AllPhotosActivityModule
import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.helper.location.MyLocationManager
import com.kirakishou.photoexchange.helper.location.RxLocationManager
import com.kirakishou.photoexchange.helper.permission.PermissionManager
import com.kirakishou.photoexchange.mvp.model.MyPhoto
import com.kirakishou.photoexchange.mvp.model.PhotoUploadingEvent
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import com.kirakishou.photoexchange.service.UploadPhotoService
import com.kirakishou.photoexchange.mvp.view.AllPhotosActivityView
import com.kirakishou.photoexchange.mvp.viewmodel.AllPhotosActivityViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.factory.AllPhotosActivityViewModelFactory
import com.kirakishou.photoexchange.ui.callback.ActivityCallback
import com.kirakishou.photoexchange.ui.dialog.GpsRationaleDialog
import com.kirakishou.photoexchange.ui.widget.FragmentTabsPager
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.rx2.asSingle
import kotlinx.coroutines.experimental.rx2.awaitFirstOrNull
import kotlinx.coroutines.experimental.withTimeoutOrNull
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class AllPhotosActivity : BaseActivity<AllPhotosActivityViewModel>(), AllPhotosActivityView, ActivityCallback,
    TabLayout.OnTabSelectedListener, ViewPager.OnPageChangeListener {

    @BindView(R.id.iv_close_button)
    lateinit var ivCloseActivityButton: ImageButton

    @BindView(R.id.sliding_tab_layout)
    lateinit var tabLayout: TabLayout

    @BindView(R.id.view_pager)
    lateinit var viewPager: ViewPager

    @BindView(R.id.take_photo_button)
    lateinit var takePhotoButton: FloatingActionButton

    @Inject
    lateinit var viewModelFactory: AllPhotosActivityViewModelFactory

    @Inject
    lateinit var coroutinesPool: CoroutineThreadPoolProvider

    @Inject
    lateinit var permissionManager: PermissionManager

    private val tag = "[${this::class.java.simpleName}] "
    private var service: UploadPhotoService? = null

    private val adapter = FragmentTabsPager(supportFragmentManager)
    private val onServiceConnectedSubject = PublishSubject.create<Unit>()
    private val locationManager by lazy { MyLocationManager(applicationContext) }

    override fun initViewModel(): AllPhotosActivityViewModel? {
        return ViewModelProviders.of(this, viewModelFactory).get(AllPhotosActivityViewModel::class.java)
    }

    override fun getContentView(): Int = R.layout.activity_all_photos

    override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
        initViews()
        checkPermissions()

        compositeDisposable += onServiceConnectedSubject
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { service?.attachCallback(this@AllPhotosActivity) }
            .doOnNext {
                service?.startPhotosUploading()
            }
            .subscribe()
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
        getViewModel().startUploadingPhotosService(isGranted)
    }

    private fun showGpsRationaleDialog() {
        GpsRationaleDialog().show(this, {
            checkPermissions()
        }, {
            onPermissionsCallback(false)
        })
    }

    override fun getCurrentLocation(): Single<LonLat> {
        return async {
            if (!locationManager.isGpsEnabled()) {
                return@async LonLat.empty()
            }

            return@async withTimeoutOrNull(15, TimeUnit.SECONDS) {
                RxLocationManager.start(locationManager).awaitFirstOrNull()
            } ?: LonLat.empty()
        }.asSingle(coroutinesPool.BG())
    }

    private fun initTabs() {
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_title_uploaded_photos)))
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_title_received_photos)))
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_title_gallery)))
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

    override fun startUploadingService() {
        val serviceIntent = Intent(this, UploadPhotoService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onUploadingEvent(event: PhotoUploadingEvent) {
        adapter.getMyPhotosFragment()?.onUploadingEvent(event)
    }

    override fun onActivityDestroy() {
        getViewModel().detach()

        service?.let { srvc ->
            srvc.detachCallback()
            unbindService(connection)
            service = null
        }
    }

    override fun showToast(message: String, duration: Int) {
        onShowToast(message, duration)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    //MyPhotosFragmentCallbacks

    override fun onUploadedPhotosRetrieved(uploadedPhotos: List<MyPhoto>) {
        adapter.getMyPhotosFragment()?.onUploadedPhotos(uploadedPhotos)
    }

    override fun resolveDaggerDependency() {
        (application as PhotoExchangeApplication).applicationComponent
            .plus(AllPhotosActivityModule(this))
            .inject(this)
    }

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, _service: IBinder) {
            Timber.e("Service connected")

            service = (_service as UploadPhotoService.UploadPhotosBinder).getService()
            onServiceConnectedSubject.onNext(Unit)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Timber.e("Service disconnected")

            unbindService(this)
            service = null
        }
    }
}