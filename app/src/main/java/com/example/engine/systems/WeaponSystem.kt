package com.example.engine.systems

import android.opengl.GLES30
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.example.engine.core.Camera
import com.example.engine.graphics.LaserProjectile
import com.example.engine.graphics.Shader
import kotlin.math.cos
import kotlin.math.sin

class WeaponSystem {
    private val poolSize = 100
    private val projectiles = Array(poolSize) { LaserProjectile() }

    private lateinit var shader: Shader
    private val vao = IntArray(1)
    private val vbo = IntArray(2) // 0 for vertices, 1 for indices
    private var numIndices = 0

    private val vertexSource = """#version 300 es
        layout(location = 0) in vec3 a_Position;
        uniform mat4 u_MVP;
        void main() {
            gl_Position = u_MVP * vec4(a_Position, 1.0);
        }
    """

    private val fragmentSource = """#version 300 es
        precision mediump float;
        out vec4 FragColor;
        uniform vec4 u_Color;
        void main() {
            FragColor = u_Color;
        }
    """

    var totalFiringCount = 0
        private set

    val activeProjectilesCount: Int
        get() = projectiles.count { it.active }

    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)

    init {
        // Create thin box for laser mesh
        val w = 0.04f
        val h = 0.04f
        val l = 3.0f // Elongated beam
        val rawVertices = floatArrayOf(
            -w, -h, -l,   w, -h, -l,   w,  h, -l,  -w,  h, -l,
            -w, -h,  l,   w, -h,  l,   w,  h,  l,  -w,  h,  l
        )
        val indices = intArrayOf(
            0,1,2, 0,2,3, 4,6,5, 4,7,6, 0,4,5, 0,5,1,
            1,5,6, 1,6,2, 2,6,7, 2,7,3, 3,7,4, 3,4,0
        )
        numIndices = indices.size

        val vBuf = ByteBuffer.allocateDirect(rawVertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        vBuf.put(rawVertices).position(0)
        val iBuf = ByteBuffer.allocateDirect(indices.size * 4).order(ByteOrder.nativeOrder()).asIntBuffer()
        iBuf.put(indices).position(0)

        GLES30.glGenVertexArrays(1, vao, 0)
        GLES30.glBindVertexArray(vao[0])

        GLES30.glGenBuffers(2, vbo, 0)
        
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, rawVertices.size * 4, vBuf, GLES30.GL_STATIC_DRAW)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 3 * 4, 0)
        GLES30.glEnableVertexAttribArray(0)

        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, vbo[1])
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indices.size * 4, iBuf, GLES30.GL_STATIC_DRAW)

        GLES30.glBindVertexArray(0)

        shader = Shader(vertexSource, fragmentSource)
    }

    fun fire(fighterX: Float, fighterY: Float, fighterZ: Float, rollDegree: Float) {
        totalFiringCount += 2
        // Spawns twin lasers from the wingtips
        val rollRad = (rollDegree * Math.PI / 180.0).toFloat()
        
        // Horizontal offsets dynamically adjusted by roll orientation
        val offsetX = 1.6f * cos(rollRad)
        val offsetY = 1.6f * sin(rollRad)
        
        // Spawn Left Wing Laser
        spawnLaser(fighterX - offsetX, fighterY - offsetY, fighterZ - 0.5f)
        
        // Spawn Right Wing Laser
        spawnLaser(fighterX + offsetX, fighterY + offsetY, fighterZ - 0.5f)
    }

    private fun spawnLaser(x: Float, y: Float, z: Float) {
        for (p in projectiles) {
            if (!p.active) {
                p.spawn(x, y, z)
                break
            }
        }
    }

    fun update(deltaTime: Float) {
        for (p in projectiles) {
            p.update(deltaTime)
        }
    }

    fun draw(camera: Camera) {
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE)
        GLES30.glDepthMask(false)
        
        shader.use()
        
        // High-intensity Cosmic Cyan (#00F3FF)
        shader.setUniform4f("u_Color", 0.0f, 0.95f, 1.0f, 0.9f)

        GLES30.glBindVertexArray(vao[0])

        Matrix.multiplyMM(vpMatrix, 0, camera.projectionMatrix, 0, camera.viewMatrix, 0)

        for (p in projectiles) {
            if (p.active) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, p.x, p.y, p.z)
                Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
                
                shader.setUniformMatrix4fv("u_MVP", mvpMatrix)

                GLES30.glDrawElements(GLES30.GL_TRIANGLES, numIndices, GLES30.GL_UNSIGNED_INT, 0)
            }
        }

        GLES30.glBindVertexArray(0)
        GLES30.glDepthMask(true)
        GLES30.glDisable(GLES30.GL_BLEND)
    }
}
