package com.kirakishou.photoexchange.helper

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

class RxLifecycle : LifecycleObserver {
  private val lifecycleSubject = BehaviorSubject.createDefault(FragmentLifecycle.onDestroy)

  fun start(lifecycleOwner: LifecycleOwner) = lifecycleOwner.lifecycle.addObserver(this)
  fun stop(lifecycleOwner: LifecycleOwner) = lifecycleOwner.lifecycle.removeObserver(this)

  fun getLifecycleObservable(): Observable<FragmentLifecycle> = lifecycleSubject
  fun getCurrentLifecycle(): FragmentLifecycle = lifecycleSubject.value!!

  @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
  fun onCreate() {
    lifecycleSubject.onNext(FragmentLifecycle.onCreate)
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_START)
  fun onStart() {
    lifecycleSubject.onNext(FragmentLifecycle.onStop)
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
  fun onResume() {
    lifecycleSubject.onNext(FragmentLifecycle.onResume)
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
  fun onPause() {
    lifecycleSubject.onNext(FragmentLifecycle.onPause)
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
  fun onStop() {
    lifecycleSubject.onNext(FragmentLifecycle.onStop)
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
  fun onDestroy() {
    lifecycleSubject.onNext(FragmentLifecycle.onDestroy)
  }

  enum class FragmentState(val value: Int) {
    Created(0),
    Started(1),
    Resumed(2),
    Paused(3),
    Stopped(4),
    Destroyed(5)
  }

  enum class FragmentLifecycle(val value: Int) {
    onCreate(0),
    onStart(1),
    onResume(2),
    onPause(3),
    onStop(4),
    onDestroy(5);

    private fun getCurrentState(): FragmentState {
      return when (this) {
        onCreate -> FragmentState.Created
        onStart -> FragmentState.Started
        onResume -> FragmentState.Resumed
        onPause -> FragmentState.Paused
        onStop -> FragmentState.Stopped
        onDestroy -> FragmentState.Destroyed
      }
    }

    //FIXME: does not work when currentState == onResume and desiredState == onCreate
    fun isAtLeast(desiredState: FragmentState): Boolean {
      return getCurrentState() >= desiredState
    }
  }
}