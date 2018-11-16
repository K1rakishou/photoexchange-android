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

  private val tag = "EndlessRecyclerOnScrollListener_$fragmentTag"
  private var loading = false
  private var previousTotal = 0
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

    val visibleItemCount = recyclerView.childCount
    val totalItemCount = gridLayoutManager.itemCount
    val firstVisibleItem = (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()

    if (loading) {
      if (totalItemCount > previousTotal) {
        loading = false
        previousTotal = totalItemCount
      }
    }

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