package com.linehu.asi.data.layout

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LayoutDao {

    @Query("SELECT * FROM home_items ORDER BY page, slot")
    fun homeItemsFlow(): Flow<List<HomeItemEntity>>

    @Query("SELECT * FROM folders ORDER BY page, slot")
    fun foldersFlow(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folder_items ORDER BY folderId, slot")
    fun folderItemsFlow(): Flow<List<FolderItemEntity>>

    @Query("SELECT COUNT(*) FROM home_items")
    suspend fun homeItemCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomeItems(items: List<HomeItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolders(folders: List<FolderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolderItems(items: List<FolderItemEntity>)

    @Query("DELETE FROM home_items")
    suspend fun clearHomeItems()

    @Query("DELETE FROM folders")
    suspend fun clearFolders()

    @Query("DELETE FROM folder_items")
    suspend fun clearFolderItems()

    /** Full rewrite of the persisted layout. Data is tiny (≤ a few hundred rows). */
    @Transaction
    suspend fun replaceLayout(
        items: List<HomeItemEntity>,
        folders: List<FolderEntity>,
        folderItems: List<FolderItemEntity>,
    ) {
        clearHomeItems()
        clearFolders()
        clearFolderItems()
        insertHomeItems(items)
        insertFolders(folders)
        insertFolderItems(folderItems)
    }
}
