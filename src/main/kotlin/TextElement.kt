package de.uniwuerzburg.omodvisualizer

import de.uniwuerzburg.omodvisualizer.graphic.Font
import de.uniwuerzburg.omodvisualizer.graphic.Renderer
import org.joml.Matrix4f

class TextElement(
    text: CharSequence,
    font: Font,
    val x: Float,
    val y: Float,
    val window: Window,
    alignment: Alignment,
    private val scale: Float = 1f
) {
    val renderer: Renderer
    val xAlignmentOffset: Float
    val measuredHeight: Float

    init {
        val (width, height) = window.getCurrentWindowSize()
        val glyphs = text.map { font.glyphs[it] ?: font.glyphs['?']!! }
        val measuredWidth = glyphs.sumOf { it.width }.toFloat() / (width / 2f)
        measuredHeight = glyphs.maxOf { it.height - font.fontSize / 3}.toFloat() / (height / 2f)
        renderer = font.staticTextRenderer(text)

        xAlignmentOffset = when(alignment) {
            Alignment.CENTER -> measuredWidth / 2f
            Alignment.RIGHT -> measuredWidth
            Alignment.LEFT -> 0f
        }
    }

    fun draw() {
        renderer.render(model = Matrix4f()
            .translate((x - xAlignmentOffset * scale) , (y - measuredHeight * scale / 2f) , 0f)
            .scale(scale, scale, 1f)
        )
    }

    fun close() {
        renderer.close()
    }
}

