package com.kirakishou.photoexchange.ui.widget

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class EndlessRecyclerOnScrollListener(
    private val fragmentTag: String,
    private val gridLayoutManager: GridLayoutManager,
    private val visibleThreshold: Int,
    private val uploadedPhotosFragmentPageToLoadSubject: Subject<Boolean>
) : RecyclerView.OnScrollListener() {

    private val tag = "EndlessRecyclerOnScrollListener_$fragmentTag"
    private var loading = AtomicBoolean(false)
    private var lastVisibleItem = 0
    private var totalItemCount = 0

    private var lastPageReached = AtomicBoolean(false)
    private var keepLoading = AtomicBoolean(true)

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        if (lastPageReached.get()) {
            return
        }

        if (!keepLoading.get()) {
            return
        }

        totalItemCount = gridLayoutManager.itemCount
        lastVisibleItem = gridLayoutManager.findLastVisibleItemPosition()

        if (totalItemCount <= (lastVisibleItem + visibleThreshold)) {
            if (loading.compareAndSet(false, true)) {
                Timber.tag(tag).d("Loading new page")
                uploadedPhotosFragmentPageToLoadSubject.onNext(false)
            }
        }
    }

    fun pageLoaded() {
        loading.set(false)
    }

    fun lastPageReached() {
        lastPageReached.set(true)
    }

    fun resumeLoading() {
        keepLoading.set(true)
    }

    fun pauseLoading() {
        keepLoading.set(false)
    }

    fun reset() {
        loading.set(false)
        keepLoading.set(true)
        lastPageReached.set(false)
        lastVisibleItem = 0
        totalItemCount = 0
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("loading", loading.get())
        outState.putInt("lastVisibleItem", lastVisibleItem)
        outState.putInt("totalItemCount", totalItemCount)
        outState.putBoolean("keepLoading", keepLoading.get())
        outState.putBoolean("lastPageReached", lastPageReached.get())
    }

    fun onRestoreInstanceState(savedInstanceState: Bundle) {
        loading.set(savedInstanceState.getBoolean("loading", false))
        lastVisibleItem = savedInstanceState.getInt("lastVisibleItem", 0)
        totalItemCount = savedInstanceState.getInt("totalItemCount", 0)
        keepLoading.set(savedInstanceState.getBoolean("keepLoading", true))
        lastPageReached.set(savedInstanceState.getBoolean("lastPageReached", false))
    }
}