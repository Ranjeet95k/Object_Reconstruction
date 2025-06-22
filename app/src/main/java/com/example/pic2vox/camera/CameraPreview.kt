package com.example.pic2vox.camera

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner

@Composable
fun CameraPreviewView(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    onPreviewReady: (CameraCaptureManager, PreviewView) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val manager = CameraCaptureManager(context = ctx, lifecycleOwner = lifecycleOwner)
            manager.setupCamera(previewView)
            onPreviewReady(manager, previewView)
            previewView
        },
        modifier = modifier
    )
}
