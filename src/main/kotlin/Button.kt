package de.uniwuerzburg.omodvisualizer

import de.uniwuerzburg.omodvisualizer.graphic.Renderer

class Button (
    val centerX: Float,
    val centerY: Float,
    val halfWidth: Float,
    val onClick: () -> Unit,
    val renderer: Renderer,
    val borderRenderer: Renderer,
    val active: Boolean = true
) {
    fun inBounds(xRel: Float, yRel: Float) : Boolean {
        if (xRel <  (centerX-halfWidth)) return false
        if (xRel >= (centerX+halfWidth)) return false
        if (yRel <  (centerY-halfWidth)) return false
        if (yRel >= (centerY+halfWidth)) return false
        return true
    }

    fun close() {
        renderer.close()
    }
}