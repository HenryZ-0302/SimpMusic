package com.maxrave.simpmusic.api

import kotlinx.serialization.Serializable

// API 基础 URL
const val HYMUSIC_API_BASE_URL = "https://hymusic.zeabur.app"

// ========== 请求模型 ==========

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val nickname: String? = null
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class SyncFavoriteItem(
    val videoId: String,
    val title: String,
    val artist: String? = null,
    val thumbnail: String? = null,
    val duration: Int? = null
)

@Serializable
data class SyncPlaylistItem(
    val id: String? = null,
    val title: String,
    val description: String? = null,
    val thumbnail: String? = null,
    val songs: List<SyncFavoriteItem>? = null
)

@Serializable
data class SyncHistoryItem(
    val videoId: String,
    val title: String,
    val artist: String? = null,
    val thumbnail: String? = null,
    val duration: Int? = null
)

@Serializable
data class SyncSettingsRequest(
    // 基本设置
    val quality: String? = null,
    val language: String? = null,
    val saveHistory: Boolean? = null,
    
    // 下载设置
    val downloadQuality: String? = null,
    val videoDownloadQuality: String? = null,
    val videoQuality: String? = null,
    
    // 播放设置
    val normalizeVolume: Boolean? = null,
    val skipSilent: Boolean? = null,
    val saveStateOfPlayback: Boolean? = null,
    val crossfadeEnabled: Boolean? = null,
    val crossfadeDuration: Int? = null,
    
    // 音乐功能
    val sponsorBlockEnabled: Boolean? = null,
    val enableTranslateLyric: Boolean? = null,
    val lyricsProvider: String? = null,
    val translationLanguage: String? = null,
    val aiProvider: String? = null,
    val useAITranslation: Boolean? = null,
    
    // UI设置
    val translucentBottomBar: Boolean? = null,
    val blurPlayerBackground: Boolean? = null,
    val blurFullscreenLyrics: Boolean? = null,
    val enableLiquidGlass: Boolean? = null,
    val explicitContentEnabled: Boolean? = null,
    val homeLimit: Int? = null,
    
    // 其他
    val watchVideoInsteadOfPlayingAudio: Boolean? = null,
    val keepYouTubePlaylistOffline: Boolean? = null
)

// ========== 响应模型 ==========

@Serializable
data class AuthResponse(
    val message: String? = null,
    val token: String? = null,
    val user: UserInfo? = null,
    val error: String? = null
)

@Serializable
data class UserInfo(
    val id: String,
    val email: String,
    val nickname: String? = null,
    val avatar: String? = null,
    val isAdmin: Boolean = false,
    val createdAt: String? = null
)

@Serializable
data class SyncAllResponse(
    val favorites: List<SyncFavoriteItem> = emptyList(),
    val playlists: List<SyncPlaylistItem> = emptyList(),
    val history: List<SyncHistoryItem> = emptyList(),
    val settings: SyncSettingsRequest? = null,
    val library: SyncLibraryResponse? = null
)

@Serializable
data class SyncLibraryResponse(
    val albums: List<SyncAlbumItem> = emptyList(),
    val artists: List<SyncArtistItem> = emptyList(),
    val playlists: List<SyncYouTubePlaylistItem> = emptyList()
)

@Serializable
data class SyncLibraryRequest(
    val albums: List<SyncAlbumItem>,
    val artists: List<SyncArtistItem>,
    val playlists: List<SyncYouTubePlaylistItem>
)

@Serializable
data class SyncAlbumItem(
    val browseId: String,
    val title: String,
    val artist: String? = null,
    val thumbnail: String? = null
)

@Serializable
data class SyncArtistItem(
    val channelId: String,
    val name: String,
    val thumbnail: String? = null
)

@Serializable
data class SyncYouTubePlaylistItem(
    val playlistId: String,
    val title: String,
    val thumbnail: String? = null
)

@Serializable
data class ApiMessageResponse(
    val message: String? = null,
    val error: String? = null,
    val count: Int? = null
)

// ========== 管理员模型 ==========

@Serializable
data class AdminUsersResponse(
    val users: List<AdminUserInfo> = emptyList(),
    val pagination: PaginationInfo? = null
)

@Serializable
data class AdminUserInfo(
    val id: String,
    val email: String,
    val nickname: String? = null,
    val avatar: String? = null,
    val isAdmin: Boolean = false,
    val isBanned: Boolean = false,
    val createdAt: String? = null
)

@Serializable
data class PaginationInfo(
    val page: Int,
    val limit: Int,
    val total: Int,
    val pages: Int
)

@Serializable
data class AdminStatsResponse(
    val stats: StatsInfo? = null,
    val recentUsers: List<AdminUserInfo> = emptyList()
)

@Serializable
data class StatsInfo(
    val totalUsers: Int = 0,
    val todayUsers: Int = 0,
    val totalFavorites: Int = 0,
    val totalPlaylists: Int = 0,
    val totalPlayHistory: Int = 0
)

// 公告数据
@Serializable
data class AnnouncementItem(
    val id: String,
    val title: String,
    val content: String,
    val isActive: Boolean = true,
    val priority: Int = 0,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

// 系统设置
@Serializable
data class SystemSettingsInfo(
    val id: String = "singleton",
    val registrationEnabled: Boolean = true,
    val updatedAt: String? = null
)

@Serializable
data class SystemSettingsResponse(
    val settings: SystemSettingsInfo
)
