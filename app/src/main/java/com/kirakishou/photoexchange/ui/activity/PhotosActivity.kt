package com.kirakishou.photoexchange.ui.activity

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.PersistableBundle
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.viewpager.widget.ViewPager
import butterknife.BindView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.jakewharton.rxbinding2.view.RxView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.di.module.activity.PhotosActivityModule
import com.kirakishou.photoexchange.helper.extension.debounceClicks
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.helper.intercom.IntercomListener
import com.kirakishou.photoexchange.helper.intercom.StateEventListener
import com.kirakishou.photoexchange.helper.intercom.event.GalleryFragmentEvent
import com.kirakishou.photoexchange.helper.intercom.event.PhotosActivityEvent
import com.kirakishou.photoexchange.helper.intercom.event.ReceivedPhotosFragmentEvent
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.helper.permission.PermissionManager
import com.kirakishou.photoexchange.usecases.CheckFirebaseAvailabilityUseCase
import com.kirakishou.photoexchange.mvrx.model.NewReceivedPhoto
import com.kirakishou.photoexchange.mvrx.viewmodel.PhotosActivityViewModel
import com.kirakishou.photoexchange.service.*
import com.kirakishou.photoexchange.ui.callback.PhotoUploadingServiceCallback
import com.kirakishou.photoexchange.ui.callback.ReceivePhotosServiceCallback
import com.kirakishou.photoexchange.ui.dialog.*
import com.kirakishou.photoexchange.ui.fragment.GalleryFragment
import com.kirakishou.photoexchange.ui.fragment.ReceivedPhotosFragment
import com.kirakishou.photoexchange.ui.fragment.UploadedPhotosFragment
import com.kirakishou.photoexchange.ui.viewstate.PhotosActivityViewState
import com.kirakishou.photoexchange.ui.widget.FragmentTabsPager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.lang.IllegalStateException
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class PhotosActivity : BaseActivity(), PhotoUploadingServiceCallback, ReceivePhotosServiceCallback,
  PopupMenu.OnMenuItemClickListener, StateEventListener<PhotosActivityEvent>, IntercomListener {

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

  val activityComponent by lazy {
    (application as PhotoExchangeApplication).applicationComponent
      .plus(PhotosActivityModule(this))
  }

  private val TAG = "PhotosActivity"
  private val FRAGMENT_SWITCH_ANIMATION_DELAY_MS = 250L
  private val SWITCH_FRAGMENT_DELAY = 250L
  private val NOTIFICATION_CANCEL_DELAY_MS = 25L

  private val UPLOADED_PHOTOS_TAB_INDEX = 0
  private val RECEIVED_PHOTOS_TAB_INDEX = 1
  private val GALLERY_PHOTOS_TAB_INDEX = 2

  private val onTabSelectedSubject = PublishSubject.create<Int>()

  private lateinit var receivePhotosServiceConnection: ReceivePhotosServiceConnection
  private lateinit var uploadPhotosServiceConnection: UploadPhotoServiceConnection

  private val adapter = FragmentTabsPager(supportFragmentManager)
  private var viewState = PhotosActivityViewState()
  private val permissionManager = PermissionManager()

  /**
   * When we receive a push notification we show a notification and send a broadcast to this activity.
   * If this activity is dead - then the user will see the notification.
   * But if it's not then we don't need to show the notification. What we need to do instead is to
   * automatically add this photo to receivedPhotos and update uploadedPhoto with the same name.
   * */
  private val notificationBroadcastReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      Timber.tag(TAG).d("Received new broadcast with action ${intent.action}")

      if (intent.action == null || intent.action != newPhotoReceivedAction) {
        return
      }

      val bundle = intent.extras?.getBundle(PhotosActivity.receivedPhotoExtra)
      val newReceivedPhoto = NewReceivedPhoto.fromBundle(bundle)
      if (newReceivedPhoto == null) {
        throw IllegalStateException("newReceivedPhoto should not be null!")
      }

      launch {
        //add slight delay here to ensure that "notificationManager.cancel" is called AFTER
        //the notification has been shown
        delay(NOTIFICATION_CANCEL_DELAY_MS)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(PushNotificationReceiverService.NOTIFICATION_ID)

        onNewPhotoNotification(newReceivedPhoto)
      }
    }
  }

  override fun getContentView(): Int = R.layout.activity_all_photos

  override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
    viewState = PhotosActivityViewState().also { it.loadFromBundle(savedInstanceState) }

    initRx()
    onNewIntent(intent)
    initViews()
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)

    val bundle = intent.extras?.getBundle(PhotosActivity.receivedPhotoExtra)
    val newReceivedPhoto = NewReceivedPhoto.fromBundle(bundle)

    if (newReceivedPhoto != null) {
      onNewPhotoNotification(newReceivedPhoto)
    }
  }

  override fun onActivityStart() {
    registerReceiver(notificationBroadcastReceiver, IntentFilter(newPhotoReceivedAction))

    receivePhotosServiceConnection = ReceivePhotosServiceConnection(this)
    uploadPhotosServiceConnection = UploadPhotoServiceConnection(this)
  }

  override fun onActivityStop() {
    receivePhotosServiceConnection.onReceivedPhotosServiceDisconnected()
    uploadPhotosServiceConnection.onUploadingServiceDisconnected()

    unregisterReceiver(notificationBroadcastReceiver)
  }

  private fun onNewPhotoNotification(newReceivedPhoto: NewReceivedPhoto) {
    launch { viewModel.addReceivedPhoto(newReceivedPhoto) }
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
      .doOnNext {
        launch { prepareToTakePhoto() }
      }
      .doOnError { Timber.e(it) }
      .subscribe()

    compositeDisposable += RxView.clicks(menuButton)
      .subscribeOn(AndroidSchedulers.mainThread())
      .debounceClicks()
      .doOnNext { createMenu() }
      .doOnError { Timber.e(it) }
      .subscribe()

    compositeDisposable += onTabSelectedSubject
      .debounce(1, TimeUnit.SECONDS)
      .subscribe({ tabIndex ->
        when (tabIndex) {
          UPLOADED_PHOTOS_TAB_INDEX -> {
            viewModel.intercom.tell<UploadedPhotosFragment>()
              .that(UploadedPhotosFragmentEvent.GeneralEvents.OnTabSelected)
          }
          RECEIVED_PHOTOS_TAB_INDEX -> {
            viewModel.intercom.tell<ReceivedPhotosFragment>()
              .that(ReceivedPhotosFragmentEvent.GeneralEvents.OnTabSelected)
          }
          GALLERY_PHOTOS_TAB_INDEX -> {
            viewModel.intercom.tell<GalleryFragment>()
              .that(GalleryFragmentEvent.GeneralEvents.OnTabSelected)
          }
        }
      })

    compositeDisposable += viewModel.intercom.photosActivityEvents.listen()
      .subscribe({ event ->
        launch { onStateEvent(event) }
      })
  }

  private fun showGpsRationaleDialog() {
    val positiveCallback = WeakReference({
      launch { checkPermissions() }
      Unit
    })
    val negativeCallback = WeakReference({
      startTakenPhotoActivity()
    })

    GpsRationaleDialog().show(this, positiveCallback, negativeCallback)
  }

  private fun showAppCannotWorkWithoutCameraPermissionDialog() {
    AppCannotWorkWithoutCameraPermissionDialog().show(this) {
      finish()
    }
  }

  private suspend fun showCameraRationaleDialog() {
    val positiveCallback = WeakReference({
      launch { prepareToTakePhoto() }
      Unit
    })
    val negativeCallback = WeakReference({
      finish()
    })

    CameraRationaleDialog().show(this, positiveCallback, negativeCallback)
  }

  private suspend fun prepareToTakePhoto() {
    //TODO: add new activity and GDPR dialog should be shown there
    //TODO: add GDPR dialog
    //TODO: disable Crashlytics if user didn't give us their permission to send crashlogs

    //2. Check firebase availability, show no firebase dialog if necessary
    val shown = try {
      isFirebaseAvailableOrDialogAlreadyShown()
    } catch (error: Throwable) {
      Timber.tag(TAG).e(error, "Error while trying to check firebase availability")
      return
    }

    if (!shown) {
      val positiveCallback = WeakReference({
        checkPermissions()
        Unit
      })

      FirebaseNotAvailableDialog().show(this, positiveCallback)
    } else {
      checkPermissions()
    }
  }

  private fun checkPermissions() {
    launch {
      //3. Check permissions, show dialogs if necessary
      when (val result = requestPermissions()) {
        PermissionRequestResult.NotGranted,
        PermissionRequestResult.Granted -> {
          val granted = result == PermissionRequestResult.Granted

          viewModel.updateGpsPermissionGranted(granted)
          startTakenPhotoActivity()
        }
        PermissionRequestResult.ShowRationaleForCamera -> {
          showCameraRationaleDialog()
        }
        PermissionRequestResult.ShowRationaleAppCannotWorkWithoutCamera -> {
          showAppCannotWorkWithoutCameraPermissionDialog()
        }
        PermissionRequestResult.ShowRationaleForGps -> {
          showGpsRationaleDialog()
        }
      }

      Unit
    }
  }

  private suspend fun isFirebaseAvailableOrDialogAlreadyShown(): Boolean {
    return viewModel.checkFirebaseAvailability() !=
      CheckFirebaseAvailabilityUseCase.FirebaseAvailabilityResult.NotAvailable
  }

  //request permissions for camera and location
  private suspend fun requestPermissions(): PermissionRequestResult {
    val requestedPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)

    return suspendCoroutine { continuation ->
      permissionManager.askForPermission(this, requestedPermissions) { permissions, grantResults ->
        val cameraIndex = permissions.indexOf(Manifest.permission.CAMERA)
        if (cameraIndex == -1) {
          continuation.resumeWithException(
            RuntimeException("Couldn't find Manifest.permission.CAMERA in result permissions")
          )
          return@askForPermission
        }

        val gpsIndex = permissions.indexOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (gpsIndex == -1) {
          continuation.resumeWithException(
            RuntimeException("Couldn't find Manifest.permission.ACCESS_FINE_LOCATION in result permissions")
          )
          return@askForPermission
        }

        if (grantResults[cameraIndex] == PackageManager.PERMISSION_DENIED) {
          if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            continuation.resume(PermissionRequestResult.ShowRationaleForCamera)
          } else {
            Timber.tag(TAG).d("Could not obtain camera permission")
            continuation.resume(PermissionRequestResult.ShowRationaleAppCannotWorkWithoutCamera)
          }
        } else {
          var granted = true

          if (grantResults[gpsIndex] == PackageManager.PERMISSION_DENIED) {
            granted = false

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
              continuation.resume(PermissionRequestResult.ShowRationaleForGps)
              return@askForPermission
            }
          }

          if (granted) {
            continuation.resume(PermissionRequestResult.Granted)
          } else {
            continuation.resume(PermissionRequestResult.NotGranted)
          }
        }
      }
    }
  }

  private fun startTakenPhotoActivity() {
    val intent = Intent(this, TakePhotoActivity::class.java)
    startActivityForResult(intent, TakePhotoActivity.TAKE_PHOTO_REQUEST_CODE)
  }

  private fun initViews() {
    initTabs()

    if (viewState.lastOpenedTab != 0) {
      switchToTab(viewState.lastOpenedTab)
    }
  }

  private fun initTabs() {
    tabLayout.removeAllTabs()
    tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.photos_activity_tab_title_uploaded_photos)))
    tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.photos_activity_tab_title_received_photos)))
    tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.photos_activity_tab_title_gallery)))
    tabLayout.tabGravity = TabLayout.GRAVITY_FILL

    viewPager.adapter = adapter
    viewPager.offscreenPageLimit = 1

    viewPager.clearOnPageChangeListeners()
    tabLayout.clearOnTabSelectedListeners()

    viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
      override fun onPageScrollStateChanged(state: Int) {
      }

      override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        tabLayout.setScrollPosition(position, positionOffset, true)
      }

      override fun onPageSelected(position: Int) {
        viewPager.currentItem = position
        onTabSelectedSubject.onNext(position)
      }
    })

    tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
      override fun onTabReselected(tab: TabLayout.Tab) {
        viewPager.currentItem = tab.position
      }

      override fun onTabUnselected(tab: TabLayout.Tab) {
      }

      override fun onTabSelected(tab: TabLayout.Tab) {
        viewPager.currentItem = tab.position
        onTabSelectedSubject.onNext(tab.position)
      }
    })
  }

  private fun createMenu() {
    val popupMenu = PopupMenu(this, menuButton)
    popupMenu.setOnMenuItemClickListener(this)
    popupMenu.menu.add(1, R.id.settings_item, 1, resources.getString(R.string.photos_activity_settings_menu_item_text))
    popupMenu.show()
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

  // TODO:
  // When the user changes network access level in the settings it is probably a good idea to
  // somehow check that and try to start uploading/receiving service right after that

  override suspend fun onStateEvent(event: PhotosActivityEvent) {
    when (event) {
      is PhotosActivityEvent.StartUploadingService -> {
        tryToStartUploadService()
      }
      is PhotosActivityEvent.StartReceivingService -> {
        tryToStartReceiveService()
      }
      is PhotosActivityEvent.ScrollEvent -> {
        if (event.isScrollingDown) {
          takePhotoButton.hide()
        } else {
          takePhotoButton.show()
        }
      }
      is PhotosActivityEvent.CancelPhotoUploading -> {
        uploadPhotosServiceConnection.cancelPhotoUploading(event.photoId)
      }
      is PhotosActivityEvent.ShowToast -> onShowToast(event.message)
      is PhotosActivityEvent.OnNewGalleryPhotos -> showNewGalleryPhotosSnackbar(event.count)
      is PhotosActivityEvent.OnNewReceivedPhotos -> showNewReceivedPhotosSnackbar(event.count)
      is PhotosActivityEvent.OnNewUploadedPhotos -> showNewUploadedPhotosSnackbar(event.count)
      is PhotosActivityEvent.ShowDeletePhotoDialog -> showDeletePhotoDialog(event.photoName)
    }.safe
  }

  private suspend fun tryToStartUploadService() {
    val canUploadPhotosResult = viewModel.checkCanUploadPhotos()
    when (canUploadPhotosResult) {
      PhotosActivityViewModel.CanUploadPhotoResult.HasQueuedUpPhotos -> {
        bindUploadingService()
      }
      PhotosActivityViewModel.CanUploadPhotoResult.PhotoUploadingDisabled -> {
        onShowToast(getString(R.string.photos_activity_cannot_upload_photo_disabled))
      }
      PhotosActivityViewModel.CanUploadPhotoResult.HasNoQueuedUpPhotos -> {
        //do nothing
      }
    }
  }

  private suspend fun tryToStartReceiveService() {
    val canReceivedPhotosResult = viewModel.checkCanReceivePhotos()
    when (canReceivedPhotosResult) {
      PhotosActivityViewModel.CanReceivePhotoResult.HasMoreUploadedPhotosThanReceived -> {
        bindReceivingService()
      }
      PhotosActivityViewModel.CanReceivePhotoResult.NetworkAccessDisabled -> {
        onShowToast(getString(R.string.photos_activity_cannot_check_for_received_photos_disabled))
      }
      PhotosActivityViewModel.CanReceivePhotoResult.HasLessOrEqualUploadedPhotosThanReceived -> {
        //do nothing
      }
    }
  }

  override fun onUploadPhotosEvent(event: UploadedPhotosFragmentEvent.PhotoUploadEvent) {
    viewModel.intercom.tell<UploadedPhotosFragment>().to(event)
  }

  override fun onReceivePhotoEvent(event: ReceivedPhotosFragmentEvent.ReceivePhotosEvent) {
    when (event) {
      is ReceivedPhotosFragmentEvent.ReceivePhotosEvent.PhotosReceived -> {
        viewModel.intercom.tell<UploadedPhotosFragment>()
          .that(UploadedPhotosFragmentEvent.ReceivePhotosEvent.OnPhotosReceived(event.receivedPhotos))
        viewModel.intercom.tell<ReceivedPhotosFragment>()
          .that(ReceivedPhotosFragmentEvent.ReceivePhotosEvent.PhotosReceived(event.receivedPhotos))

        showNewPhotoHasBeenReceivedSnackbar()
      }
      is ReceivedPhotosFragmentEvent.ReceivePhotosEvent.OnFailed -> {
        viewModel.intercom.tell<UploadedPhotosFragment>()
          .that(UploadedPhotosFragmentEvent.ReceivePhotosEvent.OnFailed(event.error))
        viewModel.intercom.tell<ReceivedPhotosFragment>()
          .that(ReceivedPhotosFragmentEvent.ReceivePhotosEvent.OnFailed(event.error))
      }
    }.safe
  }

  private fun showNewPhotoHasBeenReceivedSnackbar() {
    Snackbar.make(rootLayout, getString(R.string.photos_activity_photo_has_been_received_snackbar_text), Snackbar.LENGTH_LONG)
      .setAction(getString(R.string.photos_activity_show_snackbar_action_text), {
        launch {
          switchToTab(RECEIVED_PHOTOS_TAB_INDEX)

          //wait some time before fragments switching animation is done
          delay(FRAGMENT_SWITCH_ANIMATION_DELAY_MS)
          viewModel.intercom.tell<ReceivedPhotosFragment>()
            .to(ReceivedPhotosFragmentEvent.GeneralEvents.ScrollToTop())
        }
      }).show()
  }

  private fun showNewGalleryPhotosSnackbar(count: Int) {
    Snackbar.make(rootLayout, getString(R.string.photos_activity_new_gallery_photos, count), Snackbar.LENGTH_LONG)
      .setAction(getString(R.string.photos_activity_show_snackbar_action_text), {
        launch {
          switchToTab(GALLERY_PHOTOS_TAB_INDEX)

          //wait some time before fragments switching animation is done
          delay(FRAGMENT_SWITCH_ANIMATION_DELAY_MS)
          viewModel.intercom.tell<GalleryFragment>()
            .to(GalleryFragmentEvent.GeneralEvents.ScrollToTop)
        }
      }).show()
  }

  private fun showNewReceivedPhotosSnackbar(count: Int) {
    Snackbar.make(rootLayout, getString(R.string.photos_activity_new_received_photos, count), Snackbar.LENGTH_LONG)
      .setAction(getString(R.string.photos_activity_show_snackbar_action_text), {
        launch {
          switchToTab(RECEIVED_PHOTOS_TAB_INDEX)

          //wait some time before fragments switching animation is done
          delay(FRAGMENT_SWITCH_ANIMATION_DELAY_MS)
          viewModel.intercom.tell<ReceivedPhotosFragment>()
            .to(ReceivedPhotosFragmentEvent.GeneralEvents.ScrollToTop())
        }
      }).show()
  }

  private fun showNewUploadedPhotosSnackbar(count: Int) {
    Snackbar.make(rootLayout, getString(R.string.photos_activity_new_uploaded_photos, count), Snackbar.LENGTH_LONG)
      .setAction(getString(R.string.photos_activity_show_snackbar_action_text), {
        launch {
          switchToTab(UPLOADED_PHOTOS_TAB_INDEX)

          //wait some time before fragments switching animation is done
          delay(FRAGMENT_SWITCH_ANIMATION_DELAY_MS)
          viewModel.intercom.tell<UploadedPhotosFragment>()
            .to(UploadedPhotosFragmentEvent.GeneralEvents.ScrollToTop())
        }
      }).show()
  }

  private fun showDeletePhotoDialog(photoName: String) {
    DeletePhotoConfirmationDialog().show(this@PhotosActivity) {
      viewModel.deleteAndBlacklistPhoto(photoName)
    }
  }

  private fun switchToTab(tabIndex: Int) {
    if (viewPager.currentItem != tabIndex) {
      viewPager.currentItem = tabIndex
    }
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    when (requestCode) {
      TakePhotoActivity.TAKE_PHOTO_REQUEST_CODE -> {
        if (resultCode == Activity.RESULT_OK) {
          Timber.tag(TAG).d("Photo taken")

          if (data == null) {
            Timber.tag(TAG).w("requestCode is TAKE_PHOTO_REQUEST_CODE but the intent is null!")
            return
          }

          val intent = Intent(this, ViewTakenPhotoActivity::class.java)
          intent.putExtras(data.getBundleExtra(TakePhotoActivity.TAKEN_PHOTO_BUNDLE_KEY))

          startActivityForResult(intent, ViewTakenPhotoActivity.VIEW_TAKEN_PHOTO_REQUEST_CODE)
        }
      }
      ViewTakenPhotoActivity.VIEW_TAKEN_PHOTO_REQUEST_CODE -> {
        if (resultCode == Activity.RESULT_OK) {
          Timber.tag(TAG).d("Uploading photo")

          viewModel.intercom.tell<UploadedPhotosFragment>().to(
            UploadedPhotosFragmentEvent.GeneralEvents.ReloadAllPhotos
          )
          
          switchToTab(UPLOADED_PHOTOS_TAB_INDEX)
        }
      }
      else -> IllegalStateException("Not implemented for ${requestCode}")
    }
  }

  private fun bindReceivingService() {
    if (!receivePhotosServiceConnection.isConnected()) {
      val serviceIntent = Intent(applicationContext, ReceivePhotosService::class.java)
      startService(serviceIntent)

      bindService(serviceIntent, receivePhotosServiceConnection, Context.BIND_AUTO_CREATE)
    } else {
      receivePhotosServiceConnection.startPhotosReceiving()
    }
  }

  private fun bindUploadingService() {
    if (!uploadPhotosServiceConnection.isConnected()) {
      val serviceIntent = Intent(applicationContext, UploadPhotoService::class.java)
      startService(serviceIntent)

      bindService(serviceIntent, uploadPhotosServiceConnection, Context.BIND_AUTO_CREATE)
    } else {
      uploadPhotosServiceConnection.startPhotosUploading()
    }
  }

  override fun resolveDaggerDependency() {
    activityComponent
      .inject(this)
  }

  enum class PermissionRequestResult {
    Granted,
    NotGranted,
    ShowRationaleForCamera,
    ShowRationaleAppCannotWorkWithoutCamera,
    ShowRationaleForGps
  }

  companion object {
    const val receivedPhotoExtra = "received_photo_extra"
    const val newPhotoReceivedAction = "com.kirakishou.photoexchange.NEW_PHOTO_RECEIVED"
  }
}