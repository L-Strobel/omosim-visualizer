package de.uniwuerzburg.omodvisualizer.graphic

import org.lwjgl.opengl.GL30.*

class Vao {
    private val ref: Int = glGenVertexArrays()

    fun bind() {
        glBindVertexArray(ref)
    }

    fun unbind() {
        glBindVertexArray(0)
    }

    fun close() {
        glDeleteVertexArrays(ref)
    }
}