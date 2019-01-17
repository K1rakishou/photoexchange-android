package com.kirakishou.photoexchange.helper.util

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.os.Build
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import com.kirakishou.photoexchange.mvrx.model.PhotoSize
import timber.log.Timber


/**
 * Created by kirakishou on 7/30/2017.
 */
object AndroidUtils {
  const val SHADE_FACTOR = 0.9f

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

  fun isMarshmallowOrHigher(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
  }

  fun pxToSp(context: Context, px: Float): Float {
    val scaledDensity = context.resources.displayMetrics.scaledDensity
    return px / scaledDensity
  }

  fun spToPx(context: Context, sp: Float): Float {
    val scaledDensity = context.resources.displayMetrics.scaledDensity
    return sp * scaledDensity
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
    val viewWidthInDp = dpToPx(viewWidth.toFloat(), context).toInt()

    return when {
      screenSize / 4 >= viewWidthInDp -> 4
      screenSize / 3 >= viewWidthInDp -> 3
      screenSize / 2 >= viewWidthInDp -> 2
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

  fun getDarkerShade(color: Int): Int {
    return Color.rgb(
      (SHADE_FACTOR * Color.red(color)).toInt(),
      (SHADE_FACTOR * Color.green(color)).toInt(),
      (SHADE_FACTOR * Color.blue(color)).toInt()
    )
  }
}