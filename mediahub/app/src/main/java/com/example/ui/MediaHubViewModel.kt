package com.example.ui

import android.app.Application
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Chapter
import com.example.data.CustomPodcastEntity
import com.example.data.CustomVideoEntity
import com.example.data.Episode
import com.example.data.MediaHubDatabase
import com.example.data.MediaRepository
import com.example.data.PlaybackHistoryItem
import com.example.data.Podcast
import com.example.data.Track
import com.example.data.Video
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MediaHubViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MediaHubDatabase.getDatabase(application)
    private val repository = MediaRepository(database)

    // UI Navigation State
    private val _currentTab = MutableStateFlow("home") // "home", "music", "video", "podcasts", "search", "now_playing_audio", "now_playing_video", "podcast_episode"
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // Screen stack for back navigation
    private val navigationStack = mutableListOf<String>()

    fun navigateTo(tab: String) {
        if (_currentTab.value != tab) {
            navigationStack.add(_currentTab.value)
            _currentTab.value = tab
        }
    }

    fun navigateBack() {
        if (navigationStack.isNotEmpty()) {
            _currentTab.value = navigationStack.removeAt(navigationStack.size - 1)
        } else {
            _currentTab.value = "home"
        }
    }

    // Dynamic Database flows combined with default assets
    val customVideos: StateFlow<List<Video>> = repository.customVideos
        .combine(MutableStateFlow(repository.getDefaultVideos())) { local, defaults ->
            val localVideos = local.map {
                Video(
                    id = "custom_v_${it.id}",
                    title = it.title,
                    durationText = it.durationText,
                    durationSeconds = 600, // standard estimate
                    coverUrl = it.coverUrl,
                    videoUrl = it.videoUrl,
                    category = "Custom Streams"
                )
            }
            localVideos + defaults
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.getDefaultVideos())

    val customPodcasts: StateFlow<List<Podcast>> = repository.customPodcasts
        .combine(MutableStateFlow(repository.getDefaultPodcasts())) { local, defaults ->
            val localPodcasts = local.map {
                Podcast(
                    id = "custom_p_${it.id}",
                    title = it.title,
                    host = it.host,
                    description = it.description,
                    artworkUrl = it.artworkUrl,
                    episodesCount = it.episodesCount
                )
            }
            defaults + localPodcasts
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.getDefaultPodcasts())

    val recentPlayed: StateFlow<List<PlaybackHistoryItem>> = repository.recentPlayed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTracks = MutableStateFlow(repository.getDefaultTracks())
    val allEpisodes = MutableStateFlow(repository.getDefaultEpisodes())

    // Currently Selected objects
    private val _selectedPodcast = MutableStateFlow<Podcast?>(null)
    val selectedPodcast: StateFlow<Podcast?> = _selectedPodcast.asStateFlow()

    private val _selectedEpisode = MutableStateFlow<Episode?>(null)
    val selectedEpisode: StateFlow<Episode?> = _selectedEpisode.asStateFlow()

    fun selectPodcast(podcast: Podcast) {
        _selectedPodcast.value = podcast
        navigateTo("podcast_detail")
    }

    fun selectEpisode(episode: Episode) {
        _selectedEpisode.value = episode
        playEpisode(episode)
    }

    // Media Player engine
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _currentPlayingTrack = MutableStateFlow<Track?>(null)
    val currentPlayingTrack: StateFlow<Track?> = _currentPlayingTrack.asStateFlow()

    private val _currentPlayingEpisode = MutableStateFlow<Episode?>(null)
    val currentPlayingEpisode: StateFlow<Episode?> = _currentPlayingEpisode.asStateFlow()

    private val _currentPlayingVideo = MutableStateFlow<Video?>(null)
    val currentPlayingVideo: StateFlow<Video?> = _currentPlayingVideo.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0L) // in seconds
    val playbackProgress: StateFlow<Long> = _playbackProgress.asStateFlow()

    private val _playbackDuration = MutableStateFlow(100L) // in seconds
    val playbackDuration: StateFlow<Long> = _playbackDuration.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _shuffleActive = MutableStateFlow(false)
    val shuffleActive: StateFlow<Boolean> = _shuffleActive.asStateFlow()

    private val _repeatActive = MutableStateFlow(false)
    val repeatActive: StateFlow<Boolean> = _repeatActive.asStateFlow()

    private val _volume = MutableStateFlow(0.8f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    // Dialog state
    val videoStreamDialogShown = MutableStateFlow(false)
    val rssFeedDialogShown = MutableStateFlow(false)

    // Search state
    val searchQuery = MutableStateFlow("")
    val searchFilterType = MutableStateFlow("All") // "All", "Music", "Video", "Podcasts"
    val recentSearches = MutableStateFlow(listOf("interstellar", "chill vibes", "tech talk daily"))

    fun search(query: String) {
        searchQuery.value = query
        if (query.isNotEmpty() && !recentSearches.value.contains(query.lowercase())) {
            recentSearches.value = (listOf(query.lowercase()) + recentSearches.value).take(5)
        }
    }

    fun clearSearch() {
        searchQuery.value = ""
    }

    fun clearRecentSearches() {
        recentSearches.value = emptyList()
    }

    // Media Playback control functions
    fun playTrack(track: Track) {
        resetPlayer()
        _currentPlayingEpisode.value = null
        _currentPlayingVideo.value = null
        _currentPlayingTrack.value = track
        _playbackDuration.value = track.durationSeconds
        _playbackProgress.value = 0L

        viewModelScope.launch {
            repository.savePlaybackProgress(
                mediaId = track.id,
                title = track.title,
                subtitle = track.artist,
                type = "MUSIC",
                coverUrl = track.coverUrl,
                duration = track.durationSeconds,
                progress = 0L
            )
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(track.audioUrl)
                setVolume(_volume.value, _volume.value)
                prepareAsync()
                setOnPreparedListener {
                    start()
                    _isPlaying.value = true
                    startProgressTracker()
                }
                setOnCompletionListener {
                    if (_repeatActive.value) {
                        seekTo(0)
                        start()
                    } else {
                        playNextTrack()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MediaHubViewModel", "Error starting track", e)
            // Fallback simulated playing in case of network issue
            _isPlaying.value = true
            startProgressTrackerSimulated()
        }
        navigateTo("now_playing_audio")
    }

    fun playEpisode(episode: Episode) {
        resetPlayer()
        _currentPlayingTrack.value = null
        _currentPlayingVideo.value = null
        _currentPlayingEpisode.value = episode
        _playbackDuration.value = episode.durationSeconds
        _playbackProgress.value = 0L

        viewModelScope.launch {
            repository.savePlaybackProgress(
                mediaId = episode.id,
                title = episode.title,
                subtitle = episode.podcastTitle,
                type = "PODCAST",
                coverUrl = getPodcastArtwork(episode.podcastId),
                duration = episode.durationSeconds,
                progress = 0L
            )
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(episode.audioUrl)
                setVolume(_volume.value, _volume.value)
                prepareAsync()
                setOnPreparedListener {
                    start()
                    _isPlaying.value = true
                    startProgressTracker()
                }
                setOnCompletionListener {
                    _isPlaying.value = false
                    stopProgressTracker()
                }
            }
        } catch (e: Exception) {
            Log.e("MediaHubViewModel", "Error starting podcast episode", e)
            _isPlaying.value = true
            startProgressTrackerSimulated()
        }
        navigateTo("podcast_episode")
    }

    fun playVideo(video: Video) {
        resetPlayer()
        _currentPlayingTrack.value = null
        _currentPlayingEpisode.value = null
        _currentPlayingVideo.value = video
        _playbackDuration.value = video.durationSeconds
        _playbackProgress.value = 0L

        viewModelScope.launch {
            repository.savePlaybackProgress(
                mediaId = video.id,
                title = video.title,
                subtitle = "Video stream",
                type = "VIDEO",
                coverUrl = video.coverUrl,
                duration = video.durationSeconds,
                progress = 0L
            )
        }

        // Videos are played using the standard VideoView directly on screen
        _isPlaying.value = true
        startProgressTrackerSimulated()
        navigateTo("now_playing_video")
    }

    private fun getPodcastArtwork(podcastId: String): String {
        return customPodcasts.value.find { it.id == podcastId }?.artworkUrl ?: "https://images.unsplash.com/photo-1478737270239-2f02b77fc618?w=500&auto=format&fit=crop&q=60"
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            mediaPlayer?.pause()
            _isPlaying.value = false
            progressJob?.cancel()
        } else {
            mediaPlayer?.let {
                it.start()
                _isPlaying.value = true
                startProgressTracker()
            } ?: run {
                _isPlaying.value = true
                // Continue simulation if no media player was initialized
                if (_currentPlayingTrack.value != null) startProgressTrackerSimulated()
                if (_currentPlayingEpisode.value != null) startProgressTrackerSimulated()
                if (_currentPlayingVideo.value != null) startProgressTrackerSimulated()
            }
        }
    }

    fun seekToSeconds(seconds: Long) {
        val safeSeconds = seconds.coerceIn(0, _playbackDuration.value)
        _playbackProgress.value = safeSeconds
        try {
            mediaPlayer?.seekTo((safeSeconds * 1000).toInt())
        } catch (e: Exception) {
            Log.e("MediaHubViewModel", "Error seeking", e)
        }
    }

    fun skipForward15() {
        seekToSeconds(_playbackProgress.value + 15)
    }

    fun skipBackward15() {
        seekToSeconds(_playbackProgress.value - 15)
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        it.playbackParams = it.playbackParams.setSpeed(speed)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MediaHubViewModel", "Error setting speed", e)
        }
    }

    fun toggleShuffle() {
        _shuffleActive.value = !_shuffleActive.value
    }

    fun toggleRepeat() {
        _repeatActive.value = !_repeatActive.value
    }

    fun setVolume(vol: Float) {
        _volume.value = vol
        try {
            mediaPlayer?.setVolume(vol, vol)
        } catch (e: Exception) {
            Log.e("MediaHubViewModel", "Error setting volume", e)
        }
    }

    fun playNextTrack() {
        val tracks = allTracks.value
        if (tracks.isEmpty()) return

        val currentIndex = tracks.indexOfFirst { it.id == _currentPlayingTrack.value?.id }
        val nextIndex = if (_shuffleActive.value) {
            (0 until tracks.size).random()
        } else {
            if (currentIndex == -1 || currentIndex == tracks.size - 1) 0 else currentIndex + 1
        }
        playTrack(tracks[nextIndex])
    }

    fun playPreviousTrack() {
        val tracks = allTracks.value
        if (tracks.isEmpty()) return

        val currentIndex = tracks.indexOfFirst { it.id == _currentPlayingTrack.value?.id }
        val prevIndex = if (_shuffleActive.value) {
            (0 until tracks.size).random()
        } else {
            if (currentIndex <= 0) tracks.size - 1 else currentIndex - 1
        }
        playTrack(tracks[prevIndex])
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (_isPlaying.value) {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        _playbackProgress.value = (it.currentPosition / 1000).toLong()
                        _playbackDuration.value = (it.duration / 1000).toLong()
                    }
                }
                delay(1000)
            }
        }
    }

    private fun startProgressTrackerSimulated() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (_isPlaying.value) {
                if (_playbackProgress.value < _playbackDuration.value) {
                    _playbackProgress.value += 1
                } else {
                    _isPlaying.value = false
                    if (_repeatActive.value) {
                        _playbackProgress.value = 0L
                        _isPlaying.value = true
                        startProgressTrackerSimulated()
                    } else if (_currentPlayingTrack.value != null) {
                        playNextTrack()
                    }
                    break
                }
                delay((1000 / _playbackSpeed.value).toLong())
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
    }

    private fun resetPlayer() {
        progressJob?.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e("MediaHubViewModel", "Error resetting media player", e)
        }
        mediaPlayer = null
        _isPlaying.value = false
    }

    // Adding dynamic custom sources
    fun addNewStream(title: String, url: String) {
        viewModelScope.launch {
            repository.addCustomVideo(title, url)
        }
    }

    fun addNewPodcastFeed(title: String, host: String, description: String) {
        viewModelScope.launch {
            repository.addCustomPodcast(title, host, description)
            // Generate simple mock episode for this custom show
            val generatedEpisode = Episode(
                id = "custom_ep_${System.currentTimeMillis()}",
                podcastId = "custom_p_new", // generic ID
                podcastTitle = title,
                title = "Welcome Episode: Introduction to $title",
                dateText = "Just Now",
                durationText = "15:00",
                durationSeconds = 900,
                description = description,
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-12.mp3",
                chapters = listOf(Chapter("Welcome and Overview", "00:00", 0))
            )
            val updatedEpisodes = allEpisodes.value.toMutableList()
            updatedEpisodes.add(0, generatedEpisode)
            allEpisodes.value = updatedEpisodes
        }
    }

    override fun onCleared() {
        super.onCleared()
        resetPlayer()
    }
}
