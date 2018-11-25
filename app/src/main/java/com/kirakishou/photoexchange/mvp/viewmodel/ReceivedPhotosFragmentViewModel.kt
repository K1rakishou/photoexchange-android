package com.kirakishou.photoexchange.mvp.viewmodel

import androidx.fragment.app.FragmentActivity
import com.airbnb.mvrx.*
import com.kirakishou.fixmypc.photoexchange.BuildConfig
import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.helper.intercom.PhotosActivityViewModelIntercom
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.interactors.GetReceivedPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.viewmodel.state.ReceivedPhotosFragmentState
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.fragment.UploadedPhotosFragment
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

class ReceivedPhotosFragmentViewModel(
  initialState: ReceivedPhotosFragmentState,
  private val intercom: PhotosActivityViewModelIntercom,
  private val settingsRepository: SettingsRepository,
  private val getReceivedPhotosUseCase: GetReceivedPhotosUseCase,
  private val dispatchersProvider: DispatchersProvider
) : BaseMvRxViewModel<ReceivedPhotosFragmentState>(initialState, BuildConfig.DEBUG), CoroutineScope {
  private val TAG = "ReceivedPhotosFragmentViewModel"

  private val compositeDisposable = CompositeDisposable()
  private val job = Job()
  private val viewModelActor: SendChannel<ActorAction>

  var photosPerPage: Int = Constants.DEFAULT_PHOTOS_PER_PAGE_COUNT

  override val coroutineContext: CoroutineContext
    get() = job + dispatchersProvider.GENERAL()

  init {
    viewModelActor = actor(capacity = Channel.UNLIMITED) {
      consumeEach { action ->
        if (!isActive) {
          return@consumeEach
        }

        when (action) {
          ActorAction.LoadReceivedPhotos -> loadReceivedPhotosInternal()
        }.safe
      }
    }

    loadReceivedPhotos()
  }

  fun loadReceivedPhotos() {
    launch { viewModelActor.send(ActorAction.LoadReceivedPhotos) }
  }

  private suspend fun loadReceivedPhotosInternal() {
    withState { state ->
      if (state.receivedPhotosRequest is Loading) {
        return@withState
      }

      launch {
        val userId = settingsRepository.getUserId()
        val lastUploadedOn = state.receivedPhotos
          .lastOrNull()
          ?.uploadedOn
          ?: -1L

        val request = try {
          Success(loadPageOfReceivedPhotos(userId, lastUploadedOn, photosPerPage))
        } catch (error: Throwable) {
          Fail<List<ReceivedPhoto>>(error)
        }

        val receivedPhotos = request() ?: emptyList()

        setState {
          copy(
            receivedPhotosRequest = request,
            receivedPhotos = receivedPhotos
          )
        }
      }
    }
  }

  private suspend fun loadPageOfReceivedPhotos(
    userId: String,
    lastUploadedOn: Long,
    photosPerPage: Int
  ): List<ReceivedPhoto> {
    if (userId.isEmpty()) {
      return emptyList()
    }

    val result = getReceivedPhotosUseCase.loadPageOfPhotos(userId, lastUploadedOn, photosPerPage)
    when (result) {
      is Either.Value -> {
        intercom.tell<UploadedPhotosFragment>()
          .to(UploadedPhotosFragmentEvent.GeneralEvents.UpdateReceiverInfo(result.value))
        return result.value
      }
      is Either.Error -> {
        throw result.error
      }
    }
  }

  fun clear() {
    onCleared()
  }

  override fun onCleared() {
    super.onCleared()
    Timber.tag(TAG).d("onCleared()")

    compositeDisposable.dispose()
    job.cancel()
  }

  sealed class ActorAction {
    object LoadReceivedPhotos : ActorAction()
  }

  companion object : MvRxViewModelFactory<ReceivedPhotosFragmentState> {
    override fun create(
      activity: FragmentActivity,
      state: ReceivedPhotosFragmentState
    ): BaseMvRxViewModel<ReceivedPhotosFragmentState> {
      return (activity as PhotosActivity).viewModel.receivedPhotosFragmentViewModel
    }
  }
}