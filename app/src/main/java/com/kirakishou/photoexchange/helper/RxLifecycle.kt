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
        Destroyed(0),
        Created(1),
        Started(2),
        Resumed(3);
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
                onPause -> FragmentState.Started
                onStop -> FragmentState.Created
                onDestroy -> FragmentState.Destroyed
            }
        }

        fun isAtLeast(desiredState: FragmentState): Boolean {
            return getCurrentState() >= desiredState
        }
    }
}