package com.kirakishou.photoexchange.ui.widget

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import com.kirakishou.photoexchange.mvp.model.other.Constants
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

class EndlessRecyclerOnScrollListener(
    private val gridLayoutManager: GridLayoutManager,
    private val visibleThreshold: Int,
    private val loadMoreSubject: PublishSubject<Int>
) : RecyclerView.OnScrollListener() {

    private val tag = "EndlessRecyclerOnScrollListener"
    private var loading = AtomicBoolean(false)
    private var lastVisibleItem = 0
    private var totalItemCount = 0

    //we preload a 0th page manually
    private var currentPage = 1
    private var isEndReached = false

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        if (isEndReached) {
            return
        }

        totalItemCount = gridLayoutManager.itemCount
        lastVisibleItem = gridLayoutManager.findLastVisibleItemPosition()

        if (!loading.get() && (totalItemCount <= (lastVisibleItem + visibleThreshold))) {
            loading.set(true)

            Timber.tag(tag).d("Loading new page $currentPage")
            loadMoreSubject.onNext(currentPage)
            currentPage++
        }
    }

    fun pageLoaded() {
        loading.set(false)
    }

    fun reachedEnd() {
        isEndReached = true
    }

    fun reset() {
        loading.set(false)
        lastVisibleItem = 0
        totalItemCount = 0
        currentPage = 0
        isEndReached = false
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("loading", loading.get())
        outState.putInt("lastVisibleItem", lastVisibleItem)
        outState.putInt("totalItemCount", totalItemCount)
        outState.putInt("currentPage", currentPage)
        outState.putBoolean("isEndReached", isEndReached)
    }

    fun onRestoreInstanceState(savedInstanceState: Bundle) {
        loading.set(savedInstanceState.getBoolean("loading", false))
        lastVisibleItem = savedInstanceState.getInt("lastVisibleItem", 0)
        totalItemCount = savedInstanceState.getInt("totalItemCount", 0)
        currentPage = savedInstanceState.getInt("currentPage", 0)
        isEndReached = savedInstanceState.getBoolean("isEndReached", false)
    }
}