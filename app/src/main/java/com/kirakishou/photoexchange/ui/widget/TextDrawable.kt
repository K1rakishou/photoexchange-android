package com.kirakishou.photoexchange.ui.widget

import android.graphics.*
import android.graphics.drawable.Drawable


class TextDrawable(
  private val text: String,
  private val textColor: Int,
  private val fontSize: Float,
  private val w: Int,
  private val h: Int
) : Drawable() {
  private val textPaint: Paint = Paint().apply {
    color = textColor
    isAntiAlias = true
    style = Paint.Style.FILL
    textAlign = Paint.Align.CENTER
  }

  override fun draw(canvas: Canvas) {
    val r = bounds

    val count = canvas.save()
    canvas.drawARGB(255, 228, 228, 228)
    canvas.translate(r.left.toFloat(), r.top.toFloat())

    val width = if (w < 0) {
      r.width()
    } else {
      w
    }

    val height = if (h < 0) {
      r.height()
    } else {
      h
    }

    textPaint.textSize = if (fontSize < 0) {
      (Math.min(width, height) / 2).toFloat()
    } else {
      fontSize
    }

    canvas.drawText(
      text,
      (width / 2).toFloat(),
      height / 2 - (textPaint.descent() + textPaint.ascent()) / 2,
      textPaint
    )

    canvas.restoreToCount(count)
  }

  override fun setAlpha(alpha: Int) {
    textPaint.alpha = alpha
  }

  override fun setColorFilter(cf: ColorFilter?) {
    textPaint.colorFilter = cf
  }

  override fun getOpacity(): Int {
    return PixelFormat.TRANSLUCENT
  }

  override fun getIntrinsicWidth(): Int {
    return w
  }

  override fun getIntrinsicHeight(): Int {
    return h
  }
}