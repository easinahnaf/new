package com.example.ui

import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.Episode
import com.example.data.Podcast
import com.example.data.Track
import com.example.data.Video
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun MediaHubApp(
    viewModel: MediaHubViewModel = viewModel()
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val currentTrack by viewModel.currentPlayingTrack.collectAsState()
    val currentEpisode by viewModel.currentPlayingEpisode.collectAsState()
    val currentVideo by viewModel.currentPlayingVideo.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    val videoStreamDialogShown by viewModel.videoStreamDialogShown.collectAsState()
    val rssFeedDialogShown by viewModel.rssFeedDialogShown.collectAsState()

    // Determine layout style based on screen width class
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MediaHubBackground)
    ) {
        val isWideScreen = maxWidth > 600.dp

        Scaffold(
            bottomBar = {
                if (!isWideScreen && currentTab != "now_playing_video") {
                    MediaHubBottomNavigation(
                        currentTab = currentTab,
                        onTabSelected = { viewModel.navigateTo(it) }
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        bottom = if (!isWideScreen) innerPadding.calculateBottomPadding() else 0.dp,
                        top = innerPadding.calculateTopPadding()
                    )
            ) {
                if (isWideScreen && currentTab != "now_playing_video") {
                    MediaHubNavigationRail(
                        currentTab = currentTab,
                        onTabSelected = { viewModel.navigateTo(it) }
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    when (currentTab) {
                        "home" -> HomeScreen(viewModel)
                        "music" -> MusicScreen(viewModel)
                        "video" -> VideoScreen(viewModel)
                        "podcasts" -> PodcastsScreen(viewModel)
                        "search" -> SearchScreen(viewModel)
                        "podcast_detail" -> PodcastDetailScreen(viewModel)
                        "now_playing_audio" -> NowPlayingAudioScreen(viewModel)
                        "now_playing_video" -> NowPlayingVideoScreen(viewModel)
                        "podcast_episode" -> PodcastEpisodeScreen(viewModel)
                        else -> HomeScreen(viewModel)
                    }

                    // Bottom Floating Mini Player capsule (gorgeous glass design)
                    val showMiniPlayer = (currentTrack != null || currentEpisode != null) &&
                            currentTab != "now_playing_audio" &&
                            currentTab != "podcast_episode" &&
                            currentTab != "now_playing_video"

                    androidx.compose.animation.AnimatedVisibility(
                        visible = showMiniPlayer,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        MiniPlayerCapsule(
                            track = currentTrack,
                            episode = currentEpisode,
                            isPlaying = isPlaying,
                            onPlayPauseToggle = { viewModel.togglePlayPause() },
                            onTap = {
                                if (currentTrack != null) viewModel.navigateTo("now_playing_audio")
                                if (currentEpisode != null) viewModel.navigateTo("podcast_episode")
                            }
                        )
                    }
                }
            }
        }
    }

    // Custom Video Stream Adder Dialog
    if (videoStreamDialogShown) {
        AddStreamDialog(
            onDismiss = { viewModel.videoStreamDialogShown.value = false },
            onAdd = { title, url ->
                viewModel.addNewStream(title, url)
                viewModel.videoStreamDialogShown.value = false
            }
        )
    }

    // Custom RSS Feed Adder Dialog
    if (rssFeedDialogShown) {
        AddRssDialog(
            onDismiss = { viewModel.rssFeedDialogShown.value = false },
            onAdd = { title, host, desc ->
                viewModel.addNewPodcastFeed(title, host, desc)
                viewModel.rssFeedDialogShown.value = false
            }
        )
    }
}

// Bottom Navigation Styled like the sleek mockup
@Composable
fun MediaHubBottomNavigation(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = MediaHubSurface,
        modifier = Modifier
            .border(0.5.dp, Color(0xFF1E2130), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        val tabs = listOf(
            NavigationItem("home", "Home", Icons.Default.Home, Icons.Outlined.Home),
            NavigationItem("music", "Music", Icons.Default.MusicNote, Icons.Outlined.MusicNote),
            NavigationItem("video", "Video", Icons.Default.VideoLibrary, Icons.Outlined.VideoLibrary),
            NavigationItem("podcasts", "Podcasts", Icons.Default.Podcasts, Icons.Outlined.Podcasts),
            NavigationItem("search", "Search", Icons.Default.Search, Icons.Outlined.Search)
        )

        tabs.forEach { tab ->
            val isSelected = currentTab == tab.id || (tab.id == "podcasts" && currentTab == "podcast_detail")
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab.id) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label,
                        tint = if (isSelected) MediaHubPrimary else MediaHubTextSecondary
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MediaHubPrimary else MediaHubTextSecondary
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0x22E51937)
                ),
                modifier = Modifier.testTag("nav_${tab.id}")
            )
        }
    }
}

