package com.linehu.asi.data.recents

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Every launch of an external app made from ASI. */
@Entity(tableName = "launch_events")
data class LaunchEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appKey: String,
    val timestamp: Long,
)

@Dao
interface LaunchHistoryDao {
    @Insert
    suspend fun insert(event: LaunchEventEntity)

    @Query(
        "SELECT appKey, MAX(timestamp) AS lastUsed FROM launch_events " +
            "GROUP BY appKey ORDER BY lastUsed DESC LIMIT :limit",
    )
    suspend fun recentKeys(limit: Int): List<RecentKeyRow>

    @Query("DELETE FROM launch_events WHERE appKey = :appKey")
    suspend fun remove(appKey: String)
}

data class RecentKeyRow(val appKey: String, val lastUsed: Long)
