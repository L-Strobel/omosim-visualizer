package de.uniwuerzburg.omodvisualizer.graphic

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GL33.glVertexAttribDivisor
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil
import org.poly2tri.Poly2Tri
import org.poly2tri.geometry.polygon.Polygon
import java.awt.Color
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.tan

class Mesh(
    var vbo: Int,
    val vao: Int,
    val size: Int,
    val drawMode: Int,
    val vboIdx: Int? = null,
    val indexSize: Int = 0
) {
    private var instVBO: Int = 0
    private var instFB: FloatBuffer? = null
    var maxInstances: Int = 1

    fun close() {
        glDeleteVertexArrays(vao)
        glDeleteBuffers(vbo)
    }

    fun prepareInstancedDraw(positions: List<Pair<Float, Float>>) {
        require(positions.size <= maxInstances)

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

    fun enableInstancing(maxInstances: Int, shaderProgram: ShaderProgram) {
        if(maxInstances <= 1) {
            throw IllegalStateException("Don't enable instancing with maximum one object!")
        }
        this.maxInstances = maxInstances

        instVBO = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, instVBO)
        instFB = MemoryUtil.memAllocFloat(maxInstances * 2)
        GL20.glBufferData(GL_ARRAY_BUFFER, (maxInstances * 2 * 4).toLong(), GL_DYNAMIC_DRAW)

        val offAttrib = glGetAttribLocation(shaderProgram.ref, "offset")
        glEnableVertexAttribArray(offAttrib)
        glVertexAttribPointer(offAttrib, 2, GL_FLOAT, false, 2 * 4, 0)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glVertexAttribDivisor(offAttrib, 1)
    }

    companion object {
        private fun unpackColor(color: Color): List<Float> {
            val red = color.red / 255f
            val green = color.green / 255f
            val blue = color.blue / 255f
            val alpha = color.alpha / 255f
            return listOf(red, green, blue, alpha)
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

        fun roundedCornerRectangle(color: Color, roundedness: Double, nSegments: Int, width: Float, height: Float): Mesh {
            val r = min(width, min(height, roundedness.toFloat()))
            val cw = width - 2*r
            val ch = height - 2*r

            val colorFloats = unpackColor(color)
            val positions = mutableListOf<List<Float>>()
            // Centerpiece
            if (cw > 0f) {
                positions.addAll(
                    listOf(
                        listOf(-1f*cw, -1f*ch),
                        listOf(-1f*cw,  1f*ch),
                        listOf( 1f*cw,  1f*ch),
                        listOf( 1f*cw,  1f*ch),
                        listOf (1f*cw, -1f*ch),
                        listOf(-1f*cw, -1f*ch),
                    )
                )
            }

            // Border centers
            if (r > 0f) {
                val lSideRect = listOf(
                    listOf(-1f*r - cw - 1f*r, -1f*ch),
                    listOf(-1f*r - cw - 1f*r,  1f*ch),
                    listOf( 1f*r - cw - 1f*r,  1f*ch),
                    listOf (1f*r - cw - 1f*r,  1f*ch),
                    listOf( 1f*r - cw - 1f*r, -1f*ch),
                    listOf(-1f*r - cw - 1f*r, -1f*ch),
                )

                val rSideRect = listOf(
                    listOf(-1f*r + cw + 1f*r, -1f*ch),
                    listOf(-1f*r + cw + 1f*r,  1f*ch),
                    listOf( 1f*r + cw + 1f*r,  1f*ch),
                    listOf (1f*r + cw + 1f*r,  1f*ch),
                    listOf( 1f*r + cw + 1f*r, -1f*ch),
                    listOf(-1f*r + cw + 1f*r, -1f*ch),
                )

                val uSideRect = listOf(
                    listOf(-1f*cw, -1f*r + ch + 1f*r),
                    listOf(-1f*cw,  1f*r + ch + 1f*r),
                    listOf( 1f*cw,  1f*r + ch + 1f*r),
                    listOf (1f*cw,  1f*r + ch + 1f*r),
                    listOf( 1f*cw, -1f*r + ch + 1f*r),
                    listOf(-1f*cw, -1f*r + ch + 1f*r),
                )

                val dSideRect = listOf(
                    listOf(-1f*cw, -1f*r - ch - 1f*r),
                    listOf(-1f*cw,  1f*r - ch - 1f*r),
                    listOf( 1f*cw,  1f*r - ch - 1f*r),
                    listOf (1f*cw,  1f*r - ch - 1f*r),
                    listOf( 1f*cw, -1f*r - ch - 1f*r),
                    listOf(-1f*cw, -1f*r - ch - 1f*r),
                )

                positions.addAll(lSideRect)
                positions.addAll(rSideRect)
                positions.addAll(uSideRect)
                positions.addAll(dSideRect)
            }

            // Corners
            val theta = PI * 2 / 4 / nSegments
            val tangetialFactor = tan(theta).toFloat()
            val radialFactor = cos(theta).toFloat()

            // Center
            var x = 0.5f
            var y = 0f

            var cx = cw
            var cy = ch
            for (i in 0 until nSegments* 4) {
                if (i == nSegments) {
                    cx = -cw
                }
                if (i == nSegments*2) {
                    cy = -ch
                }
                if (i == nSegments*3) {
                    cx = cw
                }


                positions.add(listOf(cx, cy))
                positions.add(listOf(x*r*4+cx, y*r*4+cy))


                val tx = -y;
                val ty = x;

                x += tx * tangetialFactor;
                y += ty * tangetialFactor;

                x *= radialFactor;
                y *= radialFactor;

                positions.add(listOf(x*r*4+cx, y*r*4+cy))
            }

            val vertices = ArrayList<Float>(positions.size * 2 * 2)
            for (position in positions) {
                vertices.addAll(position)
                vertices.addAll(colorFloats)
            }
            return fromVertices(vertices.toFloatArray())
        }

        fun textCanvasVertices (
            glyphs: List<Glyph>,
            llX: Float, llY: Float,
            texWidth: Float, texHeight: Float,
            windowWidth: Float, windowHeight: Float
        ): FloatArray {
            val colorFloats = unpackColor(Color.WHITE)
            val vertices = ArrayList<Float>(glyphs.size * 6 * (2 + 3 + 2))

            var xOffset = 0f
            for ((i, glyph) in glyphs.withIndex()) {
                // Window position
                val ww = glyph.width.toFloat() / windowWidth
                val wh = glyph.height.toFloat() / windowHeight

                //val charAspect = glyph.height.toFloat() /  glyph.width.toFloat()

                val positions = listOf(
                    listOf( llX+   xOffset, llY),
                    listOf( llX+   xOffset, llY + wh),
                    listOf( llX+ww+xOffset, llY + wh),
                    listOf( llX+ww+xOffset, llY + wh),
                    listOf( llX+ww+xOffset, llY),
                    listOf( llX+   xOffset, llY),
                )

                // Atlas position
                val gx = glyph.x.toFloat() / texWidth
                val gy = glyph.y.toFloat() / texHeight
                val gw = glyph.width.toFloat() / texWidth
                val gh = glyph.height.toFloat() / texHeight

                val texPositions = listOf(
                    listOf(gx, gy),
                    listOf(gx, gy+gh),
                    listOf(gx+gw, gy+gh),
                    listOf(gx+gw, gy+gh),
                    listOf(gx+gw, gy),
                    listOf(gx, gy),
                )
                for ((position, texPosition) in positions.zip(texPositions)) {
                    vertices.addAll(position)
                    vertices.addAll(colorFloats)
                    vertices.addAll(texPosition)
                }
                xOffset += ww
            }
            return vertices.toFloatArray()
        }

        fun textCanvas(
            glyphs: List<Glyph>,
            llX: Float, llY: Float,
            texWidth: Float, texHeight: Float,
            windowWidth: Float, windowHeight: Float
        ): Mesh {
            val vertices = textCanvasVertices(glyphs, llX, llY, texWidth, texHeight, windowWidth, windowHeight)
            return fromVertices(vertices)
        }

        fun textureCanvas(): Mesh {
            val colorFloats = unpackColor(Color.WHITE)
            val positions = listOf(
                listOf(-1f, -1f),
                listOf(-1f, 1f),
                listOf(1f, 1f),
                listOf(1f, 1f),
                listOf(1f, -1f),
                listOf(-1f, -1f),
            )
            val texPositions = listOf(
                listOf(0f, 0f),
                listOf(0f, 1f),
                listOf(1f, 1f),
                listOf(1f, 1f),
                listOf(1f, 0f),
                listOf(0f, 0f),
            )
            val vertices = ArrayList<Float>(positions.size * (2 + 3 + 2))
            for ((position, texPosition) in positions.zip(texPositions)) {
                vertices.addAll(position)
                vertices.addAll(colorFloats)
                vertices.addAll(texPosition)
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
            return Mesh(vbo!!, vao, vertices.size, drawMode, vboIdx, indices.size)
        }

        fun load(fn: String) : Mesh {
            val drawMode: Int = GL_TRIANGLES
            val vao: Int = glGenVertexArrays()
            glBindVertexArray(vao)

            // Vertices
            val fisv = FileInputStream("${fn}_vertices")
            val vBytes = fisv.available()

            val vBuffer = BufferUtils.createByteBuffer(vBytes)
            val vChannel = fisv.channel
            vChannel.read(vBuffer)
            vBuffer.rewind()

            val vbo = glGenBuffers()
            glBindBuffer(GL_ARRAY_BUFFER, vbo)
            glBufferData(GL_ARRAY_BUFFER, vBuffer, GL_STATIC_DRAW)
            glBindBuffer(GL_ARRAY_BUFFER, 0)

            // Index
            val fisi = FileInputStream("${fn}_indices")
            val iBytes = fisi.available()

            val iBuffer = BufferUtils.createByteBuffer(vBytes)
            val iChannel = fisi.channel
            iChannel.read(iBuffer)
            iBuffer.rewind()

            val vboIdx = glGenBuffers()
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboIdx)
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, iBuffer, GL_STATIC_DRAW)
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)

            glBindVertexArray(0)
            return Mesh(vbo, vao, (vBytes / 4), drawMode, vboIdx, iBytes / 4)
        }
    }

    fun save(fn: String) {
        require(vboIdx != null)

        stackPush().use { _ ->
            val buffer = BufferUtils.createByteBuffer(this.size * 4)
            glBindBuffer(GL_ARRAY_BUFFER, vbo)
            glGetBufferSubData(GL_ARRAY_BUFFER, 0, buffer)
            glBindBuffer(GL_ARRAY_BUFFER, 0)

            println("First saved")
            println(buffer.asFloatBuffer().get(0))

            val fos = FileOutputStream("${fn}_vertices")
            val channel = fos.channel

            channel.write(buffer)

            fos.close()
        }

        stackPush().use { _ ->
            val buffer = BufferUtils.createByteBuffer(this.indexSize * 4)
            glBindBuffer(GL_ARRAY_BUFFER, vboIdx)
            glGetBufferSubData(GL_ARRAY_BUFFER, 0, buffer)
            glBindBuffer(GL_ARRAY_BUFFER, 0)

            val fos = FileOutputStream("${fn}_indices")
            val channel = fos.channel

            channel.write(buffer)

            fos.close()
        }

    }
}