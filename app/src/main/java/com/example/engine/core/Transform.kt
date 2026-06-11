package com.example.engine.core

import android.opengl.Matrix

class Transform {
    var position = FloatArray(3) { 0f }
    var rotation = FloatArray(3) { 0f } // x, y, z in degrees
    var scale = FloatArray(3) { 1f }

    fun getModelMatrix(): FloatArray {
        val matrix = FloatArray(16)
        Matrix.setIdentityM(matrix, 0)
        Matrix.translateM(matrix, 0, position[0], position[1], position[2])
        Matrix.rotateM(matrix, 0, rotation[0], 1f, 0f, 0f)
        Matrix.rotateM(matrix, 0, rotation[1], 0f, 1f, 0f)
        Matrix.rotateM(matrix, 0, rotation[2], 0f, 0f, 1f)
        Matrix.scaleM(matrix, 0, scale[0], scale[1], scale[2])
        return matrix
    }
}
