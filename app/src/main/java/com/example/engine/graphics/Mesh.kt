package com.example.engine.graphics

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Mesh(vertices: FloatArray, normals: FloatArray, uvs: FloatArray, indices: IntArray) {
    private val vao = IntArray(1)
    private val vbo = IntArray(3)
    private val ibo = IntArray(1)
    private val indexCount = indices.size

    init {
        GLES30.glGenVertexArrays(1, vao, 0)
        GLES30.glBindVertexArray(vao[0])

        GLES30.glGenBuffers(3, vbo, 0)
        
        // Vertices
        val vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(vertices)
        vertexBuffer.position(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.size * 4, vertexBuffer, GLES30.GL_STATIC_DRAW)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, 0)
        GLES30.glEnableVertexAttribArray(0)

        // Normals
        val normalBuffer = ByteBuffer.allocateDirect(normals.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(normals)
        normalBuffer.position(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[1])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, normals.size * 4, normalBuffer, GLES30.GL_STATIC_DRAW)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, 0, 0)
        GLES30.glEnableVertexAttribArray(1)

        // UVs
        val uvBuffer = ByteBuffer.allocateDirect(uvs.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(uvs)
        uvBuffer.position(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[2])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, uvs.size * 4, uvBuffer, GLES30.GL_STATIC_DRAW)
        GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, 0, 0)
        GLES30.glEnableVertexAttribArray(2)

        // Indices
        GLES30.glGenBuffers(1, ibo, 0)
        val indexBuffer = ByteBuffer.allocateDirect(indices.size * 4).order(ByteOrder.nativeOrder()).asIntBuffer().put(indices)
        indexBuffer.position(0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ibo[0])
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indices.size * 4, indexBuffer, GLES30.GL_STATIC_DRAW)

        GLES30.glBindVertexArray(0)
    }

    fun draw() {
        GLES30.glBindVertexArray(vao[0])
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_INT, 0)
        GLES30.glBindVertexArray(0)
    }
}
