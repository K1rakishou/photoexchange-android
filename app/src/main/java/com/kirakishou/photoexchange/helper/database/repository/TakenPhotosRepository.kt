package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.dao.TakenPhotosDao
import com.kirakishou.photoexchange.helper.database.entity.TakenPhotoEntity
import com.kirakishou.photoexchange.helper.mapper.TakenPhotoMapper
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvvm.model.Pageable
import com.kirakishou.photoexchange.mvvm.model.TakenPhoto
import io.reactivex.Single
import timber.log.Timber

/**
 * Created by kirakishou on 11/8/2017.
 */
class TakenPhotosRepository(
        private val database: MyDatabase,
        private val schedulers: SchedulerProvider,
        private val takenPhotoMapper: TakenPhotoMapper
) {
    private val takenPhotosDao: TakenPhotosDao by lazy { database.takenPhotosDao() }

    fun saveOne(lon: Double, lat: Double, userId: String, photoFilePath: String): Long {
        return takenPhotosDao.saveOne(TakenPhotoEntity.new(lon, lat, userId, photoFilePath))
    }

    fun findLastSaved(): Single<TakenPhoto> {
        return takenPhotosDao.findLastSaved()
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .map(takenPhotoMapper::toTakenPhoto)
                .first(TakenPhoto.empty())
    }

    fun findOnePage(pageable: Pageable): Single<List<TakenPhoto>> {
        return takenPhotosDao.findPage(pageable.page, pageable.count)
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .map(takenPhotoMapper::toTakenPhoto)
                .first(emptyList())
    }

    fun findAll(): List<TakenPhoto> {
        return takenPhotosDao.findAll()
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .map(takenPhotoMapper::toTakenPhoto)
                .blockingFirst()
    }
}