package com.example

import android.opengl.GLSurfaceView
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.engine.core.EngineRenderer
import androidx.compose.runtime.remember
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.changedToDown

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.example.engine.utils.SaveSystem
import androidx.compose.foundation.clickable

class MainActivity : ComponentActivity() {
    private var glSurfaceView: GLSurfaceView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Locked Landscape and Full Immersive Mode
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        
        enableEdgeToEdge()

        setContent {
            val context = applicationContext
            val saveSystem = remember { SaveSystem(context) }
            val renderer = remember { EngineRenderer() }
            
            var activeProjectiles by remember { mutableIntStateOf(0) }
            var warpVelocity by remember { mutableFloatStateOf(0f) }
            var currentScore by remember { mutableIntStateOf(0) }
            var highScore by remember { mutableIntStateOf(saveSystem.highScore) }
            var heading by remember { mutableFloatStateOf(0f) }
            var pilotInAircraft by remember { mutableStateOf(true) }
            
            val neonCyan = Color(0xFF00F3FF)
            val neonEmerald = Color(0xFF00FF66)
            val obsidianBlack = Color(0xB3000000) // 70% opacity black
            
            LaunchedEffect(Unit) {
                while (true) {
                    activeProjectiles = renderer.activeProjectilesCount
                    warpVelocity = 250f + (renderer.totalElapsedRuntime * 5f) % 100f
                    heading = renderer.fighterYaw
                    val s = renderer.totalFiringCount * 50
                    currentScore = s
                    if (s > highScore) {
                        highScore = s
                    }
                    saveSystem.updateMetrics(renderer.totalFiringCount, s, renderer.totalElapsedRuntime)
                    delay(32) // approx 30fps refresh for UI
                }
            }

            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                AndroidView(
                    factory = { ctx ->
                        GLSurfaceView(ctx).apply {
                            setEGLContextClientVersion(3)
                            setRenderer(renderer)
                            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                            glSurfaceView = this
                        }
                    },
                    modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val ptr = event.changes.firstOrNull()
                                if (ptr != null) {
                                    if (ptr.changedToDown()) {
                                        renderer.onFire()
                                    }
                                    renderer.onTouchDrag(ptr.position.x, ptr.position.y, size.width.toFloat(), size.height.toFloat())
                                }
                            }
                        }
                    }
                )
                
                // HUD Layer (Unified Grid Layout)
                Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                    
                    // Top Left: Telemetry & Integrity
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .background(obsidianBlack, RoundedCornerShape(0.dp)) // Sharp edges
                            .border(1.dp, neonCyan.copy(alpha = 0.5f), RoundedCornerShape(0.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "SGRD: OFFLINE",
                            color = neonCyan,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "HULL INTEGRITY: 100%\nWARP STABILITY: NOMINAL",
                            color = neonEmerald,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }

                    // Top Right: Economy & Tactical
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .background(obsidianBlack, RoundedCornerShape(0.dp))
                            .border(1.dp, neonCyan.copy(alpha = 0.5f), RoundedCornerShape(0.dp))
                            .padding(12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "CREDITS: $currentScore",
                            color = neonCyan,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "RESOURCES: $highScore\nCARGO: SECURE",
                            color = neonEmerald,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    }

                    // Center: Circular Compass (Rendered around the focal point)
                    Box(modifier = Modifier.align(Alignment.Center).size(300.dp)) {
                        // The compass ring
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(2.dp, neonCyan.copy(alpha = 0.3f), RoundedCornerShape(150.dp))
                        )
                        // The compass marker rotating based on heading
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .rotate(-heading)
                        ) {
                            Text(
                                text = "N",
                                color = neonCyan,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.align(Alignment.TopCenter).padding(8.dp)
                            )
                        }
                    }

                    // Bottom Right: Engine Metrics
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .background(obsidianBlack, RoundedCornerShape(0.dp))
                            .border(1.dp, neonCyan.copy(alpha = 0.5f), RoundedCornerShape(0.dp))
                            .padding(12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "THRUST: ${"%.1f".format(warpVelocity * 10f)} km/s",
                            color = neonCyan,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "PROJECTILES: $activeProjectiles",
                            color = neonCyan.copy(alpha = 0.8f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }

                    // Bottom Left: ENTER AIRCRAFT Toggle
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .background(obsidianBlack, RoundedCornerShape(0.dp))
                            .border(2.dp, neonEmerald, RoundedCornerShape(0.dp))
                            .clickable {
                                pilotInAircraft = !pilotInAircraft
                            }
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = if (pilotInAircraft) "EXIT AIRCRAFT" else "ENTER AIRCRAFT",
                            color = neonEmerald,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView?.onPause()
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView?.onResume()
    }
}
