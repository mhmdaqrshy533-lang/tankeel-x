package com.example.engine.graphics

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

object SphereMesh {
    fun create(radius: Float, rings: Int, sectors: Int): Mesh {
        val vertices = FloatArray(rings * sectors * 3)
        val normals = FloatArray(rings * sectors * 3)
        val uvs = FloatArray(rings * sectors * 2)
        val indices = IntArray((rings - 1) * (sectors - 1) * 6)

        val R = 1f / (rings - 1).toFloat()
        val S = 1f / (sectors - 1).toFloat()

        var vIdx = 0
        var nIdx = 0
        var tIdx = 0

        for (r in 0 until rings) {
            for (s in 0 until sectors) {
                val y = sin(-PI / 2f + PI * r * R).toFloat()
                val x = cos(2f * PI * s * S).toFloat() * sin(PI * r * R).toFloat()
                val z = sin(2f * PI * s * S).toFloat() * sin(PI * r * R).toFloat()

                uvs[tIdx++] = s * S
                uvs[tIdx++] = r * R

                vertices[vIdx++] = x * radius
                vertices[vIdx++] = y * radius
                vertices[vIdx++] = z * radius

                normals[nIdx++] = x
                normals[nIdx++] = y
                normals[nIdx++] = z
            }
        }

        var iIdx = 0
        for (r in 0 until rings - 1) {
            for (s in 0 until sectors - 1) {
                val current = r * sectors + s
                val next = current + sectors

                indices[iIdx++] = current
                indices[iIdx++] = next
                indices[iIdx++] = current + 1

                indices[iIdx++] = current + 1
                indices[iIdx++] = next
                indices[iIdx++] = next + 1
            }
        }

        return Mesh(vertices, normals, uvs, indices)
    }
}
