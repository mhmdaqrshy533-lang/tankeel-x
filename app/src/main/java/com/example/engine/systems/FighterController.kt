package com.example.engine.systems

import com.example.engine.core.GameObject

class FighterController(private val fighter: GameObject) {
    // Target positions set by touch drag mapped to -1 to +1 space
    var targetX = 0f
    var targetY = 0f

    // Current positions
    var currentX = 0f
    var currentY = 0f
    
    // Velocity/Drift state
    private var velocityX = 0f

    fun update(deltaTime: Float) {
        // LERP for Hajwalah Cosmic Inertia/Lag
        val smoothFactor = 6.0f * deltaTime
        
        val oldX = currentX
        val oldY = currentY
        currentX += (targetX - currentX) * smoothFactor
        currentY += (targetY - currentY) * smoothFactor

        // Calculate velocity for banking and pitching
        velocityX = (currentX - oldX) / deltaTime
        val velocityY = (currentY - oldY) / deltaTime

        // Update Transform Position (scaled to view frustum width)
        fighter.transform.position[0] = currentX * 3.5f
        fighter.transform.position[1] = currentY * 2.0f - 0.2f
        fighter.transform.position[2] = 1.5f // Strongly visible in Z
        
        // Rolling/Banking: aggressive tilt into turns to show off B-2 wide span
        val maxBankAngle = 45f
        val targetBank = -velocityX * 6f
        val clampedBank = targetBank.coerceIn(-maxBankAngle, maxBankAngle)
        fighter.transform.rotation[2] += (clampedBank - fighter.transform.rotation[2]) * 8f * deltaTime
        
        // Slight pointing towards movement direction (Yaw)
        fighter.transform.rotation[1] = (-velocityX * 2f).coerceIn(-20f, 20f)
        
        // Pitch: Ensure top surface is visible by maintaining a base upward pitch of 25 degrees
        val targetPitch = 25f + (velocityY * 8f).coerceIn(-15f, 15f)
        fighter.transform.rotation[0] += (targetPitch - fighter.transform.rotation[0]) * 8f * deltaTime
    }
    
    fun onTouchDrag(screenX: Float, screenY: Float, screenWidth: Int, screenHeight: Int) {
        // Map screen coordinates to -1.0 to 1.0 ranges
        targetX = (screenX / screenWidth) * 2f - 1f
        targetY = -((screenY / screenHeight) * 2f - 1f) // invert Y
    }
}
