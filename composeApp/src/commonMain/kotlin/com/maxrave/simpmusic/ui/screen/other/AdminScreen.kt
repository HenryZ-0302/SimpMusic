package com.maxrave.simpmusic.ui.screen.other

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    
    // 检查是否是管理员
    val isAdmin = currentUser?.isAdmin == true
    
    // 加载数据
    LaunchedEffect(Unit) {
        if (isAdmin) {
            scope.launch {
                // 加载统计
                apiService.adminGetStats().fold(
                    onSuccess = { response ->
                        stats = response.stats
                    },
                    onFailure = { e ->
                        errorMessage = e.message
                    }
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
                    color = Color.White.copy(alpha = 0.7f)
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 统计卡片
                item {
                    Text(
                        text = "Statistics",
                        style = typo().titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
                
                items(users) { user ->
                    UserCard(
                        user = user,
                        onBanToggle = {
                            scope.launch {
                                apiService.adminBanUser(user.id, !user.isBanned).fold(
                                    onSuccess = {
                                        // 刷新列表
                                        users = users.map { 
                                            if (it.id == user.id) it.copy(isBanned = !it.isBanned) else it 
                                        }
                                    },
                                    onFailure = { e ->
                                        errorMessage = e.message
                                    }
                                )
                            }
                        }
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
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
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("Total Users", stats?.totalUsers?.toString() ?: "0")
                StatItem("Today", stats?.todayUsers?.toString() ?: "0")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("Favorites", stats?.totalFavorites?.toString() ?: "0")
                StatItem("Playlists", stats?.totalPlaylists?.toString() ?: "0")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("Play History", stats?.totalPlayHistory?.toString() ?: "0")
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = typo().titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = typo().bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun UserCard(
    user: AdminUserInfo,
    onBanToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (user.isBanned) 
                Color.Red.copy(alpha = 0.2f) 
            else 
                Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.nickname ?: "No Name",
                        style = typo().titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    if (user.isAdmin) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF6C63FF)
                            ),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Admin",
                                style = typo().labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (user.isBanned) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Red
                            ),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Banned",
                                style = typo().labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = user.email,
                    style = typo().bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            
            // Ban/Unban 按钮
            Button(
                onClick = onBanToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (user.isBanned) Color.Green else Color.Red
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (user.isBanned) "Unban" else "Ban",
                    style = typo().labelSmall
                )
            }
        }
    }
}
