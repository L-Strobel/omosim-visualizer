package de.uniwuerzburg.omodvisualizer

import org.joml.Matrix4f
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL30.*
import org.lwjgl.system.MemoryUtil

class DynTextRenderer(val window: Window, val font: Font) {
    private val shaderProgramme = ShaderProgram(listOf("/2DTexture.vert", "/texture.frag"))
    private val vao: Int = glGenVertexArrays()
    private val vbo: Int = glGenBuffers()
    private var nVertices: Int = 0
    private val vertices = MemoryUtil.memAllocFloat(4096);
    private val width: Int
    private val height: Int
    private val aspect = window.getAspect()

    init {
        bindVAO()
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        GL15.glBufferData(GL_ARRAY_BUFFER, (vertices.capacity() * 4).toLong(), GL_DYNAMIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        shaderProgramme.link()
        specifyAttributeArrayWTexture(shaderProgramme)
        shaderProgramme.use()
        unbindVAO()

        // Start window size
        val (w, h) = window.getCurrentWindowSize()
        width = w
        height = h
    }

    fun updateTextTo(text: CharSequence, llX: Float, llY: Float) {
        val txtVertices = Mesh.textCanvasVertices(
            text.map { font.glyphs[it]!! },
            llX * aspect, llY,
            font.textureWidth.toFloat(), font.textureHeight.toFloat(),
            width.toFloat(), height.toFloat()
        )

        vertices.clear()
        nVertices = 0

        nVertices += txtVertices.size
        vertices.put(txtVertices)
        vertices.rewind()
    }

    fun render(projection: Matrix4f, model: Matrix4f) {
        bindVAO()
        shaderProgramme.use()
        glBindTexture(GL_TEXTURE_2D, font.texture)
        shaderProgramme.addUniform(projection, "projection")
        shaderProgramme.addUniform(model, "model")
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        GL15.glBufferSubData(GL_ARRAY_BUFFER, 0, vertices)
        glDrawArrays (GL_TRIANGLES, 0, nVertices)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        vertices.clear()
        nVertices = 0
        unbindVAO()
    }

    private fun specifyAttributeArrayWTexture(shaderProgram: ShaderProgram) {
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        val posAttrib = glGetAttribLocation(shaderProgram.ref, "position")
        glEnableVertexAttribArray(posAttrib)
        glVertexAttribPointer(posAttrib, 2, GL_FLOAT, false, 8 * 4, 0)

        val colAttrib = glGetAttribLocation(shaderProgram.ref, "color")
        glEnableVertexAttribArray(colAttrib)
        glVertexAttribPointer(colAttrib, 4, GL_FLOAT, false, 8 * 4, 2 * 4)

        val texAttrib = glGetAttribLocation(shaderProgram.ref, "texcoord")
        glEnableVertexAttribArray(texAttrib)
        glVertexAttribPointer(texAttrib, 2, GL_FLOAT, false, 8 * 4, 6 * 4)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
    }

    private fun bindVAO() {
        glBindVertexArray(vao)
    }

    private fun unbindVAO() {
        glBindVertexArray(0)
    }

    fun close() {
        MemoryUtil.memFree(vertices)
        shaderProgramme.close()
        glDeleteVertexArrays(vao)
        glDeleteBuffers(vbo)
    }
}