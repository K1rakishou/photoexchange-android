package com.kirakishou.photoexchange.di.component.activity

interface BasePhotosActivityComponent<T> {
  fun inject(activity: T)
}