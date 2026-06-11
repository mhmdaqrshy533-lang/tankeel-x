package com.example

import android.opengl.GLSurfaceView
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.engine.audio.SpaceAudio
import com.example.engine.graphics.SpaceRenderer
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Start atmospheric audio
        SpaceAudio.playSpaceDrone()

        setContent {
            TankeelApp()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SpaceAudio.stop()
    }
}

@Composable
fun TankeelApp() {
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(3000) // Splash screen duration
        showSplash = false
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Space 3D Scene
        AndroidView(
            factory = { context ->
                GLSurfaceView(context).apply {
                    setEGLContextClientVersion(2)
                    setRenderer(SpaceRenderer())
                    renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Splash Screen Overlay styled with "Bold Typography" theme
        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(animationSpec = tween(500)),
            exit = fadeOut(animationSpec = tween(1000))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF050208))
            ) {
                // Background blurry blobs
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 50.dp, y = (-50).dp)
                        .blur(100.dp)
                        .background(Color(0x33581C87), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(250.dp)
                        .align(Alignment.BottomStart)
                        .offset(x = (-50).dp, y = 50.dp)
                        .blur(80.dp)
                        .background(Color(0x33164E63), CircleShape)
                )

                // Debug Info Overlay (Top Left)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .alpha(0.2f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("HDR_ENABLED", color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    Text("RAY_MARCHING", color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    Text("LOD_SYSTEM_0", color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                }

                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Engine Version 4.2.0",
                            color = Color(0xFF22D3EE).copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "STABLE_BUILD_PRODUCTION",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                    }
                    
                    val infiniteTransition = rememberInfiniteTransition()
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000),
                            repeatMode = RepeatMode.Reverse
                        )
                    )

                    Row(
                        modifier = Modifier
                            .background(Color(0x0DFFFFFF), RoundedCornerShape(50))
                            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF4ADE80).copy(alpha = pulseAlpha), CircleShape)
                        )
                        Text(
                            text = "60 FPS / ULTRA",
                            color = Color(0xFFE2E8F0),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }

                // Main Content
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Glowing Ring Graphic
                    Box(modifier = Modifier.size(256.dp).padding(bottom = 48.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF22D3EE), Color(0xFF4F46E5), Color(0xFF1A0033))
                                    ),
                                    shape = CircleShape
                                )
                                .border(1.dp, Color(0x3322D3EE), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                                .border(1.dp, Color(0x1A22D3EE), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .align(Alignment.Center)
                                .background(Color(0x6622D3EE))
                        )
                    }
                    
                    Text(
                        text = "TANKEEL-X",
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        color = Color.White,
                        letterSpacing = (-2).sp
                    )
                    Text(
                        text = "Deep Space Explorer",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Light,
                        color = Color(0xFF67E8F9),
                        letterSpacing = 4.sp,
                        modifier = Modifier.alpha(0.6f)
                    )
                }

                // Footer
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, Color(0x33FFFFFF), Color.Transparent)
                            ))
                            .padding(bottom = 32.dp)
                    )
                    
                    Text(
                        text = "Modular Graphics Architecture",
                        fontSize = 9.sp,
                        color = Color(0xFF64748B),
                        letterSpacing = 3.sp,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                    Text(
                        text = "Engineer Suhail Al-Hazbari",
                        fontSize = 11.sp,
                        color = Color(0xFFCBD5E1),
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Technical Lead & Founder",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B),
                        fontStyle = FontStyle.Italic,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Arabic Translation for Context
                    Text(
                        text = "صنع بواسطة المهندس سهيل الهزبري",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.alpha(0.8f)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Decoration Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0x0DFFFFFF), RoundedCornerShape(16.dp))
                                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (it == 0) Box(modifier = Modifier.size(4.dp).background(Color.White, CircleShape))
                                if (it == 1) Box(modifier = Modifier.width(12.dp).height(1.dp).background(Color.White))
                                if (it == 2) Box(modifier = Modifier.size(8.dp).border(1.dp, Color.White))
                            }
                        }
                    }
                }
            }
        }
    }
}
