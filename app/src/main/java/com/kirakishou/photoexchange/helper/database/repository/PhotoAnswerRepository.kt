package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.dao.PhotoAnswerDao
import com.kirakishou.photoexchange.helper.database.entity.PhotoAnswerEntity
import com.kirakishou.photoexchange.helper.mapper.PhotoAnswerMapper
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mwvm.model.other.Pageable
import com.kirakishou.photoexchange.mwvm.model.other.PhotoAnswer
import io.reactivex.Observable
import io.reactivex.Single

/**
 * Created by kirakishou on 11/14/2017.
 */
class PhotoAnswerRepository(
        private val database: MyDatabase,
        private val schedulers: SchedulerProvider,
        private val mapper: PhotoAnswerMapper
) {
    private val photoAnswerDao: PhotoAnswerDao by lazy { database.photoAnswerDao() }

    fun saveOne(photoAnswer: PhotoAnswer): Single<Long> {
        val resultSingle = Single.fromCallable {
            val entity = PhotoAnswerEntity.new(photoAnswer.photoRemoteId, photoAnswer.userId, photoAnswer.photoName, photoAnswer.lon, photoAnswer.lat)
            return@fromCallable photoAnswerDao.saveOne(entity)
        }

        return resultSingle
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
    }

    fun saveMany(photoAnswerList: List<PhotoAnswer>): Single<List<Long>> {
        val resultSingle = Single.fromCallable {
            val photoAnswerArray = photoAnswerList
                    .map { PhotoAnswerEntity.new(it.photoRemoteId, it.userId, it.photoName, it.lon, it.lat) }
                    .toTypedArray()

            return@fromCallable photoAnswerDao.saveMany(*photoAnswerArray)
        }

        return resultSingle
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
    }

    fun findOnePage(pageable: Pageable): Observable<List<PhotoAnswer>> {
        return photoAnswerDao.findPage(pageable.page, pageable.count, TimeUtils.getTimeFast())
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .map(mapper::toPhotoAnswers)
                .toObservable()
    }

    fun findAll(): Single<List<PhotoAnswer>> {
        return photoAnswerDao.findAll()
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .map(mapper::toPhotoAnswers)
    }

    fun findAllDebug(): Single<List<PhotoAnswerEntity>> {
        return photoAnswerDao.findAll()
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
    }

    fun countAll(): Single<Long> {
        return photoAnswerDao.countAll()
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
    }

    fun deleteOne(photoName: String): Single<Int> {
        return Single.fromCallable { photoAnswerDao.deleteOne(photoName) }
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
    }
}