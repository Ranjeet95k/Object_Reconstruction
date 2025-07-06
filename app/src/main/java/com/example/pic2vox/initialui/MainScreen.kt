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
                    CoroutineScope(Dispatchers.IO).launch {
                        processVoxelReconstruction(
                            context = context,
                            bitmaps = capturedImages,
                            onStart = { showProgress = true },
                            onFinish = { showProgress = false },
                            onError = {
                                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                            },
                            onComplete = { onUpdateTime(it) }
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (processingTime > 0) "Time taken: ${processingTime} ms" else "",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = MaterialTheme.colorScheme.onBackground
            )

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
