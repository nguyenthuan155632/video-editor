package com.videoeditor.feature.compress

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.videoeditor.feature.compress.model.CompressUiState
import com.videoeditor.feature.compress.ui.ConfiguringStep
import com.videoeditor.feature.compress.ui.DoneStep
import com.videoeditor.feature.compress.ui.PickStep
import com.videoeditor.feature.compress.ui.RunningStep

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
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
    ) { uri ->
        uri?.let { viewModel.onVideoSelected(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compress Video") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.onBack()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is CompressUiState.Idle -> {
                    PickStep(
                        onPick = {
                            if (readPermission.status.isGranted) {
                                videoPicker.launch("video/*")
                            } else {
                                readPermission.launchPermissionRequest()
                            }
                        },
                    )
                }
                is CompressUiState.PickingVideo -> {
                    videoPicker.launch("video/*")
                }
                is CompressUiState.Configuring -> {
                    ConfiguringStep(
                        state = state,
                        onPickDifferent = {
                            if (readPermission.status.isGranted) {
                                videoPicker.launch("video/*")
                            } else {
                                readPermission.launchPermissionRequest()
                            }
                        },
                        onSmartPreset = viewModel::onSmartPresetSelected,
                        onSettingsChanged = viewModel::onSettingsChanged,
                        onSectionToggle = viewModel::onSectionToggle,
                        onStartEncode = viewModel::onStartEncode,
                    )
                }
                is CompressUiState.Running -> {
                    RunningStep(
                        progress = state.progress,
                        onCancel = viewModel::onCancelEncode,
                    )
                }
                is CompressUiState.Done -> {
                    DoneStep(
                        output = state.output,
                        ratio = state.ratio,
                        onDone = {
                            viewModel.onBack()
                            onBack()
                        },
                    )
                }
                is CompressUiState.Failed -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text("Error: ${state.reason}")
                        androidx.compose.material3.Button(onClick = viewModel::onDismissError) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }
    }
}