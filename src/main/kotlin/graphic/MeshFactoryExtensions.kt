package de.uniwuerzburg.omodvisualizer.graphic

import org.lwjgl.opengl.GL30.GL_TRIANGLE_FAN
import org.poly2tri.Poly2Tri
import org.poly2tri.geometry.polygon.Polygon
import java.awt.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.tan

fun Renderer.addCircleMesh(nSegments: Int, color: Color) : Renderer {
    val vertices = basicCircle(nSegments, color)
    val mesh = Mesh.fromVertices(this.vao, vertices.toFloatArray(), GL_TRIANGLE_FAN)
    this.addMesh(mesh)
    return this
}

fun Renderer.addRectangleMesh(color: Color) : Renderer {
    val vertices = basicRectangle(color)
    val mesh = Mesh.fromVertices(this.vao, vertices.toFloatArray())
    this.addMesh(mesh)
    return this
}

fun Renderer.addRoundedCornerRectangle(
    color: Color, roundedness: Double, nSegments: Int, width: Float, height: Float
) : Renderer {
    val vertices = roundedCornerRectangle(color, roundedness, nSegments, width, height)
    val mesh = Mesh.fromVertices(this.vao, vertices.toFloatArray())
    this.addMesh(mesh)
    return this
}

fun Renderer.addTextCanvas(
    glyphs: List<Glyph>,
    llX: Float, llY: Float,
    texWidth: Float, texHeight: Float,
    windowWidth: Float, windowHeight: Float
) : Renderer {
    val vertices = textCanvasVertices(glyphs, llX, llY, texWidth, texHeight, windowWidth, windowHeight)
    val mesh = Mesh.fromVertices(this.vao, vertices)
    this.addMesh(mesh)
    return this
}

fun Renderer.addTextureCanvas(): Renderer{
    val vertices = textureCanvas()
    val mesh = Mesh.fromVertices(this.vao, vertices.toFloatArray())
    this.addMesh(mesh)
    return this
}

fun Renderer.addMeshFrom2DPolygons(polygons: List<Polygon>, colors: List<Color>) : Renderer {
    val (vertices, indices) = from2DPolygons(polygons, colors)
    val mesh = Mesh.fromVertices(this.vao, vertices.toFloatArray(), indices.toIntArray())
    this.addMesh(mesh)
    return this
}



private fun unpackColor(color: Color): List<Float> {
    val red = color.red / 255f
    val green = color.green / 255f
    val blue = color.blue / 255f
    val alpha = color.alpha / 255f
    return listOf(red, green, blue, alpha)
}

fun basicCircle(nSegments: Int, color: Color): List<Float> {
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
        val tx = -y
        val ty = x

        x += tx * tangetialFactor
        y += ty * tangetialFactor

        x *= radialFactor
        y *= radialFactor

        vertices.add(x)
        vertices.add(y)

        vertices.addAll(colorFloats)
    }
    return vertices
}

fun basicRectangle(color: Color): List<Float> {
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
    return vertices
}

fun roundedCornerRectangle(color: Color, roundedness: Double, nSegments: Int, width: Float, height: Float): List<Float> {
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


        val tx = -y
        val ty = x

        x += tx * tangetialFactor
        y += ty * tangetialFactor

        x *= radialFactor
        y *= radialFactor

        positions.add(listOf(x*r*4+cx, y*r*4+cy))
    }

    val vertices = ArrayList<Float>(positions.size * 2 * 2)
    for (position in positions) {
        vertices.addAll(position)
        vertices.addAll(colorFloats)
    }
    return vertices
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

fun from2DPolygons(polygons: List<Polygon>, colors: List<Color>): Pair<List<Float>, List<Int>> {
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
    return vertices to indices
}

fun textureCanvas(): List<Float> {
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
    return vertices
}
