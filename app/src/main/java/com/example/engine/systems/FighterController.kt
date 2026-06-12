package com.example.engine.systems

import com.example.engine.core.GameObject

class FighterController(private val fighter: GameObject) {
    // Current positions
    var posX = 0f
    var posY = 0f
    var posZ = 1.5f
    
    // Velocity/Drift state (Momentum and Inertia)
    var velocityX = 0f
        private set
    var velocityY = 0f
        private set

    // Target inputs from player
    var targetX = 0f
    var targetY = 0f
    
    // Physical constants (Newtonian Model)
    private val mass = 1200f
    private val thrustPower = 20000f
    private val spaceFriction = 0.6f // Pure space implies less friction, allowing drift

    // Engine/Rotational state
    private var currentRoll = 0f
    private var currentPitch = 25f
    private var currentYaw = 0f

    fun update(deltaTime: Float) {
        // Calculate thrust vectors based on input directives
        val desiredThrustX = targetX * thrustPower
        val desiredThrustY = targetY * thrustPower

        // Acceleration = Force / Mass (Newton's Second Law)
        val accelX = desiredThrustX / mass
        val accelY = desiredThrustY / mass

        // Update velocity (Momentum) with acceleration and space friction decay
        velocityX += accelX * deltaTime
        velocityY += accelY * deltaTime
        
        velocityX *= (1f - spaceFriction * deltaTime)
        velocityY *= (1f - spaceFriction * deltaTime)

        // Update position using integration
        posX += velocityX * deltaTime
        posY += velocityY * deltaTime
        
        // Soft bounds mapping to the camera frustum limits
        posX = posX.coerceIn(-3.8f, 3.8f)
        posY = posY.coerceIn(-2.2f, 2.2f)

        // Update Transform
        fighter.transform.position[0] = posX
        fighter.transform.position[1] = posY
        fighter.transform.position[2] = posZ
        
        // Rotational inertia calculation
        val maxBankAngle = 75f
        // Bank heavily in the direction of momentum (not just input)
        val targetBank = -velocityX * 10f
        val clampedBank = targetBank.coerceIn(-maxBankAngle, maxBankAngle)
        currentRoll += (clampedBank - currentRoll) * 5f * deltaTime
        
        // Yaw momentum
        val targetYaw = (-velocityX * 4f).coerceIn(-30f, 30f)
        currentYaw += (targetYaw - currentYaw) * 5f * deltaTime
        
        // Pitch momentum (base visibility + momentum-induced tilt)
        val targetPitch = 25f + (velocityY * 15f).coerceIn(-25f, 25f)
        currentPitch += (targetPitch - currentPitch) * 5f * deltaTime

        fighter.transform.rotation[0] = currentPitch
        fighter.transform.rotation[1] = currentYaw
        fighter.transform.rotation[2] = currentRoll
    }
    
    fun onTouchDrag(screenX: Float, screenY: Float, screenWidth: Int, screenHeight: Int) {
        // Map screen coordinates to thrust magnitude (-1.0 to 1.0 ranges)
        targetX = (screenX / screenWidth) * 2f - 1f
        targetY = -((screenY / screenHeight) * 2f - 1f) // invert Y
    }
}
