package com.maxrave.simpmusic.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * HYMusic 后端 API 服务
 */
class HYMusicApiService {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    
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
    
    private suspend fun <T> executeRequest(
        request: Request,
        parser: (String) -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            Result.success(parser(body))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 注册
     */
    suspend fun register(email: String, password: String, nickname: String? = null): Result<AuthResponse> {
        val body = json.encodeToString(RegisterRequest(email, password, nickname))
        val request = Request.Builder()
            .url("$HYMUSIC_API_BASE_URL/api/auth/register")
            .post(body.toRequestBody(jsonMediaType))
            .build()
        
        return executeRequest(request) { responseBody ->
            val response = json.decodeFromString<AuthResponse>(responseBody)
            if (response.token != null) {
                setToken(response.token)
                _currentUser.value = response.user
            }
            response
        }
    }
    
    /**
     * 登录
     */
    suspend fun login(email: String, password: String): Result<AuthResponse> {
        val body = json.encodeToString(LoginRequest(email, password))
        val request = Request.Builder()
            .url("$HYMUSIC_API_BASE_URL/api/auth/login")
            .post(body.toRequestBody(jsonMediaType))
            .build()
        
        return executeRequest(request) { responseBody ->
            val response = json.decodeFromString<AuthResponse>(responseBody)
            if (response.token != null) {
                setToken(response.token)
                _currentUser.value = response.user
            }
            response
        }
    }
    
    /**
     * 获取当前用户信息
     */
    suspend fun getMe(): Result<UserInfo> {
        val request = Request.Builder()
            .url("$HYMUSIC_API_BASE_URL/api/auth/me")
            .addHeader("Authorization", "Bearer ${authToken ?: ""}")
            .get()
            .build()
        
        return executeRequest(request) { responseBody ->
            val response = json.decodeFromString<AuthResponse>(responseBody)
            response.user?.also { _currentUser.value = it }
                ?: throw Exception(response.error ?: "Unknown error")
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
        val request = Request.Builder()
            .url("$HYMUSIC_API_BASE_URL/api/sync/all")
            .addHeader("Authorization", "Bearer ${authToken ?: ""}")
            .get()
            .build()
        
        return executeRequest(request) { json.decodeFromString(it) }
    }
    
    /**
     * 同步收藏
     */
    suspend fun syncFavorites(favorites: List<SyncFavoriteItem>): Result<ApiMessageResponse> {
        val body = json.encodeToString(mapOf("favorites" to favorites))
        val request = Request.Builder()
            .url("$HYMUSIC_API_BASE_URL/api/sync/favorites")
            .addHeader("Authorization", "Bearer ${authToken ?: ""}")
            .post(body.toRequestBody(jsonMediaType))
            .build()
        
        return executeRequest(request) { json.decodeFromString(it) }
    }
    
    /**
     * 同步播放列表
     */
    suspend fun syncPlaylists(playlists: List<SyncPlaylistItem>): Result<ApiMessageResponse> {
        val body = json.encodeToString(mapOf("playlists" to playlists))
        val request = Request.Builder()
            .url("$HYMUSIC_API_BASE_URL/api/sync/playlists")
            .addHeader("Authorization", "Bearer ${authToken ?: ""}")
            .post(body.toRequestBody(jsonMediaType))
            .build()
        
        return executeRequest(request) { json.decodeFromString(it) }
    }
    
    /**
     * 同步播放历史
     */
    suspend fun syncHistory(history: List<SyncHistoryItem>): Result<ApiMessageResponse> {
        val body = json.encodeToString(mapOf("history" to history))
        val request = Request.Builder()
            .url("$HYMUSIC_API_BASE_URL/api/sync/history")
            .addHeader("Authorization", "Bearer ${authToken ?: ""}")
            .post(body.toRequestBody(jsonMediaType))
            .build()
        
        return executeRequest(request) { json.decodeFromString(it) }
    }
    
    /**
     * 同步设置
     */
    suspend fun syncSettings(settings: SyncSettingsRequest): Result<ApiMessageResponse> {
        val body = json.encodeToString(mapOf("settings" to settings))
        val request = Request.Builder()
            .url("$HYMUSIC_API_BASE_URL/api/sync/settings")
            .addHeader("Authorization", "Bearer ${authToken ?: ""}")
            .post(body.toRequestBody(jsonMediaType))
            .build()
        
        return executeRequest(request) { json.decodeFromString(it) }
    }
    
    // ========== 管理员功能 ==========
    
    /**
     * 获取用户列表（管理员）
     */
    suspend fun adminGetUsers(page: Int = 1, limit: Int = 20, search: String? = null): Result<AdminUsersResponse> {
        val url = buildString {
            append("$HYMUSIC_API_BASE_URL/api/admin/users")
            append("?page=$page&limit=$limit")
            search?.let { append("&search=$it") }
        }
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${authToken ?: ""}")
            .get()
            .build()
        
        return executeRequest(request) { json.decodeFromString(it) }
    }
    
    /**
     * 封禁/解封用户（管理员）
     */
    suspend fun adminBanUser(userId: String, ban: Boolean): Result<ApiMessageResponse> {
        val body = json.encodeToString(mapOf("ban" to ban))
        val request = Request.Builder()
            .url("$HYMUSIC_API_BASE_URL/api/admin/users/$userId/ban")
            .addHeader("Authorization", "Bearer ${authToken ?: ""}")
            .post(body.toRequestBody(jsonMediaType))
            .build()
        
        return executeRequest(request) { json.decodeFromString(it) }
    }
    
    /**
     * 获取统计数据（管理员）
     */
    suspend fun adminGetStats(): Result<AdminStatsResponse> {
        val request = Request.Builder()
            .url("$HYMUSIC_API_BASE_URL/api/admin/stats")
            .addHeader("Authorization", "Bearer ${authToken ?: ""}")
            .get()
            .build()
        
        return executeRequest(request) { json.decodeFromString(it) }
    }
    
    /**
     * 设置/取消用户管理员权限（管理员）
     */
    suspend fun adminSetUserAdmin(userId: String, isAdmin: Boolean): Result<ApiMessageResponse> {
        val body = json.encodeToString(mapOf("isAdmin" to isAdmin))
        val request = Request.Builder()
            .url("$HYMUSIC_API_BASE_URL/api/admin/users/$userId/admin")
            .addHeader("Authorization", "Bearer ${authToken ?: ""}")
            .post(body.toRequestBody(jsonMediaType))
            .build()
        
        return executeRequest(request) { json.decodeFromString(it) }
    }
    
    /**
     * 删除用户（管理员）
     */
    suspend fun adminDeleteUser(userId: String): Result<ApiMessageResponse> {
        val request = Request.Builder()
            .url("$HYMUSIC_API_BASE_URL/api/admin/users/$userId")
            .addHeader("Authorization", "Bearer ${authToken ?: ""}")
            .delete()
            .build()
        
        return executeRequest(request) { json.decodeFromString(it) }
    }
}
