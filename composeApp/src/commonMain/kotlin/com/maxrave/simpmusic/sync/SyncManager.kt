package com.maxrave.simpmusic.sync

import com.maxrave.domain.data.entities.AlbumEntity
import com.maxrave.domain.data.entities.ArtistEntity
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.PlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.AlbumRepository
import com.maxrave.domain.repository.ArtistRepository
import com.maxrave.domain.repository.LocalPlaylistRepository
import com.maxrave.domain.repository.PlaylistRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.extension.now
import com.maxrave.simpmusic.api.HYMusicApiService
import com.maxrave.simpmusic.api.SyncAlbumItem
import com.maxrave.simpmusic.api.SyncArtistItem
import com.maxrave.simpmusic.api.SyncFavoriteItem
import com.maxrave.simpmusic.api.SyncHistoryItem
import com.maxrave.simpmusic.api.SyncLibraryRequest
import com.maxrave.simpmusic.api.SyncPlaylistItem
import com.maxrave.simpmusic.api.SyncSettingsRequest
import com.maxrave.simpmusic.api.SyncYouTubePlaylistItem
import com.maxrave.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime

/**
 * 数据同步管理器
 * 负责本地数据与云端的同步
 * 
 * 同步内容：
 * - 收藏歌曲 (Favorites)
 * - 播放列表 (Playlists)
 * - 播放历史 (History)
 * - 用户设置 (Settings)
 * - 音乐库 (Library): Albums, Artists, Saved YouTube Playlists
 */
