package com.example.service

sealed class RecordingState {
    object Idle : RecordingState()
    data class Countdown(val secondsLeft: Int) : RecordingState()
    object Recording : RecordingState()
    object Paused : RecordingState()
    object Stopping : RecordingState()
    data class Error(val message: String) : RecordingState()
}
