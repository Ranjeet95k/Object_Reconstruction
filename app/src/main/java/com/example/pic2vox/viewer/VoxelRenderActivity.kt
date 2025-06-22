package com.example.pic2vox.viewer

import android.app.Activity
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout

class VoxelRenderActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val voxelGrid = VoxelHolder.grid
        if (voxelGrid == null) {
            finish()
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

        val frameLayout = FrameLayout(this)
        frameLayout.addView(glView)

        val resetButton = Button(this).apply {
            text = "🔄 Reset View"
            setOnClickListener {
                renderer.angleX = 0f
                renderer.angleY = 0f
                renderer.zoom = 1f
                glView.requestRender()
            }
        }

        val buttonLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 48)
            addView(resetButton)
        }

        val buttonParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        }

        frameLayout.addView(buttonLayout, buttonParams)
        setContentView(frameLayout)
    }
}
