package com.example.pic2vox.initialui

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.pic2vox.components.TopBarWithThemeToggle
import com.example.pic2vox.logic.processVoxelReconstruction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    capturedImages: List<Bitmap>,
    onCapture: (Bitmap) -> Unit,
    onClear: () -> Unit,
    onGallerySelect: () -> Unit,
    onReset: () -> Unit,
    processingTime: Long,
    onUpdateTime: (Long) -> Unit,
    darkTheme: Boolean,
    toggleTheme: () -> Unit
) {
    val context = LocalContext.current
    var showProgress by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopBarWithThemeToggle(
                appName = "Object Recons",
                darkTheme = darkTheme,
                onToggleTheme = toggleTheme
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                VoxelCaptureScreen(
                    capturedImages = capturedImages,
                    onCapture = { manager -> manager.captureImage(onCapture) },
                    onClear = onClear,
                    onSelectGallery = onGallerySelect,
                    onReconstruct = {
                        val handler = android.os.Handler(android.os.Looper.getMainLooper())

                        CoroutineScope(Dispatchers.IO).launch {
                            handler.post { showProgress = true }

                            processVoxelReconstruction(
                                context = context,
                                bitmaps = capturedImages,
                                onStart = { /* no-op */ },
                                onFinish = { handler.post { showProgress = false } },
                                onError = {
                                    handler.post {
                                        Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                                        showProgress = false
                                    }
                                },
                                onComplete = { time ->
                                    handler.post {
                                        onUpdateTime(time)
                                        showProgress = false
                                    }
                                }
                            )
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (processingTime > 0) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = "🕒 Time taken: $processingTime ms",
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onReset,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Reset")
                }
            }

            if (showProgress) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
