package com.example.engine.graphics

object FighterMesh {
    fun create(): Mesh {
        // Authentic B-2 Stealth Bomber Shape (Flying Wing, pure clean geometry)
        val rawVertices = floatArrayOf(
            // Top Center Body & Nose
            0f, 0.1f, -3.5f,     // 0 Nose Tip
            0f, 0.5f, -1.5f,     // 1 Cockpit Apex
            -0.8f, 0.3f, -1.0f,  // 2 Left Intake
            0.8f, 0.3f, -1.0f,   // 3 Right Intake
            0f, 0.2f, 0f,        // 4 Center Spine Back
            
            // Wing Tips (33 deg sweep approx)
            -6.0f, 0.05f, 2.0f,  // 5 Left Wingtip
            6.0f, 0.05f, 2.0f,   // 6 Right Wingtip
            
            // Trailing Edge (W-shape serrated)
            -3.5f, 0.1f, 1.0f,   // 7 Left Trailing Apex 1
            -1.5f, 0.1f, 1.8f,   // 8 Left Trailing Notch 1
            -0.6f, 0.1f, 1.2f,   // 9 Left Exhaust Notch
            0f, 0.1f, 1.8f,      // 10 Center Tail Point
            0.6f, 0.1f, 1.2f,    // 11 Right Exhaust Notch
            1.5f, 0.1f, 1.8f,    // 12 Right Trailing Notch 1
            3.5f, 0.1f, 1.0f,    // 13 Right Trailing Apex 1
            
            // Bottom Surface (Flat-ish)
            0f, -0.2f, -1.5f     // 14 Bottom Center
        )
        
        val indicesRaw = intArrayOf(
            // Top Cockpit to Wings
            0, 2, 1,   0, 1, 3,
            0, 5, 2,   0, 3, 6,
            2, 5, 7,   2, 7, 8,   2, 8, 4,
            3, 4, 12,  3, 12, 13, 3, 13, 6,
            1, 2, 4,   1, 4, 3,
            
            // Trailing Edge connections
            4, 8, 9,   4, 9, 10,
            4, 10, 11, 4, 11, 12,
            
            // Bottom surface
            0, 14, 5,  0, 6, 14,
            5, 14, 7,  7, 14, 8,  8, 14, 9,  9, 14, 10,
            10, 14, 11, 11, 14, 12, 12, 14, 13, 13, 14, 6
        )

        val numTriangles = indicesRaw.size / 3
        val vertices = FloatArray(numTriangles * 3 * 3)
        val normals = FloatArray(numTriangles * 3 * 3)
        val uvs = FloatArray(numTriangles * 3 * 2)
        val indices = IntArray(numTriangles * 3)

        for (i in 0 until numTriangles) {
            val i0 = indicesRaw[i * 3]
            val i1 = indicesRaw[i * 3 + 1]
            val i2 = indicesRaw[i * 3 + 2]

            val v0x = rawVertices[i0 * 3]; val v0y = rawVertices[i0 * 3 + 1]; val v0z = rawVertices[i0 * 3 + 2]
            val v1x = rawVertices[i1 * 3]; val v1y = rawVertices[i1 * 3 + 1]; val v1z = rawVertices[i1 * 3 + 2]
            val v2x = rawVertices[i2 * 3]; val v2y = rawVertices[i2 * 3 + 1]; val v2z = rawVertices[i2 * 3 + 2]

            val ux = v1x - v0x; val uy = v1y - v0y; val uz = v1z - v0z
            val vx = v2x - v0x; val vy = v2y - v0y; val vz = v2z - v0z
            
            var nx = uy * vz - uz * vy
            var ny = uz * vx - ux * vz
            var nz = ux * vy - uy * vx
            
            var len = kotlin.math.sqrt(nx*nx + ny*ny + nz*nz)
            if (len == 0f) len = 1f
            nx /= len; ny /= len; nz /= len

            vertices[i * 9 + 0] = v0x; vertices[i * 9 + 1] = v0y; vertices[i * 9 + 2] = v0z
            vertices[i * 9 + 3] = v1x; vertices[i * 9 + 4] = v1y; vertices[i * 9 + 5] = v1z
            vertices[i * 9 + 6] = v2x; vertices[i * 9 + 7] = v2y; vertices[i * 9 + 8] = v2z

            for (j in 0..2) {
                normals[i * 9 + j * 3 + 0] = nx
                normals[i * 9 + j * 3 + 1] = ny
                normals[i * 9 + j * 3 + 2] = nz
                
                indices[i * 3 + j] = i * 3 + j
            }
        }

        return Mesh(vertices, normals, uvs, indices)
    }
}
