package com.example.engine.core

import android.opengl.Matrix
import kotlin.math.cos
import kotlin.math.sin

class Camera(var fov: Float = 60f, var near: Float = 0.1f, var far: Float = 1000f) {
    var position = floatArrayOf(0f, 0f, 5f)
    var target = floatArrayOf(0f, 0f, 0f)
    var up = floatArrayOf(0f, 1f, 0f)

    var currentFov = fov

    val viewMatrix = FloatArray(16)
    val projectionMatrix = FloatArray(16)
    
    var aspect: Float = 1f

    fun updateAspect(aspect: Float) {
        this.aspect = aspect
        updateProjection()
    }

    fun updateProjection() {
        Matrix.perspectiveM(projectionMatrix, 0, currentFov, aspect, near, far)
    }

    fun updateView() {
        Matrix.setLookAtM(viewMatrix, 0, position[0], position[1], position[2], target[0], target[1], target[2], up[0], up[1], up[2])
    }

    fun cinematicUpdate(time: Float, hyperdriveFactor: Float) {
        // FOV interpolation: Wide 160 during hyperdrive to normal FOV
        currentFov = lerp(fov, 160f, hyperdriveFactor)
        updateProjection()

        // Position interpolation: deep Z to orbital rotation
        val startZ = 200f
        val startY = 20f
        val startX = 50f
        
        // Final orbit state
        val orbitDist = 6.5f
        val orbitSpeed = 0.05f
        val orbitAngle = time * orbitSpeed
        
        val targetX = cos(orbitAngle.toDouble()).toFloat() * orbitDist
        val targetZ = sin(orbitAngle.toDouble()).toFloat() * orbitDist
        val targetY = sin((time * 0.2f).toDouble()).toFloat() * 1.5f

        position[0] = lerp(targetX, startX, hyperdriveFactor)
        position[1] = lerp(targetY, startY, hyperdriveFactor)
        position[2] = lerp(targetZ, startZ, hyperdriveFactor)
    }

    private fun lerp(a: Float, b: Float, t: Float): Float {
        // smoothstep interpolation for a smoother transition
        val st = t * t * (3f - 2f * t)
        return a + (b - a) * st
    }
}
