package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.dao.UploadedPhotosDao
import com.kirakishou.photoexchange.helper.database.entity.UploadedPhotoEntity
import com.kirakishou.photoexchange.helper.mapper.UploadedPhotoMapper
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvvm.model.Pageable
import com.kirakishou.photoexchange.mvvm.model.UploadedPhoto
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Created by kirakishou on 11/8/2017.
 */
class UploadedPhotosRepository(
        private val database: MyDatabase,
        private val schedulers: SchedulerProvider,
        private val uploadedPhotoMapper: UploadedPhotoMapper
) {
    private val uploadedPhotosDao: UploadedPhotosDao by lazy { database.uploadedPhotosDao() }
    private val MAX_DB_WAIT_TIME = 3L

    fun saveOne(lon: Double, lat: Double, userId: String, photoFilePath: String, photoName: String): Single<Long> {
        val resultSingle = Single.fromCallable {
            uploadedPhotosDao.saveOne(UploadedPhotoEntity.new(lon, lat, userId, photoFilePath, photoName))
        }

        return resultSingle
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
    }

    fun findOne(id: Long): Flowable<UploadedPhoto> {
        return uploadedPhotosDao.findOne(id)
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .map(uploadedPhotoMapper::toUploadedPhoto)
    }

    fun findOnePage(pageable: Pageable): Observable<List<UploadedPhoto>> {
        return uploadedPhotosDao.findPage(pageable.page, pageable.count)
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .map(uploadedPhotoMapper::toUploadedPhotos)
                .toObservable()
    }

    fun findAll(): Single<List<UploadedPhoto>> {
        return uploadedPhotosDao.findAll()
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .map(uploadedPhotoMapper::toUploadedPhotos)
    }

    fun deleteOne(id: Long): Flowable<Int> {
        return Flowable.fromCallable { uploadedPhotosDao.deleteOne(id) }
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
    }

    fun deleteAll(): Single<Int> {
        return Single.fromCallable { uploadedPhotosDao.deleteAll() }
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
    }
}















