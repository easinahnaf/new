package com.example.data

import kotlinx.coroutines.flow.Flow

class MediaRepository(private val database: MediaHubDatabase) {

    val customVideos: Flow<List<CustomVideoEntity>> = database.customVideoDao().getAllCustomVideosFlow()
    val customPodcasts: Flow<List<CustomPodcastEntity>> = database.customPodcastDao().getAllCustomPodcastsFlow()
    val playbackHistory: Flow<List<PlaybackHistoryItem>> = database.playbackHistoryDao().getPlaybackHistoryFlow()
    val recentPlayed: Flow<List<PlaybackHistoryItem>> = database.playbackHistoryDao().getRecentPlayedFlow()

    suspend fun addCustomVideo(title: String, url: String) {
        val entity = CustomVideoEntity(
            title = title,
            videoUrl = url,
            durationText = "Live / Custom",
            coverUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=500&auto=format&fit=crop&q=60"
        )
        database.customVideoDao().insertCustomVideo(entity)
    }

    suspend fun addCustomPodcast(title: String, host: String, description: String) {
        val entity = CustomPodcastEntity(
            title = title,
            host = host,
            description = description,
            artworkUrl = "https://images.unsplash.com/photo-1590602847861-f357a9332bbc?w=500&auto=format&fit=crop&q=60",
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

    // Static Pre-populated Media Content (Modern Synth-wave/Cyberpunk themes matching the visual style of the prompt's mockup)
    fun getDefaultTracks(): List<Track> {
        return listOf(
            Track(
                id = "m1",
                title = "Midnight Drive",
                artist = "The Night Owls",
                album = "Neon Dreams",
                durationText = "3:45",
                durationSeconds = 225,
                coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&auto=format&fit=crop&q=60",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                lyrics = listOf(
                    "Driving through the neon glow",
                    "Streets of light, where the shadows go",
                    "Engine hums, the radio plays",
                    "Lost in a dark retro haze",
                    "We ride into the midnight sun",
                    "Our neon journey has just begun"
                )
            ),
            Track(
                id = "m2",
                title = "Neon Dreams",
                artist = "Luna Skies",
                album = "Synthesized Reality",
                durationText = "4:12",
                durationSeconds = 252,
                coverUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=500&auto=format&fit=crop&q=60",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                lyrics = listOf(
                    "Visions of pink and purple skies",
                    "Digital warmth before my eyes",
                    "Catch the signal, feel the beat",
                    "Electricity underneath our feet",
                    "Floating in the cyber streams",
                    "Lost inside our neon dreams"
                )
            ),
            Track(
                id = "m3",
                title = "Ocean Waves",
                artist = "Nature Sounds",
                album = "Zen Acoustics",
                durationText = "5:10",
                durationSeconds = 310,
                coverUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=500&auto=format&fit=crop&q=60",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                lyrics = listOf("[Instrumental - Deep Ocean Waves and Relaxing Ambient Synthesizer]")
            ),
            Track(
                id = "m4",
                title = "Echoes",
                artist = "The Wanderer",
                album = "Solar Winds",
                durationText = "3:58",
                durationSeconds = 238,
                coverUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=500&auto=format&fit=crop&q=60",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                lyrics = listOf(
                    "Echoes in the cosmos deep",
                    "Promises that we swore to keep",
                    "Gravity pulling us back home",
                    "Through the dark stars we roam"
                )
            ),
            Track(
                id = "m5",
                title = "Chill Vibes",
                artist = "Various Artists",
                album = "Study Session Lofi",
                durationText = "4:30",
                durationSeconds = 270,
                coverUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=500&auto=format&fit=crop&q=60",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                lyrics = listOf("[Chill Lofi Beats for Relaxing and Coding]")
            ),
            Track(
                id = "m6",
                title = "Golden Hour",
                artist = "Tom Misch",
                album = "Beat Tape 2",
                durationText = "3:30",
                durationSeconds = 210,
                coverUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&auto=format&fit=crop&q=60",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
                lyrics = listOf("Warm sun falling down", "Light hitting the sleepy town...")
            )
        )
    }

    fun getDefaultVideos(): List<Video> {
        return listOf(
            Video(
                id = "v1",
                title = "Big Buck Bunny",
                durationText = "9:56",
                durationSeconds = 596,
                coverUrl = "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=500&auto=format&fit=crop&q=60",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                category = "Animation"
            ),
            Video(
                id = "v2",
                title = "Elephants Dream",
                durationText = "10:53",
                durationSeconds = 653,
                coverUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=500&auto=format&fit=crop&q=60",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                category = "Sci-Fi"
            ),
            Video(
                id = "v3",
                title = "Sintel Movie Trailer",
                durationText = "0:52",
                durationSeconds = 52,
                coverUrl = "https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?w=500&auto=format&fit=crop&q=60",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                category = "Trailer"
            ),
            Video(
                id = "v4",
                title = "Tears of Steel",
                durationText = "12:14",
                durationSeconds = 734,
                coverUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=500&auto=format&fit=crop&q=60",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                category = "CGI Action"
            ),
            Video(
                id = "v5",
                title = "For Bigger Blazes",
                durationText = "0:15",
                durationSeconds = 15,
                coverUrl = "https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?w=500&auto=format&fit=crop&q=60",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                category = "Promo"
            )
        )
    }

    fun getDefaultPodcasts(): List<Podcast> {
        return listOf(
            Podcast(
                id = "p1",
                title = "Tech Talk Daily",
                host = "Alex Rivera",
                description = "Breaking down AI technology, cybersecurity, software engineering trends, and product development in compact, bite-sized episodes.",
                artworkUrl = "https://images.unsplash.com/photo-1478737270239-2f02b77fc618?w=500&auto=format&fit=crop&q=60",
                episodesCount = 4
            ),
            Podcast(
                id = "p2",
                title = "The Daily Brief",
                host = "Sarah Jenkins",
                description = "Your standard morning download covering global news, economic updates, and cultural stories in under 20 minutes.",
                artworkUrl = "https://images.unsplash.com/photo-1590602847861-f357a9332bbc?w=500&auto=format&fit=crop&q=60",
                episodesCount = 3
            ),
            Podcast(
                id = "p3",
                title = "Hidden Brain",
                host = "Dr. Michael Chen",
                description = "Exploring the unseen patterns that drive human behavior, decisions, relationships, and subconscious motivations.",
                artworkUrl = "https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=500&auto=format&fit=crop&q=60",
                episodesCount = 2
            )
        )
    }

    fun getDefaultEpisodes(): List<Episode> {
        return listOf(
            Episode(
                id = "ep1_1",
                podcastId = "p1",
                podcastTitle = "Tech Talk Daily",
                title = "AI in 2024: What's Next?",
                dateText = "May 21, 2024",
                durationText = "42:16",
                durationSeconds = 2536,
                description = "We unpack deep learning advancements, the rise of specialized small-language models, edge computing, and AI hardware acceleration coming to consumer laptops.",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
                chapters = listOf(
                    Chapter("Intro to Next-Gen AI", "00:00", 0),
                    Chapter("The State of Edge AI", "05:12", 312),
                    Chapter("Future AI hardware trends", "18:45", 1125),
                    Chapter("Conclusion & Q&A", "36:20", 2180)
                )
            ),
            Episode(
                id = "ep1_2",
                podcastId = "p1",
                podcastTitle = "Tech Talk Daily",
                title = "The Quantum Computing Race",
                dateText = "May 14, 2024",
                durationText = "35:45",
                durationSeconds = 2145,
                description = "An exploration into superconducting qubits, quantum error correction, and when businesses can expect the first commercial-grade quantum processors.",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3"
            ),
            Episode(
                id = "ep2_1",
                podcastId = "p2",
                podcastTitle = "The Daily Brief",
                title = "Global Market Shakes",
                dateText = "May 22, 2024",
                durationText = "24:12",
                durationSeconds = 1452,
                description = "A deep dive into latest macroeconomic trends, supply-chain restructuring, and interest rate adjustments across leading world central banks.",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3"
            ),
            Episode(
                id = "ep2_2",
                podcastId = "p2",
                podcastTitle = "The Daily Brief",
                title = "Renewable Energy Grid Solutions",
                dateText = "May 15, 2024",
                durationText = "19:45",
                durationSeconds = 1185,
                description = "How cities are transforming power grids to store solar and wind energy dynamically using advanced solid-state grid batteries.",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3"
            ),
            Episode(
                id = "ep3_1",
                podcastId = "p3",
                podcastTitle = "Hidden Brain",
                title = "The Power of Habits",
                dateText = "May 20, 2024",
                durationText = "53:18",
                durationSeconds = 3198,
                description = "Why habits form, how they shape our character, and psychological methodologies to program beneficial actions while extinguishing old routines.",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-11.mp3"
            )
        )
    }
}
