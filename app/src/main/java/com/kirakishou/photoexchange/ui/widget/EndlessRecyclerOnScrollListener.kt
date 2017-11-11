package com.kirakishou.photoexchange.ui.widget

import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

/**
 * Created by kirakishou on 11/10/2017.
 */

class EndlessRecyclerOnScrollListener(
        private val gridLayoutManager: GridLayoutManager,
        private val loadMoreSubject: PublishSubject<Int>
): RecyclerView.OnScrollListener() {

    private var loading = false
    private val visibleThreshold = 5 * gridLayoutManager.spanCount
    private var lastVisibleItem = 0
    private var totalItemCount = 0
    private var currentPage = 0
    private var isEndReached = false

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        if (isEndReached) {
            return
        }

        totalItemCount = gridLayoutManager.itemCount
        lastVisibleItem = gridLayoutManager.findLastVisibleItemPosition()

        Timber.d("${!loading} && ($totalItemCount <= (${lastVisibleItem + visibleThreshold})")
        if (!loading && (totalItemCount <= (lastVisibleItem + visibleThreshold))) {
            Timber.d("Loading!")
            loading = true

            loadMoreSubject.onNext(currentPage)
            currentPage++
        }
    }

    fun pageLoaded() {
        loading = false
    }

    fun reachedEnd() {
        isEndReached = true
    }

    fun reset() {
        loading = false
        lastVisibleItem = 0
        totalItemCount = 0
        currentPage = 0
        isEndReached = false
   }

    fun triggerOnScrolled(rv: RecyclerView, dx: Int, dy: Int) {
        onScrolled(rv, dx, dy)
    }
}