package com.kirakishou.photoexchange.helper.concurrency.scheduler

import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * Created by kirakishou on 9/17/2017.
 */
class NormalSchedulers : SchedulerProvider {
    override fun provideIo(): Scheduler = Schedulers.io()
    override fun provideMain(): Scheduler = AndroidSchedulers.mainThread()
}