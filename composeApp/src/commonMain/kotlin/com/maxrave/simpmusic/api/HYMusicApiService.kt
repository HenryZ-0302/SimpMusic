package com.maxrave.simpmusic.api

import com.maxrave.domain.manager.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
class HYMusicApiService(
    private val dataStoreManager: DataStoreManager
) {
    
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // 用户登录状态
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn
    
    private val _currentUser = MutableStateFlow<UserInfo?>(null)
    val currentUser: StateFlow<UserInfo?> = _currentUser
    
    // Token 存储 (内存缓存)
    private var authToken: String? = null
    
    companion object {
        private const val KEY_HYMUSIC_TOKEN = "hymusic_auth_token"
        private const val KEY_HYMUSIC_USER = "hymusic_user_json"
    }
    
    init {
        // 启动时从 DataStore 恢复登录状态
        scope.launch {
            restoreLoginState()
        }
    }
    
    /**
     * 从 DataStore 恢复登录状态
     */
    private suspend fun restoreLoginState() {
        val savedToken = dataStoreManager.getString(KEY_HYMUSIC_TOKEN).first()
        if (!savedToken.isNullOrEmpty()) {
            authToken = savedToken
            _isLoggedIn.value = true
            
            // 恢复用户信息
            val savedUserJson = dataStoreManager.getString(KEY_HYMUSIC_USER).first()
            if (!savedUserJson.isNullOrEmpty()) {
                try {
                    _currentUser.value = json.decodeFromString<UserInfo>(savedUserJson)
                } catch (e: Exception) {
                    // 如果解析失败，尝试从服务器获取
                    getMe()
                }
            } else {
                // 尝试从服务器获取用户信息
                getMe()
            }
        }
    }
    
    /**
     * 设置认证 Token 并持久化
     */
    private suspend fun setTokenAndPersist(token: String?, user: UserInfo? = null) {
        authToken = token
        _isLoggedIn.value = token != null
        
        if (token != null) {
            dataStoreManager.putString(KEY_HYMUSIC_TOKEN, token)
            user?.let {
                _currentUser.value = it
                dataStoreManager.putString(KEY_HYMUSIC_USER, json.encodeToString(it))
            }
        } else {
            dataStoreManager.putString(KEY_HYMUSIC_TOKEN, "")
            dataStoreManager.putString(KEY_HYMUSIC_USER, "")
            _currentUser.value = null
        }
    }
    
    private suspend fun <T> executeRequest(
        request: Request,
        parser: (String) -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            
            // 检测 403 状态码 - 用户被封禁，自动登出
            if (response.code == 403) {
                setTokenAndPersist(null)
                return@withContext Result.failure(Exception("User is banned"))
            }
            
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
                runBlocking {
                    setTokenAndPersist(response.token, response.user)
                }
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
                runBlocking {
                    setTokenAndPersist(response.token, response.user)
                }
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
            response.user?.also { 
                _currentUser.value = it
                scope.launch {
                    dataStoreManager.putString(KEY_HYMUSIC_USER, json.encodeToString(it))
                }
            } ?: throw Exception(response.error ?: "Unknown error")
        }
    }
    
    /**
     * 登出
     */
    fun logout() {
        scope.launch {
            setTokenAndPersist(null)
        }
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
