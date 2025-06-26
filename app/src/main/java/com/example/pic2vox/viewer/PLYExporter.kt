package com.example.pic2vox.viewer

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PLYExporter {
    fun exportToPLY(context: Context, voxelGrid: Array<Array<BooleanArray>>): Boolean {
        return try {
            val plyHeader = buildString {
                appendLine("ply")
                appendLine("format ascii 1.0")

                val vertexCount = voxelGrid.sumOf { plane ->
                    plane.sumOf { row -> row.count { it } }
                }
                appendLine("element vertex $vertexCount")
                appendLine("property float x")
                appendLine("property float y")
                appendLine("property float z")
                appendLine("end_header")
            }

            val plyBody = StringBuilder()
            val sizeX = voxelGrid.size
            val sizeY = voxelGrid[0].size
            val sizeZ = voxelGrid[0][0].size
            val offsetX = sizeX / 2f
            val offsetY = sizeY / 2f
            val offsetZ = sizeZ / 2f

            for (x in voxelGrid.indices) {
                for (y in voxelGrid[x].indices) {
                    for (z in voxelGrid[x][y].indices) {
                        if (voxelGrid[x][y][z]) {
                            val px = x - offsetX
                            val py = y - offsetY
                            val pz = z - offsetZ
                            plyBody.appendLine("$px $py $pz")
                        }
                    }
                }
            }

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "voxel_$timeStamp.ply"
            val file = File(downloadsDir, fileName)


            FileOutputStream(file).use { fos ->
                fos.write(plyHeader.toByteArray())
                fos.write(plyBody.toString().toByteArray())
            }
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf("application/octet-stream"),
                null
            )

            Log.d("PLYExporter", "PLY file saved to: ${file.absolutePath}")
            true

        } catch (e: Exception) {
            Log.e("PLYExporter", "Export failed: ${e.message}", e)
            false
        }

    }

}
