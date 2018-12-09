package com.kirakishou.photoexchange.di.module.activity

import androidx.lifecycle.ViewModelProviders
import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.ReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.intercom.PhotosActivityViewModelIntercom
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.interactors.*
import com.kirakishou.photoexchange.mvp.viewmodel.GalleryFragmentViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.PhotosActivityViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.ReceivedPhotosFragmentViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.UploadedPhotosFragmentViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.factory.PhotosActivityViewModelFactory
import com.kirakishou.photoexchange.mvp.viewmodel.state.GalleryFragmentState
import com.kirakishou.photoexchange.mvp.viewmodel.state.ReceivedPhotosFragmentState
import com.kirakishou.photoexchange.mvp.viewmodel.state.UploadedPhotosFragmentState
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import dagger.Module
import dagger.Provides

/**
 * Created by kirakishou on 3/11/2018.
 */

@Module
open class PhotosActivityModule(
  val activity: PhotosActivity
) {

  @PerActivity
  @Provides
  fun providePhotosActivityViewModelIntercom(): PhotosActivityViewModelIntercom {
    return PhotosActivityViewModelIntercom()
  }

  @PerActivity
  @Provides
  fun provideUploadedPhotosFragmentState(): UploadedPhotosFragmentState {
    return UploadedPhotosFragmentState()
  }

  @PerActivity
  @Provides
  fun provideReceivedPhotosFragmentState(): ReceivedPhotosFragmentState {
    return ReceivedPhotosFragmentState()
  }

  @PerActivity
  @Provides
  fun provideGalleryFragmentState(): GalleryFragmentState {
    return GalleryFragmentState()
  }

  @PerActivity
  @Provides
  fun provideUploadedPhotosFragmentViewModel(intercom: PhotosActivityViewModelIntercom,
                                             viewState: UploadedPhotosFragmentState,
                                             takenPhotosRepository: TakenPhotosRepository,
                                             uploadedPhotosRepository: UploadedPhotosRepository,
                                             receivedPhotosRepository: ReceivedPhotosRepository,
                                             getUploadedPhotosUseCase: GetUploadedPhotosUseCase,
                                             timeUtils: TimeUtils,
                                             dispatchersProvider: DispatchersProvider): UploadedPhotosFragmentViewModel {
    return UploadedPhotosFragmentViewModel(
      viewState,
      intercom,
      takenPhotosRepository,
      uploadedPhotosRepository,
      receivedPhotosRepository,
      getUploadedPhotosUseCase,
      timeUtils,
      dispatchersProvider
    )
  }

  @PerActivity
  @Provides
  fun provideReceivedPhotosFragmentViewModel(intercom: PhotosActivityViewModelIntercom,
                                             viewState: ReceivedPhotosFragmentState,
                                             timeUtils: TimeUtils,
                                             receivedPhotosRepository: ReceivedPhotosRepository,
                                             getReceivedPhotosUseCase: GetReceivedPhotosUseCase,
                                             dispatchersProvider: DispatchersProvider): ReceivedPhotosFragmentViewModel {
    return ReceivedPhotosFragmentViewModel(
      viewState,
      intercom,
      timeUtils,
      receivedPhotosRepository,
      getReceivedPhotosUseCase,
      dispatchersProvider
    )
  }

  @PerActivity
  @Provides
  fun provideGalleryFragmentViewModel(intercom: PhotosActivityViewModelIntercom,
                                      viewState: GalleryFragmentState,
                                      getGalleryPhotosUseCase: GetGalleryPhotosUseCase,
                                      favouritePhotoUseCase: FavouritePhotoUseCase,
                                      reportPhotoUseCase: ReportPhotoUseCase,
                                      dispatchersProvider: DispatchersProvider): GalleryFragmentViewModel {
    return GalleryFragmentViewModel(
      viewState,
      intercom,
      getGalleryPhotosUseCase,
      favouritePhotoUseCase,
      reportPhotoUseCase,
      dispatchersProvider
    )
  }

  @PerActivity
  @Provides
  fun provideViewModelFactory(uploadedPhotosFragmentViewModel: UploadedPhotosFragmentViewModel,
                              receivedPhotosFragmentViewModel: ReceivedPhotosFragmentViewModel,
                              galleryFragmentViewModel: GalleryFragmentViewModel,
                              intercom: PhotosActivityViewModelIntercom,
                              settingsRepository: SettingsRepository,
                              takenPhotosRepository: TakenPhotosRepository,
                              uploadedPhotosRepository: UploadedPhotosRepository,
                              receivedPhotosRepository: ReceivedPhotosRepository,
                              blacklistPhotoUseCase: BlacklistPhotoUseCase): PhotosActivityViewModelFactory {
    return PhotosActivityViewModelFactory(
      uploadedPhotosFragmentViewModel,
      receivedPhotosFragmentViewModel,
      galleryFragmentViewModel,
      intercom,
      settingsRepository,
      takenPhotosRepository,
      uploadedPhotosRepository,
      receivedPhotosRepository,
      blacklistPhotoUseCase
    )
  }

  @PerActivity
  @Provides
  fun provideViewModel(viewModelFactory: PhotosActivityViewModelFactory): PhotosActivityViewModel {
    return ViewModelProviders.of(activity, viewModelFactory).get(PhotosActivityViewModel::class.java)
  }
}