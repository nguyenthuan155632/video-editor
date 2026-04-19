package com.videoeditor.feature.compress

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.videoeditor.core.designsys.AuroraBackground
import com.videoeditor.core.designsys.StepIndicator
import com.videoeditor.core.theme.AuroraMotion
import com.videoeditor.core.theme.AuroraTextPrimary
import com.videoeditor.core.theme.glass
import com.videoeditor.feature.compress.model.CompressUiState
import com.videoeditor.feature.compress.ui.ConfiguringStep
import com.videoeditor.feature.compress.ui.DoneStep
import com.videoeditor.feature.compress.ui.FailedStep
import com.videoeditor.feature.compress.ui.PickStep
import com.videoeditor.feature.compress.ui.RunningStep

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CompressScreen(
    onBack: () -> Unit,
    viewModel: CompressViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    val readPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        rememberPermissionState(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri -> uri?.let { viewModel.onVideoSelected(it) } }

    val pickAction: () -> Unit = {
        if (readPermission.status.isGranted) {
            videoPicker.launch("video/*")
        } else {
            readPermission.launchPermissionRequest()
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is CompressUiState.PickingVideo) {
            pickAction()
        }
    }

    val activeIndex = when (uiState) {
        is CompressUiState.Idle, is CompressUiState.PickingVideo -> 0
        is CompressUiState.Configuring -> 1
        is CompressUiState.Running -> 2
        is CompressUiState.Done -> 3
        is CompressUiState.Failed -> 1
    }
    val runningPhase = uiState is CompressUiState.Running

    AuroraBackground(static = runningPhase) {
        Column(modifier = Modifier.fillMaxSize()) {
            CompressTopBar(
                activeIndex = activeIndex,
                onBack = {
                    viewModel.onBack()
                    onBack()
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = uiState,
                    contentKey = { state -> state::class },
                    label = "compress-step",
                    transitionSpec = {
                        (slideInHorizontally(
                            tween(AuroraMotion.DURATION_MEDIUM_MS, easing = AuroraMotion.auroraEaseOut),
                        ) { it / 6 } + fadeIn(tween(AuroraMotion.DURATION_MEDIUM_MS))) togetherWith
                            (slideOutHorizontally(
                                tween(AuroraMotion.DURATION_MEDIUM_MS, easing = AuroraMotion.auroraEaseOut),
                            ) { -it / 6 } + fadeOut(tween(AuroraMotion.DURATION_MEDIUM_MS / 2)))
                    },
                ) { state ->
                    when (state) {
                        is CompressUiState.Idle,
                        is CompressUiState.PickingVideo,
                        -> PickStep(onPick = pickAction)
                        is CompressUiState.Configuring -> ConfiguringStep(
                            state = state,
                            onPickDifferent = pickAction,
                            onSmartPreset = viewModel::onSmartPresetSelected,
                            onSettingsChanged = viewModel::onSettingsChanged,
                            onSectionToggle = viewModel::onSectionToggle,
                            onStartEncode = viewModel::onStartEncode,
                        )
                        is CompressUiState.Running -> RunningStep(
                            progress = state.progress,
                            onCancel = viewModel::onCancelEncode,
                        )
                        is CompressUiState.Done -> DoneStep(
                            output = state.output,
                            ratio = state.ratio,
                        )
                        is CompressUiState.Failed -> FailedStep(
                            reason = state.reason,
                            onDismiss = viewModel::onDismissError,
                            onRetry = viewModel::onDismissError,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompressTopBar(activeIndex: Int, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .glass(cornerRadius = 20.dp, surfaceAlpha = 0.45f),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AuroraTextPrimary,
                )
            }
        }
        StepIndicator(steps = CompressSteps.ALL, activeIndex = activeIndex)
        Box(modifier = Modifier.size(40.dp))
    }
}
