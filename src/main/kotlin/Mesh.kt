package de.uniwuerzburg.omodvisualizer

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GL33.glVertexAttribDivisor
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil
import org.poly2tri.Poly2Tri
import org.poly2tri.geometry.polygon.Polygon
import java.awt.Color
import java.nio.FloatBuffer
import kotlin.collections.ArrayList
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.tan

class Mesh(
    val vbo: Int,
    val vao: Int,
    val size: Int,
    val drawMode: Int,
    val vboIdx: Int? = null,
    val indices: IntArray = intArrayOf()
) {
    private var instVBO: Int = 0
    private var instFB: FloatBuffer? = null
    var instances: Int = 1

    fun close() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo)
    }

    fun prepareInstancedDraw(positions: List<Pair<Float, Float>>) {
        require(positions.size == instances)

        // Fill buffer with new instance locations
        for ((x, y) in positions) {
            instFB!!.put(x)
            instFB!!.put(y)
        }
        instFB!!.rewind()
        glBindBuffer(GL_ARRAY_BUFFER, instVBO);
        GL20.glBufferSubData(GL_ARRAY_BUFFER, 0, instFB!!);
        glBindBuffer(GL_ARRAY_BUFFER, 0)
    }

    fun cleanUpInstancedDraw() {
        instFB!!.clear()
    }

    fun enableInstancing(n: Int, shaderProgram: ShaderProgram) {
        if(n <= 1) {
            throw IllegalStateException("Need at least to objects for instancing!")
        }
        instances = n

        instVBO = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, instVBO)
        instFB = MemoryUtil.memAllocFloat(n * 2)
        GL20.glBufferData(GL_ARRAY_BUFFER, (n * 2 * 4).toLong(), GL_DYNAMIC_DRAW)

        val offAttrib = glGetAttribLocation(shaderProgram.ref, "offset")
        glEnableVertexAttribArray(offAttrib)
        glVertexAttribPointer(offAttrib, 2, GL_FLOAT, false, 2 * 4, 0)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glVertexAttribDivisor(offAttrib, 1)
    }

    fun specifyAttributeArray(shaderProgram: ShaderProgram) {
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        val posAttrib = glGetAttribLocation(shaderProgram.ref, "position")
        glEnableVertexAttribArray(posAttrib)
        glVertexAttribPointer(posAttrib, 2, GL_FLOAT, false, 5 * 4, 0)

        val colAttrib = glGetAttribLocation(shaderProgram.ref, "color")
        glEnableVertexAttribArray(colAttrib)
        glVertexAttribPointer(colAttrib, 3, GL_FLOAT, false, 5 * 4, 2 * 4)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
    }

    companion object {
        private fun unpackColor(color: Color): List<Float> {
            val red = color.red / 255f
            val green = color.green / 255f
            val blue = color.blue / 255f
            return listOf(red, green, blue)
        }

        fun basicCircle(nSegments: Int, color: Color): Mesh {
            val colorFloats = unpackColor(color)
            val vertices = mutableListOf<Float>()

            val theta = PI * 2 / nSegments
            val tangetialFactor = tan(theta).toFloat()
            val radialFactor = cos(theta).toFloat()

            // Center
            vertices.add(0f)
            vertices.add(0f)
            vertices.addAll(colorFloats)

            // Add segments
            var x = 0.5f
            var y = 0f
            for (i in 0..nSegments) {
                val tx = -y;
                val ty = x;

                x += tx * tangetialFactor;
                y += ty * tangetialFactor;

                x *= radialFactor;
                y *= radialFactor;

                vertices.add(x)
                vertices.add(y)

                vertices.addAll(colorFloats)
            }
            return fromVertices(vertices.toFloatArray(), GL_TRIANGLE_FAN)
        }

        fun basicRectangle(color: Color): Mesh {
            val colorFloats = unpackColor(color)
            val positions = listOf(
                listOf(-1f, -1f),
                listOf(-1f, 1f),
                listOf(1f, 1f),
                listOf(1f, 1f),
                listOf(1f, -1f),
                listOf(-1f, -1f),
            )
            val vertices = ArrayList<Float>(positions.size * 2 * 2)
            for (position in positions) {
                vertices.addAll(position)
                vertices.addAll(colorFloats)
            }
            return fromVertices(vertices.toFloatArray())
        }

        fun from2DPolygons(polygons: List<Polygon>, colors: List<Color>): Mesh {
            val vertices = ArrayList<Float>()
            val indices = ArrayList<Int>()
            val uniqueColors = colors.toSet()
            val idxMap = mutableMapOf<Color, MutableMap<Pair<Float, Float>, Int>>()
            for (color in uniqueColors) {
                idxMap[color] = mutableMapOf()
            }
            var runningIdx = 0

            for ((polygon, color) in polygons.zip(colors)) {
                // Triangulate
                try {
                    Poly2Tri.triangulate(polygon)
                    val triangles = polygon.triangles

                    // Get vertices
                    val colorFloats = unpackColor(color)
                    for (triangle in triangles) {
                        for (point in triangle.points) {
                            val x = point.x.toFloat()
                            val y = point.y.toFloat()
                            val pnt = Pair(x, y)
                            if (idxMap[color]!!.containsKey(pnt)) {
                                val vIdx = idxMap[color]!![pnt]!!
                                indices.add(vIdx)
                            } else {
                                vertices.add(x)
                                vertices.add(y)
                                vertices.addAll(colorFloats)
                                idxMap[color]!![pnt] = runningIdx
                                indices.add(runningIdx)
                                runningIdx += 1
                            }
                        }
                    }
                } catch (e: Exception) {
                    // TODO triangulation fails rarely. Why?
                }
            }
            return fromVertices(vertices.toFloatArray(), indices.toIntArray())
        }

        fun from2DPolygon(polygon: Polygon, color: Color): Mesh {
            // Triangulate
            Poly2Tri.triangulate(polygon)
            val triangles = polygon.triangles

            // Get vertices
            val colorFloats = unpackColor(color)
            val vertices = ArrayList<Float>(triangles.size * 3 * 5)
            for (triangle in triangles) {
                for (point in triangle.points) {
                    vertices.add(point.x.toFloat())
                    vertices.add(point.y.toFloat())

                    vertices.addAll(colorFloats)
                }
            }
            return fromVertices(vertices.toFloatArray())
        }

        private fun fromVertices(vertices: FloatArray, drawMode: Int = GL_TRIANGLES): Mesh {
            val vao: Int = glGenVertexArrays()
            glBindVertexArray(vao)
            var vbo: Int?
            stackPush().use { _ ->
                val buffer = BufferUtils.createFloatBuffer(vertices.size)
                buffer.put(vertices)
                buffer.rewind()

                vbo = glGenBuffers()
                glBindBuffer(GL_ARRAY_BUFFER, vbo!!)
                glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW)
                glBindBuffer(GL_ARRAY_BUFFER, 0)
            }
            glBindVertexArray(0)
            return Mesh(vbo!!, vao, vertices.size, drawMode)
        }

        private fun fromVertices(vertices: FloatArray, indices: IntArray, drawMode: Int = GL_TRIANGLES): Mesh {
            val vao: Int = glGenVertexArrays()
            glBindVertexArray(vao)
            var vbo: Int?
            stackPush().use { _ ->
                val buffer = BufferUtils.createFloatBuffer(vertices.size)
                buffer.put(vertices)
                buffer.rewind()

                vbo = glGenBuffers()
                glBindBuffer(GL_ARRAY_BUFFER, vbo!!)
                glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW)
                glBindBuffer(GL_ARRAY_BUFFER, 0)
            }
            var vboIdx: Int?
            stackPush().use { _ ->
                val buffer = BufferUtils.createIntBuffer(indices.size)
                buffer.put(indices)
                buffer.rewind()

                vboIdx = glGenBuffers()
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboIdx!!)
                glBufferData(GL_ELEMENT_ARRAY_BUFFER, buffer, GL_STATIC_DRAW)
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
            }
            glBindVertexArray(0)
            return Mesh(vbo!!, vao, vertices.size, drawMode, vboIdx, indices)
        }
    }
}