// Navigation Rail for Wide Screens (Tablets / Chromebooks)
@Composable
fun MediaHubNavigationRail(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationRail(
        containerColor = MediaHubSurface,
        modifier = Modifier
            .width(80.dp)
            .fillMaxHeight()
            .border(0.5.dp, Color(0xFF1E2130))
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        // Brand logo
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Brush.radialGradient(listOf(MediaHubPrimary, Color.Transparent)), shape = CircleShape)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Logo", tint = Color.White)
        }
        Spacer(modifier = Modifier.height(48.dp))

        val tabs = listOf(
            NavigationItem("home", "Home", Icons.Default.Home, Icons.Outlined.Home),
            NavigationItem("music", "Music", Icons.Default.MusicNote, Icons.Outlined.MusicNote),
            NavigationItem("video", "Video", Icons.Default.VideoLibrary, Icons.Outlined.VideoLibrary),
            NavigationItem("podcasts", "Podcasts", Icons.Default.Podcasts, Icons.Outlined.Podcasts),
            NavigationItem("search", "Search", Icons.Default.Search, Icons.Outlined.Search)
        )

        tabs.forEach { tab ->
            val isSelected = currentTab == tab.id
            NavigationRailItem(
                selected = isSelected,
                onClick = { onTabSelected(tab.id) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label,
                        tint = if (isSelected) MediaHubPrimary else MediaHubTextSecondary
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        fontSize = 11.sp,
                        color = if (isSelected) MediaHubPrimary else MediaHubTextSecondary
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    indicatorColor = Color(0x22E51937)
                ),
                modifier = Modifier.testTag("rail_nav_${tab.id}")
            )
        }
    }
}

