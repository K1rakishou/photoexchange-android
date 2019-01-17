package com.kirakishou.photoexchange.di.module.activity

import androidx.lifecycle.ViewModelProviders
import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.*
import com.kirakishou.photoexchange.helper.intercom.PhotosActivityViewModelIntercom
import com.kirakishou.photoexchange.helper.util.NetUtils
import com.kirakishou.photoexchange.usecases.*
import com.kirakishou.photoexchange.mvrx.viewmodel.GalleryFragmentViewModel
import com.kirakishou.photoexchange.mvrx.viewmodel.PhotosActivityViewModel
import com.kirakishou.photoexchange.mvrx.viewmodel.ReceivedPhotosFragmentViewModel
import com.kirakishou.photoexchange.mvrx.viewmodel.UploadedPhotosFragmentViewModel
import com.kirakishou.photoexchange.mvrx.viewmodel.factory.PhotosActivityViewModelFactory
import com.kirakishou.photoexchange.mvrx.viewmodel.state.GalleryFragmentState
import com.kirakishou.photoexchange.mvrx.viewmodel.state.ReceivedPhotosFragmentState
import com.kirakishou.photoexchange.mvrx.viewmodel.state.UploadedPhotosFragmentState
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
                                             getUploadedPhotosUseCase: GetUploadedPhotosUseCase,
                                             getFreshPhotosUseCase: GetFreshPhotosUseCase,
                                             dispatchersProvider: DispatchersProvider): UploadedPhotosFragmentViewModel {
    return UploadedPhotosFragmentViewModel(
      viewState,
      intercom,
      takenPhotosRepository,
      uploadedPhotosRepository,
      getUploadedPhotosUseCase,
      getFreshPhotosUseCase,
      dispatchersProvider
    )
  }

  @PerActivity
  @Provides
  fun provideReceivedPhotosFragmentViewModel(intercom: PhotosActivityViewModelIntercom,
                                             viewState: ReceivedPhotosFragmentState,
                                             receivedPhotosRepository: ReceivedPhotosRepository,
                                             getReceivedPhotosUseCase: GetReceivedPhotosUseCase,
                                             favouritePhotoUseCase: FavouritePhotoUseCase,
                                             reportPhotoUseCase: ReportPhotoUseCase,
                                             getPhotoAdditionalInfoUseCase: GetPhotoAdditionalInfoUseCase,
                                             getFreshPhotosUseCase: GetFreshPhotosUseCase,
                                             dispatchersProvider: DispatchersProvider): ReceivedPhotosFragmentViewModel {
    return ReceivedPhotosFragmentViewModel(
      viewState,
      intercom,
      receivedPhotosRepository,
      getReceivedPhotosUseCase,
      favouritePhotoUseCase,
      reportPhotoUseCase,
      getPhotoAdditionalInfoUseCase,
      getFreshPhotosUseCase,
      dispatchersProvider
    )
  }

  @PerActivity
  @Provides
  fun provideGalleryFragmentViewModel(intercom: PhotosActivityViewModelIntercom,
                                      viewState: GalleryFragmentState,
                                      galleryPhotosRepository: GalleryPhotosRepository,
                                      getGalleryPhotosUseCase: GetGalleryPhotosUseCase,
                                      favouritePhotoUseCase: FavouritePhotoUseCase,
                                      getFreshPhotosUseCase: GetFreshPhotosUseCase,
                                      reportPhotoUseCase: ReportPhotoUseCase,
                                      dispatchersProvider: DispatchersProvider): GalleryFragmentViewModel {
    return GalleryFragmentViewModel(
      viewState,
      intercom,
      galleryPhotosRepository,
      getGalleryPhotosUseCase,
      favouritePhotoUseCase,
      getFreshPhotosUseCase,
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
                              netUtils: NetUtils,
                              settingsRepository: SettingsRepository,
                              takenPhotosRepository: TakenPhotosRepository,
                              uploadedPhotosRepository: UploadedPhotosRepository,
                              receivedPhotosRepository: ReceivedPhotosRepository,
                              blacklistPhotoUseCase: BlacklistPhotoUseCase,
                              checkFirebaseAvailabilityUseCase: CheckFirebaseAvailabilityUseCase,
                              dispatchersProvider: DispatchersProvider): PhotosActivityViewModelFactory {
    return PhotosActivityViewModelFactory(
      uploadedPhotosFragmentViewModel,
      receivedPhotosFragmentViewModel,
      galleryFragmentViewModel,
      intercom,
      netUtils,
      settingsRepository,
      takenPhotosRepository,
      uploadedPhotosRepository,
      receivedPhotosRepository,
      blacklistPhotoUseCase,
      checkFirebaseAvailabilityUseCase,
      dispatchersProvider
    )
  }

  @PerActivity
  @Provides
  fun provideViewModel(viewModelFactory: PhotosActivityViewModelFactory): PhotosActivityViewModel {
    return ViewModelProviders.of(activity, viewModelFactory).get(PhotosActivityViewModel::class.java)
  }
}