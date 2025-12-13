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
    val quality: String? = null,
    val language: String? = null,
    val autoPlay: Boolean? = null,
    val saveHistory: Boolean? = null
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
    val settings: SyncSettingsRequest? = null
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
