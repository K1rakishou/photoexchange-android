package com.kirakishou.photoexchange.mvp.viewmodel.state

sealed class UpdateStateResult<T> {
  class Update<T>(val update: T) : UpdateStateResult<T>()
  class SendIntercom<T> : UpdateStateResult<T>()
  class NothingToUpdate<T> : UpdateStateResult<T>()
}