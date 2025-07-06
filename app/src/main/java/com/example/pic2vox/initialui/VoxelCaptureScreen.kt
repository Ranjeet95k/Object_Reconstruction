package com.example.pic2vox.initialui

import android.graphics.Bitmap
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.pic2vox.camera.CameraCaptureManager
import com.example.pic2vox.camera.CameraPreviewView

@Composable
fun VoxelCaptureScreen(
    capturedImages: List<Bitmap>,
    onCapture: (CameraCaptureManager) -> Unit,
    onClear: () -> Unit,
    onReconstruct: () -> Unit,
    onSelectGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraManager by remember { mutableStateOf<CameraCaptureManager?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        CameraPreviewView(
            context = context,
            lifecycleOwner = lifecycleOwner,
            onPreviewReady = { manager, _ -> cameraManager = manager },
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .border(1.dp, Color.Gray)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (capturedImages.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                capturedImages.forEach { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(90.dp)
                            .border(1.dp, Color.Gray)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FloatingActionButton(
                onClick = { cameraManager?.let { onCapture(it) } },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Text("📷")
            }

            FloatingActionButton(
                onClick = onSelectGallery,
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Text("🖼️")
            }

            FloatingActionButton(
                onClick = onClear,
                containerColor = MaterialTheme.colorScheme.error
            ) {
                Text("🗑️")
            }
        }

        if (capturedImages.isNotEmpty()) {
            Button(
                onClick = onReconstruct,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("🔄 Reconstruct")
            }
        }
    }
}
