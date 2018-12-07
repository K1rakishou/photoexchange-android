package com.kirakishou.photoexchange.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.di.module.activity.SettingsActivityModule
import com.kirakishou.photoexchange.mvp.viewmodel.SettingsActivityViewModel
import com.kirakishou.photoexchange.ui.dialog.EnterOldUserIdDialog
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
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
      //TODO: use actor
      launch {
        try {
          viewModel.resetMakePublicPhotoOption()

          Toast.makeText(this@SettingsActivity, "Done", Toast.LENGTH_SHORT).show()
          finish()
        } catch (error: Exception) {
          Timber.tag(TAG).e(error)
        }
      }
    }

    userIdHolder.setOnClickListener {
      copyUserIdToClipBoard()
      Toast.makeText(this, "UserId copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    restoreFromOldUserIdButton.setOnClickListener {
      //TODO: use actor
      launch {
        val userId = EnterOldUserIdDialog().show(this@SettingsActivity).await()
        if (userId.isBlank()) {
          return@launch
        }


        val isOk = try {
          viewModel.restoreOldAccount(userId)
        } catch (error: Exception) {
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

    //TODO: use actor
    launch {
      val userId = viewModel.getUserId()
      if (userId.isEmpty()) {
        userIdTextView.text = "Empty userId"
      } else {
        userIdTextView.text = userId
      }
    }
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
