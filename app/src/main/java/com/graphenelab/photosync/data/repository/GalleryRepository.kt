package com.graphenelab.photosync.data.repository

import android.net.Uri
import com.graphenelab.photosync.data.local.mediastore.PhotoLocalDataSource
import com.graphenelab.photosync.domain.model.GalleryPhoto
import com.graphenelab.photosync.domain.model.TimeInterval
import com.graphenelab.photosync.domain.repositroy.IGalleryRepository
import javax.inject.Inject
class GalleryRepositoryImpl @Inject constructor(
    private val localDataSource: PhotoLocalDataSource
) : IGalleryRepository {

    override fun getPhotos(startTimeSeconds: Long): List<GalleryPhoto> {
        return localDataSource.getPhotos(startTimeSeconds)
    }

    override fun getPhotosInInterval(start: Long, end: Long): List<GalleryPhoto> {
        return localDataSource.getPhotosInInterval(start, end)
    }

    override fun getSyncedPhotosUris(intervals: List<TimeInterval>): List<Uri> {
        return localDataSource.getSyncedPhotosUris(intervals)
    }
}

