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
    private val encoder = Module.load(assetFilePath(context, "encoder_model.pt"))
    private val decoder = Module.load(assetFilePath(context, "decoder_model.pt"))
    private val merger = Module.load(assetFilePath(context, "merger_model.pt"))

    private val meanRGB = floatArrayOf(0.485f, 0.456f, 0.406f)
    private val stdRGB = floatArrayOf(0.229f, 0.224f, 0.225f)
    private val inputSize = 224

    fun runInference(bitmaps: List<Bitmap>): Tensor {
        val N = bitmaps.size.coerceAtMost(3)
        if (N == 0) throw IllegalArgumentException("No input images provided")

        val C = 3
        val H = inputSize
        val W = inputSize

        val floatData = FloatArray(N * C * H * W)
        for (i in 0 until N) {
            val bmp = bitmaps[i].scale(W, H)
            val tensor = TensorImageUtils.bitmapToFloat32Tensor(bmp, meanRGB, stdRGB)
            val data = tensor.dataAsFloatArray
            System.arraycopy(data, 0, floatData, i * C * H * W, C * H * W)
        }

        val inputTensor = Tensor.fromBlob(floatData, longArrayOf(1, N.toLong(), C.toLong(), H.toLong(), W.toLong()))
        val encoderOutput = encoder.forward(IValue.from(inputTensor)).toTensor()
        Log.d("ModelRunner", "Encoder output: ${encoderOutput.shape().contentToString()}")

        val decoderOutputTuple = decoder.forward(IValue.from(encoderOutput)).toTuple()
        val rawFeatures = decoderOutputTuple[0].toTensor()
        val coarseVolumes = decoderOutputTuple[1].toTensor()
        Log.d("ModelRunner", "Decoder rawFeatures: ${rawFeatures.shape().contentToString()}")
        Log.d("ModelRunner", "Decoder coarseVolumes: ${coarseVolumes.shape().contentToString()}")

        val finalOutput = merger.forward(
            IValue.from(rawFeatures),
            IValue.from(coarseVolumes)
        ).toTensor()

        Log.d("ModelRunner", "Merger output: ${finalOutput.shape().contentToString()}")
        return finalOutput
    }

    fun release() {
        encoder.destroy()
        decoder.destroy()
        merger.destroy()
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
