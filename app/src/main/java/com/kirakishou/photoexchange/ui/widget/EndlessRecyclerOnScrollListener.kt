package com.kirakishou.photoexchange.ui.widget

import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.subjects.PublishSubject
import androidx.recyclerview.widget.LinearLayoutManager
import timber.log.Timber


class EndlessRecyclerOnScrollListener(
  private val fragmentTag: String,
  private val gridLayoutManager: GridLayoutManager,
  private val visibleThreshold: Int,
  private val loadMoreSubject: PublishSubject<Unit>,
  private val scrollSubject: PublishSubject<Boolean>
) : RecyclerView.OnScrollListener() {

  private val TAG = "EndlessRecyclerOnScrollListener_$fragmentTag"
  private var loading = false
  private var lastVisibleItem = 0
  private var prevLastVisibleItem = lastVisibleItem
  private var totalItemCount = 0

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

    //do nothing if the recyclerview is empty
    val totalItemCount = gridLayoutManager.itemCount
    if (totalItemCount == 0) {
      return
    }

    val visibleItemCount = recyclerView.childCount
    val firstVisibleItem = (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()

    if (!loading && totalItemCount - visibleItemCount <= firstVisibleItem + visibleThreshold) {
      loadMoreSubject.onNext(Unit)
      loading = true
    }
  }

  fun pageLoaded() {
    loading = false
  }

  fun reachedEnd() {
    isEndReached = true
  }

  fun reset() {
    if (isEndReached) {
      loading = false
      lastVisibleItem = 0
      prevLastVisibleItem = 0
      totalItemCount = 0
      isEndReached = false
    }
  }

  fun onSaveInstanceState(outState: Bundle) {
    outState.putBoolean("loading", loading)
    outState.putInt("lastVisibleItem", lastVisibleItem)
    outState.putInt("prevLastVisibleItem", prevLastVisibleItem)
    outState.putInt("totalItemCount", totalItemCount)
    outState.putBoolean("isEndReached", isEndReached)
  }

  fun onRestoreInstanceState(savedInstanceState: Bundle) {
    loading = savedInstanceState.getBoolean("loading", false)
    lastVisibleItem = savedInstanceState.getInt("lastVisibleItem", 0)
    prevLastVisibleItem = savedInstanceState.getInt("prevLastVisibleItem", 0)
    totalItemCount = savedInstanceState.getInt("totalItemCount", 0)
    isEndReached = savedInstanceState.getBoolean("isEndReached", false)
  }
}