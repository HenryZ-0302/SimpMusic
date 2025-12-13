package com.maxrave.simpmusic.ui.screen.other

import androidx.compose.foundation.Image
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.maxrave.simpmusic.expect.openUrl
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.navigation.destination.home.HiddenSettingsDestination
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.utils.VersionManager
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import hymusic.composeapp.generated.resources.*

private const val HIDDEN_PASSWORD = "madebyhenry"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun CreditScreen(
    paddingValues: PaddingValues,
    navController: NavController,
) {
    val hazeState = rememberHazeState()
    
    // Feedback dialog states
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var feedbackText by remember { mutableStateOf("") }
    
    // Password dialog states
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(top = 64.dp)
                .verticalScroll(rememberScrollState())
                .hazeSource(state = hazeState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        // App icon
        Image(
            painter = painterResource(Res.drawable.app_icon),
            contentDescription = "App Icon",
            modifier =
                Modifier
                    .size(150.dp)
                    .clip(CircleShape),
        )

        Spacer(modifier = Modifier.height(30.dp))

        // App name
        Text(
            text = stringResource(Res.string.app_name),
            style = typo().titleLarge,
            fontSize = 22.sp,
        )

        // Version
        Text(
            text = stringResource(Res.string.version_format, VersionManager.getVersionName()),
            style = typo().bodySmall,
            fontSize = 13.sp,
        )

        // Developer
        Text(
            text = stringResource(Res.string.maxrave_dev),
            style = typo().bodyMedium,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // App description
        Text(
            text = stringResource(Res.string.credit_app),
            style = typo().bodyMedium,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 25.dp),
            textAlign = TextAlign.Start,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Feedback Card (hidden settings entry)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 25.dp)
                .clickable { showFeedbackDialog = true },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.1f)
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.baseline_edit_24),
                    contentDescription = "Feedback",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.size(12.dp))
                Column {
                    Text(
                        text = "Feedback",
                        style = typo().bodyMedium,
                        color = Color.White,
                    )
                    Text(
                        text = "Send us your feedback",
                        style = typo().bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            // TODO: 添加你自己的链接
            // 原作者链接已移除
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Copyright text
        Text(
            text = stringResource(Res.string.copyright),
            style = typo().bodySmall,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 25.dp, vertical = 5.dp),
            textAlign = TextAlign.Start,
        )

        // Bottom spacing
        Spacer(modifier = Modifier.height(200.dp))
    }
    
    TopAppBar(
        modifier =
            Modifier
                .hazeEffect(state = hazeState, style = HazeMaterials.ultraThin()) {
                    blurEnabled = true
                },
        title = {
            Text(
                text = stringResource(Res.string.app_name),
                style = typo().titleMedium,
                maxLines = 1,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(
                            align = Alignment.CenterVertically,
                        ).basicMarquee(
                            iterations = Int.MAX_VALUE,
                            animationMode = MarqueeAnimationMode.Immediately,
                        ).focusable(),
            )
        },
        navigationIcon = {
            Box(Modifier.padding(horizontal = 5.dp)) {
                RippleIconButton(
                    Res.drawable.baseline_arrow_back_ios_new_24,
                    Modifier
                        .size(32.dp),
                    true,
                ) {
                    navController.navigateUp()
                }
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                Color.Transparent,
                Color.Unspecified,
                Color.Unspecified,
                Color.Unspecified,
                Color.Unspecified,
            ),
    )

    // Feedback Dialog (fake feedback, actually password entry)
    if (showFeedbackDialog) {
        AlertDialog(
            onDismissRequest = { 
                showFeedbackDialog = false
                feedbackText = ""
            },
            title = { Text("Feedback") },
            text = {
                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = { 
                        feedbackText = it
                        // Check if password is entered
                        if (it == HIDDEN_PASSWORD) {
                            showFeedbackDialog = false
                            feedbackText = ""
                            showPasswordDialog = true
                        }
                    },
                    label = { Text("Enter your feedback") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
            },
            confirmButton = {
                TextButton(onClick = { 
                    showFeedbackDialog = false
                    feedbackText = ""
                }) {
                    Text("Send")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showFeedbackDialog = false
                    feedbackText = ""
                }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Password confirmation dialog (double check)
    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { 
                showPasswordDialog = false
                passwordInput = ""
                passwordError = false
            },
            title = { Text("Developer Access") },
            text = {
                Column {
                    Text("Enter password to continue")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { 
                            passwordInput = it
                            passwordError = false
                        },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = passwordError,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (passwordError) {
                        Text(
                            text = "Wrong password",
                            color = Color.Red,
                            style = typo().bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    if (passwordInput == HIDDEN_PASSWORD) {
                        showPasswordDialog = false
                        passwordInput = ""
                        navController.navigate(HiddenSettingsDestination)
                    } else {
                        passwordError = true
                    }
                }) {
                    Text("Enter")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showPasswordDialog = false
                    passwordInput = ""
                    passwordError = false
                }) {
                    Text("Cancel")
                }
            },
        )
    }
}