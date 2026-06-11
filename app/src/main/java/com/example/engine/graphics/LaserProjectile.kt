package com.example.engine.graphics

class LaserProjectile {
    var active = false
    var x = 0f
    var y = 0f
    var z = 0f
    var velocityZ = -90f // Travels fast into the background
    var lifetime = 0f
    val maxLifetime = 1.2f

    fun spawn(startX: Float, startY: Float, startZ: Float) {
        x = startX
        y = startY
        z = startZ
        lifetime = 0f
        active = true
    }

    fun update(deltaTime: Float) {
        if (!active) return
        z += velocityZ * deltaTime
        lifetime += deltaTime
        if (lifetime >= maxLifetime) {
            active = false
        }
    }
}
