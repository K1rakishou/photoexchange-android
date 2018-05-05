package com.kirakishou.photoexchange.ui.activity

import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.AppCompatButton
import android.widget.Toast
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.di.module.SettingsActivityModule
import com.kirakishou.photoexchange.mvp.viewmodel.SettingsActivityViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class SettingsActivity : BaseActivity() {

    @BindView(R.id.reset_make_public_photo_option_button)
    lateinit var resetButton: AppCompatButton

    @Inject
    lateinit var viewModel: SettingsActivityViewModel

    private val TAG = "SettingsActivity"

    override fun getContentView(): Int = R.layout.activity_settings

    override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
        resetButton.setOnClickListener {
            compositeDisposable += viewModel.resetMakePublicPhotoOption()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete {
                    Toast.makeText(this, "Done", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .doOnError { Timber.tag(TAG).e(it) }
                .subscribe()
        }
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