class SyncManager(
    private val apiService: HYMusicApiService,
    private val songRepository: SongRepository,
    private val localPlaylistRepository: LocalPlaylistRepository,
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository,
    private val playlistRepository: PlaylistRepository,
    private val dataStoreManager: DataStoreManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // 定期同步间隔（5分钟）
    private val SYNC_INTERVAL_MS = 5 * 60 * 1000L
    private val TAG = "SyncManager"
    
    init {
        startPeriodicSync()
    }
    
    private fun startPeriodicSync() {
        scope.launch {
            while (true) {
                delay(SYNC_INTERVAL_MS)
                if (apiService.isLoggedIn.value) {
                    try { uploadAll() } catch (e: Exception) { }
                }
            }
        }
    }
    
    sealed class SyncState {
        object Idle : SyncState()
        object Syncing : SyncState()
        data class Success(val message: String) : SyncState()
        data class Failed(val error: String) : SyncState()
    }
    
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState
    
    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime
    
    fun onLoginSuccess() {
        if (!apiService.isLoggedIn.value) return
        
        scope.launch {
            try {
                _syncState.value = SyncState.Syncing
                downloadAndMerge()
                uploadAll()
                _lastSyncTime.value = System.currentTimeMillis()
                _syncState.value = SyncState.Success("Sync completed")
            } catch (e: Exception) {
                Logger.e(TAG, "Sync failed: ${e.message}")
                _syncState.value = SyncState.Failed(e.message ?: "Sync failed")
            }
        }
    }
    
    /**
     * 手动上传数据到云端 (Full Overwrite)
     */
    fun uploadNow() {
        if (!apiService.isLoggedIn.value) return
        scope.launch {
            try {
                _syncState.value = SyncState.Syncing
                uploadAll()
                _lastSyncTime.value = System.currentTimeMillis()
                _syncState.value = SyncState.Success("Upload completed")
            } catch (e: Exception) {
                Logger.e(TAG, "Upload failed: ${e.message}")
                _syncState.value = SyncState.Failed(e.message ?: "Upload failed")
            }
        }
    }

    /**
     * 手动从云端下载数据 (Merge)
     */
    fun downloadNow() {
        if (!apiService.isLoggedIn.value) return
        scope.launch {
            try {
                _syncState.value = SyncState.Syncing
                downloadAndMerge()
                _lastSyncTime.value = System.currentTimeMillis()
                _syncState.value = SyncState.Success("Download completed")
            } catch (e: Exception) {
                Logger.e(TAG, "Download failed: ${e.message}")
                _syncState.value = SyncState.Failed(e.message ?: "Download failed")
            }
        }
    }

    /**
     * 执行完整同步 (Upload and Download)
     */
    fun fullSync() {
        if (!apiService.isLoggedIn.value) return
        scope.launch {
            try {
                _syncState.value = SyncState.Syncing
                // 先下载合并，再上传最新的全量
                downloadAndMerge()
                uploadAll()
                _lastSyncTime.value = System.currentTimeMillis()
                _syncState.value = SyncState.Success("Full sync completed")
            } catch (e: Exception) {
                Logger.e(TAG, "Full sync failed: ${e.message}")
                _syncState.value = SyncState.Failed(e.message ?: "Sync failed")
            }
        }
    }
    
    /**
     * 下载云端数据并合并到本地
     */
    private suspend fun downloadAndMerge() {
        val result = apiService.syncGetAll()
        result.fold(
            onSuccess = { response ->
                Logger.d(TAG, "Download success: Fav=${response.favorites.size}, PL=${response.playlists.size}, Lib=${response.library != null}")

                // 恢复收藏
                response.favorites.forEach { cloudFav ->
                    try {
                        val localSong = songRepository.getSongById(cloudFav.videoId).first()
                        if (localSong == null) {
                            songRepository.insertSong(cloudFav.toSongEntity()).first()
                        } else if (!localSong.liked) {
                            // 如果本地已有但未收藏，强制更新为收藏状态
                            songRepository.updateLikeStatus(
                                videoId = cloudFav.videoId,
                                likeStatus = 1 // 1 = LIKE
                            )
                        }
                    } catch (e: Exception) { }
                }
                
                // 恢复播放历史 (部分) - 这里只做简单合并，不做详细处理，因为历史记录比较复杂
                response.history.forEach { cloudHistory ->
                    try {
                        val localSong = songRepository.getSongById(cloudHistory.videoId).first()
                        if (localSong == null) {
                            songRepository.insertSong(cloudHistory.toSongEntity()).first()
                        }
                    } catch (e: Exception) { }
                }
                
                // 恢复播放列表 (本地自建)
                response.playlists.forEach { cloudPlaylist ->
                    try {
                        Logger.d(TAG, "Restoring playlist '${cloudPlaylist.title}' with ${cloudPlaylist.songs?.size ?: 0} songs")
                        
                        // 检查是否已存在同名歌单
                        val localPlaylists = localPlaylistRepository.getAllLocalPlaylists().first()
                        val existing = localPlaylists.find { it.title == cloudPlaylist.title }
                        
                        if (existing != null) {
                            Logger.d(TAG, "Playlist '${cloudPlaylist.title}' already exists, skipping")
                            return@forEach // 已存在则跳过
                        }
                        
                        // 1. 先确保所有歌曲都在本地数据库中
                        val videoIds = mutableListOf<String>()
                        cloudPlaylist.songs.orEmpty().forEach { songItem ->
                            try {
                                val localSong = songRepository.getSongById(songItem.videoId).first()
                                if (localSong == null) {
                                    songRepository.insertSong(songItem.toSongEntity()).first()
                                }
                                videoIds.add(songItem.videoId)
                            } catch (e: Exception) {
                                Logger.e(TAG, "Failed to restore song ${songItem.videoId}: ${e.message}")
                            }
                        }
                        
                        // 2. 创建歌单，包含 tracks 字段
                        val newPlaylist = LocalPlaylistEntity(
                            title = cloudPlaylist.title,
                            thumbnail = cloudPlaylist.thumbnail,
                            tracks = videoIds, // 直接设置 tracks 字段！
                            inLibrary = now()
                        )
                        localPlaylistRepository.insertLocalPlaylist(newPlaylist, "Restored from cloud").first()
                        Logger.d(TAG, "Successfully restored playlist '${cloudPlaylist.title}' with ${videoIds.size} tracks")
                        
                    } catch (e: Exception) {
                        Logger.e(TAG, "Error restoring playlist ${cloudPlaylist.title}: ${e.message}")
                    }
                }

                // 恢复 Library (Albums, Artists, YouTube Playlists)
                response.library?.let { lib ->
                    // Albums
                    lib.albums.forEach { item ->
                        try {
                            // 检查是否已存在，不存在可直接插入 (IGNORE on conflict usually)
                            // 这里我们用 toAlbumEntity 里的 InLibrary 时间
                            val entity = item.toAlbumEntity()
                            albumRepository.insertAlbum(entity)
                        } catch (e: Exception) { Logger.e(TAG, "Restore Album Error: $e") }
                    }
                    // Artists
                    lib.artists.forEach { item ->
                        try {
                            val entity = item.toArtistEntity()
                            artistRepository.insertArtist(entity) // void return
                        } catch (e: Exception) { Logger.e(TAG, "Restore Artist Error: $e") }
                    }
                    // YouTube Playlists (Saved)
                    lib.playlists.forEach { item ->
                        try {
                            val entity = item.toPlaylistEntity()
                            playlistRepository.insertPlaylist(entity) // void return
                        } catch (e: Exception) { Logger.e(TAG, "Restore YT Playlist Error: $e") }
                    }
                }
                
                // 恢复设置
                response.settings?.let { s ->
                    try {
                        // 基本设置
                        s.quality?.let { dataStoreManager.setQuality(it) }
                        s.language?.let { dataStoreManager.putString("language", it) }
                        s.saveHistory?.let { dataStoreManager.setSaveRecentSongAndQueue(it) }
                        
                        // 下载设置
                        s.downloadQuality?.let { dataStoreManager.setDownloadQuality(it) }
                        s.videoDownloadQuality?.let { dataStoreManager.setVideoDownloadQuality(it) }
                        s.videoQuality?.let { dataStoreManager.setVideoQuality(it) }
                        
                        // 播放设置
                        s.normalizeVolume?.let { dataStoreManager.setNormalizeVolume(it) }
                        s.skipSilent?.let { dataStoreManager.setSkipSilent(it) }
                        s.saveStateOfPlayback?.let { dataStoreManager.setSaveStateOfPlayback(it) }
                        s.crossfadeEnabled?.let { dataStoreManager.setCrossfadeEnabled(it) }
                        s.crossfadeDuration?.let { dataStoreManager.setCrossfadeDuration(it) }
                        
                        // 音乐功能
                        s.sponsorBlockEnabled?.let { dataStoreManager.setSponsorBlockEnabled(it) }
                        s.enableTranslateLyric?.let { dataStoreManager.setEnableTranslateLyric(it) }
                        s.lyricsProvider?.let { dataStoreManager.setLyricsProvider(it) }
                        s.translationLanguage?.let { dataStoreManager.setTranslationLanguage(it) }
                        s.aiProvider?.let { dataStoreManager.setAIProvider(it) }
                        s.useAITranslation?.let { dataStoreManager.setUseAITranslation(it) }
                        
                        // UI设置
                        s.translucentBottomBar?.let { dataStoreManager.setTranslucentBottomBar(it) }
                        s.blurPlayerBackground?.let { dataStoreManager.setBlurPlayerBackground(it) }
                        s.blurFullscreenLyrics?.let { dataStoreManager.setBlurFullscreenLyrics(it) }
                        s.enableLiquidGlass?.let { dataStoreManager.setEnableLiquidGlass(it) }
                        s.explicitContentEnabled?.let { dataStoreManager.setExplicitContentEnabled(it) }
                        s.homeLimit?.let { dataStoreManager.setHomeLimit(it) }
                        
                        // 其他
                        s.watchVideoInsteadOfPlayingAudio?.let { dataStoreManager.setWatchVideoInsteadOfPlayingAudio(it) }
                        s.keepYouTubePlaylistOffline?.let { dataStoreManager.setKeepYouTubePlaylistOffline(it) }
                    } catch (e: Exception) { }
                }
                
            },
            onFailure = { e ->
                Logger.e(TAG, "Failed to download data: ${e.message}")
                throw e
            }
        )
    }
    
    private suspend fun uploadAll() {
        uploadFavorites()
        uploadPlaylists()
        uploadHistory()
        uploadLibrary()
        uploadSettings()
    }
    
    suspend fun uploadFavorites() {
        if (!apiService.isLoggedIn.value) return
        try {
            val likedSongs = songRepository.getLikedSongs().first()
            Logger.d(TAG, "Uploading ${likedSongs.size} favorites")
            // 允许上传空列表，以便清空云端数据
            val syncItems = likedSongs.map { it.toSyncFavoriteItem() }
            apiService.syncFavorites(syncItems)
        } catch (e: Exception) { }
    }
    
    suspend fun uploadHistory() {
        if (!apiService.isLoggedIn.value) return
        try {
            val mostPlayed = songRepository.getMostPlayedSongs().first()
            val syncItems = mostPlayed.take(100).map { it.toSyncHistoryItem() }
            apiService.syncHistory(syncItems)
        } catch (e: Exception) { }
    }
    
    suspend fun uploadPlaylists() {
        if (!apiService.isLoggedIn.value) return
        try {
            val playlists = localPlaylistRepository.getAllLocalPlaylists().first()
            Logger.d(TAG, "Uploading ${playlists.size} local playlists")
            
            val syncItems = playlists.map { playlist ->
                // 方法1: 尝试从 tracks 字段获取视频 ID 列表
                val trackVideoIds = playlist.tracks ?: emptyList()
                Logger.d(TAG, "Playlist '${playlist.title}' has ${trackVideoIds.size} track IDs from tracks field")
                
                // 获取每首歌的详细信息
                val songItems = trackVideoIds.mapNotNull { videoId ->
                    try {
                        songRepository.getSongById(videoId).first()?.toSyncFavoriteItem()
                    } catch (e: Exception) {
                        Logger.e(TAG, "Failed to get song $videoId: ${e.message}")
                        null
                    }
                }
                Logger.d(TAG, "Playlist '${playlist.title}' resolved ${songItems.size} songs")
                
                SyncPlaylistItem(
                    title = playlist.title,
                    thumbnail = playlist.thumbnail,
                    songs = songItems
                )
            }
            Logger.d(TAG, "Syncing ${syncItems.size} playlists to cloud")
            apiService.syncPlaylists(syncItems)
        } catch (e: Exception) {
            Logger.e(TAG, "Upload Playlists Error: ${e.message}")
        }
    }
    
    /**
     * 上传全部用户设置
     */
    suspend fun uploadSettings() {
        if (!apiService.isLoggedIn.value) return
        try {
            val settings = SyncSettingsRequest(
                // 基本设置
                quality = dataStoreManager.quality.first(),
                language = dataStoreManager.language.first(),
                saveHistory = dataStoreManager.saveRecentSongAndQueue.first() == "TRUE",
                
                // 下载设置
                downloadQuality = dataStoreManager.downloadQuality.first(),
                videoDownloadQuality = dataStoreManager.videoDownloadQuality.first(),
                videoQuality = dataStoreManager.videoQuality.first(),
                
                // 播放
                normalizeVolume = dataStoreManager.normalizeVolume.first() == DataStoreManager.TRUE,
                skipSilent = dataStoreManager.skipSilent.first() == DataStoreManager.TRUE,
                saveStateOfPlayback = dataStoreManager.saveStateOfPlayback.first() == DataStoreManager.TRUE,
                crossfadeEnabled = dataStoreManager.crossfadeEnabled.first() == DataStoreManager.TRUE,
                crossfadeDuration = dataStoreManager.crossfadeDuration.first(),
                
                // 音乐
                sponsorBlockEnabled = dataStoreManager.sponsorBlockEnabled.first() == DataStoreManager.TRUE,
                enableTranslateLyric = dataStoreManager.enableTranslateLyric.first() == DataStoreManager.TRUE,
                lyricsProvider = dataStoreManager.lyricsProvider.first(),
                translationLanguage = dataStoreManager.translationLanguage.first(),
                aiProvider = dataStoreManager.aiProvider.first(),
                useAITranslation = dataStoreManager.useAITranslation.first() == DataStoreManager.TRUE,
                
                // UI
                translucentBottomBar = dataStoreManager.translucentBottomBar.first() == DataStoreManager.TRUE,
                blurPlayerBackground = dataStoreManager.blurPlayerBackground.first() == DataStoreManager.TRUE,
                blurFullscreenLyrics = dataStoreManager.blurFullscreenLyrics.first() == DataStoreManager.TRUE,
                enableLiquidGlass = dataStoreManager.enableLiquidGlass.first() == DataStoreManager.TRUE,
                explicitContentEnabled = dataStoreManager.explicitContentEnabled.first() == DataStoreManager.TRUE,
                homeLimit = dataStoreManager.homeLimit.first(),
                
                // 其他
                watchVideoInsteadOfPlayingAudio = dataStoreManager.watchVideoInsteadOfPlayingAudio.first() == DataStoreManager.TRUE,
                keepYouTubePlaylistOffline = dataStoreManager.keepYouTubePlaylistOffline.first() == DataStoreManager.TRUE
            )
            apiService.syncSettings(settings)
        } catch (e: Exception) { }
    }

    /**
     * 上传音乐库 (Albums, Artists, YouTube Playlists)
     */
    suspend fun uploadLibrary() {
        if (!apiService.isLoggedIn.value) return
        try {
            val albums = albumRepository.getAllAlbums(1000).first()
            val artists = artistRepository.getAllArtists(1000).first()
            val playlists = playlistRepository.getAllPlaylists(1000).first()

            val request = SyncLibraryRequest(
                albums = albums.map { it.toSyncAlbumItem() },
                artists = artists.map { it.toSyncArtistItem() },
                playlists = playlists.map { it.toSyncYouTubePlaylistItem() }
            )
            Logger.d(TAG, "Uploading Library: Albums=${request.albums.size}, Artists=${request.artists.size}, Playlists=${request.playlists.size}")
            apiService.syncLibrary(request)
        } catch (e: Exception) {
            Logger.e(TAG, "Upload Library Error: ${e.message}")
        }
    }
    
    fun onFavoriteChanged() {
        if (!apiService.isLoggedIn.value) return
        scope.launch { uploadFavorites() }
    }
    
    fun onPlaylistChanged() {
        if (!apiService.isLoggedIn.value) return
        scope.launch { uploadPlaylists() }
    }
    
    fun onSettingsChanged() {
        if (!apiService.isLoggedIn.value) return
        scope.launch { uploadSettings() }
    }
    
    fun syncNow() {
        onLoginSuccess()
    }
}

