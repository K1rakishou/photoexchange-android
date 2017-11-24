package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.dao.TakenPhotosDao
import com.kirakishou.photoexchange.helper.database.entity.TakenPhotoEntity
import com.kirakishou.photoexchange.helper.mapper.TakenPhotoMapper
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mwvm.model.other.Pageable
import com.kirakishou.photoexchange.mwvm.model.other.TakenPhoto
import io.reactivex.Completable
import io.reactivex.Observable
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
            takenPhotosDao.saveOne(TakenPhotoEntity.new(lon, lat, userId, photoFilePath))
        }

        return resultSingle
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
    }

    fun findOne(id: Long): Single<TakenPhoto> {
        return takenPhotosDao.findOne(id)
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .map(mapper::toTakenPhoto)
    }

    fun findOnePage(pageable: Pageable): Observable<List<TakenPhoto>> {
        return takenPhotosDao.findPage(pageable.page, pageable.count)
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .map(mapper::toTakenPhotos)
                .toObservable()
    }

    fun findAllQueuedUp(): Single<List<TakenPhoto>> {
        return takenPhotosDao.findAllQueuedUp()
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .map(mapper::toTakenPhotos)
    }

    fun findAll(): Single<List<TakenPhoto>> {
        return takenPhotosDao.findAll()
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .map(mapper::toTakenPhotos)
    }

    fun countAll(): Single<Long> {
        return takenPhotosDao.countAll()
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
    }

    fun updateOneSetIsUploading(id: Long, isUploading: Boolean): Completable {
        val result = Completable.fromCallable {
            takenPhotosDao.updateOneSetIsUploading(isUploading, id)
        }

        return result
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
    }

    fun updateOneSetUploaded(id: Long, photoName: String): Completable {
        val result = Completable.fromCallable {
            takenPhotosDao.updateOneSetUploaded(photoName, id)
        }

        return result
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
    }

    fun deleteOne(id: Long): Single<Int> {
        return Single.fromCallable { takenPhotosDao.deleteOne(id) }
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
    }

    fun deleteAll(): Single<Int> {
        return Single.fromCallable { takenPhotosDao.deleteAll() }
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
    }
}