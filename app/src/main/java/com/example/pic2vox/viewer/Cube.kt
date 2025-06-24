// Cube.kt
package com.example.pic2vox.viewer

import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import android.util.Log

class Cube(x: Float, y: Float, z: Float, size: Float = 1.0f, val color: FloatArray = floatArrayOf(0f, 0.6f, 1f, 1f)) {
    private val vertexBuffer: FloatBuffer
    private val drawListBuffer: ShortBuffer

    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private val vertexShaderCode = """
        uniform mat4 uMVPMatrix;
        attribute vec4 vPosition;
        void main() {
            gl_Position = uMVPMatrix * vPosition;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        uniform vec4 vColor;
        void main() {
            gl_FragColor = vColor;
        }
    """.trimIndent()

    private val program: Int

    init {
        val half = size / 2f

        val cubeCoords = floatArrayOf(
            -half,  half,  half,
            half,  half,  half,
            -half, -half,  half,
            half, -half,  half,
            -half,  half, -half,
            half,  half, -half,
            -half, -half, -half,
            half, -half, -half
        )

        val drawOrder = shortArrayOf(
            0, 1, 2, 1, 3, 2,
            4, 5, 6, 5, 7, 6,
            4, 0, 6, 0, 2, 6,
            1, 5, 3, 5, 7, 3,
            4, 5, 0, 5, 1, 0,
            2, 3, 6, 3, 7, 6
        )

        vertexBuffer = ByteBuffer.allocateDirect(cubeCoords.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(cubeCoords)
                position(0)
            }

        drawListBuffer = ByteBuffer.allocateDirect(drawOrder.size * 2)
            .order(ByteOrder.nativeOrder()).asShortBuffer().apply {
                put(drawOrder)
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

        val colorHandle = GLES20.glGetUniformLocation(program, "vColor")
        GLES20.glUniform4fv(colorHandle, 1, color, 0)

        val mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawListBuffer.capacity(), GLES20.GL_UNSIGNED_SHORT, drawListBuffer)
        GLES20.glDisableVertexAttribArray(positionHandle)
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
