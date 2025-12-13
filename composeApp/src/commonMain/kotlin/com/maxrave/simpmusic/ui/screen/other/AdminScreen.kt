package com.maxrave.simpmusic.ui.screen.other

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.maxrave.simpmusic.api.AdminUserInfo
import com.maxrave.simpmusic.api.HYMusicApiService
import com.maxrave.simpmusic.api.StatsInfo
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.theme.typo
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import hymusic.composeapp.generated.resources.*

/**
 * 管理员界面 - 用户管理和统计
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun AdminScreen(
    paddingValues: PaddingValues,
    navController: NavController,
    apiService: HYMusicApiService = koinInject(),
) {
    val hazeState = rememberHazeState()
    val scope = rememberCoroutineScope()
    
    // 当前用户
    val currentUser by apiService.currentUser.collectAsStateWithLifecycle()
    
    // 状态
    var users by remember { mutableStateOf<List<AdminUserInfo>>(emptyList()) }
    var stats by remember { mutableStateOf<StatsInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf<AdminUserInfo?>(null) }
    
    // 检查是否是管理员
    val isAdmin = currentUser?.isAdmin == true
    
    // 刷新用户列表
    fun refreshUsers() {
        scope.launch {
            apiService.adminGetUsers().fold(
                onSuccess = { response -> users = response.users },
                onFailure = { e -> errorMessage = e.message }
            )
        }
    }
    
    // 加载数据
    LaunchedEffect(Unit) {
        if (isAdmin) {
            scope.launch {
                // 加载统计
                apiService.adminGetStats().fold(
                    onSuccess = { response -> stats = response.stats },
                    onFailure = { e -> errorMessage = e.message }
                )
                
                // 加载用户列表
                apiService.adminGetUsers().fold(
                    onSuccess = { response ->
                        users = response.users
                        isLoading = false
                    },
                    onFailure = { e ->
                        errorMessage = e.message
                        isLoading = false
                    }
                )
            }
        } else {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(top = 64.dp)
            .hazeSource(state = hazeState),
    ) {
        if (!isAdmin) {
            // 非管理员提示
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Access Denied\nAdmin privileges required",
                    style = typo().titleMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        } else if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 统计卡片
                item {
                    Text(
                        text = "Statistics",
                        style = typo().titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    StatsCard(stats)
                }
                
                // 用户列表
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Users (${users.size})",
                        style = typo().titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                items(users, key = { it.id }) { user ->
                    UserCard(
                        user = user,
                        currentUserId = currentUser?.id,
                        onBanToggle = {
                            scope.launch {
                                apiService.adminBanUser(user.id, !user.isBanned).fold(
                                    onSuccess = {
                                        users = users.map { 
                                            if (it.id == user.id) it.copy(isBanned = !it.isBanned) else it 
                                        }
                                    },
                                    onFailure = { e -> errorMessage = e.message }
                                )
                            }
                        },
                        onAdminToggle = {
                            scope.launch {
                                apiService.adminSetUserAdmin(user.id, !user.isAdmin).fold(
                                    onSuccess = {
                                        users = users.map { 
                                            if (it.id == user.id) it.copy(isAdmin = !it.isAdmin) else it 
                                        }
                                    },
                                    onFailure = { e -> errorMessage = e.message }
                                )
                            }
                        },
                        onDelete = { showDeleteDialog = user }
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
    
    // 删除确认对话框
    showDeleteDialog?.let { user ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete User") },
            text = { Text("Are you sure you want to delete ${user.nickname ?: user.email}? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            apiService.adminDeleteUser(user.id).fold(
                                onSuccess = {
                                    users = users.filter { it.id != user.id }
                                    showDeleteDialog = null
                                },
                                onFailure = { e -> errorMessage = e.message }
                            )
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // TopAppBar
    TopAppBar(
        modifier = Modifier
            .hazeEffect(state = hazeState, style = HazeMaterials.ultraThin()) {
                blurEnabled = true
            },
        title = {
            Text(
                text = "Admin Panel",
                style = typo().titleMedium,
            )
        },
        navigationIcon = {
            RippleIconButton(
                Res.drawable.baseline_arrow_back_ios_new_24,
                Modifier.size(32.dp).padding(start = 5.dp),
                true,
            ) {
                navController.navigateUp()
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            Color.Transparent,
            Color.Unspecified,
            Color.Unspecified,
            Color.Unspecified,
            Color.Unspecified,
        ),
    )
}

@Composable
private fun StatsCard(stats: StatsInfo?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF6C63FF).copy(alpha = 0.3f),
                            Color(0xFF4ECDC4).copy(alpha = 0.3f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                // 第一行 - 主要统计
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItemEnhanced("Total Users", stats?.totalUsers ?: 0, Color(0xFF6C63FF))
                    StatItemEnhanced("Today", stats?.todayUsers ?: 0, Color(0xFF4ECDC4))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 第二行 - 内容统计
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItemSmall("Favorites", stats?.totalFavorites ?: 0)
                    StatItemSmall("Playlists", stats?.totalPlaylists ?: 0)
                    StatItemSmall("History", stats?.totalPlayHistory ?: 0)
                }
            }
        }
    }
}

@Composable
private fun StatItemEnhanced(label: String, value: Int, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        // 数字圆形背景
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value.toString(),
                style = typo().headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = typo().bodySmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun StatItemSmall(label: String, value: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value.toString(),
            style = typo().titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = typo().labelSmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun UserCard(
    user: AdminUserInfo,
    currentUserId: String?,
    onBanToggle: () -> Unit,
    onAdminToggle: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val isSelf = user.id == currentUserId
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                user.isBanned -> Color.Red.copy(alpha = 0.15f)
                user.isAdmin -> Color(0xFF6C63FF).copy(alpha = 0.15f)
                else -> Color.White.copy(alpha = 0.08f)
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 用户信息
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.nickname ?: "No Name",
                        style = typo().titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    
                    // 标签
                    if (user.isAdmin) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Badge(
                            containerColor = Color(0xFF6C63FF),
                            contentColor = Color.White
                        ) {
                            Text("Admin", fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }
                    if (user.isBanned) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Badge(
                            containerColor = Color.Red,
                            contentColor = Color.White
                        ) {
                            Text("Banned", fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = user.email,
                    style = typo().bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            
            // 操作按钮
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    // 封禁/解封
                    DropdownMenuItem(
                        text = { Text(if (user.isBanned) "Unban" else "Ban") },
                        onClick = {
                            showMenu = false
                            onBanToggle()
                        },
                        enabled = !isSelf
                    )
                    
                    // 管理员权限
                    DropdownMenuItem(
                        text = { Text(if (user.isAdmin) "Remove Admin" else "Make Admin") },
                        onClick = {
                            showMenu = false
                            onAdminToggle()
                        },
                        enabled = !isSelf
                    )
                    
                    HorizontalDivider()
                    
                    // 删除
                    DropdownMenuItem(
                        text = { Text("Delete", color = Color.Red) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color.Red
                            )
                        },
                        enabled = !isSelf
                    )
                }
            }
        }
    }
}
