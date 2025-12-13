package com.maxrave.simpmusic.ui.screen.other

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxrave.simpmusic.api.HYMusicApiService
import com.maxrave.simpmusic.ui.theme.typo
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * 强制登录界面 - 未登录时全屏显示
 */
@Composable
fun LoginGateScreen(
    apiService: HYMusicApiService = koinInject(),
    onLoginSuccess: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    
    // UI State
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1a1a2e),
                        Color(0xFF16213e),
                        Color(0xFF0f0f23)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Logo/Title
            Text(
                text = "HYMusic",
                style = typo().displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Your Music, Your Way",
                style = typo().bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Mode Title
            Text(
                text = if (isLoginMode) "Welcome Back" else "Create Account",
                style = typo().titleLarge,
                color = Color.White,
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 注册模式显示昵称输入（必填，放在 Email 上面）
            if (!isLoginMode) {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { 
                        nickname = it
                        errorMessage = null
                    },
                    label = { Text("Nickname") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6C63FF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = Color(0xFF6C63FF),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                        cursorColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Email 输入
            OutlinedTextField(
                value = email,
                onValueChange = { 
                    email = it
                    errorMessage = null
                },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6C63FF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedLabelColor = Color(0xFF6C63FF),
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
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6C63FF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedLabelColor = Color(0xFF6C63FF),
                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                )
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 错误消息
            errorMessage?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Red.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = it,
                        color = Color.Red,
                        style = typo().bodySmall,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // 登录/注册按钮
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Email and password are required"
                        return@Button
                    }
                    
                    // 注册模式验证昵称必填
                    if (!isLoginMode && nickname.isBlank()) {
                        errorMessage = "Nickname is required"
                        return@Button
                    }
                    
                    // 验证邮箱必须是 gmail.com
                    if (!email.lowercase().endsWith("@gmail.com")) {
                        errorMessage = "Only Gmail addresses are allowed"
                        return@Button
                    }
                    
                    isLoading = true
                    errorMessage = null
                    
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
                                } else if (response.token != null) {
                                    onLoginSuccess()
                                }
                            },
                            onFailure = { e ->
                                errorMessage = e.message ?: "Connection error"
                            }
                        )
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6C63FF)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (isLoginMode) "Login" else "Create Account",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 切换登录/注册模式
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isLoginMode) "Don't have an account?" else "Already have an account?",
                    color = Color.White.copy(alpha = 0.7f),
                    style = typo().bodySmall
                )
                TextButton(
                    onClick = {
                        isLoginMode = !isLoginMode
                        errorMessage = null
                    }
                ) {
                    Text(
                        text = if (isLoginMode) "Register" else "Login",
                        color = Color(0xFF6C63FF),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
