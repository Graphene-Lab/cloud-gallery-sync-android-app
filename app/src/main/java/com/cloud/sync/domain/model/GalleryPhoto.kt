package com.cloud.sync.domain.model

import android.net.Uri

/**
 * Represents a photo from the device's gallery.
 */
data class GalleryPhoto(
    val id: Long,
    val dateAdded: Long, // Timestamp in seconds
    val lastModifiedSeconds: Long = dateAdded,
    val displayName: String,
    val path: Uri
)