// ========== Entity 转 SyncItem ==========

private fun SongEntity.toSyncFavoriteItem() = SyncFavoriteItem(
    videoId = videoId,
    title = title,
    artist = artistName?.firstOrNull(),
    thumbnail = thumbnails,
    duration = durationSeconds
)

private fun SongEntity.toSyncHistoryItem() = SyncHistoryItem(
    videoId = videoId,
    title = title,
    artist = artistName?.firstOrNull(),
    thumbnail = thumbnails,
    duration = durationSeconds
)

private fun LocalPlaylistEntity.toSyncPlaylistItem() = SyncPlaylistItem(
    id = id.toString(),
    title = title,
    description = null,
    thumbnail = thumbnail,
    songs = null
)

// ========== SyncItem 转 Entity ==========

private fun SyncFavoriteItem.toSongEntity() = SongEntity(
    videoId = videoId,
    title = title,
    artistName = artist?.let { listOf(it) },
    thumbnails = thumbnail,
    duration = duration?.let { "${it / 60}:${String.format("%02d", it % 60)}" } ?: "0:00",
    durationSeconds = duration ?: 0,
    isAvailable = true,
    isExplicit = false,
    likeStatus = "LIKE",
    videoType = "MUSIC_VIDEO_TYPE_ATV",
    category = null,
    resultType = null,
    liked = true
)

