package com.kirakishou.photoexchange.helper.util

interface NetUtils {
  suspend fun canLoadImages(): Boolean
  suspend fun canAccessNetwork(): Boolean
}