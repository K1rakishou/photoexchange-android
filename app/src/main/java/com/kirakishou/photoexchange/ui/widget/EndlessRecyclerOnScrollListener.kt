package com.kirakishou.photoexchange.ui.widget

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class EndlessRecyclerOnScrollListener(
    private val fragmentTag: String,
    private val gridLayoutManager: GridLayoutManager,
    private val visibleThreshold: Int,
    private val loadMoreSubject: PublishSubject<Int>,
    private val scrollSubject: PublishSubject<Boolean>,
    private val startWithPage: Int
) : RecyclerView.OnScrollListener() {

    private val tag = "EndlessRecyclerOnScrollListener_$fragmentTag"
    private var loading = AtomicBoolean(false)
    private var lastVisibleItem = 0
    private var prevLastVisibleItem = lastVisibleItem
    private var totalItemCount = 0

    private val currentPage = AtomicInteger(startWithPage)
    private var isEndReached = false

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        /**
         * True means that we are scrolling down, false otherwise.
         * When dy is greater than zero that means we are scrolling down
         * */
        scrollSubject.onNext(dy > 0)

        if (isEndReached) {
            return
        }

        totalItemCount = gridLayoutManager.itemCount
        lastVisibleItem = gridLayoutManager.findLastVisibleItemPosition() + 1

        if ((totalItemCount - lastVisibleItem) <= visibleThreshold) {
            if (Math.abs((lastVisibleItem - prevLastVisibleItem)) >= visibleThreshold) {
                prevLastVisibleItem = lastVisibleItem

                if (loading.compareAndSet(false, true)) {
                    Timber.tag(tag).d("Loading new page ${currentPage.get()}")
                    loadMoreSubject.onNext(currentPage.get())
                }
            }
        } 
    }

    fun pageLoading() {
        loading.set(true)
    }

    fun pageLoaded() {
        currentPage.incrementAndGet()
        loading.set(false)
    }

    fun reachedEnd() {
        isEndReached = true
    }

    fun reset() {
        loading.set(false)
        lastVisibleItem = 0
        prevLastVisibleItem = 0
        totalItemCount = 0
        currentPage.set(0)
        isEndReached = false
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("loading", loading.get())
        outState.putInt("currentPage", currentPage.get())
        outState.putInt("lastVisibleItem", lastVisibleItem)
        outState.putInt("prevLastVisibleItem", prevLastVisibleItem)
        outState.putInt("totalItemCount", totalItemCount)
        outState.putBoolean("isEndReached", isEndReached)
    }

    fun onRestoreInstanceState(savedInstanceState: Bundle) {
        loading.set(savedInstanceState.getBoolean("loading", false))
        currentPage.set(savedInstanceState.getInt("currentPage", 0))
        lastVisibleItem = savedInstanceState.getInt("lastVisibleItem", 0)
        prevLastVisibleItem = savedInstanceState.getInt("prevLastVisibleItem", 0)
        totalItemCount = savedInstanceState.getInt("totalItemCount", 0)
        isEndReached = savedInstanceState.getBoolean("isEndReached", false)
    }
}