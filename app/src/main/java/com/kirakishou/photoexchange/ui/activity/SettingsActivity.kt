package com.kirakishou.photoexchange.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.widget.AppCompatButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import butterknife.BindView
import com.jakewharton.rxbinding2.view.RxView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.di.module.SettingsActivityModule
import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.mvp.viewmodel.SettingsActivityViewModel
import com.kirakishou.photoexchange.ui.dialog.EnterOldUserIdDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject


class SettingsActivity : BaseActivity() {

  @BindView(R.id.reset_make_public_photo_option_button)
  lateinit var resetButton: AppCompatButton

  @BindView(R.id.restore_from_old_user_id)
  lateinit var restoreFromOldUserIdButton: AppCompatButton

  @BindView(R.id.user_id_text_view)
  lateinit var userIdTextView: TextView

  @BindView(R.id.user_id_holder)
  lateinit var userIdHolder: LinearLayout

  @Inject
  lateinit var viewModel: SettingsActivityViewModel

  private val TAG = "SettingsActivity"

  override fun getContentView(): Int = R.layout.activity_settings

  override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
    resetButton.setOnClickListener {
      compositeDisposable += RxView.clicks(resetButton)
        .subscribeOn(Schedulers.io())
        .concatMap { viewModel.resetMakePublicPhotoOption() }
        .observeOn(AndroidSchedulers.mainThread())
        .doOnComplete {
          Toast.makeText(this, "Done", Toast.LENGTH_SHORT).show()
          finish()
        }
        .doOnError { Timber.tag(TAG).e(it) }
        .subscribe()
    }

    compositeDisposable += RxView.clicks(userIdHolder)
      .subscribeOn(AndroidSchedulers.mainThread())
      .doOnNext { copyUserIdToClipBoard() }
      .doOnNext { Toast.makeText(this, "UserId copied to clipboard", Toast.LENGTH_SHORT).show() }
      .subscribe()

    compositeDisposable += viewModel.getUserId()
      .subscribe({ userId ->
        if (userId.isEmpty()) {
          userIdTextView.text = "Empty userId"
        } else {
          userIdTextView.text = userId
        }
      })

    compositeDisposable += RxView.clicks(restoreFromOldUserIdButton)
      .concatMap { EnterOldUserIdDialog().show(this).toObservable() }
      .filter { userId -> userId.isNotEmpty() }
      .concatMap { userId -> viewModel.restoreOldAccount(userId).toObservable() }
      .subscribe({ result ->
        when (result) {
          is Either.Value -> {
            onShowToast("Successfully restored old account")
            finish()
          }
          is Either.Error -> showErrorCodeToast(result.error)
        }
      }, { error ->
        Timber.tag(TAG).e(error)
        onShowToast("Unknown error while trying to restore account: ${error.message
          ?: "empty error message"}")
      })
  }

  private fun copyUserIdToClipBoard() {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clip = android.content.ClipData.newPlainText("user_id", userIdTextView.text.toString())
    clipboard.primaryClip = clip
  }

  override fun onActivityStart() {
  }

  override fun onActivityStop() {
  }

  override fun resolveDaggerDependency() {
    (application as PhotoExchangeApplication).applicationComponent
      .plus(SettingsActivityModule(this))
      .inject(this)
  }
}
