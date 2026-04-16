package com.graphenelab.photosync.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a time interval for synchronization.
 */
@Serializable
data class TimeInterval(val start: Long, val end: Long)
