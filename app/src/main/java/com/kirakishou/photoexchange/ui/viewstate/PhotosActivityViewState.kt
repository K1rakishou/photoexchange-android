package com.kirakishou.photoexchange.ui.viewstate

import android.os.Bundle

class PhotosActivityViewState(
  var lastOpenedTab: Int = 0
) : BaseViewState {

  override fun saveToBundle(bundle: Bundle?) {
    if (bundle == null) {
      return
    }

    bundle.putInt(LAST_OPENED_TAB, lastOpenedTab)
  }

  override fun loadFromBundle(bundle: Bundle?) {
    if (bundle != null) {
      lastOpenedTab = bundle.getInt(LAST_OPENED_TAB, 0)
    }
  }

  companion object {
    const val LAST_OPENED_TAB = "last_opened_tab"
  }
}