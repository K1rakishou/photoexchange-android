package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.interactors.FindPhotoAnswersUseCase
import io.reactivex.Single

class FindPhotoAnswerServicePresenter(
    private val myPhotosRepository: PhotosRepository,
    private val settingsRepository: SettingsRepository,
    private val schedulerProvider: SchedulerProvider,
    private val findPhotoAnswersUseCase: FindPhotoAnswersUseCase
) {

    fun startFindPhotoAnswers(): Single<Boolean> {
        return Single.fromCallable { findPhotoAnswersUseCase.getPhotoAnswers() }
            .subscribeOn(schedulerProvider.BG())
            .observeOn(schedulerProvider.BG())
    }
}