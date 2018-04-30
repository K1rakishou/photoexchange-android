package com.kirakishou.photoexchange.ui.fragment


import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.view.View
import android.view.animation.*
import androidx.core.animation.addListener
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.ui.activity.ViewTakenPhotoActivity
import io.reactivex.Completable

class AddToGalleryDialogFragment : BaseFragment(), ViewTakenPhotoActivity.BackPressAwareFragment {

    @BindView(R.id.make_photo_public_container)
    lateinit var viewContainer: ConstraintLayout

    override fun getContentView(): Int = R.layout.fragment_add_to_gallery_dialog

    var viewHeight: Float = 0f

    override fun onFragmentViewCreated(savedInstanceState: Bundle?) {
        val context = requireContext()
        viewHeight = AndroidUtils.dpToPx(context.resources.getDimension(R.dimen.make_photo_public_dialog_container_height), context)

        viewContainer.translationY = viewContainer.translationY + viewHeight
        viewContainer.alpha = 0f
    }

    override fun onResume() {
        super.onResume()

        animateAppear()
    }

    private fun animateAppear() {
        val set = AnimatorSet()

        val animation1 = ObjectAnimator.ofFloat(viewContainer, View.TRANSLATION_Y, viewHeight, 0f)
        animation1.setInterpolator(AccelerateDecelerateInterpolator())

        val animation2 = ObjectAnimator.ofFloat(viewContainer, View.ALPHA, 0f, 1f)
        animation2.setInterpolator(LinearInterpolator())

        set.playTogether(animation1, animation2)
        set.setDuration(350)
        set.start()
    }

    private fun animateDisappear(): Completable {
        return Completable.create { emitter ->
            val set = AnimatorSet()

            val animation1 = ObjectAnimator.ofFloat(viewContainer, View.TRANSLATION_Y, 0f, viewHeight)
            animation1.setInterpolator(AccelerateInterpolator())

            val animation2 = ObjectAnimator.ofFloat(viewContainer, View.ALPHA, 1f, 0f)
            animation2.setInterpolator(LinearInterpolator())

            set.playTogether(animation1, animation2)
            set.setDuration(350)
            set.addListener(onEnd = {
                emitter.onComplete()
            })
            set.start()
        }
    }

    override fun onFragmentViewDestroy() {
    }

    override fun onBackPressed(): Completable {
        return animateDisappear()
    }

    override fun resolveDaggerDependency() {
    }

    companion object {
        const val TAG = "ADD_TO_GALLERY_DIALOG_FRAGMENT"
    }
}
