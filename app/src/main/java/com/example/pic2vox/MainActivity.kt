package com.example.pic2vox

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
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
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {

    private var cameraPermissionGranted by mutableStateOf(false)
    private val capturedImages = mutableStateListOf<Bitmap>()

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

        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        }

        val pickImagesLauncher = registerForActivityResult(
            ActivityResultContracts.GetMultipleContents()
        ) { uris ->
            val contentResolver = applicationContext.contentResolver
            val resizedBitmaps = uris.take(3).mapNotNull { uri ->
                try {
                    val original = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    Bitmap.createScaledBitmap(original, 224, 224, true)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            capturedImages.addAll(resizedBitmaps)
        }

        setContent {
            val context = LocalContext.current
            var showProgress by remember { mutableStateOf(false) }
            var processingTime by remember { mutableStateOf(0L) }

            Pic2voxTheme {
                if (cameraPermissionGranted) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        VoxelCaptureScreen(
                            capturedImages = capturedImages,
                            onCapture = { manager ->
                                manager.captureImage { bitmap ->
                                    capturedImages.add(bitmap)
                                }
                            },
                            onClear = {
                                capturedImages.clear()
                            },
                            onSelectGallery = {
                                pickImagesLauncher.launch("image/*")
                            },
                            onReconstruct = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    showProgress = true
                                    val startTime = System.currentTimeMillis()
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
                                        val voxelTensor = modelRunner.runFullPipeline(capturedImages)

                                        val shape = voxelTensor.shape()
                                        Log.d("VoxelReconstruction", "Final voxel tensor shape: ${shape.contentToString()}")

                                        if (shape.size != 4 || shape[0] != 1L || shape[1] != 32L || shape[2] != 32L || shape[3] != 32L) {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(
                                                    context,
                                                    "Unexpected voxel tensor shape: ${shape.contentToString()}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                            modelRunner.release()
                                            return@launch
                                        }

                                        val gridSize = 32
                                        val threshold = 0.12f
                                        val voxelData = voxelTensor.dataAsFloatArray

                                        val voxelGrid = Array(gridSize) {
                                            Array(gridSize) {
                                                BooleanArray(gridSize)
                                            }
                                        }

                                        var index = 0
                                        for (x in 0 until gridSize) {
                                            for (y in 0 until gridSize) {
                                                for (z in 0 until gridSize) {
                                                    voxelGrid[x][y][z] = voxelData[index++] > threshold
                                                }
                                            }
                                        }

                                        val endTime = System.currentTimeMillis()
                                        processingTime = endTime - startTime

                                        withContext(Dispatchers.Main) {
                                            VoxelHolder.grid = voxelGrid
                                            val intent = Intent(context, VoxelRenderActivity::class.java)
                                            intent.putExtra("processing_time", processingTime)
                                            startActivity(intent)
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

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (processingTime > 0) "Time taken: ${processingTime} ms" else "",
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                capturedImages.clear()
                                processingTime = 0L
                            },
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
