package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.dao.UploadedPhotosDao
import com.kirakishou.photoexchange.helper.database.entity.UploadedPhotoEntity
import com.kirakishou.photoexchange.helper.mapper.UploadedPhotoMapper
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvvm.model.Pageable
import com.kirakishou.photoexchange.mvvm.model.UploadedPhoto
import io.reactivex.Flowable

/**
 * Created by kirakishou on 11/8/2017.
 */
class UploadedPhotosRepository(
        private val database: MyDatabase,
        private val schedulers: SchedulerProvider,
        private val uploadedPhotoMapper: UploadedPhotoMapper
) {
    private val uploadedPhotosDao: UploadedPhotosDao by lazy { database.uploadedPhotosDao() }

    fun saveOne(lon: Double, lat: Double, userId: String, photoFilePath: String): Flowable<Long> {
        return Flowable.fromCallable { uploadedPhotosDao.saveOne(UploadedPhotoEntity.new(lon, lat, userId, photoFilePath)) }
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
    }

    fun findOne(id: Long): Flowable<UploadedPhoto> {
        return uploadedPhotosDao.findOne(id)
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .map(uploadedPhotoMapper::toTakenPhoto)
    }

    fun findOnePage(pageable: Pageable): Flowable<List<UploadedPhoto>> {
        return uploadedPhotosDao.findPage(pageable.page, pageable.count)
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .map(uploadedPhotoMapper::toTakenPhoto)
    }

    fun findAll(): Flowable<List<UploadedPhoto>> {
        return uploadedPhotosDao.findAll()
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .map(uploadedPhotoMapper::toTakenPhoto)
    }

    fun deleteOne(id: Long): Flowable<Int> {
        return Flowable.fromCallable { uploadedPhotosDao.deleteOne(id) }
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
    }

    fun deleteAllNotSent(): Flowable<Int> {
        return Flowable.fromCallable { uploadedPhotosDao.deleteAll() }
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
    }
}















