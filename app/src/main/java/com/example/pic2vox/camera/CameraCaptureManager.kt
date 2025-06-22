package com.example.pic2vox.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import androidx.core.graphics.scale

class CameraCaptureManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private val cameraExecutor: Executor = ContextCompat.getMainExecutor(context)

    fun setupCamera(previewView: PreviewView): ImageCapture {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val imageCaptureBuilder = ImageCapture.Builder()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        val imageCapture = imageCaptureBuilder.build()
        this.imageCapture = imageCapture

        outputDirectory = getOutputDirectory()

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e("CameraCaptureManager", "Camera binding failed", exc)
            }
        }, cameraExecutor)

        return imageCapture
    }

    fun captureImage(onImageCaptured: (Bitmap) -> Unit) {
        val imageCapture = imageCapture ?: return
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val uri = Uri.fromFile(photoFile)
                    val originalBitmap = BitmapFactory.decodeFile(uri.path)
                    val resizedBitmap = originalBitmap.downscaleIfTooLarge()
                    onImageCaptured(resizedBitmap)
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraCaptureManager", "Photo capture failed: ${exc.message}", exc)
                }
            }
        )
    }

    private fun getOutputDirectory(): File {
        val mediaDir = context.getExternalFilesDir(null)?.let {
            File(it, "pic2vox").apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else context.filesDir
    }

    fun Bitmap.downscaleIfTooLarge(maxSize: Int = 2048): Bitmap {
        if (width <= maxSize && height <= maxSize) return this
        val scale = maxSize.toFloat() / maxOf(width, height)
        return this.scale((width * scale).toInt(), (height * scale).toInt())
    }


}
