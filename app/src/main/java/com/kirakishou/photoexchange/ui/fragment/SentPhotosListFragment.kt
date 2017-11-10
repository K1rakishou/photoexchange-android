package com.kirakishou.photoexchange.ui.fragment


import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.base.BaseFragment
import com.kirakishou.photoexchange.di.component.DaggerAllPhotoViewActivityComponent
import com.kirakishou.photoexchange.di.module.AllPhotoViewActivityModule
import com.kirakishou.photoexchange.mvvm.model.*
import com.kirakishou.photoexchange.mvvm.model.dto.PhotoNameWithId
import com.kirakishou.photoexchange.mvvm.viewmodel.AllPhotosViewActivityViewModel
import com.kirakishou.photoexchange.mvvm.viewmodel.factory.AllPhotosViewActivityViewModelFactory
import com.kirakishou.photoexchange.ui.activity.AllPhotosViewActivity
import com.kirakishou.photoexchange.ui.adapter.TakenPhotosAdapter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject

class SentPhotosListFragment : BaseFragment<AllPhotosViewActivityViewModel>() {

    @BindView(R.id.sent_photos_list)
    lateinit var sentPhotosRv: RecyclerView

    @BindView(R.id.swipe_refresh_layout)
    lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private lateinit var adapter: TakenPhotosAdapter

    @Inject
    lateinit var viewModelFactory: AllPhotosViewActivityViewModelFactory

    private val retryButtonSubject = PublishSubject.create<UploadedPhoto>()

    override fun initViewModel(): AllPhotosViewActivityViewModel {
        return ViewModelProviders.of(activity, viewModelFactory).get(AllPhotosViewActivityViewModel::class.java)
    }

    override fun getContentView(): Int = R.layout.fragment_sent_photos_list

    override fun onFragmentViewCreated(savedInstanceState: Bundle?) {
        initRx()
        initRecycler()
        initSwipeRefreshLayout(arguments)
    }

    override fun onFragmentViewDestroy() {
    }

    private fun initSwipeRefreshLayout(arguments: Bundle) {
        val isUploadingPhoto = arguments.getBoolean("is_uploading_photo", false)

        if (isUploadingPhoto) {
            showRefreshIndicator()
        } else {
            hideRefreshIndicator()
        }
    }

    private fun initRx() {
        compositeDisposable += retryButtonSubject
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onRetryButtonClicked)
    }

    private fun initRecycler() {
        val layoutManager = LinearLayoutManager(activity)

        adapter = TakenPhotosAdapter(activity, retryButtonSubject)
        adapter.init()

        sentPhotosRv.layoutManager = layoutManager
        sentPhotosRv.adapter = adapter
        sentPhotosRv.setHasFixedSize(true)
    }

    private fun onRetryButtonClicked(uploadedPhoto: UploadedPhoto) {
        Timber.e("photoName: ${uploadedPhoto.photoName}")
    }

    fun onPhotoUploaded(photo: UploadedPhoto) {
        check(isAdded)
        hideRefreshIndicator()

        adapter.runOnAdapterHandler {
            adapter.addFirst(AdapterItem(photo, AdapterItemType.VIEW_ITEM))
        }
    }

    fun onFailedToUploadPhoto() {
        //adapter.add(AdapterItem(photo, AdapterItemType.VIEW_FAILED_TO_UPLOAD))
    }

    private fun showRefreshIndicator() {
        swipeRefreshLayout.isRefreshing = true
    }

    private fun hideRefreshIndicator() {
        swipeRefreshLayout.isRefreshing = false
    }

    override fun resolveDaggerDependency() {
        DaggerAllPhotoViewActivityComponent.builder()
                .applicationComponent(PhotoExchangeApplication.applicationComponent)
                .allPhotoViewActivityModule(AllPhotoViewActivityModule(activity as AllPhotosViewActivity))
                .build()
                .inject(this)
    }

    companion object {
        fun newInstance(isUploadingPhoto: Boolean): SentPhotosListFragment {
            val fragment = SentPhotosListFragment()
            val args = Bundle()
            args.putBoolean("is_uploading_photo", isUploadingPhoto)

            fragment.arguments = args
            return fragment
        }
    }
}
