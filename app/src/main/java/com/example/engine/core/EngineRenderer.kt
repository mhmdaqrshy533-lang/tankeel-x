package com.example.engine.core

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.os.SystemClock
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import com.example.engine.graphics.*
import com.example.engine.systems.SpaceDustSystem
import com.example.engine.systems.FighterController
import com.example.engine.systems.WeaponSystem
import kotlin.math.max

class EngineRenderer : GLSurfaceView.Renderer {

    private lateinit var camera: Camera
    private lateinit var tankeelObj: GameObject
    private lateinit var fighterObj: GameObject
    private lateinit var spaceDustSystem: SpaceDustSystem
    private lateinit var fighterController: FighterController
    private lateinit var weaponSystem: WeaponSystem
    
    private val lightDir = floatArrayOf(-1f, -0.2f, -0.8f)
    private val nebulaColorInfluence = FloatArray(3)
    private val ambientLight = FloatArray(3)
    private val overlayCamera = Camera(fov = 40f).apply {
        position = floatArrayOf(0f, 0f, 6.5f)
        target = floatArrayOf(0f, 0f, 0f)
    }

    // Engine stats for MainActivity
    var totalElapsedRuntime = 0f
        private set
    val activeProjectilesCount: Int
        get() = if (this::weaponSystem.isInitialized) weaponSystem.activeProjectilesCount else 0
    val totalFiringCount: Int
        get() = if (this::weaponSystem.isInitialized) weaponSystem.totalFiringCount else 0
        
    private var lastTime: Long = 0
    private var startTime: Long = 0

    // PBR-lite shader specifically designed for Mobile OpenGL ES 3.0
    private val vertexShader = """#version 300 es
        layout(location = 0) in vec3 a_Position;
        layout(location = 1) in vec3 a_Normal;
        layout(location = 2) in vec2 a_UV;

        uniform mat4 u_Model;
        uniform mat4 u_View;
        uniform mat4 u_Projection;

        out vec3 v_FragPos;
        out vec3 v_Normal;
        out vec2 v_UV;
        out vec3 v_LocalPos;

        void main() {
            vec4 worldPos = u_Model * vec4(a_Position, 1.0);
            v_FragPos = worldPos.xyz;
            mat3 normalMatrix = mat3(transpose(inverse(u_Model)));
            v_Normal = normalize(normalMatrix * a_Normal);
            v_UV = a_UV;
            v_LocalPos = a_Position;
            gl_Position = u_Projection * u_View * worldPos;
        }
    """

