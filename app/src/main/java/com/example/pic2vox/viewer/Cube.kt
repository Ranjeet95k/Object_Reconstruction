// Cube.kt
package com.example.pic2vox.viewer

import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import android.util.Log

class Cube(x: Float, y: Float, z: Float, size: Float = 1.0f) {
    private val vertexBuffer: FloatBuffer
    private val drawListBuffer: ShortBuffer
    private val colorBuffer: FloatBuffer

    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private val vertexShaderCode = """
        uniform mat4 uMVPMatrix;
        attribute vec4 vPosition;
        attribute vec4 aColor;
        varying vec4 vColor;
        void main() {
            gl_Position = uMVPMatrix * vPosition;
            vColor = aColor;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        varying vec4 vColor;
        void main() {
            gl_FragColor = vColor;
        }
    """.trimIndent()

    private val program: Int

    init {
        val half = size / 2f

        val cubeCoords = floatArrayOf(
            -half,  half,  half,   // front top left     0
            half,  half,  half,   // front top right    1
            -half, -half,  half,   // front bottom left  2
            half, -half,  half,   // front bottom right 3
            -half,  half, -half,   // back top left      4
            half,  half, -half,   // back top right     5
            -half, -half, -half,   // back bottom left   6
            half, -half, -half    // back bottom right  7
        )

        val faceColors = arrayOf(
            floatArrayOf(1f, 0.5f, 0.5f, 1f), // front - red
            floatArrayOf(0.5f, 1f, 0.5f, 1f), // back - green
            floatArrayOf(0.5f, 0.5f, 1f, 1f), // left - blue
            floatArrayOf(1f, 1f, 0.5f, 1f),   // right - yellow
            floatArrayOf(1f, 0.5f, 1f, 1f),   // top - magenta
            floatArrayOf(0.5f, 1f, 1f, 1f)    // bottom - cyan
        )

        val faceIndices = arrayOf(
            shortArrayOf(0, 1, 2, 1, 3, 2),     // front
            shortArrayOf(4, 5, 6, 5, 7, 6),     // back
            shortArrayOf(4, 0, 6, 0, 2, 6),     // left
            shortArrayOf(1, 5, 3, 5, 7, 3),     // right
            shortArrayOf(4, 5, 0, 5, 1, 0),     // top
            shortArrayOf(2, 3, 6, 3, 7, 6)      // bottom
        )

        val colors = mutableListOf<Float>()
        val drawOrder = mutableListOf<Short>()

        for (i in faceIndices.indices) {
            val color = faceColors[i]
            for (j in faceIndices[i]) {
                drawOrder.add(j)
                colors.addAll(color.toList())
            }
        }

        vertexBuffer = ByteBuffer.allocateDirect(cubeCoords.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(cubeCoords)
                position(0)
            }

        drawListBuffer = ByteBuffer.allocateDirect(drawOrder.size * 2)
            .order(ByteOrder.nativeOrder()).asShortBuffer().apply {
                drawOrder.forEach { put(it) }
                position(0)
            }

        colorBuffer = ByteBuffer.allocateDirect(colors.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                colors.forEach { put(it) }
                position(0)
            }

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(it, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                Log.e("Cube", "Program linking failed:\n" + GLES20.glGetProgramInfoLog(it))
                GLES20.glDeleteProgram(it)
            }
        }
    }

    fun draw(vpMatrix: FloatArray) {
        GLES20.glUseProgram(program)
        val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 3 * 4, vertexBuffer)

        val colorHandle = GLES20.glGetAttribLocation(program, "aColor")
        GLES20.glEnableVertexAttribArray(colorHandle)
        GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, 4 * 4, colorBuffer)

        val mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawListBuffer.capacity(), GLES20.GL_UNSIGNED_SHORT, drawListBuffer)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(colorHandle)
    }

    private fun loadShader(type: Int, code: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, code)
            GLES20.glCompileShader(shader)
            val compileStatus = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
            if (compileStatus[0] == 0) {
                Log.e("Cube", "Shader compilation failed:\n" + GLES20.glGetShaderInfoLog(shader))
                GLES20.glDeleteShader(shader)
            }
        }
    }
}