data class NavigationItem(
    val id: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

// HOME SCREEN
@Composable
fun HomeScreen(viewModel: MediaHubViewModel) {
    val recentPlayed by viewModel.recentPlayed.collectAsState()
    val allTracks by viewModel.allTracks.collectAsState()
    val customPodcasts by viewModel.customPodcasts.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Media",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Hub",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MediaHubPrimary
                    )
                }
                IconButton(
                    onClick = { },
                    modifier = Modifier
                        .background(MediaHubSurface, CircleShape)
                        .size(44.dp)
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.White)
                }
            }
        }

        // Search Bar trigger (navigates to Search View)
        item {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Search music, videos, podcasts...", color = MediaHubTextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MediaHubTextSecondary) },
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .clickable { viewModel.navigateTo("search") }
                    .testTag("home_search_trigger"),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = Color(0xFF1E2130),
                    disabledContainerColor = MediaHubSurface,
                    disabledPlaceholderColor = MediaHubTextSecondary
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Horizontal Category Pills
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CategoryCard(title = "Music", icon = Icons.Default.MusicNote, color = MediaHubPrimary) {
                    viewModel.navigateTo("music")
                }
                CategoryCard(title = "Video", icon = Icons.Default.VideoLibrary, color = Color(0xFF2196F3)) {
                    viewModel.navigateTo("video")
                }
                CategoryCard(title = "Podcasts", icon = Icons.Default.Podcasts, color = Color(0xFF9C27B0)) {
                    viewModel.navigateTo("podcasts")
                }
            }
        }

        // Recently Played list
        if (recentPlayed.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recently Played", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        "See All",
                        fontSize = 12.sp,
                        color = MediaHubPrimary,
                        modifier = Modifier.clickable { }
                    )
                }
            }

            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(recentPlayed) { played ->
                        RecentPlayedCard(
                            title = played.title,
                            subtitle = played.subtitle,
                            coverUrl = played.coverUrl,
                            type = played.type,
                            onClick = {
                                when (played.type) {
                                    "MUSIC" -> {
                                        val t = viewModel.allTracks.value.find { track -> track.id == played.mediaId }
                                        if (t != null) viewModel.playTrack(t)
                                    }
                                    "PODCAST" -> {
                                        val ep = viewModel.allEpisodes.value.find { episode -> episode.id == played.mediaId }
                                        if (ep != null) viewModel.playEpisode(ep)
                                    }
                                    else -> {
                                        val v = viewModel.customVideos.value.find { video -> video.id == played.mediaId }
                                        if (v != null) viewModel.playVideo(v)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // Continue Listening section
        item {
            Text("Continue Listening", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(bottom = 12.dp))
        }

        val continueList = allTracks.take(3)
        items(continueList) { track ->
            ContinueListeningItem(
                track = track,
                onClick = { viewModel.playTrack(track) }
            )
        }
    }
}

@Composable
fun CategoryCard(title: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(90.dp)
            .width(104.dp)
            .background(MediaHubSurface, RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF1E2130), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("category_card_$title"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White)
        }
    }
}

@Composable
fun RecentPlayedCard(title: String, subtitle: String, coverUrl: String, type: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(130.dp)
                .background(MediaHubSurfaceVariant, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
        ) {
            AsyncImage(
                model = coverUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Play icon overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .background(Color(0xCC000000), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (type == "VIDEO") Icons.Default.PlayArrow else Icons.Default.MusicNote,
                    contentDescription = "Play",
                    tint = MediaHubPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, fontSize = 11.sp, color = MediaHubTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun ContinueListeningItem(track: Track, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(MediaHubSurface, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = track.coverUrl,
            contentDescription = track.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(track.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("${track.artist} • ${track.album}", fontSize = 12.sp, color = MediaHubTextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            // Simulated listening progress
            LinearProgressIndicator(
                progress = 0.45f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = MediaHubPrimary,
                trackColor = Color(0xFF1E2130)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFF1E2130), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}


// MUSIC SCREEN
@Composable
fun MusicScreen(viewModel: MediaHubViewModel) {
    val allTracks by viewModel.allTracks.collectAsState()
    var selectedSubTab by remember { mutableStateOf("Songs") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Music Library", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(bottom = 16.dp))

        // Sub Tabs row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf("Songs", "Albums", "Artists", "Genres")
            tabs.forEach { tab ->
                val isSelected = selectedSubTab == tab
                Button(
                    onClick = { selectedSubTab = tab },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MediaHubPrimary else MediaHubSurface,
                        contentColor = if (isSelected) Color.White else MediaHubTextSecondary
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(tab, fontSize = 13.sp)
                }
            }
        }

        // Shuffle all button
        Button(
            onClick = {
                if (allTracks.isNotEmpty()) {
                    viewModel.toggleShuffle()
                    viewModel.playTrack(allTracks.random())
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AE51937), contentColor = MediaHubPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .border(1.dp, MediaHubPrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .testTag("shuffle_all_button")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Shuffle All Tracks (${allTracks.size})", fontWeight = FontWeight.Bold)
            }
        }

        // Songs list
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(allTracks) { track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.playTrack(track) }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = track.coverUrl,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(track.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(track.artist, fontSize = 12.sp, color = MediaHubTextSecondary)
                    }
                    Text(track.durationText, fontSize = 12.sp, color = MediaHubTextSecondary, modifier = Modifier.padding(end = 8.dp))
                    IconButton(onClick = { viewModel.playTrack(track) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = MediaHubPrimary)
                    }
                }
            }
        }
    }
}


// VIDEO SCREEN
@Composable
fun VideoScreen(viewModel: MediaHubViewModel) {
    val videos by viewModel.customVideos.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Video Library", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Button(
                onClick = { viewModel.videoStreamDialogShown.value = true },
                colors = ButtonDefaults.buttonColors(containerColor = MediaHubPrimary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("add_stream_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Stream", fontSize = 12.sp)
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(videos) { video ->
                VideoGridCard(
                    video = video,
                    onClick = { viewModel.playVideo(video) }
                )
            }
        }
    }
}

@Composable
fun VideoGridCard(video: Video, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("video_card_${video.id}"),
        colors = CardDefaults.cardColors(containerColor = MediaHubSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, Color(0xFF1E2130))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(MediaHubSurfaceVariant)
            ) {
                AsyncImage(
                    model = video.coverUrl,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Duration Overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(Color(0xB3000000), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(video.durationText, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = video.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = video.category,
                    fontSize = 11.sp,
                    color = MediaHubTextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}


// PODCASTS SCREEN
@Composable
fun PodcastsScreen(viewModel: MediaHubViewModel) {
    val podcasts by viewModel.customPodcasts.collectAsState()
    val allEpisodes by viewModel.allEpisodes.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Podcasts", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Button(
                    onClick = { viewModel.rssFeedDialogShown.value = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MediaHubPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_rss_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = "Add RSS", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add RSS", fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Text("Subscribed Channels", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(bottom = 12.dp))
        }

        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(podcasts) { podcast ->
                    PodcastChannelCard(
                        podcast = podcast,
                        onClick = { viewModel.selectPodcast(podcast) }
                    )
                }
            }
        }

        item {
            Text("Latest Episodes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(bottom = 12.dp))
        }

        items(allEpisodes) { episode ->
            EpisodeRowItem(
                episode = episode,
                onClick = { viewModel.selectEpisode(episode) }
            )
        }
    }
}

@Composable
fun PodcastChannelCard(podcast: Podcast, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .clickable(onClick = onClick)
            .testTag("podcast_card_${podcast.id}")
    ) {
        AsyncImage(
            model = podcast.artworkUrl,
            contentDescription = podcast.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(110.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(0.5.dp, Color(0xFF1E2130), RoundedCornerShape(12.dp))
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(podcast.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${podcast.episodesCount} eps", fontSize = 10.sp, color = MediaHubTextSecondary)
    }
}

@Composable
fun EpisodeRowItem(episode: Episode, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(MediaHubSurface, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color(0xFF1E2130), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = MediaHubPrimary)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(episode.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${episode.podcastTitle} • ${episode.dateText}", fontSize = 11.sp, color = MediaHubTextSecondary)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(episode.durationText, fontSize = 11.sp, color = MediaHubTextSecondary)
    }
}


// PODCAST DETAIL SCREEN
@Composable
fun PodcastDetailScreen(viewModel: MediaHubViewModel) {
    val podcast by viewModel.selectedPodcast.collectAsState()
    val allEpisodes by viewModel.allEpisodes.collectAsState()

    if (podcast == null) return

    val currentPodcastEpisodes = allEpisodes.filter { it.podcastId == podcast!!.id }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            IconButton(
                onClick = { viewModel.navigateBack() },
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .background(MediaHubSurface, CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                AsyncImage(
                    model = podcast!!.artworkUrl,
                    contentDescription = podcast!!.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(podcast!!.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Host: ${podcast!!.host}", fontSize = 13.sp, color = MediaHubPrimary, modifier = Modifier.padding(top = 2.dp))
                    Text("${podcast!!.episodesCount} Episodes Available", fontSize = 11.sp, color = MediaHubTextSecondary, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        item {
            Text(
                text = podcast!!.description,
                fontSize = 12.sp,
                color = MediaHubTextSecondary,
                lineHeight = 18.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }

        item {
            Text("Episodes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(bottom = 12.dp))
        }

        if (currentPodcastEpisodes.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No local episodes. Default streams will play.", color = MediaHubTextSecondary, fontSize = 12.sp)
                }
            }
        } else {
            items(currentPodcastEpisodes) { episode ->
                EpisodeRowItem(
                    episode = episode,
                    onClick = { viewModel.selectEpisode(episode) }
                )
            }
        }
    }
}


// UNIVERSAL SEARCH SCREEN
@Composable
fun SearchScreen(viewModel: MediaHubViewModel) {
    val query by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.searchFilterType.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()

    val tracks by viewModel.allTracks.collectAsState()
    val videos by viewModel.customVideos.collectAsState()
    val podcasts by viewModel.customPodcasts.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Search", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(bottom = 16.dp))

        // Search Input Box
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.search(it) },
            placeholder = { Text("What do you want to play?", color = MediaHubTextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.clearSearch() }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                    }
                }
            }
        )

        // ... remaining UI omitted for brevity in patch
    }
}
