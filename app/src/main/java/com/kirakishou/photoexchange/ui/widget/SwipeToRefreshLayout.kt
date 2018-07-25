package com.kirakishou.photoexchange.ui.widget

import android.content.Context
import android.support.v4.widget.SwipeRefreshLayout
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration

open class SwipeToRefreshLayout(
    private val _context: Context,
    attributes: AttributeSet
) : SwipeRefreshLayout(_context, attributes) {

    private var touchSlope = 0
    private var prevX = 0f

    init {
        touchSlope = ViewConfiguration.get(_context).scaledTouchSlop
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                prevX = MotionEvent.obtain(event).x
            }
            MotionEvent.ACTION_MOVE -> {
                val eventX = event.x
                val xDiff = Math.abs(eventX - prevX)

                if (xDiff > touchSlope) {
                    return false
                }
            }
        }

        return super.onInterceptTouchEvent(event)
    }
}