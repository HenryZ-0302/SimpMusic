package com.maxrave.simpmusic.ui.screen.other

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.api.HYMusicApiService
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.SettingItem
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.SettingsViewModel
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import hymusic.composeapp.generated.resources.Res
import hymusic.composeapp.generated.resources.baseline_arrow_back_ios_new_24
import hymusic.composeapp.generated.resources.enable_liquid_glass_effect
import hymusic.composeapp.generated.resources.enable_liquid_glass_effect_description

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun HiddenSettingsScreen(
    paddingValues: PaddingValues,
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel(),
    apiService: HYMusicApiService = koinInject(),
) {
    val hazeState = rememberHazeState()
    val enableLiquidGlass by viewModel.enableLiquidGlass.collectAsStateWithLifecycle()
    val currentUser by apiService.currentUser.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    
    var makeAdminStatus by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(top = 64.dp)
            .verticalScroll(rememberScrollState())
            .hazeSource(state = hazeState),
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Developer Settings",
            style = typo().titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        if (getPlatform() == Platform.Android) {
            SettingItem(
                title = stringResource(Res.string.enable_liquid_glass_effect),
                subtitle = stringResource(Res.string.enable_liquid_glass_effect_description),
                smallSubtitle = true,
                switch = (enableLiquidGlass to { viewModel.setEnableLiquidGlass(it) }),
            )
        }
        
        // 临时管理员设置按钮 (使用后删除这段代码)
        Text(
            text = "Admin Setup",
            style = typo().titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        
        SettingItem(
            title = "Make Me Admin",
            subtitle = makeAdminStatus ?: if (currentUser?.isAdmin == true) "You are already admin" else "Set current user as admin",
            onClick = {
                if (currentUser?.isAdmin == true) {
                    makeAdminStatus = "You are already admin!"
                    return@SettingItem
                }
                
                val userId = currentUser?.id ?: return@SettingItem
                makeAdminStatus = "Setting admin..."
                
                scope.launch {
                    apiService.adminSetSelfAdmin(userId).fold(
                        onSuccess = {
                            makeAdminStatus = "Success! Please re-login to apply."
                        },
                        onFailure = { e ->
                            makeAdminStatus = "Error: ${e.message}"
                        }
                    )
                }
            },
        )

        Spacer(modifier = Modifier.height(200.dp))
    }

    TopAppBar(
        modifier = Modifier
            .hazeEffect(state = hazeState, style = HazeMaterials.ultraThin()) {
                blurEnabled = true
            },
        title = {
            Text(
                text = "Hidden Settings",
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

