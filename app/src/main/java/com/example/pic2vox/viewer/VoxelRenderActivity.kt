
// File: VoxelRenderActivity.kt
package com.example.pic2vox.viewer

import android.app.Activity
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.MotionEvent
import android.view.ScaleGestureDetector

class VoxelRenderActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val voxelGrid = VoxelHolder.grid
        if (voxelGrid == null) {
            finish() // or show error
            return
        }
        val renderer = VoxelRenderer(voxelGrid)
        val glView = object : GLSurfaceView(this) {
            private val scaleDetector = ScaleGestureDetector(context, renderer.scaleListener)
            private var previousX = 0f
            private var previousY = 0f

            override fun onTouchEvent(event: MotionEvent): Boolean {
                scaleDetector.onTouchEvent(event)

                if (event.pointerCount == 1 && event.action == MotionEvent.ACTION_MOVE) {
                    val dx = event.x - previousX
                    val dy = event.y - previousY
                    renderer.angleX += dy * 0.5f
                    renderer.angleY += dx * 0.5f
                    requestRender()
                }

                previousX = event.x
                previousY = event.y

                performClick()

                return true
            }

            override fun performClick(): Boolean {
                super.performClick()
                return true
            }

        }


        glView.setEGLContextClientVersion(2)
        glView.setRenderer(renderer)
        glView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

        setContentView(glView)
    }

}
