package de.uniwuerzburg.omodvisualizer.graphic

import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL30.glGenBuffers

class Ibo {
    val ref = glGenBuffers()

    fun bind() {
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ref)
    }

    fun unbind() {
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    fun close() {
        glDeleteBuffers(ref)
    }
}