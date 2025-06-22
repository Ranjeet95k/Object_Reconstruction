package com.example.pic2vox.initialui

import android.graphics.Bitmap
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.pic2vox.camera.CameraCaptureManager
import com.example.pic2vox.camera.CameraPreviewView

@Composable
fun VoxelCaptureScreen(
    capturedImages: List<Bitmap>,
    onCapture: (CameraCaptureManager) -> Unit,
    onClear: () -> Unit,
    onReconstruct: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var cameraManager by remember { mutableStateOf<CameraCaptureManager?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("3D Capture", fontSize = 22.sp)

        CameraPreviewView(
            context = context,
            lifecycleOwner = lifecycleOwner,
            onPreviewReady = { manager, _ -> cameraManager = manager },
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .border(1.dp, Color.Gray)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            capturedImages.forEach { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .border(1.dp, Color.Gray)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    cameraManager?.let { onCapture(it) }
                }
            ) {
                Text("Capture")
            }
            Button(onClick = onClear, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                Text("Clear")
            }
            if (capturedImages.isNotEmpty()) {
                Button(onClick = onReconstruct) {
                    Text("Reconstruct")
                }
            }
        }
    }
}
