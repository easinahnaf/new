package com.example.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.TimeUnit

class MediaRepository(
    private val context: Context,
    private val database: MediaHubDatabase
) {

    val customVideos: Flow<List<CustomVideoEntity>> = database.customVideoDao().getAllCustomVideosFlow()
    val customPodcasts: Flow<List<CustomPodcastEntity>> = database.customPodcastDao().getAllCustomPodcastsFlow()
    val playbackHistory: Flow<List<PlaybackHistoryItem>> = database.playbackHistoryDao().getPlaybackHistoryFlow()
    val recentPlayed: Flow<List<PlaybackHistoryItem>> = database.playbackHistoryDao().getRecentPlayedFlow()

    // ==========================================
    // ১. ফোন থেকে সব অডিও / গান / গজল লোড করার ফাংশন
    // ==========================================
    suspend fun getLocalAudioFiles(): List<Track> = withContext(Dispatchers.IO) {
        val trackList = mutableListOf<Track>()
        val contentResolver = context.contentResolver

        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )

        // শুধুমাত্র অডিও ফাইল ফিল্টার করার জন্য
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.DURATION} > 10000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        contentResolver.query(uri, projection, selection, null, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown Audio"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val album = cursor.getString(albumColumn) ?: "Storage"
                val durationMs = cursor.getLong(durationColumn)
                val albumId = cursor.getLong(albumIdColumn)

                val contentUri: Uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                
                // অ্যালবামের ছবি পাওয়ার Uri (যদি থাকে)
                val artworkUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                ).toString()

                val durationSeconds = (durationMs / 1000)
                val durationText = formatDuration(durationMs)

                trackList.add(
                    Track(
                        id = id.toString(),
                        title = title,
                        artist = artist,
                        album = album,
                        durationText = durationText,
                        durationSeconds = durationSeconds,
                        coverUrl = artworkUri,
                        audioUrl = contentUri.toString(),
                        lyrics = listOf("[Local Audio File]")
                    )
                )
            }
        }
        return@withContext trackList
    }

    // ==========================================
    // ২. ফোন থেকে সব ভিডিও ফাইল লোড করার ফাংশন
    // ==========================================
    suspend fun getLocalVideoFiles(): List<Video> = withContext(Dispatchers.IO) {
        val videoList = mutableListOf<Video>()
        val contentResolver = context.contentResolver

        val uri: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DISPLAY_NAME
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: cursor.getString(nameColumn) ?: "Unknown Video"
                val durationMs = cursor.getLong(durationColumn)

                val contentUri: Uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                val durationSeconds = (durationMs / 1000)
                val durationText = formatDuration(durationMs)

                videoList.add(
                    Video(
                        id = id.toString(),
                        title = title,
                        durationText = durationText,
                        durationSeconds = durationSeconds,
                        coverUrl = contentUri.toString(), // Thumbnail হিসেবে Content Uri
                        videoUrl = contentUri.toString(),
                        category = "Local Video"
                    )
                )
            }
        }
        return@withContext videoList
    }

    // টাইম ফরম্যাটিং হেল্পার (04:15)
    private fun formatDuration(durationMs: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    // ==========================================
    // ডেটাবেজ রিলেটেড পূর্বের ফাংশনগুলো
    // ==========================================
    suspend fun addCustomVideo(title: String, url: String) {
        val entity = CustomVideoEntity(
            title = title,
            videoUrl = url,
            durationText = "Live / Custom",
            coverUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=500"
        )
        database.customVideoDao().insertCustomVideo(entity)
    }

    suspend fun addCustomPodcast(title: String, host: String, description: String) {
        val entity = CustomPodcastEntity(
            title = title,
            host = host,
            description = description,
            artworkUrl = "https://images.unsplash.com/photo-1590602847861-f357a9332bbc?w=500",
            episodesCount = 1
        )
        database.customPodcastDao().insertCustomPodcast(entity)
    }

    suspend fun savePlaybackProgress(mediaId: String, title: String, subtitle: String, type: String, coverUrl: String, duration: Long, progress: Long) {
        val item = PlaybackHistoryItem(
            mediaId = mediaId,
            title = title,
            subtitle = subtitle,
            type = type,
            coverUrl = coverUrl,
            durationSeconds = duration,
            progressSeconds = progress,
            lastPlayedTime = System.currentTimeMillis()
        )
        database.playbackHistoryDao().insertHistoryItem(item)
    }

    suspend fun deleteHistoryItem(mediaId: String) {
        database.playbackHistoryDao().deleteHistoryItem(mediaId)
    }

    suspend fun clearAllHistory() {
        database.playbackHistoryDao().clearHistory()
    }
}
