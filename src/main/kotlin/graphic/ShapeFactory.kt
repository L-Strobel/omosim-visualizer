package de.uniwuerzburg.omodvisualizer.graphic

import org.poly2tri.Poly2Tri
import org.poly2tri.geometry.polygon.Polygon
import java.awt.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.tan

private fun unpackColor(color: Color): List<Float> {
    val red = color.red / 255f
    val green = color.green / 255f
    val blue = color.blue / 255f
    val alpha = color.alpha / 255f
    return listOf(red, green, blue, alpha)
}

/**
 * Draw mode most be TRIANGLE_FAN
 */
fun circle(color: Color, nSegments: Int): FloatArray {
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
    return vertices.toFloatArray()
}

fun rectangle(color: Color): Pair<FloatArray, IntArray> {
    val colorFloats = unpackColor(color)
    val positions = rectangleCoords(-1f, 1f, -1f, 1f)
    val vertices = ArrayList<Float>(positions.size * 2 * 4)
    for (position in positions) {
        vertices.addAll(position)
        vertices.addAll(colorFloats)
    }
    val indices = listOf(0, 1, 3, 3, 1, 2)
    return vertices.toFloatArray() to indices.toIntArray()
}

private fun rectangleCoords(xMin: Float, xMax: Float, yMin: Float, yMax: Float) : List<List<Float>> {
    return listOf(
        listOf(xMin, yMin),
        listOf(xMin, yMax),
        listOf(xMax, yMax),
        listOf(xMax, yMin),
    )
}

fun roundedCornerRectangle(
    color: Color, roundness: Float, nSegments: Int, width: Float, height: Float
): Pair<FloatArray, IntArray> {
    val r = min(width, min(height, roundness.toFloat()))
    val cw = width - 2*r
    val ch = height - 2*r

    val colorFloats = unpackColor(color)
    val positions = mutableListOf<List<Float>>()
    val indices = mutableListOf<Int>()

    // Rectangle Vertices
    // Right side
    positions.addAll(
        rectangleCoords(
            -1f*r - cw - 1f*r,
            1f*r - cw - 1f*r,
            -1f*ch,
            1f*ch
        )
    )
    // Left side
    positions.addAll(
        rectangleCoords(
            -1f*r + cw + 1f*r,
            1f*r + cw + 1f*r,
            -1f*ch,
            1f*ch
        )
    )
    // Upper side
    val uCoords = rectangleCoords(
        -1f*cw,
        1f*cw,
        -1f*r + ch + 1f*r,
        1f*r + ch + 1f*r
    )
    positions.add(uCoords[1])
    positions.add(uCoords[2])
    // Lower side
    val lCoords = rectangleCoords(
        -1f*cw,
        1f*cw,
        -1f*r - ch - 1f*r,
        1f*r - ch - 1f*r
    )
    positions.add(lCoords[0])
    positions.add(lCoords[3])

    indices.addAll(
        listOf(
            0, 1, 3, 3, 1, 2, // left
            4, 5, 7, 7, 5, 6, // right
            2, 8, 5, 5, 8, 9, // upper
            10, 3, 11, 11, 3, 4, // lower
            3, 2, 4, 4, 2, 5 // center
        )
    )

    // Corners
    val theta = PI * 2 / 4 / nSegments
    val tangetialFactor = tan(theta).toFloat()
    val radialFactor = cos(theta).toFloat()

    // Center
    var mxIndex = 11
    var lastIndex = 6
    var x = 0.5f
    var y = 0f

    var cx = cw
    var cy = ch
    var centerIdx = 5
    for (i in 0 until nSegments*4) {
        if (i == nSegments) {
            cx = -cw
            lastIndex = 8
            centerIdx = 2
        }
        if (i == nSegments*2) {
            cy = -ch
            lastIndex = 0
            centerIdx = 3
        }
        if (i == nSegments*3) {
            cx = cw
            lastIndex = 11
            centerIdx = 4
        }

        val tx = -y
        val ty = x

        x += tx * tangetialFactor
        y += ty * tangetialFactor

        x *= radialFactor
        y *= radialFactor

        positions.add(listOf(x*r*4+cx, y*r*4+cy))
        indices.add(centerIdx)
        indices.add(mxIndex + 1)
        indices.add(lastIndex)
        mxIndex += 1
        lastIndex = mxIndex
    }

    // Add colors
    val vertices = ArrayList<Float>(positions.size * 2 * 4)
    for (position in positions) {
        vertices.addAll(position)
        vertices.addAll(colorFloats)
    }
    return vertices.toFloatArray() to indices.toIntArray()
}

fun textCanvas (
    glyphs: List<Glyph>,
    texWidth: Float, texHeight: Float,
    windowWidth: Float, windowHeight: Float
): Pair<FloatArray, IntArray> {
    val colorFloats = unpackColor(Color.WHITE)
    val vertices = ArrayList<Float>(glyphs.size * 4 * (2 + 3 + 4))
    val indices = ArrayList<Int>(glyphs.size * 6)
    var xOffset = 0f
    var iOffset = 0
    for (glyph in glyphs) {
        // Window position
        val ww = glyph.width.toFloat() / (windowWidth / 2f)
        val wh = glyph.height.toFloat() / (windowHeight / 2f)
        val positions =  rectangleCoords(xOffset, ww+xOffset, 0f, wh)

        // Atlas position
        val gx = glyph.x.toFloat() / texWidth
        val gy = glyph.y.toFloat() / texHeight
        val gw = glyph.width.toFloat() / texWidth
        val gh = glyph.height.toFloat() / texHeight
        val texPositions =  rectangleCoords(gx, gx+gw, gy, gy+gh)

        for ((position, texPosition) in positions.zip(texPositions)) {
            vertices.addAll(position)
            vertices.addAll(colorFloats)
            vertices.addAll(texPosition)
        }
        val newIndices = listOf(0, 1, 3, 3, 1, 2).map { it + iOffset }
        indices.addAll(newIndices)

        iOffset += 4
        xOffset += ww
    }
    return vertices.toFloatArray() to indices.toIntArray()
}


fun textureCanvas(): Pair<FloatArray, IntArray> {
    val colorFloats = unpackColor(Color.WHITE)
    val positions =  rectangleCoords(-1f, 1f, -1f, 1f)
    val texPositions =  rectangleCoords(0f, 1f, 0f, 1f)
    val vertices = ArrayList<Float>(positions.size * (2 + 3 + 2))
    for ((position, texPosition) in positions.zip(texPositions)) {
        vertices.addAll(position)
        vertices.addAll(colorFloats)
        vertices.addAll(texPosition)
    }
    val indices = listOf(0, 1, 3, 3, 1, 2)
    return vertices.toFloatArray() to indices.toIntArray()
}

fun from2DPolygons(polygons: List<Polygon>, colors: List<Color>): Pair<FloatArray, IntArray> {
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
    return vertices.toFloatArray() to indices.toIntArray()
}
