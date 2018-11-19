package com.kirakishou.photoexchange.helper

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

class RxLifecycle {
  private val lifecycleSubject = BehaviorSubject.createDefault(FragmentLifecycle.onDestroy)

  fun getLifecycle(): Observable<FragmentLifecycle> = lifecycleSubject

  fun onCreate() {
    lifecycleSubject.onNext(FragmentLifecycle.onCreate)
  }

  fun onStart() {
    lifecycleSubject.onNext(FragmentLifecycle.onStop)
  }

  fun onResume() {
    lifecycleSubject.onNext(FragmentLifecycle.onResume)
  }

  fun onPause() {
    lifecycleSubject.onNext(FragmentLifecycle.onPause)
  }

  fun onStop() {
    lifecycleSubject.onNext(FragmentLifecycle.onStop)
  }

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

    fun isAtLeast(desiredState: FragmentState): Boolean {
      return getCurrentState() >= desiredState
    }
  }
}