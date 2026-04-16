package com.graphenelab.photosync.data.local.mediastore

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.graphenelab.photosync.BuildConfig
import com.graphenelab.photosync.domain.model.GalleryPhoto
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PhotoLocalDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun getPhotos(startTimeSeconds: Long): List<GalleryPhoto> {
        return queryPhotos(startTimeSeconds = startTimeSeconds)
    }

    fun getPhotosInInterval(start: Long, end: Long): List<GalleryPhoto> {
        if (start > end) return emptyList()
        return queryPhotos(startTimeSeconds = start, endTimeSeconds = end)
    }

    private fun queryPhotos(
        startTimeSeconds: Long? = null,
        endTimeSeconds: Long? = null
    ): List<GalleryPhoto> {
        val photos = mutableListOf<GalleryPhoto>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.DATA
        )

        val selectionParts = mutableListOf<String>()
        val selectionArgsList = mutableListOf<String>()

        startTimeSeconds?.let {
            selectionParts.add("${MediaStore.Images.Media.DATE_ADDED} >= ?")
            selectionArgsList.add(it.toString())
        }

        endTimeSeconds?.let {
            selectionParts.add("${MediaStore.Images.Media.DATE_ADDED} <= ?")
            selectionArgsList.add(it.toString())
        }

        selectionParts.add("${MediaStore.Images.Media.DATA} LIKE ?")
        val cameraFolder = if (BuildConfig.DEBUG) DEBUG_CAMERA_FOLDER else RELEASE_CAMERA_FOLDER
        val dcimCameraPath =
            "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath}/$cameraFolder"
        selectionArgsList.add("$dcimCameraPath%")

        val selection = selectionParts.joinToString(" AND ")
        val selectionArgs = selectionArgsList.toTypedArray()

        //TODO: problem can occur with photos with same date added since DATE_ADDED is in seconds. Consider adding _ID to sort order for tie-breaking.
        // for example user took 2 photos in same second, and when full syncing we sync first photo and app crashes before syncing second photo,
        // so when sync starts again we omit second, since we already saved last sync time of the interval when syncing first one, and now we start from last sync time + 1...
        // (on the other hand to solve this issue if we start from last sync (without +1) time then we will have reuploads...)
        // Ps crash while syncing exactly at the moment syncing happens with same second can happen in rare cases so not critical for now.
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} ASC"
        //note: if no photos found check if permission granted.
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                val dateAdded =
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED))
                val lastModifiedSeconds =
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED))
                val displayName =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                photos.add(
                    GalleryPhoto(
                        id = id,
                        dateAdded = dateAdded,
                        lastModifiedSeconds = lastModifiedSeconds,
                        displayName = displayName,
                        path = contentUri
                    )
                )
            }
        }

        return photos
    }

    /**
     * Gets URIs of synced photos for deletion.
     * Returns list of photo URIs to delete.
     */
    fun getSyncedPhotosUris(intervals: List<com.graphenelab.photosync.domain.model.TimeInterval>): List<Uri> {
        if (intervals.isEmpty()) return emptyList()

        val photoUris = mutableListOf<Uri>()

        // Collect photo URIs for each interval
        intervals.forEach { interval ->
            val photos = getPhotosInInterval(interval.start, interval.end)
            photoUris.addAll(photos.map { it.path })
        }

        return photoUris
    }

    companion object {
        private const val DEBUG_CAMERA_FOLDER = "Camera"
        private const val RELEASE_CAMERA_FOLDER = "Camera"
    }

}
