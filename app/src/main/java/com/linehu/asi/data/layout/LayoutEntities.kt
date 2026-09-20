package com.linehu.asi.data.layout

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A cell on a home page. [page] == PAGE_DOCK marks the dock strip. */
@Entity(tableName = "home_items")
data class HomeItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String, // "app" | "builtin"
    val appKey: String? = null,
    val appId: String? = null,
    val page: Int,
    // "index"/"order" are SQLite keywords — column is named "slot".
    val slot: Int,
) {
    companion object {
        const val PAGE_DOCK = -1
        const val KIND_APP = "app"
        const val KIND_BUILTIN = "builtin"
    }
}

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val folderId: String,
    val title: String,
    val page: Int,
    val slot: Int,
)

/** Membership of an app/builtin inside a folder. */
@Entity(tableName = "folder_items")
data class FolderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderId: String,
    val slot: Int,
    val kind: String,
    val appKey: String? = null,
    val appId: String? = null,
)
