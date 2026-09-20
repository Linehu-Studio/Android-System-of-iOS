package com.linehu.asi.data.timer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * App-wide countdown timer state. Lives in the AppContainer so the Clock app
 * and the Dynamic Island observe the same timer.
 */
class SharedTimer {

    data class TimerState(
        val running: Boolean = false,
        val totalMs: Long = 0L,
        val endTimestamp: Long = 0L,
        val pausedRemainingMs: Long = 0L,
    ) {
        fun remainingAt(now: Long): Long = when {
            !running -> pausedRemainingMs
            else -> (endTimestamp - now).coerceAtLeast(0)
        }
    }

    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    fun start(durationMs: Long) = _state.update {
        TimerState(
            running = true,
            totalMs = durationMs,
            endTimestamp = System.currentTimeMillis() + durationMs,
        )
    }

    fun pause() = _state.update {
        if (!it.running) it
        else it.copy(running = false, pausedRemainingMs = it.remainingAt(System.currentTimeMillis()))
    }

    fun resume() = _state.update {
        if (it.running || it.pausedRemainingMs <= 0) it
        else it.copy(running = true, endTimestamp = System.currentTimeMillis() + it.pausedRemainingMs)
    }

    fun cancel() = _state.update { TimerState() }
}
