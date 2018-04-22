package com.kirakishou.photoexchange.ui.fragment


import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvp.model.PhotoAnswer
import com.kirakishou.photoexchange.mvp.model.PhotoFindEvent
import com.kirakishou.photoexchange.mvp.viewmodel.AllPhotosActivityViewModel
import com.kirakishou.photoexchange.ui.activity.AllPhotosActivity
import com.kirakishou.photoexchange.ui.adapter.ReceivedPhotosAdapter
import com.kirakishou.photoexchange.ui.adapter.ReceivedPhotosAdapterSpanSizeLookup
import com.kirakishou.photoexchange.ui.viewstate.ReceivedPhotosFragmentViewStateEvent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class ReceivedPhotosFragment : BaseFragment() {

    @BindView(R.id.received_photos_list)
    lateinit var receivedPhotosList: RecyclerView

    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var viewModel: AllPhotosActivityViewModel

    lateinit var adapter: ReceivedPhotosAdapter

    private val PHOTO_ADAPTER_VIEW_WIDTH = 288

    override fun getContentView(): Int = R.layout.fragment_received_photos

    override fun onFragmentViewCreated(savedInstanceState: Bundle?) {
        initRx()
        initRecyclerView()
        loadPhotos()
    }

    private fun initRx() {
        compositeDisposable += viewModel.onPhotoFindEventSubject
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { event -> onPhotoFindEvent(event) }
            .subscribe()

        compositeDisposable += viewModel.receivedPhotosFragmentViewStateSubject
            .observeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { viewState -> onViewStateChanged(viewState) }
            .subscribe()
    }

    private fun initRecyclerView() {
        val columnsCount = AndroidUtils.calculateNoOfColumns(requireContext(), PHOTO_ADAPTER_VIEW_WIDTH)

        adapter = ReceivedPhotosAdapter(requireContext(), imageLoader)
        adapter.init()

        val layoutManager = GridLayoutManager(requireContext(), columnsCount)
        layoutManager.spanSizeLookup = ReceivedPhotosAdapterSpanSizeLookup(adapter, columnsCount)

        receivedPhotosList.layoutManager = layoutManager
        receivedPhotosList.adapter = adapter
    }

    private fun loadPhotos() {
        viewModel.loadPhotoAnswers()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess { photos -> addPhotosToAdapter(photos) }
            .subscribe()
    }

    private fun onViewStateChanged(viewStateEvent: ReceivedPhotosFragmentViewStateEvent) {
        if (!isAdded) {
            return
        }

        requireActivity().runOnUiThread {
            when (viewStateEvent) {
                is ReceivedPhotosFragmentViewStateEvent.ScrollToTop -> {
                    receivedPhotosList.scrollToPosition(0)
                }
                else -> throw IllegalArgumentException("Unknown MyPhotosFragmentViewStateEvent $viewStateEvent")
            }
        }
    }

    private fun onPhotoFindEvent(event: PhotoFindEvent) {
        if (!isAdded) {
            return
        }

        requireActivity().runOnUiThread {
            when (event) {
                is PhotoFindEvent.OnPhotoAnswerFound -> {
                    adapter.addPhotoAnswer(event.photoAnswer)
                }
                is PhotoFindEvent.OnFailed,
                is PhotoFindEvent.OnUnknownError -> {
                    //do nothing here
                }
            }
        }
    }

    private fun addPhotosToAdapter(photoAnswers: List<PhotoAnswer>) {
        if (!isAdded) {
            return
        }

        requireActivity().runOnUiThread {
            if (photoAnswers.isNotEmpty()) {
                adapter.addPhotoAnswers(photoAnswers)
            } else {
                //TODO: show notification that no photos has been uploaded yet
            }
        }
    }

    override fun onFragmentViewDestroy() {
    }

    override fun resolveDaggerDependency() {
        (requireActivity() as AllPhotosActivity).activityComponent
            .inject(this)
    }

    companion object {
        fun newInstance(): ReceivedPhotosFragment {
            val fragment = ReceivedPhotosFragment()
            val args = Bundle()

            fragment.arguments = args
            return fragment
        }
    }
}
