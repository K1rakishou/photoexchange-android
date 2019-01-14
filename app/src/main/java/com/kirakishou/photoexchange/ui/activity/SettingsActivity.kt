package com.kirakishou.photoexchange.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatSpinner
import butterknife.BindView
import com.jakewharton.rxbinding2.widget.RxAdapterView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.di.module.activity.SettingsActivityModule
import com.kirakishou.photoexchange.helper.NetworkAccessLevel
import com.kirakishou.photoexchange.helper.PhotosVisibility
import com.kirakishou.photoexchange.mvp.viewmodel.SettingsActivityViewModel
import com.kirakishou.photoexchange.ui.dialog.EnterOldUserIdDialog
import io.reactivex.rxkotlin.plusAssign
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import timber.log.Timber
import javax.inject.Inject


class SettingsActivity : BaseActivity() {

  @BindView(R.id.photo_visibility_spinner)
  lateinit var photoVisibilitySpinner: AppCompatSpinner

  @BindView(R.id.network_access_level_spinner)
  lateinit var networkAccessLevelSpinner: AppCompatSpinner

  @BindView(R.id.restore_from_old_user_id)
  lateinit var restoreFromOldUserIdButton: AppCompatButton

  @BindView(R.id.user_id_text_view)
  lateinit var userIdTextView: TextView

  @BindView(R.id.user_id_holder)
  lateinit var userIdHolder: LinearLayout

  @Inject
  lateinit var viewModel: SettingsActivityViewModel

  private val TAG = "SettingsActivity"

  private val photoVisibilitySpinnerList by lazy {
    ArrayList<String>().apply {
      add(getString(R.string.settings_activity_always_public))
      add(getString(R.string.settings_activity_always_private))
      add(getString(R.string.settings_activity_ask_every_time))
    }
  }

  private val networkAccessLevelSpinnerList by lazy {
    ArrayList<String>().apply {
      add(getString(R.string.settings_activity_can_load_images))
      add(getString(R.string.settings_activity_can_access_internet))
      add(getString(R.string.settings_activity_neither))
    }
  }

  override fun getContentView(): Int = R.layout.activity_settings

  override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
    launch {
      initViews()
    }
  }

  private suspend fun initViews() {
    initPhotoVisibilitySpinner()
    initNetworkAccessLevelSpinner()

    compositeDisposable += RxAdapterView.itemSelections(photoVisibilitySpinner)
      .map { PhotosVisibility.fromInt(it) }
      .subscribe({ visibility ->
        viewModel.updatePhotoVisibility(visibility)
      }, { error -> Timber.tag(TAG).e(error) })

    compositeDisposable += RxAdapterView.itemSelections(networkAccessLevelSpinner)
      .map { NetworkAccessLevel.fromInt(it) }
      .subscribe({ level ->
        viewModel.updateNetworkAccessLevel(level)
      }, { error -> Timber.tag(TAG).e(error) })

    userIdHolder.setOnClickListener {
      copyUserIdToClipBoard()
      Toast.makeText(this, getString(R.string.settings_activity_user_id_copied_to_clipboard), Toast.LENGTH_SHORT).show()
    }

    restoreFromOldUserIdButton.setOnClickListener {
      launch {
        val userId = EnterOldUserIdDialog().show(this@SettingsActivity).await()
        if (userId.isBlank()) {
          return@launch
        }

        val isOk = try {
          viewModel.restoreOldAccount(userId)
        } catch (error: Throwable) {
          val message = error.message ?: "empty error message"
          onShowToast(getString(R.string.settings_activity_unknown_error, message))
          return@launch
        }

        if (isOk) {
          onShowToast(getString(R.string.settings_activity_successfully_restored_old_account))
          finish()
        } else {
          onShowToast(getString(R.string.settings_activity_account_does_not_exist))
        }
      }
    }

    launch {
      val userId = viewModel.getUserId()
      if (userId.isEmpty()) {
        userIdTextView.text = getString(R.string.settings_activity_current_user_id_is_empty_msg)
      } else {
        userIdTextView.text = userId
      }
    }
  }

  private suspend fun initNetworkAccessLevelSpinner() {
    val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, networkAccessLevelSpinnerList)
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    networkAccessLevelSpinner.adapter = adapter

    val level = viewModel.getNetworkAccessLevel()
    networkAccessLevelSpinner.setSelection(level.value)
  }

  private suspend fun initPhotoVisibilitySpinner() {
    val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, photoVisibilitySpinnerList)
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    photoVisibilitySpinner.adapter = adapter

    val visibility = viewModel.getPhotoVisibility()
    photoVisibilitySpinner.setSelection(visibility.value)
  }

  override fun onActivityStart() {
  }

  override fun onActivityStop() {
  }

  private fun copyUserIdToClipBoard() {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clip = android.content.ClipData.newPlainText(CLIPBOARD_LABEL, userIdTextView.text.toString())
    clipboard.primaryClip = clip
  }

  override fun resolveDaggerDependency() {
    (application as PhotoExchangeApplication).applicationComponent
      .plus(SettingsActivityModule(this))
      .inject(this)
  }

  companion object {
    const val CLIPBOARD_LABEL = "user_id_label"
  }
}
