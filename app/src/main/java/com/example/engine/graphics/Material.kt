package com.example.engine.graphics

class Material(val shader: Shader) {
    var albedo = floatArrayOf(1f, 1f, 1f)
    var metallic = 0f
    var roughness = 1f
    var emissionColor = floatArrayOf(0f, 0f, 0f)
    var emissionIntensity = 0f
    var isPlanet = 0
    
    fun apply() {
        shader.use()
        shader.setUniform3f("u_Albedo", albedo[0], albedo[1], albedo[2])
        shader.setUniform1f("u_Metallic", metallic)
        shader.setUniform1f("u_Roughness", roughness)
        shader.setUniform3f("u_EmissionColor", emissionColor[0], emissionColor[1], emissionColor[2])
        shader.setUniform1f("u_EmissionIntensity", emissionIntensity)
        shader.setUniform1i("u_IsPlanet", isPlanet)
    }
}
