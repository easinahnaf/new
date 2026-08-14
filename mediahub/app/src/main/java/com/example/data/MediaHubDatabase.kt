package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomVideoDao {
    @Query("SELECT * FROM custom_videos ORDER BY addedAt DESC")
    fun getAllCustomVideosFlow(): Flow<List<CustomVideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomVideo(video: CustomVideoEntity)

    @Delete
    suspend fun deleteCustomVideo(video: CustomVideoEntity)
}

@Dao
interface CustomPodcastDao {
    @Query("SELECT * FROM custom_podcasts ORDER BY addedAt DESC")
    fun getAllCustomPodcastsFlow(): Flow<List<CustomPodcastEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomPodcast(podcast: CustomPodcastEntity)

    @Delete
    suspend fun deleteCustomPodcast(podcast: CustomPodcastEntity)
}

@Dao
interface PlaybackHistoryDao {
    @Query("SELECT * FROM playback_history ORDER BY lastPlayedTime DESC")
    fun getPlaybackHistoryFlow(): Flow<List<PlaybackHistoryItem>>

    @Query("SELECT * FROM playback_history ORDER BY lastPlayedTime DESC LIMIT 10")
    fun getRecentPlayedFlow(): Flow<List<PlaybackHistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryItem(item: PlaybackHistoryItem)

    @Query("DELETE FROM playback_history WHERE mediaId = :mediaId")
    suspend fun deleteHistoryItem(mediaId: String)

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()
}

@Database(
    entities = [CustomVideoEntity::class, CustomPodcastEntity::class, PlaybackHistoryItem::class],
    version = 1,
    exportSchema = false
)
abstract class MediaHubDatabase : RoomDatabase() {
    abstract fun customVideoDao(): CustomVideoDao
    abstract fun customPodcastDao(): CustomPodcastDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: MediaHubDatabase? = null

        fun getDatabase(context: Context): MediaHubDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MediaHubDatabase::class.java,
                    "mediahub_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
