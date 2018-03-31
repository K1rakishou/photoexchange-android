package com.kirakishou.photoexchange.helper.concurrency.rx.scheduler

import io.reactivex.Scheduler

/**
 * Created by kirakishou on 9/17/2017.
 */
interface SchedulerProvider {
    fun BG(): Scheduler
    fun UI(): Scheduler
}