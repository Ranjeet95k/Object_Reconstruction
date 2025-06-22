package com.example.pic2vox

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.pic2vox.initialui.VoxelCaptureScreen
import com.example.pic2vox.model.ModelRunner
import com.example.pic2vox.ui.theme.Pic2voxTheme
import com.example.pic2vox.viewer.VoxelRenderActivity
import com.example.pic2vox.viewer.VoxelHolder


//import com.example.pic2vox.viewer.VoxelRenderActivity
//import com.example.pic2vox.viewer.VoxelSliceViewer
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {

    private var cameraPermissionGranted by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                cameraPermissionGranted = isGranted
            }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionGranted = true
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            val context = LocalContext.current
            var capturedImages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
            var showProgress by remember { mutableStateOf(false) }

            Pic2voxTheme {
                if (cameraPermissionGranted) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        VoxelCaptureScreen(
                            capturedImages = capturedImages,
                            onCapture = { manager ->
                                manager.captureImage { bitmap ->
                                    capturedImages = capturedImages + bitmap
                                }
                            },
                            onClear = {
                                capturedImages = emptyList()
                            },
                            onReconstruct = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    showProgress = true
                                    try {
                                        if (capturedImages.isEmpty()) {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(
                                                    context,
                                                    "Capture at least one image first",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            return@launch
                                        }

                                        val modelRunner = ModelRunner(context)
                                        val voxelTensor = modelRunner.runInference(capturedImages)
                                        val shape = voxelTensor.shape()
                                        Log.d("VoxelOutput", "Voxel shape: ${shape.contentToString()}")

                                        val voxelData = voxelTensor.dataAsFloatArray
                                        val gridSize = 32
                                        val threshold = 0.4f

                                        val voxelGrid = Array(gridSize) { Array(gridSize) { BooleanArray(gridSize) } }
                                        var index = 0
                                        for (x in 0 until gridSize) {
                                            for (y in 0 until gridSize) {
                                                for (z in 0 until gridSize) {
                                                    voxelGrid[x][y][z] = voxelData[index++] > threshold
                                                }
                                            }
                                        }

                                        withContext(Dispatchers.Main) {
//                                            setContent {
//                                                Pic2voxTheme {
//                                                    VoxelSliceViewer(voxelGrid = voxelGrid)
//                                                }
//                                            }

                                            VoxelHolder.grid = voxelGrid
                                            val intent = Intent(context, VoxelRenderActivity::class.java)
                                            startActivity(intent)


//                                            VoxelHolder.grid = voxelGrid
//                                            val intent = Intent(context, VoxelRenderActivity::class.java)
//                                            startActivity(intent)
                                        }



                                        modelRunner.release()
                                    } catch (e: Exception) {
                                        Log.e("ModelRunner", "Inference failed", e)
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                "Error: ${e.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    } finally {
                                        withContext(Dispatchers.Main) {
                                            showProgress = false
                                        }
                                    }
                                }
                            }
                        )

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
                } else {
                    PermissionDeniedScreen()
                }
            }
        }
    }
}

@Composable
fun PermissionDeniedScreen() {
    Text(
        text = "Camera permission is required to use this feature.",
        modifier = Modifier.padding(16.dp)
    )
}


private fun renderVoxelGrid(voxelGrid: Array<Array<BooleanArray>>) {
    // For now, log the number of active voxels
    var count = 0
    for (x in voxelGrid.indices) {
        for (y in voxelGrid[x].indices) {
            for (z in voxelGrid[x][y].indices) {
                if (voxelGrid[x][y][z]) count++
            }
        }
    }
    Log.d("VoxelRender", "Active voxels: $count")
}

