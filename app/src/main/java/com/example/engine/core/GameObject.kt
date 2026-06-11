package com.example.engine.core

import com.example.engine.graphics.Mesh
import com.example.engine.graphics.Material

class GameObject(val name: String) {
    val transform = Transform()
    var mesh: Mesh? = null
    var material: Material? = null

    fun draw(camera: Camera, lightDir: FloatArray, ambient: FloatArray) {
        val m = mesh ?: return
        val mat = material ?: return

        mat.apply()
        val shader = mat.shader
        shader.setUniformMatrix4fv("u_Model", transform.getModelMatrix())
        shader.setUniformMatrix4fv("u_View", camera.viewMatrix)
        shader.setUniformMatrix4fv("u_Projection", camera.projectionMatrix)
        
        shader.setUniform3f("u_LightDir", lightDir[0], lightDir[1], lightDir[2])
        shader.setUniform3f("u_Ambient", ambient[0], ambient[1], ambient[2])
        shader.setUniform3f("u_CameraPos", camera.position[0], camera.position[1], camera.position[2])

        m.draw()
    }
}
