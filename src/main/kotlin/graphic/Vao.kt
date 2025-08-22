package de.uniwuerzburg.omosimvisualizer.graphic

import org.lwjgl.opengl.GL30.*

class Vao {
    private val ref: Int = glGenVertexArrays()

    private fun bind() {
        glBindVertexArray(ref)
    }

    private fun unbind() {
        glBindVertexArray(0)
    }

    fun close() {
        glDeleteVertexArrays(ref)
    }

    /**
     * Do the given action with the buffer bound.
     */
    fun withBound (action: () -> Unit) {
        bind()
        action()
        unbind()
    }
}