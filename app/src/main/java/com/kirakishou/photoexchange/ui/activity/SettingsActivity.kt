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

  private val photoVisibilitySpinnerList = ArrayList<String>().apply {
    add("Always Public")
    add("Always Private")
    //TODO: change to "Ask me every time"
    add("Neither")
  }

  private val networkAccessLevelSpinnerList = ArrayList<String>().apply {
    add("Can Load Images")
    add("Can Access Internet")
    add("Neither")
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
      Toast.makeText(this, "UserId copied to clipboard", Toast.LENGTH_SHORT).show()
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
          onShowToast("Unknown error while trying to restore account: ${error.message
            ?: "empty error message"}")
          return@launch
        }

        if (isOk) {
          onShowToast("Successfully restored old account")
          finish()
        } else {
          onShowToast("Account does not exist")
        }
      }
    }

    launch {
      val userId = viewModel.getUserId()
      if (userId.isEmpty()) {
        userIdTextView.text = "Empty userId"
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
    val clip = android.content.ClipData.newPlainText("user_id", userIdTextView.text.toString())
    clipboard.primaryClip = clip
  }

  override fun resolveDaggerDependency() {
    (application as PhotoExchangeApplication).applicationComponent
      .plus(SettingsActivityModule(this))
      .inject(this)
  }
}
