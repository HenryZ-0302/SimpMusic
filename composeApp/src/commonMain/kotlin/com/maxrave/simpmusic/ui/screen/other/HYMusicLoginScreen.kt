package com.maxrave.simpmusic.ui.screen.other

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.maxrave.simpmusic.api.HYMusicApiService
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
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun HYMusicLoginScreen(
    paddingValues: PaddingValues,
    navController: NavController,
    apiService: HYMusicApiService = koinInject(),
) {
    val hazeState = rememberHazeState()
    val scope = rememberCoroutineScope()
    
    // UI State
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    
    // 登录状态
    val isLoggedIn by apiService.isLoggedIn.collectAsStateWithLifecycle()
    val currentUser by apiService.currentUser.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(top = 64.dp)
            .verticalScroll(rememberScrollState())
            .hazeSource(state = hazeState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(30.dp))
        
        if (isLoggedIn && currentUser != null) {
            // 已登录状态
            Text(
                text = "Welcome, ${currentUser?.nickname ?: currentUser?.email}!",
                style = typo().titleLarge,
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = currentUser?.email ?: "",
                style = typo().bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
            )
            
            Spacer(modifier = Modifier.height(30.dp))
            
            Button(
                onClick = {
                    apiService.logout()
                    successMessage = "Logged out successfully"
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red.copy(alpha = 0.8f)
                )
            ) {
                Text("Logout")
            }
        } else {
            // 未登录状态
            Text(
                text = if (isLoginMode) "Login to HYMusic" else "Create Account",
                style = typo().titleLarge,
            )
            
            Spacer(modifier = Modifier.height(30.dp))
            
            // Email 输入
            OutlinedTextField(
                value = email,
                onValueChange = { 
                    email = it
                    errorMessage = null
                },
                label = { Text("Email") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Password 输入
            OutlinedTextField(
                value = password,
                onValueChange = { 
                    password = it
                    errorMessage = null
                },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                )
            )
            
            // 注册模式显示昵称输入
            if (!isLoginMode) {
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Nickname (optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                        cursorColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 错误消息
            errorMessage?.let {
                Text(
                    text = it,
                    color = Color.Red,
                    style = typo().bodySmall,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // 成功消息
            successMessage?.let {
                Text(
                    text = it,
                    color = Color.Green,
                    style = typo().bodySmall,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // 登录/注册按钮
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Email and password are required"
                        return@Button
                    }
                    
                    isLoading = true
                    errorMessage = null
                    successMessage = null
                    
                    scope.launch {
                        val result = if (isLoginMode) {
                            apiService.login(email, password)
                        } else {
                            apiService.register(email, password, nickname.takeIf { it.isNotBlank() })
                        }
                        
                        isLoading = false
                        
                        result.fold(
                            onSuccess = { response ->
                                if (response.error != null) {
                                    errorMessage = response.error
                                } else {
                                    successMessage = if (isLoginMode) "Login successful!" else "Registration successful!"
                                    // 清空输入
                                    email = ""
                                    password = ""
                                    nickname = ""
                                }
                            },
                            onFailure = { e ->
                                errorMessage = e.message ?: "An error occurred"
                            }
                        )
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (isLoginMode) "Login" else "Register")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 切换登录/注册模式
            TextButton(
                onClick = {
                    isLoginMode = !isLoginMode
                    errorMessage = null
                    successMessage = null
                }
            ) {
                Text(
                    text = if (isLoginMode) "Don't have an account? Register" else "Already have an account? Login",
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(200.dp))
    }
    
    // TopAppBar
    TopAppBar(
        modifier = Modifier
            .hazeEffect(state = hazeState, style = HazeMaterials.ultraThin()) {
                blurEnabled = true
            },
        title = {
            Text(
                text = if (isLoggedIn) "Account" else if (isLoginMode) "Login" else "Register",
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
