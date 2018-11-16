package com.kirakishou.photoexchange.helper.concurrency.rx.scheduler

import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers

class TestSchedulers : SchedulerProvider {
  override fun IO(): Scheduler = Schedulers.trampoline()
  override fun CALC(): Scheduler = Schedulers.trampoline()
  override fun UI(): Scheduler = Schedulers.trampoline()
}