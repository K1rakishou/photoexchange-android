package com.kirakishou.photoexchange.ui.epoxy.row

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.kirakishou.fixmypc.photoexchange.R

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class SectionRow @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
  private val sectionText: TextView

  init {
    inflate(context, R.layout.epoxy_adapter_item_section, this)

    sectionText = findViewById(R.id.section_text)
  }

  @ModelProp
  fun setText(text: String) {
    sectionText.text = text
  }
}