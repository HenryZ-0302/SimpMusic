package com.maxrave.simpmusic.sync

import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.repository.LocalPlaylistRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.simpmusic.api.HYMusicApiService
import com.maxrave.simpmusic.api.SyncFavoriteItem
import com.maxrave.simpmusic.api.SyncHistoryItem
import com.maxrave.simpmusic.api.SyncPlaylistItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 数据同步管理器
 * 负责本地数据与云端的同步
 */
class SyncManager(
    private val apiService: HYMusicApiService,
    private val songRepository: SongRepository,
    private val localPlaylistRepository: LocalPlaylistRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // 同步状态
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
    
    /**
     * 登录后触发全量同步
     * - 从云端下载数据合并到本地
     * - 然后上传本地数据到云端
     */
    fun onLoginSuccess() {
        if (!apiService.isLoggedIn.value) return
        
        scope.launch {
            try {
                _syncState.value = SyncState.Syncing
                
                // 1. 下载云端数据合并到本地
                downloadAndMerge()
                
                // 2. 上传本地数据到云端
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
                // 合并收藏 - 只添加本地没有的
                response.favorites.forEach { cloudFav ->
                    val localSong = songRepository.getSongById(cloudFav.videoId).first()
                    if (localSong == null) {
                        // 本地没有，从云端添加
                        songRepository.insertSong(cloudFav.toSongEntity())
                    }
                }
                
                // 合并播放历史
                response.history.forEach { cloudHistory ->
                    val localSong = songRepository.getSongById(cloudHistory.videoId).first()
                    if (localSong == null) {
                        songRepository.insertSong(cloudHistory.toSongEntity())
                    }
                }
                
                // 播放列表暂不合并（结构复杂，需要更多处理）
            },
            onFailure = { /* 下载失败不影响，继续上传 */ }
        )
    }
    
    /**
     * 上传所有本地数据到云端
     */
    private suspend fun uploadAll() {
        uploadFavorites()
        uploadHistory()
        uploadPlaylists()
    }
    
    /**
     * 上传收藏歌曲
     */
    suspend fun uploadFavorites() {
        if (!apiService.isLoggedIn.value) return
        
        try {
            val likedSongs = songRepository.getLikedSongs().first()
            if (likedSongs.isEmpty()) return
            
            val syncItems = likedSongs.map { it.toSyncFavoriteItem() }
            apiService.syncFavorites(syncItems)
        } catch (e: Exception) {
            // 同步失败不影响本地使用
        }
    }
    
    /**
     * 上传播放历史
     */
    suspend fun uploadHistory() {
        if (!apiService.isLoggedIn.value) return
        
        try {
            val mostPlayed = songRepository.getMostPlayedSongs().first()
            if (mostPlayed.isEmpty()) return
            
            val syncItems = mostPlayed.take(100).map { it.toSyncHistoryItem() }
            apiService.syncHistory(syncItems)
        } catch (e: Exception) {
            // 同步失败不影响本地使用
        }
    }
    
    /**
     * 上传播放列表
     */
    suspend fun uploadPlaylists() {
        if (!apiService.isLoggedIn.value) return
        
        try {
            val playlists = localPlaylistRepository.getAllLocalPlaylists().first()
            if (playlists.isEmpty()) return
            
            val syncItems = playlists.map { it.toSyncPlaylistItem() }
            apiService.syncPlaylists(syncItems)
        } catch (e: Exception) {
            // 同步失败不影响本地使用
        }
    }
    
    /**
     * 收藏歌曲后触发同步
     */
    fun onFavoriteChanged() {
        if (!apiService.isLoggedIn.value) return
        scope.launch { uploadFavorites() }
    }
    
    /**
     * 播放列表变更后触发同步
     */
    fun onPlaylistChanged() {
        if (!apiService.isLoggedIn.value) return
        scope.launch { uploadPlaylists() }
    }
    
    /**
     * 手动触发全量同步
     */
    fun syncNow() {
        onLoginSuccess()
    }
}

// ========== 扩展函数：Entity 转 SyncItem ==========

private fun SongEntity.toSyncFavoriteItem() = SyncFavoriteItem(
    videoId = videoId,
    title = title,
    artist = artistName?.firstOrNull(),
    thumbnail = thumbnails,
    duration = duration?.toIntOrNull()
)

private fun SongEntity.toSyncHistoryItem() = SyncHistoryItem(
    videoId = videoId,
    title = title,
    artist = artistName?.firstOrNull(),
    thumbnail = thumbnails,
    duration = duration?.toIntOrNull()
)

private fun LocalPlaylistEntity.toSyncPlaylistItem() = SyncPlaylistItem(
    id = id.toString(),
    title = title,
    description = null,
    thumbnail = thumbnail,
    songs = null // 暂时不同步播放列表内歌曲（需要额外查询）
)

// ========== 扩展函数：SyncItem 转 Entity ==========

private fun SyncFavoriteItem.toSongEntity() = SongEntity(
    videoId = videoId,
    title = title,
    artistName = artist?.let { listOf(it) },
    thumbnails = thumbnail,
    duration = duration?.toString(),
    liked = true,
    inLibrary = kotlinx.datetime.Clock.System.now().toEpochMilliseconds().let { 
        kotlinx.datetime.Instant.fromEpochMilliseconds(it) 
    }.let { 
        kotlinx.datetime.TimeZone.currentSystemDefault().let { tz ->
            it.toLocalDateTime(tz)
        }
    }
)

private fun SyncHistoryItem.toSongEntity() = SongEntity(
    videoId = videoId,
    title = title,
    artistName = artist?.let { listOf(it) },
    thumbnails = thumbnail,
    duration = duration?.toString(),
    totalPlayTime = 1,
    inLibrary = kotlinx.datetime.Clock.System.now().toEpochMilliseconds().let { 
        kotlinx.datetime.Instant.fromEpochMilliseconds(it) 
    }.let { 
        kotlinx.datetime.TimeZone.currentSystemDefault().let { tz ->
            it.toLocalDateTime(tz)
        }
    }
)
