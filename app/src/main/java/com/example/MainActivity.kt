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
            val renderer = remember { EngineRenderer() }
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                AndroidView(
                    factory = { context ->
                        GLSurfaceView(context).apply {
                            setEGLContextClientVersion(3)
                            setRenderer(renderer)
                            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                            glSurfaceView = this
                            post {
                                requestLayout()
                                invalidate()
                            }
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
