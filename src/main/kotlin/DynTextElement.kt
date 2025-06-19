package de.uniwuerzburg.omodvisualizer

import de.uniwuerzburg.omodvisualizer.graphic.DynTextRenderer
import de.uniwuerzburg.omodvisualizer.graphic.Font
import org.joml.Matrix4f

class DynTextElement (
    text: CharSequence,
    val font: Font,
    val x: Float,
    val y: Float,
    val window: Window,
    alignment: Alignment
) {
    val renderer: DynTextRenderer
    var xAlignmentOffset: Float = 0f
    var measuredHeight: Float = 0f

    init {
        renderer = DynTextRenderer(window, font)
        renderer.updateTextTo(text)
        align(text, alignment)
    }

    fun draw() {
        renderer.render(model = Matrix4f().translate(x - xAlignmentOffset, y - measuredHeight / 2f, 0f))
    }

    fun update(text: CharSequence, alignment: Alignment) {
        renderer.updateTextTo(text)
        align(text, alignment)
    }

    private fun align(text: CharSequence, alignment: Alignment) {
        val (width, height) = window.getCurrentWindowSize()
        val glyphs = text.map { font.glyphs[it] ?: font.glyphs['?']!! }
        if (glyphs.isEmpty()) { return }

        val measuredWidth = glyphs.sumOf { it.width }.toFloat() / (width / 2f)
        measuredHeight = glyphs.maxOf { it.height - font.fontSize / 3}.toFloat() / (height / 2f)

        xAlignmentOffset = when(alignment) {
            Alignment.CENTER -> measuredWidth / 2f
            Alignment.RIGHT -> measuredWidth
            Alignment.LEFT -> 0f
        }
    }

    fun close() {
        renderer.close()
    }
}