package com.kirakishou.photoexchange.ui.activity

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.PersistableBundle
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.viewpager.widget.ViewPager
import butterknife.BindView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.jakewharton.rxbinding2.view.RxView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.di.module.PhotosActivityModule
import com.kirakishou.photoexchange.helper.extension.debounceClicks
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.helper.intercom.IntercomListener
import com.kirakishou.photoexchange.helper.intercom.StateEventListener
import com.kirakishou.photoexchange.helper.intercom.event.GalleryFragmentEvent
import com.kirakishou.photoexchange.helper.intercom.event.PhotosActivityEvent
import com.kirakishou.photoexchange.helper.intercom.event.ReceivedPhotosFragmentEvent
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.helper.permission.PermissionManager
import com.kirakishou.photoexchange.mvp.model.PhotoExchangedData
import com.kirakishou.photoexchange.mvp.viewmodel.PhotosActivityViewModel
import com.kirakishou.photoexchange.service.*
import com.kirakishou.photoexchange.ui.callback.PhotoUploadingServiceCallback
import com.kirakishou.photoexchange.ui.callback.ReceivePhotosServiceCallback
import com.kirakishou.photoexchange.ui.fragment.GalleryFragment
import com.kirakishou.photoexchange.ui.fragment.ReceivedPhotosFragment
import com.kirakishou.photoexchange.ui.fragment.UploadedPhotosFragment
import com.kirakishou.photoexchange.ui.viewstate.PhotosActivityViewState
import com.kirakishou.photoexchange.ui.widget.FragmentTabsPager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.lang.IllegalStateException
import javax.inject.Inject


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
      val photoExchangedData = PhotoExchangedData.fromBundle(bundle)
      if (photoExchangedData == null) {
        throw IllegalStateException("photoExchangedData should not be null!")
      }

      launch {
        //add slight delay here to ensure that "notificationManager.cancel" is called AFTER
        //the notification has been shown
        delay(NOTIFICATION_CANCEL_DELAY_MS)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(PushNotificationReceiverService.NOTIFICATION_ID)

        onNewPhotoNotification(photoExchangedData)
      }
    }
  }

  override fun getContentView(): Int = R.layout.activity_all_photos

  override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
    viewState = PhotosActivityViewState().also { it.loadFromBundle(savedInstanceState) }

    receivePhotosServiceConnection = ReceivePhotosServiceConnection(this)
    uploadPhotosServiceConnection = UploadPhotoServiceConnection(this)

    onNewIntent(intent)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)

    val bundle = intent.extras?.getBundle(PhotosActivity.receivedPhotoExtra)
    val photoExchangedData = PhotoExchangedData.fromBundle(bundle)

    if (photoExchangedData != null) {
      onNewPhotoNotification(photoExchangedData)
    }
  }

  override fun onActivityStart() {
    registerReceiver(notificationBroadcastReceiver, IntentFilter(newPhotoReceivedAction))

    initRx()
    initViews()
  }

  override fun onActivityStop() {
    receivePhotosServiceConnection.onFindingServiceDisconnected()
    uploadPhotosServiceConnection.onUploadingServiceDisconnected()

    unregisterReceiver(notificationBroadcastReceiver)
  }

  private fun onNewPhotoNotification(photoExchangedData: PhotoExchangedData) {
    launch { viewModel.addReceivedPhoto(photoExchangedData) }
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

    compositeDisposable += viewModel.intercom.photosActivityEvents.listen()
      .subscribe({ event ->
        launch { onStateEvent(event) }
      })
  }

  private fun initViews() {
    initTabs()

    if (viewState.lastOpenedTab != 0) {
      switchToTab(viewState.lastOpenedTab)
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

    viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
      override fun onPageScrollStateChanged(state: Int) {
      }

      override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        tabLayout.setScrollPosition(position, positionOffset, true)
      }

      override fun onPageSelected(position: Int) {
        viewPager.currentItem = position
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
      }
    })
  }

  private fun createMenu() {
    val popupMenu = PopupMenu(this, menuButton)
    popupMenu.setOnMenuItemClickListener(this)
    popupMenu.menu.add(1, R.id.settings_item, 1, resources.getString(R.string.settings_menu_item_text))
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

  override suspend fun onStateEvent(event: PhotosActivityEvent) {
    when (event) {
      is PhotosActivityEvent.StartUploadingService -> {
        val hasPhotosToUpload = viewModel.checkHasPhotosToUpload()
        if (hasPhotosToUpload) {
          bindUploadingService(event.callerClass, event.reason)
        } else {
          //do nothing
        }
      }
      is PhotosActivityEvent.StartReceivingService -> {
        val hasPhotosToReceive = viewModel.checkCanReceivePhotos()
        if (hasPhotosToReceive) {
          bindReceivingService(event.callerClass, event.reason)
        } else {
          //do nothing
        }
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
      PhotosActivityEvent.OnNewPhotoReceived -> showNewPhotoHasBeenReceivedSnackbar()
      is PhotosActivityEvent.ShowToast -> onShowToast(event.message)
      is PhotosActivityEvent.OnNewGalleryPhotos -> showNewGalleryPhotosSnackbar(event.count)
    }.safe
  }

  override fun onUploadPhotosEvent(event: UploadedPhotosFragmentEvent.PhotoUploadEvent) {
    viewModel.intercom.tell<UploadedPhotosFragment>().to(event)
  }

  override fun onReceivePhotoEvent(event: ReceivedPhotosFragmentEvent.ReceivePhotosEvent) {
    when (event) {
      is ReceivedPhotosFragmentEvent.ReceivePhotosEvent.PhotosReceived -> {
        viewModel.intercom.tell<UploadedPhotosFragment>()
          .that(UploadedPhotosFragmentEvent.ReceivePhotosEvent.PhotosReceived(event.receivedPhotos))
        viewModel.intercom.tell<ReceivedPhotosFragment>()
          .that(ReceivedPhotosFragmentEvent.ReceivePhotosEvent.PhotosReceived(event.receivedPhotos))

        showNewPhotoHasBeenReceivedSnackbar()
      }
      is ReceivedPhotosFragmentEvent.ReceivePhotosEvent.NoPhotosReceived -> {
        viewModel.intercom.tell<UploadedPhotosFragment>()
          .that(UploadedPhotosFragmentEvent.ReceivePhotosEvent.NoPhotosReceived())
        viewModel.intercom.tell<ReceivedPhotosFragment>()
          .that(ReceivedPhotosFragmentEvent.ReceivePhotosEvent.NoPhotosReceived())
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
    Snackbar.make(rootLayout, getString(R.string.photo_has_been_received_snackbar_text), Snackbar.LENGTH_LONG)
      .setAction(getString(R.string.show_snackbar_action_text), {
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
    Snackbar.make(rootLayout, "You have ${count} new gallery photos", Snackbar.LENGTH_LONG)
      .setAction(getString(R.string.show_snackbar_action_text), {
        launch {
          switchToTab(GALLERY_PHOTOS_TAB_INDEX)

          //wait some time before fragments switching animation is done
          delay(FRAGMENT_SWITCH_ANIMATION_DELAY_MS)
          viewModel.intercom.tell<GalleryFragment>()
            .to(GalleryFragmentEvent.GeneralEvents.ScrollToTop)
        }
      }).show()
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

  private fun bindReceivingService(
    callerClass: Class<*>,
    reason: String
  ) {
    if (!receivePhotosServiceConnection.isConnected()) {
      Timber.tag(TAG).d("(callerClass = $callerClass, reason = $reason) bindReceivingService")

      val serviceIntent = Intent(applicationContext, ReceivePhotosService::class.java)
      startService(serviceIntent)

      bindService(serviceIntent, receivePhotosServiceConnection, Context.BIND_AUTO_CREATE)
    } else {
      Timber.tag(TAG).d("(callerClass = $callerClass, reason = $reason) Already connected, force startPhotosReceiving")
      receivePhotosServiceConnection.startPhotosReceiving()
    }
  }

  private fun bindUploadingService(
    callerClass: Class<*>,
    reason: String
  ) {
    if (!uploadPhotosServiceConnection.isConnected()) {
      Timber.tag(TAG).d("(callerClass = $callerClass, reason = $reason) bindUploadingService")

      val serviceIntent = Intent(applicationContext, UploadPhotoService::class.java)
      startService(serviceIntent)

      bindService(serviceIntent, uploadPhotosServiceConnection, Context.BIND_AUTO_CREATE)
    } else {
      Timber.tag(TAG).d("(callerClass = $callerClass, reason = $reason) Already connected, force startPhotosUploading")
      uploadPhotosServiceConnection.startPhotosUploading()
    }
  }

  override fun resolveDaggerDependency() {
    activityComponent
      .inject(this)
  }

  companion object {
    const val receivedPhotoExtra = "received_photo_extra"
    const val newPhotoReceivedAction = "com.kirakishou.photoexchange.NEW_PHOTO_RECEIVED"
  }
}