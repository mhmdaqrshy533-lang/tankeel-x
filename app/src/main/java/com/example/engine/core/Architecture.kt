package com.example.engine.core

interface Component {
    fun update(deltaTime: Float)
}

abstract class Entity {
    open fun update(deltaTime: Float) {}
}
