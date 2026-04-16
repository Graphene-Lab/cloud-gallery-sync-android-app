package com.graphenelab.photosync.data.local.datastore


import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.graphenelab.photosync.domain.model.TimeInterval
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sync_settings")

class SyncPreferencesDataSource @Inject constructor(
        @ApplicationContext private val context: Context
) {

    companion object {
        private val SYNC_INTERVALS_KEY = stringPreferencesKey("sync_intervals")
        private val SYNC_FROM_NOW_POINT_KEY = longPreferencesKey("sync_from_now_point")
    }

    val syncedIntervals: Flow<List<TimeInterval>> =
    context.dataStore.data.map { prefs ->
            Json.decodeFromString(prefs[SYNC_INTERVALS_KEY] ?: "[]")
    }

    suspend fun saveSyncedIntervals(intervals: List<TimeInterval>) {
        context.dataStore.edit {
            it[SYNC_INTERVALS_KEY] = Json.encodeToString(intervals)
        }
    }

    val syncFromNowPoint: Flow<Long> =
    context.dataStore.data.map { it[SYNC_FROM_NOW_POINT_KEY] ?: 0L }

    suspend fun saveSyncFromNowPoint(timestamp: Long) {
        context.dataStore.edit { it[SYNC_FROM_NOW_POINT_KEY] = timestamp }
    }

    suspend fun deleteSyncFromNowPoint() {
        context.dataStore.edit { it.remove(SYNC_FROM_NOW_POINT_KEY) }
    }

    suspend fun clearAllData() {
        context.dataStore.edit { it.clear() }
    }
}
