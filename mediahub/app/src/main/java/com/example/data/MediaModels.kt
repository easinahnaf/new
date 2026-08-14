package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MediaType {
    MUSIC, VIDEO, PODCAST
}

// Room entity to save user custom video streams
@Entity(tableName = "custom_videos")
data class CustomVideoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val videoUrl: String,
    val durationText: String,
    val coverUrl: String,
    val addedAt: Long = System.currentTimeMillis()
)

// Room entity to save user custom podcasts RSS/feeds
@Entity(tableName = "custom_podcasts")
data class CustomPodcastEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val host: String,
    val description: String,
    val artworkUrl: String,
    val episodesCount: Int,
    val addedAt: Long = System.currentTimeMillis()
)

// Play history
@Entity(tableName = "playback_history")
data class PlaybackHistoryItem(
    @PrimaryKey val mediaId: String,
    val title: String,
    val subtitle: String,
    val type: String, // "MUSIC", "VIDEO", "PODCAST"
    val coverUrl: String,
    val durationSeconds: Long,
    val progressSeconds: Long,
    val lastPlayedTime: Long = System.currentTimeMillis()
)

// Domain Models
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationText: String,
    val durationSeconds: Long,
    val coverUrl: String,
    val audioUrl: String,
    val lyrics: List<String> = emptyList()
)

data class Video(
    val id: String,
    val title: String,
    val durationText: String,
    val durationSeconds: Long,
    val coverUrl: String,
    val videoUrl: String,
    val category: String = "All Videos"
)

data class Podcast(
    val id: String,
    val title: String,
    val host: String,
    val description: String,
    val artworkUrl: String,
    val episodesCount: Int
)

data class Episode(
    val id: String,
    val podcastId: String,
    val podcastTitle: String,
    val title: String,
    val dateText: String,
    val durationText: String,
    val durationSeconds: Long,
    val description: String,
    val audioUrl: String,
    val chapters: List<Chapter> = emptyList()
)

data class Chapter(
    val title: String,
    val timeText: String,
    val seconds: Long
)
