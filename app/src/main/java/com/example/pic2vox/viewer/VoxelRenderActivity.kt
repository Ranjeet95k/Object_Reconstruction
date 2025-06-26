package com.example.pic2vox.viewer

import android.annotation.SuppressLint
import android.app.Activity
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.*
import android.widget.LinearLayout.LayoutParams
import android.widget.Toast

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

        // Time taken text
        val timeTaken = intent.getLongExtra("processing_time", 0L)
        val timeTextView = TextView(this).apply {
            text = "Time taken: ${(timeTaken / 1000f)} s"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 16f
        }

        val timeParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = 130  // Increased to move above buttons
        }
        layout.addView(timeTextView, timeParams)

        // Horizontal button layout
        val buttonLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val resetButton = Button(this).apply {
            text = "Reset View"
            setOnClickListener {
                renderer.angleX = 0f
                renderer.angleY = 0f
                renderer.zoom = 100f
                glView.requestRender()
            }
        }

        val downloadButton = Button(this).apply {
            text = "Download .ply"
            setOnClickListener {
                val success = PLYExporter.exportToPLY(this@VoxelRenderActivity, voxelGrid)
                Toast.makeText(
                    this@VoxelRenderActivity,
                    if (success) "PLY file saved!" else "Export failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Add buttons with spacing
        val buttonSpacing = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(20, 0, 20, 0) // Horizontal spacing
        }

        buttonLayout.addView(resetButton, buttonSpacing)
        buttonLayout.addView(downloadButton, buttonSpacing)

        val frameParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = 40  // Leave room for time text above
        }

        layout.addView(buttonLayout, frameParams)

        setContentView(layout)
    }
}
