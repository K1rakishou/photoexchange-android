package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.dao.PhotoAnswerDao
import com.kirakishou.photoexchange.helper.database.entity.PhotoAnswerEntity
import com.kirakishou.photoexchange.helper.mapper.PhotoAnswerMapper
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mwvm.model.other.PhotoAnswer
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

    fun saveMany(photoAnswerList: List<PhotoAnswer>): Single<List<Long>> {
        val resultSingle = Single.fromCallable {
            val photoAnswerArray = photoAnswerList
                    .map { PhotoAnswerEntity.new(it.photoRemoteId, it.userId, it.photoName, it.lon, it.lat) }
                    .toTypedArray()

            photoAnswerDao.saveMany(*photoAnswerArray)
        }

        return resultSingle
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
    }

    fun findAll(): Single<List<PhotoAnswer>> {
        return photoAnswerDao.findAll()
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .map(mapper::toPhotoAnswers)
    }
}