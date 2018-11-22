package com.kirakishou.photoexchange.ui.adapter.epoxy

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.kirakishou.fixmypc.photoexchange.R

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class FooterTextRow @JvmOverloads constructor(
  context: Context, 
  attrs: AttributeSet? = null, 
  defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

  private val footerTextView: TextView

  init {
    inflate(context, R.layout.adapter_item_message, this)
    footerTextView = findViewById(R.id.message)
  }

  @ModelProp
  fun setText(text: String) {
    footerTextView.text = text
  }

  @CallbackProp
  fun setCallback(clickListener: OnClickListener?) {
    setOnClickListener(clickListener)
  }
}