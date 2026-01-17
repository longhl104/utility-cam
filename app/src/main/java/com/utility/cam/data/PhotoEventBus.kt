package com.utility.cam.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Event bus for photo-related events across the app
 * Allows components to be notified when photos are added, deleted, or updated
 */
object PhotoEventBus {

    private val _photoEvents = MutableSharedFlow<PhotoEvent>(replay = 0)
    val photoEvents: SharedFlow<PhotoEvent> = _photoEvents.asSharedFlow()

    suspend fun emit(event: PhotoEvent) {
        _photoEvents.emit(event)
    }
}

sealed class PhotoEvent {
    object PhotosDeleted : PhotoEvent()
    object PhotoAdded : PhotoEvent()
    data class PhotoUpdated(val photoId: String) : PhotoEvent()
}
