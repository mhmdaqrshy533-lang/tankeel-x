package com.example.engine.systems

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.example.engine.core.Camera
import com.example.engine.graphics.Shader
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.exp
import kotlin.random.Random

class SpaceDustSystem(val numStars: Int = 15000) {
    private val vao = IntArray(1)
    private val vbo = IntArray(1)
    private lateinit var starShader: Shader

    private val starVertexSource = """#version 300 es
        layout(location = 0) in vec3 a_Position;
        layout(location = 1) in float a_Alpha;
        layout(location = 2) in vec3 a_Color;

        uniform mat4 u_ViewProjection;
        uniform float u_Time;

        out vec4 v_Color;

        void main() {
            vec3 pos = a_Position;
            
            // Subtle rotation around Y-Axis for galactic spin
            float c = cos(u_Time * 0.05);
            float s = sin(u_Time * 0.05);
            float rx = pos.x * c - pos.z * s;
            float rz = pos.x * s + pos.z * c;
            pos.x = rx;
            pos.z = rz;
            
            gl_Position = u_ViewProjection * vec4(pos, 1.0);
            
            // Point size based on distance and random alpha (1.0 to 2.5)
            float dist = max(length(pos), 1.0);
            gl_PointSize = mix(1.0, 2.5, a_Alpha) * (150.0 / dist);
            
            // Twinkle Effect: sine wave based on time and position
            float twinkle = sin(u_Time * (5.0 + a_Alpha * 5.0) + a_Position.x * 12.0) * 0.5 + 0.5;
            float currentAlpha = mix(a_Alpha * 0.5, 1.0, twinkle);
            
            // Distance fade out at edges
            float distFade = smoothstep(120.0, 40.0, dist);
            
            v_Color = vec4(a_Color, currentAlpha * distFade);
        }
    """

    private val starFragmentSource = """#version 300 es
        precision mediump float;
        in vec4 v_Color;
        out vec4 FragColor;

        void main() {
            // Soft circular points for stars
            vec2 ptc = gl_PointCoord - vec2(0.5);
            float distSq = dot(ptc, ptc);
            if (distSq > 0.25) discard;
            
            FragColor = v_Color;
        }
    """

    init {
        val starBufferSize = numStars * 7 * 4 
        val buffer = ByteBuffer.allocateDirect(starBufferSize).order(ByteOrder.nativeOrder()).asFloatBuffer()

        val r = Random(42)
        for (i in 0 until numStars) {
            // Logarithmic spiral Milky Way distribution
            val maxTheta = 4.0 * Math.PI // 2 arms
            val theta = r.nextDouble() * maxTheta
            val a = 5.0
            val b = 0.25
            val radius = a * exp(b * theta)
            
            val spreadX = (r.nextFloat() - 0.5f) * (40f + radius.toFloat() * 0.5f)
            val spreadY = (r.nextFloat() - 0.5f) * 15f // Flatter galactic disk
            val spreadZ = (r.nextFloat() - 0.5f) * (40f + radius.toFloat() * 0.5f)

            // Two arms by flipping sign for half the stars
            val arm = if (r.nextBoolean()) 1.0 else -1.0
            
            val x = (cos(theta).toFloat() * radius.toFloat() * arm.toFloat()) + spreadX
            val y = spreadY
            val z = (sin(theta).toFloat() * radius.toFloat() * arm.toFloat()) + spreadZ
            val alpha = r.nextFloat() * 0.5f + 0.5f
            
            // Colors: Ice Blue (#E0F7FA), Electric Teal (#00E5FF), Soft Purple (#9C27B0), Gold (#FFD700)
            val colType = r.nextInt(100)
            val rCol: Float
            val gCol: Float
            val bCol: Float
            if(colType < 40) { // Ice Blue
                rCol = 0.88f; gCol = 0.97f; bCol = 0.98f
            } else if(colType < 70) { // Electric Teal
                rCol = 0.0f; gCol = 0.90f; bCol = 1.0f
            } else if(colType < 90) { // Soft Purple
                rCol = 0.61f; gCol = 0.15f; bCol = 0.69f
            } else { // Gold
                rCol = 1.0f; gCol = 0.84f; bCol = 0.0f
            }

            buffer.put(x).put(y).put(z).put(alpha).put(rCol).put(gCol).put(bCol)
        }
        buffer.position(0)

        GLES30.glGenVertexArrays(1, vao, 0)
        GLES30.glBindVertexArray(vao[0])
        GLES30.glGenBuffers(1, vbo, 0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, starBufferSize, buffer, GLES30.GL_STATIC_DRAW)

        val stride = 7 * 4
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(1, 1, GLES30.GL_FLOAT, false, stride, 3 * 4)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(2, 3, GLES30.GL_FLOAT, false, stride, 4 * 4)
        GLES30.glEnableVertexAttribArray(2)

        GLES30.glBindVertexArray(0)

        starShader = Shader(starVertexSource, starFragmentSource)
    }

    fun draw(camera: Camera, time: Float) {
        // Enable blending for softly glowing points
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE)
        GLES30.glDepthMask(false)
        
        val vpMatrix = FloatArray(16)
        android.opengl.Matrix.multiplyMM(vpMatrix, 0, camera.projectionMatrix, 0, camera.viewMatrix, 0)
        
        starShader.use()
        starShader.setUniformMatrix4fv("u_ViewProjection", vpMatrix)
        starShader.setUniform1f("u_Time", time)

        GLES30.glBindVertexArray(vao[0])
        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, numStars)
        GLES30.glBindVertexArray(0)

        GLES30.glDepthMask(true)
        GLES30.glDisable(GLES30.GL_BLEND)
    }
}
