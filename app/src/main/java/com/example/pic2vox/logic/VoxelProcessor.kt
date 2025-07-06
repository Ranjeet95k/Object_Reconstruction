package com.example.pic2vox.logic

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import com.example.pic2vox.model.ModelRunner
import com.example.pic2vox.viewer.VoxelHolder
import com.example.pic2vox.viewer.VoxelRenderActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun processVoxelReconstruction(
    context: Context,
    bitmaps: List<Bitmap>,
    onStart: () -> Unit = {},
    onFinish: () -> Unit = {},
    onError: (String) -> Unit = {},
    onComplete: (Long) -> Unit = {}
) {
    withContext(Dispatchers.IO) {
        onStart()
        val startTime = System.currentTimeMillis()

        try {
            if (bitmaps.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Please provide at least one image", Toast.LENGTH_SHORT).show()
                }
                return@withContext
            }

            val modelRunner = ModelRunner(context)
            val voxelTensor = modelRunner.runFullPipeline(bitmaps)

            val shape = voxelTensor.shape()
            Log.d("VoxelReconstruction", "Output shape: ${shape.contentToString()}")

            if (!shape.contentEquals(longArrayOf(1, 32, 32, 32))) {
                withContext(Dispatchers.Main) {
                    onError("Unexpected voxel tensor shape: ${shape.contentToString()}")
                }
                modelRunner.release()
                return@withContext
            }

            val data = voxelTensor.dataAsFloatArray
            val voxelGrid = Array(32) { Array(32) { BooleanArray(32) } }
            var index = 0
            for (x in 0 until 32)
                for (y in 0 until 32)
                    for (z in 0 until 32)
                        voxelGrid[x][y][z] = data[index++] > 0.12f

            val elapsed = System.currentTimeMillis() - startTime

            withContext(Dispatchers.Main) {
                VoxelHolder.grid = voxelGrid
                val intent = Intent(context, VoxelRenderActivity::class.java)
                intent.putExtra("processing_time", elapsed)
                context.startActivity(intent)
                onComplete(elapsed)
            }

            modelRunner.release()
        } catch (e: Exception) {
            Log.e("VoxelProcessor", "Error during voxel processing", e)
            withContext(Dispatchers.Main) {
                onError(e.localizedMessage ?: "Unknown error")
            }
        } finally {
            withContext(Dispatchers.Main) {
                onFinish()
            }
        }
    }
}
