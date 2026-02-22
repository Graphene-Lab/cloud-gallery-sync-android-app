package com.cloud.sync.data.repository

import android.net.Uri
import com.cloud.sync.data.local.mediastore.PhotoLocalDataSource
import com.cloud.sync.domain.model.GalleryPhoto
import com.cloud.sync.domain.model.TimeInterval
import com.cloud.sync.domain.repositroy.IGalleryRepository
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

