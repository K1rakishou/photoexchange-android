package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.dao.UploadedPhotosDao
import com.kirakishou.photoexchange.helper.database.entity.UploadedPhotoEntity
import com.kirakishou.photoexchange.helper.mapper.UploadedPhotoMapper
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mwvm.model.other.Pageable
import com.kirakishou.photoexchange.mwvm.model.other.UploadedPhoto
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single

/**
 * Created by kirakishou on 11/8/2017.
 */
class UploadedPhotosRepository(
        private val database: MyDatabase,
        private val schedulers: SchedulerProvider,
        private val uploadedPhotoMapper: UploadedPhotoMapper
) {
    private val uploadedPhotosDao: UploadedPhotosDao by lazy { database.uploadedPhotosDao() }

    fun saveOne(id: Long, lon: Double, lat: Double, userId: String, photoFilePath: String, photoName: String): Single<Long> {
        val resultSingle = Single.fromCallable {
            uploadedPhotosDao.saveOne(UploadedPhotoEntity.new(id, lon, lat, userId, photoFilePath, photoName))
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

    fun countAll(): Single<Long> {
        return uploadedPhotosDao.countAll()
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
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















