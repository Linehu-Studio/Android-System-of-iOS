package com.linehu.asi.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.linehu.asi.data.layout.FolderEntity
import com.linehu.asi.data.layout.FolderItemEntity
import com.linehu.asi.data.layout.HomeItemEntity
import com.linehu.asi.data.layout.LayoutDao
import com.linehu.asi.data.notes.NoteDao
import com.linehu.asi.data.notes.NoteEntity
import com.linehu.asi.data.recents.LaunchEventEntity
import com.linehu.asi.data.recents.LaunchHistoryDao

@Database(
    entities = [
        HomeItemEntity::class,
        FolderEntity::class,
        FolderItemEntity::class,
        NoteEntity::class,
        LaunchEventEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun layoutDao(): LayoutDao
    abstract fun noteDao(): NoteDao
    abstract fun launchHistoryDao(): LaunchHistoryDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "asi.db",
            ).build().also { instance = it }
        }
    }
}
