package com.example.engine.graphics

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.SystemClock
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class SpaceRenderer : GLSurfaceView.Renderer {

    private var program = 0
    private var positionHandle = 0
    private var resolutionHandle = 0
    private var timeHandle = 0

    private val vertexShaderCode = """
        attribute vec4 vPosition;
        void main() {
            gl_Position = vPosition;
        }
    """.trimIndent()

    // Highly optimized spherical raytracer with procedural noise for TANKEEL-X
    private val fragmentShaderCode = """
        precision mediump float;
        
        uniform vec2 u_resolution;
        uniform float u_time;
        
        // --- NOISE FUNCTIONS ---
        // 3D Simplex noise approximation for mobile
        vec4 taylorInvSqrt(vec4 r){return 1.79284291400159 - 0.85373472095314 * r;}
        vec3 mod289(vec3 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
        vec4 mod289(vec4 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
        vec4 permute(vec4 x) { return mod289(((x*34.0)+1.0)*x); }

        float snoise(vec3 v) {
            const vec2  C = vec2(1.0/6.0, 1.0/3.0) ;
            const vec4  D = vec4(0.0, 0.5, 1.0, 2.0);
            vec3 i  = floor(v + dot(v, C.yyy) );
            vec3 x0 = v - i + dot(i, C.xxx) ;
            vec3 g = step(x0.yzx, x0.xyz);
            vec3 l = 1.0 - g;
            vec3 i1 = min( g.xyz, l.zxy );
            vec3 i2 = max( g.xyz, l.zxy );
            vec3 x1 = x0 - i1 + C.xxx;
            vec3 x2 = x0 - i2 + C.yyy;
            vec3 x3 = x0 - D.yyy;
            i = mod289(i);
            vec4 p = permute( permute( permute(
                        i.z + vec4(0.0, i1.z, i2.z, 1.0 ))
                      + i.y + vec4(0.0, i1.y, i2.y, 1.0 ))
                      + i.x + vec4(0.0, i1.x, i2.x, 1.0 ));
            float n_ = 0.142857142857; // 1.0/7.0
            vec3  ns = n_ * D.wyz - D.xzx;
            vec4 j = p - 49.0 * floor(p * ns.z * ns.z);
            vec4 x_ = floor(j * ns.z);
            vec4 y_ = floor(j - 7.0 * x_ );
            vec4 x = x_ *ns.x + ns.yyyy;
            vec4 y = y_ *ns.x + ns.yyyy;
            vec4 h = 1.0 - abs(x) - abs(y);
            vec4 b0 = vec4( x.xy, y.xy );
            vec4 b1 = vec4( x.zw, y.zw );
            vec4 s0 = floor(b0)*2.0 + 1.0;
            vec4 s1 = floor(b1)*2.0 + 1.0;
            vec4 sh = -step(h, vec4(0.0));
            vec4 a0 = b0.xzyw + s0.xzyw*sh.xxyy ;
            vec4 a1 = b1.xzyw + s1.xzyw*sh.zzww ;
            vec3 p0 = vec3(a0.xy,h.x);
            vec3 p1 = vec3(a0.zw,h.y);
            vec3 p2 = vec3(a1.xy,h.z);
            vec3 p3 = vec3(a1.zw,h.w);
            vec4 norm = taylorInvSqrt(vec4(dot(p0,p0), dot(p1,p1), dot(p2, p2), dot(p3,p3)));
            p0 *= norm.x;
            p1 *= norm.y;
            p2 *= norm.z;
            p3 *= norm.w;
            vec4 m = max(0.6 - vec4(dot(x0,x0), dot(x1,x1), dot(x2,x2), dot(x3,x3)), 0.0);
            m = m * m;
            return 42.0 * dot( m*m, vec4( dot(p0,x0), dot(p1,x1), dot(p2,x2), dot(p3,x3) ) );
        }
        
        // --- FBM ---
        float fbm(vec3 p) {
            float f = 0.0;
            f += 0.5000 * snoise(p); p *= 2.02;
            f += 0.2500 * snoise(p); p *= 2.03;
            f += 0.1250 * snoise(p); p *= 2.01;
            f += 0.0625 * snoise(p);
            return f;
        }

        // --- MATH & UTILS ---
        mat3 rotY(float a) {
            float c = cos(a), s = sin(a);
            return mat3(c, 0.0, s, 0.0, 1.0, 0.0, -s, 0.0, c);
        }
        mat3 rotX(float a) {
            float c = cos(a), s = sin(a);
            return mat3(1.0, 0.0, 0.0, 0.0, c, -s, 0.0, s, c);
        }

        float sphereIntersect(vec3 ro, vec3 rd, vec3 pos, float r, out vec3 normal) {
            vec3 oc = ro - pos;
            float b = dot(oc, rd);
            float c = dot(oc, oc) - r * r;
            float h = b * b - c;
            if(h < 0.0) return -1.0;
            float t = -b - sqrt(h);
            if(t > 0.0) {
                normal = normalize((ro + rd * t) - pos);
                return t;
            }
            return -1.0;
        }

        // --- BACKGROUND STARS & NEBULA ---
        vec3 renderBackground(vec3 rd) {
            // Nebula
            float n = fbm(rd * 3.0 + vec3(0.0, 0.0, u_time*0.01));
            vec3 col = vec3(0.1, 0.0, 0.2) * smoothstep(0.1, 0.8, n);
            
            // Nebula 2
            float n2 = fbm(rd * 5.0 - vec3(u_time*0.005));
            col += vec3(0.0, 0.1, 0.3) * smoothstep(0.3, 0.9, n2);
            
            // Stars
            float stars = pow(snoise(rd * 150.0), 10.0);
            if(stars > 0.0) {
                // adding flicker
                stars *= (0.5 + 0.5 * sin(u_time * 5.0 + rd.x * 100.0));
                col += vec3(1.0, 0.9, 1.0) * stars * 5.0; // intense stars
            }
            
            // Distant galaxies / Milky Way
            float galaxy = pow(fbm(rd * 8.0), 3.0);
            col += vec3(0.15, 0.05, 0.25) * galaxy;

            return col;
        }

        // --- PLANET SURFACE ---
        vec3 renderPlanet(vec3 ro, vec3 rd, float t, vec3 normal) {
            vec3 p = ro + rd * t;
            
            // The planet rotates slowly
            mat3 rot = rotY(u_time * 0.05) * rotX(u_time * 0.01);
            vec3 lp = rot * normal; // local position/normal
            
            // Surface Texture via Procedural Noise
            float n1 = fbm(lp * 3.0);
            float n2 = fbm(lp * 10.0);
            
            // Colors: Electric Blue and Dark Purple
            vec3 colorDark = vec3(0.05, 0.0, 0.15); // Deep purple
            vec3 colorLight = vec3(0.0, 0.3, 1.0);  // Electric blue
            
            // Mix factor based on noise
            float mixVal = smoothstep(0.2, 0.8, n1 * 0.6 + n2 * 0.4 + 0.5);
            vec3 baseColor = mix(colorDark, colorLight, mixVal);
            
            // Energy lines (glowing)
            float energy = smoothstep(0.9, 1.0, abs(sin(fbm(lp * 2.0) * 20.0 + u_time * 0.5)));
            vec3 energyColor = vec3(0.0, 0.8, 1.0) * energy * 2.0;

            // Clouds / glowing atmospheric wisps near surface
            float cloudNoise = fbm(lp * 6.0 + vec3(u_time * 0.02));
            vec3 cloudColor = vec3(0.6, 0.5, 1.0) * smoothstep(0.5, 1.0, cloudNoise);
            
            baseColor += energyColor + cloudColor * 0.5;
            
            // Lighting
            vec3 lightDir = normalize(vec3(1.0, 0.5, -1.0)); // Fixed cinematic light
            float diff = max(dot(normal, lightDir), 0.0);
            float wrap = max(dot(normal, lightDir) * 0.5 + 0.5, 0.0); // wrap diffuse for soft terminator
            float ambient = 0.05;
            
            // PBR-ish Fresnel for rim lighting
            float fresnel = pow(1.0 - max(dot(normal, -rd), 0.0), 3.0);
            
            vec3 litColor = baseColor * (wrap * 0.8 + ambient) + vec3(0.5, 0.2, 1.0) * fresnel * diff;
            return litColor;
        }

        // --- MAIN RENDER ---
        void main() {
            vec2 uv = (gl_FragCoord.xy - 0.5 * u_resolution.xy) / u_resolution.y;
            
            // Cinematic camera orbit (120 sec loop requested =  pi*2 in 120s => 2*pi/120 = 0.052)
            float camAngle = u_time * 0.052;
            vec3 ro = vec3(sin(camAngle)*4.0, sin(camAngle*0.5)*0.5, cos(camAngle)*4.0);
            vec3 ta = vec3(0.0, 0.0, 0.0);
            
            // Camera setup
            vec3 cw = normalize(ta - ro);
            vec3 cp = vec3(0.0, 1.0, 0.0);
            vec3 cu = normalize(cross(cw, cp));
            vec3 cv = normalize(cross(cu, cw));
            vec3 rd = normalize(uv.x*cu + uv.y*cv + 1.5*cw); // 1.5 is FOV

            // Render background
            vec3 col = renderBackground(rd);
            
            // Raytrace Planet
            vec3 normal;
            float t = sphereIntersect(ro, rd, vec3(0.0), 1.5, normal); // Sphere at origin, radius 1.5
            
            if(t > 0.0) {
                col = renderPlanet(ro, rd, t, normal);
            }
            
            // Atmosphere glow (Rayleigh scattering simulation / Volumetric effect outer glow)
            // By finding closest distance from ray to center of sphere
            vec3 L = vec3(0.0) - ro;
            float tca = dot(L, rd);
            if(tca > 0.0) { // planet is in front
                float d2 = dot(L, L) - tca * tca;
                float atmosRadius = 1.7; // larger than planet
                if (d2 < atmosRadius * atmosRadius) {
                    // ray goes through atmosphere
                    float falloff = (atmosRadius*atmosRadius - d2);
                    // Bloom/glow logic
                    float glow = pow(falloff, 1.5) * 0.1;
                    vec3 atmosColor = vec3(0.2, 0.0, 0.8);
                    
                    // Add sun rim based on light pos
                    vec3 lightDir = normalize(vec3(1.0, 0.5, -1.0));
                    vec3 p = ro + rd * tca; // closest point
                    float lightFalloff = max(dot(normalize(p), lightDir), 0.0);
                    
                    col += atmosColor * glow * (lightFalloff + 0.2); 
                }
            }

            // Global Bloom / ACES Tonemapping
            col = col / (col + vec3(1.0)); // simple tonemap
            col = pow(col, vec3(1.0/2.2)); // Gamma correction

            gl_FragColor = vec4(col, 1.0);
        }
    """.trimIndent()

    private var vertexBuffer: FloatBuffer

    init {
        // Simple quad filling the screen
        val quadCoords = floatArrayOf(
            -1.0f,  1.0f, 0.0f, 
            -1.0f, -1.0f, 0.0f,
             1.0f, -1.0f, 0.0f,
             1.0f,  1.0f, 0.0f
        )
        val bb = ByteBuffer.allocateDirect(quadCoords.size * 4)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer.put(quadCoords)
        vertexBuffer.position(0)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val err = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw RuntimeException("Error compiling shader: " + err)
        }
        return shader
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        resolutionHandle = GLES20.glGetUniformLocation(program, "u_resolution")
        timeHandle = GLES20.glGetUniformLocation(program, "u_time")
        
        startTime = SystemClock.uptimeMillis()
    }

    private var width = 0f
    private var height = 0f
    private var startTime: Long = 0

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        this.width = width.toFloat()
        this.height = height.toFloat()
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)

        // Pass resolution
        GLES20.glUniform2f(resolutionHandle, width, height)

        // Pass time in seconds
        val time = (SystemClock.uptimeMillis() - startTime) / 1000.0f
        GLES20.glUniform1f(timeHandle, time)

        // Draw quad using GL_TRIANGLE_FAN
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 3 * 4, vertexBuffer)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4)
        GLES20.glDisableVertexAttribArray(positionHandle)
    }
}
