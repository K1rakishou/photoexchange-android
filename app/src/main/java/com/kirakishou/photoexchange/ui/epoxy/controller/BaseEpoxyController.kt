package com.kirakishou.photoexchange.ui.epoxy.controller

import io.reactivex.disposables.CompositeDisposable

abstract class BaseEpoxyController {
  protected val compositeDisposable = CompositeDisposable()

  fun destroy() {
  }
}