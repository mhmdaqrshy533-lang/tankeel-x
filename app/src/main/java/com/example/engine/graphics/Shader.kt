package com.example.engine.graphics

import android.opengl.GLES30
import android.util.Log

class Shader(vertexSource: String, fragmentSource: String) {
    var programId: Int = 0
        private set

    init {
        var vShader = compileShader(GLES30.GL_VERTEX_SHADER, vertexSource)
        var fShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource)

        if (vShader == 0 || fShader == 0) {
            Log.e("Shader", "Original shader compile failed. Falling back.")
            vShader = compileShader(GLES30.GL_VERTEX_SHADER, fallbackVertex)
            fShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fallbackFragment)
        }

        programId = GLES30.glCreateProgram()
        GLES30.glAttachShader(programId, vShader)
        GLES30.glAttachShader(programId, fShader)
        GLES30.glLinkProgram(programId)

        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            Log.e("Shader", "Error linking shader: " + GLES30.glGetProgramInfoLog(programId))
            GLES30.glDeleteProgram(programId)
            
            // Ultimate fallback
            vShader = compileShader(GLES30.GL_VERTEX_SHADER, fallbackVertex)
            fShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fallbackFragment)
            programId = GLES30.glCreateProgram()
            GLES30.glAttachShader(programId, vShader)
            GLES30.glAttachShader(programId, fShader)
            GLES30.glLinkProgram(programId)
        }
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)
        val compileStatus = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            Log.e("Shader", "Error compiling shader type $type: " + GLES30.glGetShaderInfoLog(shader))
            GLES30.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    fun use() {
        GLES30.glUseProgram(programId)
    }

    fun setUniformMatrix4fv(name: String, matrix: FloatArray) {
        val location = GLES30.glGetUniformLocation(programId, name)
        if (location != -1) {
            var isNaN = false
            for (v in matrix) {
                if (v.isNaN()) isNaN = true
            }
            if (isNaN) {
                val identity = floatArrayOf(
                    1f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f,
                    0f, 0f, 1f, 0f,
                    0f, 0f, 0f, 1f
                )
                GLES30.glUniformMatrix4fv(location, 1, false, identity, 0)
            } else {
                GLES30.glUniformMatrix4fv(location, 1, false, matrix, 0)
            }
        }
    }

    fun setUniform3f(name: String, x: Float, y: Float, z: Float) {
        val location = GLES30.glGetUniformLocation(programId, name)
        if (location != -1) GLES30.glUniform3f(location, x, y, z)
    }

    fun setUniform4f(name: String, x: Float, y: Float, z: Float, w: Float) {
        val location = GLES30.glGetUniformLocation(programId, name)
        if (location != -1) GLES30.glUniform4f(location, x, y, z, w)
    }

    fun setUniform1f(name: String, value: Float) {
        val location = GLES30.glGetUniformLocation(programId, name)
        if (location != -1) GLES30.glUniform1f(location, value)
    }

    fun setUniform1i(name: String, value: Int) {
        val location = GLES30.glGetUniformLocation(programId, name)
        if (location != -1) GLES30.glUniform1i(location, value)
    }

    companion object {
        private const val fallbackVertex = """#version 300 es
            layout(location = 0) in vec3 a_Position;
            void main() {
                gl_Position = vec4(a_Position * 0.25, 1.0);
            }
        """
        private const val fallbackFragment = """#version 300 es
            precision mediump float;
            out vec4 FragColor;
            void main() {
                FragColor = vec4(1.0, 0.0, 1.0, 1.0); // Flat Magenta for visible debug
            }
        """
    }
}
