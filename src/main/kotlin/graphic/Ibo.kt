package de.uniwuerzburg.omosimvisualizer.graphic

import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL30.glGenBuffers

class Ibo {
    val ref = glGenBuffers()

    private fun bind() {
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ref)
    }

    private fun unbind() {
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    fun close() {
        glDeleteBuffers(ref)
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