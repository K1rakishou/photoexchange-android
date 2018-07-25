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

    private var keepLoading = false

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        if (!keepLoading) {
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

    fun pageLoading() {
        loading.set(true)
    }

    fun pageLoaded() {
        loading.set(false)
    }

    fun startLoading() {
        keepLoading = true
    }

    fun stopLoading() {
        keepLoading = false
    }

    fun reset() {
        loading.set(false)
        lastVisibleItem = 0
        totalItemCount = 0
        keepLoading = false
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("loading", loading.get())
        outState.putInt("lastVisibleItem", lastVisibleItem)
        outState.putInt("totalItemCount", totalItemCount)
        outState.putBoolean("keepLoading", keepLoading)
    }

    fun onRestoreInstanceState(savedInstanceState: Bundle) {
        loading.set(savedInstanceState.getBoolean("loading", false))
        lastVisibleItem = savedInstanceState.getInt("lastVisibleItem", 0)
        totalItemCount = savedInstanceState.getInt("totalItemCount", 0)
        keepLoading = savedInstanceState.getBoolean("keepLoading", false)
    }
}