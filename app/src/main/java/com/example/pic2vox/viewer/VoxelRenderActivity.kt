package com.example.pic2vox.viewer

import android.annotation.SuppressLint
import android.app.Activity
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView

class VoxelRenderActivity : Activity() {
    @SuppressLint("SetTextI18n")
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

        val layout = FrameLayout(this)
        layout.addView(glView)

        val resetButton = Button(this).apply {
            text = "Reset View"
            setOnClickListener {
                renderer.angleX = 0f
                renderer.angleY = 0f
                renderer.zoom = 100f
                glView.requestRender()
            }
        }

        val buttonParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            topMargin = 24
            rightMargin = 24
        }

        layout.addView(resetButton, buttonParams)

        val timeTaken = intent.getLongExtra("processing_time", 0L)
        val timeTextView = TextView(this).apply {
            text = "Time taken: ${(timeTaken/1000).toFloat()} s"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 16f
        }

        val timeParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = 24
        }

        layout.addView(timeTextView, timeParams)

        setContentView(layout)
    }
}
