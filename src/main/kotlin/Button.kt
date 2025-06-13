package de.uniwuerzburg.omodvisualizer

class Button (
    val centerX: Float,
    val centerY: Float,
    val halfWidth: Float,
    val onClick: () -> Unit,
    val renderer: Renderer
) {
    fun inBounds(xRel: Float, yRel: Float, aspect: Float) : Boolean {
        if (xRel <  (-1f+(centerX-halfWidth)*aspect)) return false
        if (xRel >= (-1f+(centerX+halfWidth)*aspect)) return false
        if (yRel <  (-1f+centerY-halfWidth)) return false
        if (yRel >= (-1f+centerY+halfWidth)) return false
        return true
    }
}