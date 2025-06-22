// File: VoxelRenderer.kt
package com.example.pic2vox.viewer

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.ScaleGestureDetector
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

data class VoxelCube(val cube: Cube, val x: Int, val y: Int, val z: Int)

class VoxelRenderer(private val grid: Array<Array<BooleanArray>>) : GLSurfaceView.Renderer {

    enum class SliceMode { FULL, XY, XZ, YZ }

    var angleX = 0f
    var angleY = 0f
    var zoom = 100f
    var showGrid = true
    var sliceMode = SliceMode.FULL
    var sliceIndex = 16

    val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            zoom /= detector.scaleFactor
            zoom = zoom.coerceIn(30f, 300f)
            return true
        }
    }

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)

    private val allCubes = mutableListOf<VoxelCube>()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.15f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)

        val sizeX = grid.size
        val sizeY = grid[0].size
        val sizeZ = grid[0][0].size

        val offsetX = sizeX / 2f
        val offsetY = sizeY / 2f
        val offsetZ = sizeZ / 2f

        for (x in grid.indices) {
            for (y in grid[x].indices) {
                for (z in grid[x][y].indices) {
                    if (grid[x][y][z]) {
                        val cube = Cube(
                            x - offsetX, y - offsetY, z - offsetZ,
                            size = 1.0f, gridSize = sizeZ
                        )
                        allCubes.add(VoxelCube(cube, x, y, z))
                    }
                }
            }
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 500f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, zoom, 0f, 0f, 0f, 0f, 1f, 0f)

        val rotationX = FloatArray(16)
        val rotationY = FloatArray(16)
        val rotationMatrix = FloatArray(16)

        Matrix.setRotateM(rotationX, 0, angleX, 1f, 0f, 0f)
        Matrix.setRotateM(rotationY, 0, angleY, 0f, 1f, 0f)
        Matrix.multiplyMM(rotationMatrix, 0, rotationX, 0, rotationY, 0)

        val tempMatrix = FloatArray(16)
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, rotationMatrix, 0)
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)

        val filteredCubes = when (sliceMode) {
            SliceMode.FULL -> allCubes
            SliceMode.XY -> allCubes.filter { it.z == sliceIndex }
            SliceMode.XZ -> allCubes.filter { it.y == sliceIndex }
            SliceMode.YZ -> allCubes.filter { it.x == sliceIndex }
        }

        for (voxel in filteredCubes) {
            if (showGrid) voxel.cube.draw(vpMatrix)
        }
    }

    fun resetView() {
        angleX = 0f
        angleY = 0f
        zoom = 100f
    }

    fun toggleGrid() {
        showGrid = !showGrid
    }

    fun nextSliceMode() {
        sliceMode = when (sliceMode) {
            SliceMode.FULL -> SliceMode.XY
            SliceMode.XY -> SliceMode.XZ
            SliceMode.XZ -> SliceMode.YZ
            SliceMode.YZ -> SliceMode.FULL
        }
    }
}
