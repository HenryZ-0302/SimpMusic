package com.maxrave.simpmusic.sync

import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.LocalPlaylistRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.simpmusic.api.HYMusicApiService
import com.maxrave.simpmusic.api.SyncFavoriteItem
import com.maxrave.simpmusic.api.SyncHistoryItem
import com.maxrave.simpmusic.api.SyncPlaylistItem
import com.maxrave.simpmusic.api.SyncSettingsRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 数据同步管理器
 * 负责本地数据与云端的同步
 * 
 * 同步内容：
 * - 收藏歌曲 (Favorites)
 * - 播放列表 (Playlists)
 * - 播放历史 (History)
 * - 用户设置 (Settings) - 25+ 项全面同步
 */
class SyncManager(
    private val apiService: HYMusicApiService,
    private val songRepository: SongRepository,
    private val localPlaylistRepository: LocalPlaylistRepository,
    private val dataStoreManager: DataStoreManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // 定期同步间隔（5分钟）
    private val SYNC_INTERVAL_MS = 5 * 60 * 1000L
    
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
                // 恢复收藏
                response.favorites.forEach { cloudFav ->
                    try {
                        val localSong = songRepository.getSongById(cloudFav.videoId).first()
                        if (localSong == null) {
                            songRepository.insertSong(cloudFav.toSongEntity()).first()
                        }
                    } catch (e: Exception) { }
                }
                
                // 恢复播放历史
                response.history.forEach { cloudHistory ->
                    try {
                        val localSong = songRepository.getSongById(cloudHistory.videoId).first()
                        if (localSong == null) {
                            songRepository.insertSong(cloudHistory.toSongEntity()).first()
                        }
                    } catch (e: Exception) { }
                }
                
                // 恢复播放列表
                response.playlists.forEach { cloudPlaylist ->
                    try {
                        // 检查本地是否已有同名播放列表
                        val localPlaylists = localPlaylistRepository.getAllLocalPlaylists().first()
                        val exists = localPlaylists.any { it.title == cloudPlaylist.title }
                        if (!exists) {
                            localPlaylistRepository.insertLocalPlaylist(
                                LocalPlaylistEntity(
                                    title = cloudPlaylist.title,
                                    thumbnail = cloudPlaylist.thumbnail
                                ),
                                "Synced from cloud"
                            ).first()
                        }
                    } catch (e: Exception) { }
                }
                
                // 恢复全部设置
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
            onFailure = { }
        )
    }
    
    private suspend fun uploadAll() {
        uploadFavorites()
        uploadHistory()
        uploadPlaylists()
        uploadSettings()
    }
    
    suspend fun uploadFavorites() {
        if (!apiService.isLoggedIn.value) return
        try {
            val likedSongs = songRepository.getLikedSongs().first()
            if (likedSongs.isEmpty()) return
            val syncItems = likedSongs.map { it.toSyncFavoriteItem() }
            apiService.syncFavorites(syncItems)
        } catch (e: Exception) { }
    }
    
    suspend fun uploadHistory() {
        if (!apiService.isLoggedIn.value) return
        try {
            val mostPlayed = songRepository.getMostPlayedSongs().first()
            if (mostPlayed.isEmpty()) return
            val syncItems = mostPlayed.take(100).map { it.toSyncHistoryItem() }
            apiService.syncHistory(syncItems)
        } catch (e: Exception) { }
    }
    
    suspend fun uploadPlaylists() {
        if (!apiService.isLoggedIn.value) return
        try {
            val playlists = localPlaylistRepository.getAllLocalPlaylists().first()
            if (playlists.isEmpty()) return
            val syncItems = playlists.map { it.toSyncPlaylistItem() }
            apiService.syncPlaylists(syncItems)
        } catch (e: Exception) { }
    }
    
    /**
     * 上传全部用户设置
     */
    suspend fun uploadSettings() {
        if (!apiService.isLoggedIn.value) return
        try {
            val settings = SyncSettingsRequest(
                // 基本
                quality = dataStoreManager.quality.first(),
                language = dataStoreManager.getString("language").first(),
                saveHistory = dataStoreManager.saveRecentSongAndQueue.first() == DataStoreManager.TRUE,
                
                // 下载
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
