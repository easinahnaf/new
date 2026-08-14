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
                    items(recentPlayed) { item ->
                        RecentPlayedCard(
                            title = item.title,
                            subtitle = item.subtitle,
                            coverUrl = item.coverUrl,
                            type = item.type,
                            onClick = {
                                if (item.type == "MUSIC") {
                                    val t = viewModel.allTracks.value.find { it.id == item.mediaId }
                                    if (t != null) viewModel.playTrack(t)
                                } else if (item.type == "PODCAST") {
                                    val ep = viewModel.allEpisodes.value.find { it.id == item.mediaId }
                                    if (ep != null) viewModel.playEpisode(ep)
                                } else {
                                    val v = viewModel.customVideos.value.find { it.id == item.mediaId }
                                    if (v != null) viewModel.playVideo(v)
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
                progress = { 0.45f },
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
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("universal_search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MediaHubSurface,
                unfocusedContainerColor = MediaHubSurface,
                focusedBorderColor = MediaHubPrimary,
                unfocusedBorderColor = Color(0xFF1E2130)
            ),
            shape = RoundedCornerShape(12.dp)
        )

        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf("All", "Music", "Video", "Podcasts")
            filters.forEach { filter ->
                val isSelected = selectedFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.searchFilterType.value = filter },
                    label = { Text(filter, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MediaHubPrimary,
                        selectedLabelColor = Color.White,
                        containerColor = MediaHubSurface,
                        labelColor = MediaHubTextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = Color(0xFF1E2130),
                        selectedBorderColor = MediaHubPrimary
                    ),
                    modifier = Modifier.testTag("filter_$filter")
                )
            }
        }

        // Search Results
        if (query.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Searches", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                if (recentSearches.isNotEmpty()) {
                    Text(
                        "Clear",
                        fontSize = 12.sp,
                        color = MediaHubPrimary,
                        modifier = Modifier.clickable { viewModel.clearRecentSearches() }
                    )
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(recentSearches) { term ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.search(term) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.History, contentDescription = "History", tint = MediaHubTextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(term, color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        } else {
            val lowercaseQuery = query.lowercase()

            val filteredTracks = if (selectedFilter == "All" || selectedFilter == "Music") {
                tracks.filter { it.title.lowercase().contains(lowercaseQuery) || it.artist.lowercase().contains(lowercaseQuery) }
            } else emptyList()

            val filteredVideos = if (selectedFilter == "All" || selectedFilter == "Video") {
                videos.filter { it.title.lowercase().contains(lowercaseQuery) }
            } else emptyList()

            val filteredPodcasts = if (selectedFilter == "All" || selectedFilter == "Podcasts") {
                podcasts.filter { it.title.lowercase().contains(lowercaseQuery) || it.host.lowercase().contains(lowercaseQuery) }
            } else emptyList()

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (filteredTracks.isNotEmpty()) {
                    item { Text("Songs", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MediaHubPrimary) }
                    items(filteredTracks) { track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.playTrack(track) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = track.coverUrl,
                                contentDescription = track.title,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(track.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(track.artist, color = MediaHubTextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }

                if (filteredVideos.isNotEmpty()) {
                    item { Text("Videos", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3)) }
                    items(filteredVideos) { video ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.playVideo(video) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = video.coverUrl,
                                contentDescription = video.title,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(video.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(video.category, color = MediaHubTextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }

                if (filteredPodcasts.isNotEmpty()) {
                    item { Text("Podcasts", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9C27B0)) }
                    items(filteredPodcasts) { podcast ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectPodcast(podcast) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = podcast.artworkUrl,
                                contentDescription = podcast.title,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(podcast.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(podcast.host, color = MediaHubTextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }

                if (filteredTracks.isEmpty() && filteredVideos.isEmpty() && filteredPodcasts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No match found. Try a different query.", color = MediaHubTextSecondary)
                        }
                    }
                }
            }
        }
    }
}


// NOW PLAYING - AUDIO SCREEN
@Composable
fun NowPlayingAudioScreen(viewModel: MediaHubViewModel) {
    val track by viewModel.currentPlayingTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.playbackProgress.collectAsState()
    val duration by viewModel.playbackDuration.collectAsState()
    val shuffleActive by viewModel.shuffleActive.collectAsState()
    val repeatActive by viewModel.repeatActive.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val speed by viewModel.playbackSpeed.collectAsState()

    if (track == null) return

    val progressPercent = if (duration > 0) progress.toFloat() / duration else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MediaHubBackground)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Custom Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateBack() },
                modifier = Modifier.background(MediaHubSurface, CircleShape)
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimize", tint = Color.White)
            }
            Text("Now Playing", fontSize = 15.sp, color = MediaHubTextSecondary, fontWeight = FontWeight.Medium)
            IconButton(
                onClick = { },
                modifier = Modifier.background(MediaHubSurface, CircleShape)
            ) {
                Icon(Icons.Default.PlaylistPlay, contentDescription = "Queue", tint = Color.White)
            }
        }

        // Giant Glowing Disk Artwork (with Cyberpunk atmosphere)
        Box(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .border(4.dp, MediaHubPrimary.copy(alpha = 0.5f), CircleShape)
                    .clip(CircleShape)
            ) {
                AsyncImage(
                    model = track!!.coverUrl,
                    contentDescription = track!!.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Track Details
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(track!!.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
            Text(track!!.artist, fontSize = 15.sp, color = MediaHubPrimary, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))
        }

        // Mini Scrolling Lyrics visualizer (very cool detail from mockup)
        Box(
            modifier = Modifier
                .height(60.dp)
                .fillMaxWidth()
                .background(Color(0x0AFFFFFF), RoundedCornerShape(8.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            val lyricIndex = (progress / 15).toInt() % maxOf(1, track!!.lyrics.size)
            val currentLyric = if (track!!.lyrics.isNotEmpty()) track!!.lyrics[lyricIndex] else "[No Lyric available]"
            Text(
                text = currentLyric,
                fontSize = 12.sp,
                color = MediaHubPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progress Slider
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = progressPercent,
                onValueChange = { viewModel.seekToSeconds((it * duration).toLong()) },
                colors = SliderDefaults.colors(
                    thumbColor = MediaHubPrimary,
                    activeTrackColor = MediaHubPrimary,
                    inactiveTrackColor = Color(0xFF1E2130)
                ),
                modifier = Modifier.testTag("audio_playback_slider")
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(progress), fontSize = 11.sp, color = MediaHubTextSecondary)
                Text(formatTime(duration), fontSize = 11.sp, color = MediaHubTextSecondary)
            }
        }

        // Control Panel (Shuffle, Previous, Play/Pause, Next, Repeat)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.toggleShuffle() }) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (shuffleActive) MediaHubPrimary else MediaHubTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = { viewModel.playPreviousTrack() }) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(28.dp))
            }
            IconButton(
                onClick = { viewModel.togglePlayPause() },
                modifier = Modifier
                    .size(60.dp)
                    .background(MediaHubPrimary, CircleShape)
                    .testTag("audio_play_pause_button")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(onClick = { viewModel.playNextTrack() }) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(28.dp))
            }
            IconButton(onClick = { viewModel.toggleRepeat() }) {
                Icon(
                    imageVector = Icons.Default.Repeat,
                    contentDescription = "Repeat",
                    tint = if (repeatActive) MediaHubPrimary else MediaHubTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Volume slider & Speed Controller row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.VolumeDown, contentDescription = "Volume", tint = MediaHubTextSecondary, modifier = Modifier.size(16.dp))
            Slider(
                value = volume,
                onValueChange = { viewModel.setVolume(it) },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .testTag("volume_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color(0xFF1E2130)
                )
            )
            Icon(Icons.Default.VolumeUp, contentDescription = "Volume", tint = MediaHubTextSecondary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(16.dp))

            // Playback speed pill
            Button(
                onClick = {
                    val nextSpeed = when (speed) {
                        1.0f -> 1.25f
                        1.25f -> 1.5f
                        1.5f -> 2.0f
                        else -> 1.0f
                    }
                    viewModel.setSpeed(nextSpeed)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MediaHubSurface),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text("${speed}x", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}


// NOW PLAYING - VIDEO SCREEN (Custom ExoPlayer / VideoView)
@Composable
fun NowPlayingVideoScreen(viewModel: MediaHubViewModel) {
    val video by viewModel.currentPlayingVideo.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    var isLocked by remember { mutableStateOf(false) }
    var selectedResolution by remember { mutableStateOf("1080p HD") }
    var showResolutionDropdown by remember { mutableStateOf(false) }

    if (video == null) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Native Android VideoView to actually play custom video URLs!
        AndroidView(
            factory = { context ->
                VideoView(context).apply {
                    setVideoURI(Uri.parse(video!!.videoUrl))
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        start()
                    }
                }
            },
            update = { view ->
                if (isPlaying) view.start() else view.pause()
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Interface
        if (!isLocked) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.background(Color(0x99000000), CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    Text(
                        text = video!!.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    // Resolution button
                    Box {
                        Button(
                            onClick = { showResolutionDropdown = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x99000000)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(selectedResolution, fontSize = 12.sp, color = Color.White)
                        }

                        DropdownMenu(
                            expanded = showResolutionDropdown,
                            onDismissRequest = { showResolutionDropdown = false },
                            modifier = Modifier.background(MediaHubSurface)
                        ) {
                            val resolutions = listOf("1080p HD", "720p HD", "480p", "360p")
                            resolutions.forEach { res ->
                                DropdownMenuItem(
                                    text = { Text(res, color = Color.White) },
                                    onClick = {
                                        selectedResolution = res
                                        showResolutionDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Middle HUD controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.skipBackward15() },
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .background(Color(0x66000000), CircleShape)
                    ) {
                        Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                    }

                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier
                            .size(64.dp)
                            .background(MediaHubPrimary, CircleShape)
                            .testTag("video_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.skipForward15() },
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .background(Color(0x66000000), CircleShape)
                    ) {
                        Icon(Icons.Default.Forward10, contentDescription = "Forward 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }

                // Bottom Seek & Screen locks
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x99000000), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { isLocked = true }) {
                            Icon(Icons.Default.LockOpen, contentDescription = "Lock Controls", tint = Color.White)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Volume", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Icon(Icons.Default.AspectRatio, contentDescription = "Ratio", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        } else {
            // Screen is locked
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Button(
                    onClick = { isLocked = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xB3E51937)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = "Unlock", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unlock HUD", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}


// PODCAST EPISODE PLAYER SCREEN
@Composable
fun PodcastEpisodeScreen(viewModel: MediaHubViewModel) {
    val episode by viewModel.selectedEpisode.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.playbackProgress.collectAsState()
    val duration by viewModel.playbackDuration.collectAsState()
    val speed by viewModel.playbackSpeed.collectAsState()

    if (episode == null) return

    val progressPercent = if (duration > 0) progress.toFloat() / duration else 0f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MediaHubBackground)
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
                IconButton(
                    onClick = { viewModel.navigateBack() },
                    modifier = Modifier.background(MediaHubSurface, CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text("Podcast Episode", fontSize = 14.sp, color = MediaHubTextSecondary, fontWeight = FontWeight.Bold)
                IconButton(
                    onClick = { },
                    modifier = Modifier.background(MediaHubSurface, CircleShape)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.White)
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Podcast Channel Title Card
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .background(MediaHubSurface, RoundedCornerShape(16.dp))
                        .border(1.dp, MediaHubPrimary, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(Icons.Default.Podcasts, contentDescription = "Podcast", tint = MediaHubPrimary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = episode!!.podcastTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = episode!!.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Published ${episode!!.dateText}",
                    fontSize = 12.sp,
                    color = MediaHubPrimary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Play Controls Area
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = progressPercent,
                    onValueChange = { viewModel.seekToSeconds((it * duration).toLong()) },
                    colors = SliderDefaults.colors(
                        thumbColor = MediaHubPrimary,
                        activeTrackColor = MediaHubPrimary,
                        inactiveTrackColor = Color(0xFF1E2130)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(progress), fontSize = 11.sp, color = MediaHubTextSecondary)
                    Text(formatTime(duration), fontSize = 11.sp, color = MediaHubTextSecondary)
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.skipBackward15() },
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .background(MediaHubSurface, CircleShape)
                ) {
                    Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s", tint = Color.White)
                }

                IconButton(
                    onClick = { viewModel.togglePlayPause() },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .size(56.dp)
                        .background(MediaHubPrimary, CircleShape)
                        .testTag("podcast_play_pause")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.skipForward15() },
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .background(MediaHubSurface, CircleShape)
                ) {
                    Icon(Icons.Default.Forward10, contentDescription = "Forward 10s", tint = Color.White)
                }
            }
        }

        // Speed selector row & Actions (Download, Share, Show Notes)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speed pill
                Button(
                    onClick = {
                        val nextSpeed = when (speed) {
                            1.0f -> 1.25f
                            1.25f -> 1.5f
                            1.5f -> 2.0f
                            else -> 1.0f
                        }
                        viewModel.setSpeed(nextSpeed)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MediaHubSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Speed: ${speed}x", fontSize = 11.sp, color = Color.White)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconButton(onClick = {}) { Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.White) }
                    IconButton(onClick = {}) { Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White) }
                    IconButton(onClick = {}) { Icon(Icons.Default.Description, contentDescription = "Notes", tint = Color.White) }
                }
            }
        }

        // Show chapters list if available
        if (episode!!.chapters.isNotEmpty()) {
            item {
                Text(
                    text = "Chapters",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            items(episode!!.chapters) { chapter ->
                val isActive = progress >= chapter.seconds && (episode!!.chapters.indexOf(chapter) == episode!!.chapters.lastIndex || progress < episode!!.chapters[episode!!.chapters.indexOf(chapter) + 1].seconds)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(if (isActive) Color(0x33E51937) else MediaHubSurface, RoundedCornerShape(8.dp))
                        .clickable { viewModel.seekToSeconds(chapter.seconds) }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chapter.title,
                        fontSize = 13.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isActive) MediaHubPrimary else Color.White
                    )
                    Text(
                        text = chapter.timeText,
                        fontSize = 11.sp,
                        color = if (isActive) MediaHubPrimary else MediaHubTextSecondary
                    )
                }
            }
        }
    }
}


// Floating Miniature Player Capsule Styled like the glassy mockup bar
@Composable
fun MiniPlayerCapsule(
    track: Track?,
    episode: Episode?,
    isPlaying: Boolean,
    onPlayPauseToggle: () -> Unit,
    onTap: () -> Unit
) {
    val title = track?.title ?: episode?.title ?: ""
    val artist = track?.artist ?: episode?.podcastTitle ?: ""
    val coverUrl = track?.coverUrl ?: "https://images.unsplash.com/photo-1590602847861-f357a9332bbc?w=500&auto=format&fit=crop&q=60"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(Color(0xF012141F), RoundedCornerShape(32.dp))
            .border(1.dp, Color(0xFF1E2130), RoundedCornerShape(32.dp))
            .clickable(onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = coverUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = artist,
                fontSize = 11.sp,
                color = MediaHubPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(
            onClick = onPlayPauseToggle,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play/Pause",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}


// POPUPS / DIALOGS
@Composable
fun AddStreamDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Video Stream", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Input a standard streaming video URL (e.g. mp4, m3u8) to add to your MediaHub library.", color = MediaHubTextSecondary, fontSize = 12.sp)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Stream Title", color = MediaHubTextSecondary) },
                    modifier = Modifier.fillMaxWidth().testTag("add_video_title_input"),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Video Stream URL (MP4)", color = MediaHubTextSecondary) },
                    modifier = Modifier.fillMaxWidth().testTag("add_video_url_input"),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotEmpty() && url.isNotEmpty()) onAdd(title, url) },
                colors = ButtonDefaults.buttonColors(containerColor = MediaHubPrimary),
                enabled = title.isNotEmpty() && url.isNotEmpty()
            ) {
                Text("Add", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MediaHubTextSecondary)
            }
        },
        containerColor = MediaHubSurface
    )
}

@Composable
fun AddRssDialog(onDismiss: () -> Unit, onAdd: (String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Podcast Channel", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Input custom podcast details to simulate subscription and play automatic episodes.", color = MediaHubTextSecondary, fontSize = 12.sp)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Show Name / Title", color = MediaHubTextSecondary) },
                    modifier = Modifier.fillMaxWidth().testTag("add_podcast_title_input"),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Host", color = MediaHubTextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Brief Description", color = MediaHubTextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotEmpty()) onAdd(title, host, desc) },
                colors = ButtonDefaults.buttonColors(containerColor = MediaHubPrimary),
                enabled = title.isNotEmpty()
            ) {
                Text("Subscribe", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MediaHubTextSecondary)
            }
        },
        containerColor = MediaHubSurface
    )
}

// Helpers
private fun formatTime(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%d:%02d", m, s)
}
