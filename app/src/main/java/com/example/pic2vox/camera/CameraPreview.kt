package com.example.pic2vox.camera

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner

@SuppressLint("ClickableViewAccessibility")
@Composable
fun CameraPreviewView(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    onPreviewReady: (CameraCaptureManager, PreviewView) -> Unit,
    modifier: Modifier = Modifier
) {
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            outlineProvider = null
            clipToOutline = true
        }
    }

    val cameraManagerState = remember { mutableStateOf<CameraCaptureManager?>(null) }

    AndroidView(
        factory = {
            val manager = CameraCaptureManager(context = context, lifecycleOwner = lifecycleOwner)
            manager.setupCamera(previewView)
            cameraManagerState.value = manager
            onPreviewReady(manager, previewView)

            // Gesture: Pinch to Zoom
            val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val camera = manager.camera ?: return true
                    val zoomState = camera.cameraInfo.zoomState.value ?: return true
                    val newZoom = zoomState.zoomRatio * detector.scaleFactor
                    camera.cameraControl.setZoomRatio(
                        newZoom.coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
                    )
                    return true
                }
            })

            // Gesture: Tap to Focus
            previewView.setOnTouchListener { _, event ->
                scaleDetector.onTouchEvent(event)
                if (event.pointerCount == 1 && event.action == MotionEvent.ACTION_UP) {
                    val point = previewView.meteringPointFactory.createPoint(event.x, event.y)
                    val action = FocusMeteringAction.Builder(point).build()
                    manager.camera?.cameraControl?.startFocusAndMetering(action)
                }
                true
            }

            previewView
        },
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp)
            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
    )
}
