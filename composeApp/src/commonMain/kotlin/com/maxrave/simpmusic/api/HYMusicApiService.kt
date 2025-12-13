package com.maxrave.simpmusic.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json

/**
 * HYMusic 后端 API 服务
 */
class HYMusicApiService {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 10000
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }
    
    // 用户登录状态
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn
    
    private val _currentUser = MutableStateFlow<UserInfo?>(null)
    val currentUser: StateFlow<UserInfo?> = _currentUser
    
    // Token 存储
    private var authToken: String? = null
    
    /**
     * 设置认证 Token
     */
    fun setToken(token: String?) {
        authToken = token
        _isLoggedIn.value = token != null
    }
    
    /**
     * 注册
     */
    suspend fun register(email: String, password: String, nickname: String? = null): Result<AuthResponse> {
        return try {
            val response = client.post("$HYMUSIC_API_BASE_URL/api/auth/register") {
                setBody(RegisterRequest(email, password, nickname))
            }.body<AuthResponse>()
            
            if (response.token != null) {
                setToken(response.token)
                _currentUser.value = response.user
            }
            
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 登录
     */
    suspend fun login(email: String, password: String): Result<AuthResponse> {
        return try {
            val response = client.post("$HYMUSIC_API_BASE_URL/api/auth/login") {
                setBody(LoginRequest(email, password))
            }.body<AuthResponse>()
            
            if (response.token != null) {
                setToken(response.token)
                _currentUser.value = response.user
            }
            
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取当前用户信息
     */
    suspend fun getMe(): Result<UserInfo> {
        return try {
            val response = client.get("$HYMUSIC_API_BASE_URL/api/auth/me") {
                bearerAuth(authToken ?: "")
            }.body<AuthResponse>()
            
            response.user?.let {
                _currentUser.value = it
                Result.success(it)
            } ?: Result.failure(Exception(response.error ?: "Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 登出
     */
    fun logout() {
        setToken(null)
        _currentUser.value = null
    }
    
    // ========== 数据同步 ==========
    
    /**
     * 获取所有同步数据
     */
    suspend fun syncGetAll(): Result<SyncAllResponse> {
        return try {
            val response = client.get("$HYMUSIC_API_BASE_URL/api/sync/all") {
                bearerAuth(authToken ?: "")
            }.body<SyncAllResponse>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 同步收藏
     */
    suspend fun syncFavorites(favorites: List<SyncFavoriteItem>): Result<ApiMessageResponse> {
        return try {
            val response = client.post("$HYMUSIC_API_BASE_URL/api/sync/favorites") {
                bearerAuth(authToken ?: "")
                setBody(mapOf("favorites" to favorites))
            }.body<ApiMessageResponse>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 同步播放列表
     */
    suspend fun syncPlaylists(playlists: List<SyncPlaylistItem>): Result<ApiMessageResponse> {
        return try {
            val response = client.post("$HYMUSIC_API_BASE_URL/api/sync/playlists") {
                bearerAuth(authToken ?: "")
                setBody(mapOf("playlists" to playlists))
            }.body<ApiMessageResponse>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 同步播放历史
     */
    suspend fun syncHistory(history: List<SyncHistoryItem>): Result<ApiMessageResponse> {
        return try {
            val response = client.post("$HYMUSIC_API_BASE_URL/api/sync/history") {
                bearerAuth(authToken ?: "")
                setBody(mapOf("history" to history))
            }.body<ApiMessageResponse>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 同步设置
     */
    suspend fun syncSettings(settings: SyncSettingsRequest): Result<ApiMessageResponse> {
        return try {
            val response = client.post("$HYMUSIC_API_BASE_URL/api/sync/settings") {
                bearerAuth(authToken ?: "")
                setBody(mapOf("settings" to settings))
            }.body<ApiMessageResponse>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ========== 管理员功能 ==========
    
    /**
     * 获取用户列表（管理员）
     */
    suspend fun adminGetUsers(page: Int = 1, limit: Int = 20, search: String? = null): Result<AdminUsersResponse> {
        return try {
            val response = client.get("$HYMUSIC_API_BASE_URL/api/admin/users") {
                bearerAuth(authToken ?: "")
                parameter("page", page)
                parameter("limit", limit)
                search?.let { parameter("search", it) }
            }.body<AdminUsersResponse>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 封禁/解封用户（管理员）
     */
    suspend fun adminBanUser(userId: String, ban: Boolean): Result<ApiMessageResponse> {
        return try {
            val response = client.post("$HYMUSIC_API_BASE_URL/api/admin/users/$userId/ban") {
                bearerAuth(authToken ?: "")
                setBody(mapOf("ban" to ban))
            }.body<ApiMessageResponse>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取统计数据（管理员）
     */
    suspend fun adminGetStats(): Result<AdminStatsResponse> {
        return try {
            val response = client.get("$HYMUSIC_API_BASE_URL/api/admin/stats") {
                bearerAuth(authToken ?: "")
            }.body<AdminStatsResponse>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