    private val fragmentShader = """#version 300 es
        precision highp float;

        in vec3 v_FragPos;
        in vec3 v_Normal;
        in vec2 v_UV;
        in vec3 v_LocalPos;

        out vec4 FragColor;

        uniform vec3 u_Albedo;
        uniform float u_Metallic;
        uniform float u_Roughness;
        uniform vec3 u_EmissionColor;
        uniform float u_EmissionIntensity;
        uniform int u_IsPlanet;

        uniform vec3 u_LightDir; // Directional light from distant sun
        uniform vec3 u_Ambient;
        uniform vec3 u_CameraPos;

        // Procedural noise for veins
        vec3 hash33(vec3 p) {
            p = vec3( dot(p,vec3(127.1,311.7, 74.7)),
                      dot(p,vec3(269.5,183.3,246.1)),
                      dot(p,vec3(113.5,271.9,124.6)));
            return fract(sin(p)*43758.5453123);
        }
        
        float vnoise( in vec3 p ) {
            vec3 i = floor(p);
            vec3 f = fract(p);
            vec3 u = f*f*(3.0-2.0*f);
            return mix( mix( mix( dot(hash33(i + vec3(0.0,0.0,0.0)), f - vec3(0.0,0.0,0.0)), 
                                  dot(hash33(i + vec3(1.0,0.0,0.0)), f - vec3(1.0,0.0,0.0)), u.x),
                             mix( dot(hash33(i + vec3(0.0,1.0,0.0)), f - vec3(0.0,1.0,0.0)), 
                                  dot(hash33(i + vec3(1.0,1.0,0.0)), f - vec3(1.0,1.0,0.0)), u.x), u.y),
                        mix( mix( dot(hash33(i + vec3(0.0,0.0,1.0)), f - vec3(0.0,0.0,1.0)), 
                                  dot(hash33(i + vec3(1.0,0.0,1.0)), f - vec3(1.0,0.0,1.0)), u.x),
                             mix( dot(hash33(i + vec3(0.0,1.0,1.0)), f - vec3(0.0,1.0,1.0)), 
                                  dot(hash33(i + vec3(1.0,1.0,1.0)), f - vec3(1.0,1.0,1.0)), u.x), u.y), u.z );
        }

        void main() {
            vec3 N = normalize(v_Normal);
            vec3 V = normalize(u_CameraPos - v_FragPos);
            vec3 L = normalize(-u_LightDir);
            vec3 H = normalize(V + L);

            vec3 albedo;
            vec3 emission;
            float diff = max(dot(N, L), 0.0);

            if (u_IsPlanet == 1) {
                float n = vnoise(v_FragPos * 4.0);
                vec3 albedoBase = vec3(0.23, 0.24, 0.25);
                vec3 albedoCopper = vec3(0.55, 0.35, 0.24);
                albedo = mix(albedoBase, albedoCopper, n * 0.5 + 0.5);
                
                float veinNoise = vnoise(v_FragPos * 12.0);
                float vein = smoothstep(0.3, 0.45, veinNoise);
                emission = u_EmissionColor * vein * u_EmissionIntensity;
            } else {
                albedo = u_Albedo;
                // Tactical highlight on edges - use abs() because we render double-sided wings without culling
                float edge = 1.0 - abs(dot(N, V));
                edge = pow(edge, 3.0);
                
                // Deep Matte Dark Gray with Neon Cyan Wireframe
                emission = u_EmissionColor * edge * u_EmissionIntensity;
            }

            float specParams = mix(10.0, 128.0, 1.0 - u_Roughness);
            float spec = pow(max(dot(N, H), 0.0), specParams);
            vec3 specular = vec3(1.0) * spec * u_Metallic;

            vec3 ambient = u_Ambient * vec3(0.23, 0.24, 0.25);
            vec3 color = ambient + (albedo * diff) + specular + emission;
            
            // Tone mapping & Gamma correction
            color = color / (color + vec3(1.0));
            color = pow(color, vec3(1.0/2.2));

            FragColor = vec4(color, 1.0);
        }
    """

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.05f, 0.05f, 0.08f, 1.0f) // Cosmic dark blue
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glCullFace(GLES30.GL_BACK)

        camera = Camera(fov = 60f)
        camera.position = floatArrayOf(0f, 0f, 6.5f)

        spaceDustSystem = SpaceDustSystem()
        weaponSystem = WeaponSystem()

        val shader = Shader(vertexShader, fragmentShader)
        val material = Material(shader).apply {
            roughness = 0.6f
            metallic = 0.8f
            emissionColor = floatArrayOf(0.0f, 1.0f, 0.4f) // Neon Emerald Green
            emissionIntensity = 1.2f
            isPlanet = 1
        }

        // Extremely high vertex density for smooth sphere
        val mesh = SphereMesh.create(radius = 2.5f, rings = 120, sectors = 120)

        tankeelObj = GameObject("TANKEEL-X").apply {
            this.mesh = mesh
            this.material = material
            this.transform.rotation[0] = 15f // Tilt slightly
        }

        // Instantiate Fighter Mesh
        val fighterMaterial = Material(shader).apply {
            albedo = floatArrayOf(0.102f, 0.106f, 0.11f) // Tactical Matte Dark Gray #1A1B1C
            roughness = 0.35f
            metallic = 1.0f
            emissionColor = floatArrayOf(0.0f, 1.0f, 1.0f) // Neon Cyan
            emissionIntensity = 1.8f
            isPlanet = 0
        }

        fighterObj = GameObject("Cyber-Fighter").apply {
            this.mesh = FighterMesh.create()
            this.material = fighterMaterial
            this.transform.scale = floatArrayOf(0.25f, 0.25f, 0.25f) // Adjusted for massive prominent look
        }

        fighterController = FighterController(fighterObj)

        startTime = SystemClock.uptimeMillis()
        lastTime = startTime
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        if (width == 0 || height == 0) return
        GLES30.glViewport(0, 0, width, height)
        camera.updateAspect(width.toFloat() / height.toFloat())
        overlayCamera.updateAspect(width.toFloat() / height.toFloat())
        overlayCamera.updateProjection()
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClearColor(0.05f, 0.05f, 0.08f, 1.0f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        val time = SystemClock.uptimeMillis()
        val deltaTime = (time - lastTime) / 1000f
        val elapsedTime = (time - startTime) / 1000f
        lastTime = time
        totalElapsedRuntime = elapsedTime

        // Engine Logic - Update Loop (Time.deltaTime architecture)
        // Continuously rotate locally on the Y-Axis
        tankeelObj.transform.rotation[1] += 8f * deltaTime 

        // Hollywood-style panoramic view sweep
        camera.position[0] = kotlin.math.sin(elapsedTime * 0.1f) * 2f
        camera.position[2] = 6.5f + kotlin.math.cos(elapsedTime * 0.1f) * 0.5f

        camera.cinematicUpdate(elapsedTime, 0f) // Keep fixed or neutral camera tracking
        camera.updateView()

        // Dynamic Ambient Light interpolating Nebula colors
        nebulaColorInfluence[0] = 0.5f + kotlin.math.sin(elapsedTime * 0.5f) * 0.3f
        nebulaColorInfluence[1] = 0.2f + kotlin.math.sin(elapsedTime * 0.3f) * 0.2f
        nebulaColorInfluence[2] = 0.8f + kotlin.math.sin(elapsedTime * 0.4f) * 0.2f
        
        ambientLight[0] = nebulaColorInfluence[0] * 0.3f
        ambientLight[1] = nebulaColorInfluence[1] * 0.3f
        ambientLight[2] = nebulaColorInfluence[2] * 0.3f

        // Render Scene Objects
        tankeelObj.draw(camera, lightDir, ambientLight)
        
        // Volumetric Space Dust (Pure Points)
        spaceDustSystem.draw(camera, elapsedTime)

        // Update Fighter Physics
        fighterController.update(deltaTime)
        weaponSystem.update(deltaTime)
        
        overlayCamera.updateView()
        
        // Clear Depth Buffer: render Fighter definitively in foreground. Map without Culling for flat B-2 edges.
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT)
        GLES30.glDisable(GLES30.GL_CULL_FACE)
        fighterObj.draw(overlayCamera, lightDir, ambientLight)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        
        // Keep Weapon System in the same perspective space but behind Fighter
        weaponSystem.draw(overlayCamera)
    }

    fun onFire() {
        if (this::fighterController.isInitialized && this::weaponSystem.isInitialized) {
            val posX = fighterObj.transform.position[0]
            val posY = fighterObj.transform.position[1]
            val posZ = fighterObj.transform.position[2]
            val roll = fighterObj.transform.rotation[2]
            weaponSystem.fire(posX, posY, posZ, roll)
        }
    }

    fun onTouchDrag(x: Float, y: Float, width: Float, height: Float) {
        if (this::fighterController.isInitialized) {
            fighterController.onTouchDrag(x, y, width.toInt(), height.toInt())
        }
    }
}
