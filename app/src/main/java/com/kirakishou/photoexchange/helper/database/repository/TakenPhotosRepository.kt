package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.dao.TakenPhotosDao
import com.kirakishou.photoexchange.helper.database.entity.TakenPhotoEntity
import com.kirakishou.photoexchange.helper.mapper.TakenPhotoMapper
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mwvm.model.other.Pageable
import com.kirakishou.photoexchange.mwvm.model.state.PhotoState
import com.kirakishou.photoexchange.mwvm.model.other.TakenPhoto
import io.reactivex.Completable
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

    fun saveOne(lon: Double, lat: Double, photoFilePath: String, userId: String, state: PhotoState): Single<Long> {
        val resultSingle = Single.fromCallable {
            takenPhotosDao.saveOne(TakenPhotoEntity.new(lon, lat, userId, photoFilePath, state))
        }

        return resultSingle
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
    }

    fun findOne(id: Long): Single<TakenPhoto> {
        return takenPhotosDao.findOne(id)
                .onErrorReturn { TakenPhotoEntity() }
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .map(mapper::toTakenPhoto)
    }

    fun findOnePage(pageable: Pageable): Single<List<TakenPhoto>> {
        return takenPhotosDao.findPage(pageable.page, pageable.count)
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .map(mapper::toTakenPhotos)
    }

    fun findAllQueuedUp(): Single<List<TakenPhoto>> {
        return takenPhotosDao.findAllQueuedUp()
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .map(mapper::toTakenPhotos)
    }

    fun findAllFailedToUpload(): Single<List<TakenPhoto>> {
        return takenPhotosDao.findAllFailedToUpload()
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .map(mapper::toTakenPhotos)
    }

    fun findAllTaken(): Single<List<TakenPhoto>> {
        return takenPhotosDao.findAllTaken()
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

    fun countQueuedUp(): Single<Long> {
        return takenPhotosDao.countQueuedUp()
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
    }

    fun updateSetUploaded(id: Long, photoName: String): Completable {
        val result = Completable.fromCallable {
            database.runInTransaction {
                takenPhotosDao.updateSetState(PhotoState.UPLOADED_STATE, id)
                takenPhotosDao.updateSetPhotoName(photoName, id)
            }
        }

        return result
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
    }

    fun updateSetFailedToUpload(id: Long): Completable {
        val result = Completable.fromCallable {
            takenPhotosDao.updateSetState(PhotoState.FAILED_TO_UPLOAD_STATE, id)
        }

        return result
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
    }

    fun updateSetQueuedUp(id: Long): Completable {
        val result = Completable.fromCallable {
            takenPhotosDao.updateSetState(PhotoState.QUEUED_UP_STATE, id)
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

    fun deleteManyById(ids: List<Long>): Single<Int> {
        return Single.fromCallable { takenPhotosDao.deleteManyById(ids) }
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
    }

    /*fun deleteAll(): Single<Int> {
       return Single.fromCallable { takenPhotosDao.deleteAll() }
               .subscribeOn(schedulers.provideIo())
               .observeOn(schedulers.provideIo())
   }*/

    //Debug DB requests
    fun findAllDebug(): Single<List<TakenPhotoEntity>> {
        return takenPhotosDao.findAll()
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
    }
}