package com.nexusplayer.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        VideoResumeEntity::class,
        BookmarkEntity::class,
        PlaylistEntity::class,
        PlaylistVideoCrossRef::class,
        WatchAnalyticsEntity::class,
        VideoTagEntity::class,
        VideoTagCrossRef::class
    ],
    version = 2,
    exportSchema = false
)
abstract class NexusDatabase : RoomDatabase() {
    abstract fun videoResumeDao(): VideoResumeDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun watchAnalyticsDao(): WatchAnalyticsDao
    abstract fun videoTagDao(): VideoTagDao

    companion object {
        @Volatile
        private var INSTANCE: NexusDatabase? = null

        fun getInstance(context: Context): NexusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NexusDatabase::class.java,
                    "nexus_player.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
