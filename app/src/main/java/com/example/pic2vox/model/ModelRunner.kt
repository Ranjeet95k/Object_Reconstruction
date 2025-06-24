package com.example.pic2vox.model

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.scale
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import java.io.FileOutputStream

class ModelRunner(context: Context) {
    private val encoder = Module.load(assetFilePath(context, "multi_encoder_model_1.pt"))
    private val decoder = Module.load(assetFilePath(context, "decoder_model_1.pt"))
    private val merger = Module.load(assetFilePath(context, "merger_model_1.pt"))

    private val meanRGB = floatArrayOf(0.485f, 0.456f, 0.406f)
    private val stdRGB = floatArrayOf(0.229f, 0.224f, 0.225f)
    private val inputSize = 224
    fun runFullPipeline(bitmaps: List<Bitmap>): Tensor {
        val N = bitmaps.size.coerceAtMost(3)
        require(N > 0) { "At least one bitmap required." }

        val C = 3
        val H = inputSize
        val W = inputSize
        val floatData = FloatArray(N * C * H * W)

        // Preprocess and stack input images
        for (i in 0 until N) {
            val bmp = bitmaps[i].scale(W, H)
            val tensor = TensorImageUtils.bitmapToFloat32Tensor(bmp, meanRGB, stdRGB)
            val data = tensor.dataAsFloatArray
            System.arraycopy(data, 0, floatData, i * C * H * W, C * H * W)
        }

        val inputTensor = Tensor.fromBlob(
            floatData,
            longArrayOf(1, N.toLong(), C.toLong(), H.toLong(), W.toLong())  // Shape: [1, N, 3, 224, 224]
        )

        // Run encoder
        val encoderOutput = encoder.forward(IValue.from(inputTensor)).toTensor()
        Log.d("ModelRunner", "Encoder output shape: ${encoderOutput.shape().contentToString()}")

        // Run decoder
        val outputs = decoder.forward(IValue.from(encoderOutput)).toTuple()
        val raw = outputs[0].toTensor()
        val coarse = outputs[1].toTensor()
        Log.d("ModelRunner", "Decoder raw: ${raw.shape().contentToString()}")
        Log.d("ModelRunner", "Decoder coarse: ${coarse.shape().contentToString()}")

        // Run merger
        val merged = merger.forward(IValue.from(raw), IValue.from(coarse)).toTensor()
        Log.d("ModelRunner", "Merger output: ${merged.shape().contentToString()}")  // Should be [1, 32, 32, 32]

        return merged
    }

    fun release() {
        encoder.destroy()
//        decoder.destroy()
//        merger.destroy()
    }

    private fun assetFilePath(context: Context, assetName: String): String {
        val file = File(context.filesDir, assetName)
        if (file.exists() && file.length() > 0) return file.absolutePath

        context.assets.open(assetName).use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return file.absolutePath
    }
}
