package de.uniwuerzburg.omosimvisualizer

import de.uniwuerzburg.omosimvisualizer.graphic.Renderer
import org.joml.Matrix4f

class Button (
    val centerX: Float,
    val centerY: Float,
    val halfWidth: Float,
    val onClick: (Button) -> Unit,
    val renderer: Renderer,
    val borderRenderer: Renderer?,
    var active: Boolean = true
) {
    fun inBounds(xRel: Float, yRel: Float) : Boolean {
        if (xRel <  (centerX-halfWidth)) return false
        if (xRel >= (centerX+halfWidth)) return false
        if (yRel <  (centerY-halfWidth)) return false
        if (yRel >= (centerY+halfWidth)) return false
        return true
    }

    fun draw(aspect: Float) {
        borderRenderer?.render(
            model = Matrix4f()
                .translate(centerX, centerY, 0f)
                .scale((halfWidth*1.3f) * aspect , (halfWidth*1.3f) , 1f)
        )
        if (active) {
            renderer.render(
                model = Matrix4f()
                    .translate(centerX, centerY, 0f)
                    .scale(halfWidth * aspect, halfWidth, 1f)
            )
        }
    }

    fun close() {
        renderer.close()
    }
}