
// VoxelGLSurfaceView.kt
package com.example.pic2vox.viewer

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Log

class VoxelGLSurfaceView : GLSurfaceView {

    private lateinit var renderer: VoxelRenderer

    constructor(context: Context, grid: Array<Array<BooleanArray>>) : super(context) {
        initWithGrid(grid)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        val fallbackGrid = VoxelHolder.grid ?: run {
            Log.w("VoxelGLSurfaceView", "VoxelHolder.grid is null. Using empty fallback grid.")
            Array(1) { Array(1) { BooleanArray(1) } }
        }
        initWithGrid(fallbackGrid)
    }

    private fun initWithGrid(grid: Array<Array<BooleanArray>>) {
        setEGLContextClientVersion(2)
        renderer = VoxelRenderer(grid)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun requestRedraw() {
        requestRender()
    }

    fun getRenderer(): VoxelRenderer = renderer
}
