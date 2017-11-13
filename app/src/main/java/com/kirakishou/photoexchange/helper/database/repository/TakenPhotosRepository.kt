package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.dao.TakenPhotosDao
import com.kirakishou.photoexchange.helper.database.entity.TakenPhotoEntity
import com.kirakishou.photoexchange.helper.mapper.TakenPhotoMapper
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvvm.model.other.TakenPhoto
import io.reactivex.Single

/**
 * Created by kirakishou on 11/10/2017.
 */
class TakenPhotosRepository(
        private val database: MyDatabase,
        private val schedulers: SchedulerProvider,
        private val mapper: TakenPhotoMapper
) {
    private val takenPhotosDao: TakenPhotosDao by lazy { database.takenPhotosDao() }

    fun saveOne(lon: Double, lat: Double, photoFilePath: String, userId: String): Single<Long> {
        val resultSingle = Single.fromCallable {
            takenPhotosDao.saveOne(TakenPhotoEntity.new(lon, lat, photoFilePath, userId))
        }

        return resultSingle
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
    }

    fun findAll(): Single<List<TakenPhoto>> {
        return takenPhotosDao.findAll()
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .map(mapper::toTakenPhotos)
    }

    fun deleteAll(): Single<Int> {
        return Single.fromCallable { takenPhotosDao.deleteAll() }
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
    }
}