package me.esei.esboilerplatemvi.model

data class WrappedEffect<T>(
    val effect: T,
    val timeStamp: Long = System.currentTimeMillis()
)
