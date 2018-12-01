package com.kirakishou.photoexchange.helper

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

class RxLifecycle(
  private val lifecycleOwner: LifecycleOwner
) : LifecycleObserver {
  private val TAG = "RxLifecycle"
  private val lifecycleSubject = BehaviorSubject.createDefault(Lifecycle.State.INITIALIZED)

  fun start() = lifecycleOwner.lifecycle.addObserver(this)
  fun stop() = lifecycleOwner.lifecycle.removeObserver(this)

  fun getStateObservable(): Observable<Lifecycle.State> = lifecycleSubject
  fun getCurrentState(): Lifecycle.State = lifecycleSubject.value!!

  @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
  fun onCreate() {
    lifecycleSubject.onNext(Lifecycle.State.CREATED)
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_START)
  fun onStart() {
    lifecycleSubject.onNext(Lifecycle.State.STARTED)
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
  fun onResume() {
    lifecycleSubject.onNext(Lifecycle.State.RESUMED)
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
  fun onPause() {
    lifecycleSubject.onNext(Lifecycle.State.RESUMED)
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
  fun onStop() {
    lifecycleSubject.onNext(Lifecycle.State.STARTED)
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
  fun onDestroy() {
    lifecycleSubject.onNext(Lifecycle.State.DESTROYED)
  }

  fun isAtLeast(currentState: Lifecycle.State, desiredState: Lifecycle.State): Boolean {
    return currentState.isAtLeast(desiredState)
  }
}