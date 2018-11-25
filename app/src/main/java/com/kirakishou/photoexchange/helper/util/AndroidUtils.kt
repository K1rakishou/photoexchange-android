package com.kirakishou.photoexchange.helper.util

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.os.Build
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import com.kirakishou.photoexchange.helper.PhotoSize
import timber.log.Timber


/**
 * Created by kirakishou on 7/30/2017.
 */
object AndroidUtils {

  fun checkIsOnMainThread(): Boolean {
    return Looper.myLooper() == Looper.getMainLooper()
  }

  fun throwIfOnMainThread() {
    if (checkIsOnMainThread()) {
      Timber.d("Current operation cannot be executed on the main thread")
      throw RuntimeException("Current operation cannot be executed on the main thread")
    }
  }

  fun isOreoOrHigher(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
  }

  fun pxToSp(context: Context, px: Float): Float {
    val scaledDensity = context.resources.displayMetrics.scaledDensity
    return px / scaledDensity
  }

  fun dpToPx(dp: Float, context: Context): Float {
    val resources = context.resources
    val metrics = resources.displayMetrics
    val px = dp * (metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    return px
  }

  fun pxToDp(px: Float, context: Context): Float {
    val resources = context.resources
    val metrics = resources.displayMetrics
    val dp = px / (metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    return dp
  }

  fun getScreenWidth(context: Context): Int {
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val display = wm.defaultDisplay
    val size = Point()
    display.getSize(size)

    return size.x
  }

  fun hideSoftKeyboard(activity: Activity) {
    val inputMethodManager = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager

    activity.currentFocus?.let { cf ->
      inputMethodManager.hideSoftInputFromWindow(cf.windowToken, 0)
    }
  }

  fun getScreenSize(context: Context): Int {
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val display = wm.defaultDisplay
    val size = Point()
    display.getSize(size)

    return size.x
  }

  fun calculateNoOfColumns(context: Context, viewWidth: Int): Int {
    val screenSize = getScreenSize(context)
    val dp = dpToPx(viewWidth.toFloat(), context).toInt()

    return when {
      screenSize / 4 >= dp -> 4
      screenSize / 3 >= dp -> 3
      screenSize / 2 >= dp -> 2
      else -> 1
    }
  }

  fun figureOutPhotosSizes(context: Context): PhotoSize {
    val density = context.resources.displayMetrics.density

    return if (density < 2.0) {
      PhotoSize.Small
    } else if (density >= 2.0 && density < 3.0) {
      PhotoSize.Medium
    } else {
      PhotoSize.Big
    }
  }
}