private fun SyncHistoryItem.toSongEntity() = SongEntity(
    videoId = videoId,
    title = title,
    artistName = artist?.let { listOf(it) },
    thumbnails = thumbnail,
    duration = duration?.let { "${it / 60}:${String.format("%02d", it % 60)}" } ?: "0:00",
    durationSeconds = duration ?: 0,
    isAvailable = true,
    isExplicit = false,
    likeStatus = "INDIFFERENT",
    videoType = "MUSIC_VIDEO_TYPE_ATV",
    category = null,
    resultType = null,
    totalPlayTime = 1
)

// ========== Library Entity 转 SyncItem ==========

private fun AlbumEntity.toSyncAlbumItem() = SyncAlbumItem(
    browseId = browseId,
    title = title,
    artist = artistName?.joinToString(", "),
    thumbnail = thumbnails
)

private fun ArtistEntity.toSyncArtistItem() = SyncArtistItem(
    channelId = channelId,
    name = name,
    thumbnail = thumbnails
)

private fun PlaylistEntity.toSyncYouTubePlaylistItem() = SyncYouTubePlaylistItem(
    playlistId = id,
    title = title,
    thumbnail = thumbnails
)

// ========== Library SyncItem 转 Entity ==========

private fun SyncAlbumItem.toAlbumEntity() = AlbumEntity(
    browseId = browseId,
    title = title,
    artistName = artist?.let { listOf(it) },
    thumbnails = thumbnail,
    audioPlaylistId = browseId, // Use browseId as fallback
    description = "",
    duration = null,
    durationSeconds = 0,
    trackCount = 0,
    type = "Album",
    year = null,
    liked = true,
    inLibrary = now()
)

private fun SyncArtistItem.toArtistEntity() = ArtistEntity(
    channelId = channelId,
    name = name,
    thumbnails = thumbnail,
    followed = true,
    inLibrary = now()
)

private fun SyncYouTubePlaylistItem.toPlaylistEntity() = PlaylistEntity(
    id = playlistId,
    title = title,
    thumbnails = thumbnail ?: "",
    description = "",
    duration = "",
    durationSeconds = 0,
    trackCount = 0,
    liked = true,
    inLibrary = now()
)

// ========== LocalPlaylist 转换 ==========

private fun SyncPlaylistItem.toLocalPlaylistEntity() = LocalPlaylistEntity(
    title = title,
    thumbnail = thumbnail,
    inLibrary = now()
)
