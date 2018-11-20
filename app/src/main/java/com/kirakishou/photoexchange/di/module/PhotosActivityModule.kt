package com.kirakishou.photoexchange.di.module

import androidx.lifecycle.ViewModelProviders
import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.ReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.interactors.*
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.viewmodel.GalleryFragmentViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.PhotosActivityViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.ReceivedPhotosFragmentViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.UploadedPhotosFragmentViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.factory.PhotosActivityViewModelFactory
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
  fun provideUploadedPhotosFragmentViewModel(takenPhotosRepository: TakenPhotosRepository,
                                             settingsRepository: SettingsRepository,
                                             getUploadedPhotosUseCase: GetUploadedPhotosUseCase,
                                             dispatchersProvider: DispatchersProvider): UploadedPhotosFragmentViewModel {
    return UploadedPhotosFragmentViewModel(
      takenPhotosRepository,
      settingsRepository,
      getUploadedPhotosUseCase,
      dispatchersProvider
    )
  }

  @PerActivity
  @Provides
  fun provideReceivedPhotosFragmentViewModel(settingsRepository: SettingsRepository,
                                             getReceivedPhotosUseCase: GetReceivedPhotosUseCase,
                                             dispatchersProvider: DispatchersProvider): ReceivedPhotosFragmentViewModel {
    return ReceivedPhotosFragmentViewModel(
      settingsRepository,
      getReceivedPhotosUseCase,
      dispatchersProvider
    )
  }

  @PerActivity
  @Provides
  fun provideGalleryFragmentViewModel(galleryPhotosUseCase: GetGalleryPhotosUseCase,
                                      dispatchersProvider: DispatchersProvider): GalleryFragmentViewModel {
    return GalleryFragmentViewModel(
      galleryPhotosUseCase,
      dispatchersProvider
    )
  }

  @PerActivity
  @Provides
  fun provideViewModelFactory(settingsRepository: SettingsRepository,
                              takenPhotosRepository: TakenPhotosRepository,
                              uploadedPhotosRepository: UploadedPhotosRepository,
                              receivedPhotosRepository: ReceivedPhotosRepository,
                              uploadedPhotosFragmentViewModel: UploadedPhotosFragmentViewModel,
                              receivedPhotosFragmentViewModel: ReceivedPhotosFragmentViewModel,
                              galleryFragmentViewModel: GalleryFragmentViewModel,
                              reportPhotoUseCase: ReportPhotoUseCase,
                              favouritePhotoUseCase: FavouritePhotoUseCase,
                              schedulerProvider: SchedulerProvider): PhotosActivityViewModelFactory {
    return PhotosActivityViewModelFactory(
      settingsRepository,
      takenPhotosRepository,
      uploadedPhotosRepository,
      receivedPhotosRepository,
      uploadedPhotosFragmentViewModel,
      receivedPhotosFragmentViewModel,
      galleryFragmentViewModel,
      reportPhotoUseCase,
      favouritePhotoUseCase,
      schedulerProvider)
  }

  @PerActivity
  @Provides
  fun provideViewModel(viewModelFactory: PhotosActivityViewModelFactory): PhotosActivityViewModel {
    return ViewModelProviders.of(activity, viewModelFactory).get(PhotosActivityViewModel::class.java)
  }
}