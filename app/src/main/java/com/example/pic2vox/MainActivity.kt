package com.example.pic2vox

import android.Manifest
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.pic2vox.splashscreen.SplashScreen
import com.example.pic2vox.initialui.MainScreen
import com.example.pic2vox.ui.theme.Pic2voxTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private var cameraPermissionGranted by mutableStateOf(false)
    private val capturedImages = mutableStateListOf<Bitmap>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                cameraPermissionGranted = granted
            }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionGranted = true
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        }

        val pickImagesLauncher = registerForActivityResult(
            ActivityResultContracts.GetMultipleContents()
        ) { uris ->
            val contentResolver = applicationContext.contentResolver
            val resized = uris.take(3).mapNotNull {
                try {
                    val original = MediaStore.Images.Media.getBitmap(contentResolver, it)
                    Bitmap.createScaledBitmap(original, 224, 224, true)
                } catch (e: Exception) {
                    e.printStackTrace(); null
                }
            }
            capturedImages.addAll(resized)
        }

        setContent {
            val context = LocalContext.current
            var showSplash by remember { mutableStateOf(true) }
            var darkTheme by remember { mutableStateOf(false) }
            var processingTime by remember { mutableStateOf(0L) }

            LaunchedEffect(Unit) {
                delay(2000)
                showSplash = false
            }

            Pic2voxTheme(darkTheme = darkTheme) {
                if (showSplash) {
                    SplashScreen()
                } else {
                    if (cameraPermissionGranted) {
                        MainScreen(
                            capturedImages = capturedImages,
                            onCapture = { bitmap -> capturedImages.add(bitmap) },
                            onClear = { capturedImages.clear() },
                            onGallerySelect = { pickImagesLauncher.launch("image/*") },
                            onReset = {
                                capturedImages.clear()
                                processingTime = 0L
                            },
                            processingTime = processingTime,
                            onUpdateTime = { processingTime = it },
                            darkTheme = darkTheme,
                            toggleTheme = { darkTheme = !darkTheme }
                        )
                    } else {
                        Toast.makeText(context, "Camera permission denied", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
