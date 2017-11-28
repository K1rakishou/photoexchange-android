package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.helper.database.repository.PhotoAnswerRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mwvm.viewmodel.factory.TakePhotoActivityViewModelFactory
import dagger.Module
import dagger.Provides

/**
 * Created by kirakishou on 11/3/2017.
 */

@Module
class TakePhotoActivityModule {

    @PerActivity
    @Provides
    fun provideViewModelFactory(schedulers: SchedulerProvider,
                                takenPhotosRepo: TakenPhotosRepository,
                                photoAnswerRepo: PhotoAnswerRepository): TakePhotoActivityViewModelFactory {
        return TakePhotoActivityViewModelFactory(takenPhotosRepo, photoAnswerRepo, schedulers)
    }
}