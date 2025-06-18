package de.uniwuerzburg.omodvisualizer.graphic

import de.uniwuerzburg.omodvisualizer.Window
import org.joml.Matrix4f
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL30.*
import org.lwjgl.system.MemoryUtil

class DynTextRenderer(val window: Window, val font: Font) {
    private val shaderProgramme = ShaderProgram(listOf("/2DTexture.vert", "/texture.frag"))
    private val vao: Vao = Vao()
    private val vbo: Vbo = Vbo()
    private var nVertices: Int = 0
    private val vertices = MemoryUtil.memAllocFloat(4096);
    private val width: Int
    private val height: Int
    private val aspect = window.getAspect()

    init {
        vao.bind()

        // Reserve memory
        vbo.bind()
        GL15.glBufferData(GL_ARRAY_BUFFER, (vertices.capacity() * 4).toLong(), GL_DYNAMIC_DRAW)
        vbo.unbind()

        // Prepare shader
        shaderProgramme.link()
        specifyAttributeArrayWTexture(vbo.ref, shaderProgramme)
        shaderProgramme.use()

        vao.unbind()

        // Start window size
        val (w, h) = window.getCurrentWindowSize()
        width = w
        height = h
    }

    fun updateTextTo(text: CharSequence, llX: Float, llY: Float) {
        // Clear old data
        vertices.clear()
        nVertices = 0

        // Create text canvas
        val txtVertices = Mesh.textCanvasVertices(
            text.map { font.glyphs[it]!! },
            llX * aspect, llY,
            font.textureWidth.toFloat(), font.textureHeight.toFloat(),
            width.toFloat(), height.toFloat()
        )

        // Store canvas in buffer
        nVertices += txtVertices.size
        vertices.put(txtVertices)
        vertices.rewind()
    }

    fun render(projection: Matrix4f, model: Matrix4f) {
        vao.bind()
        shaderProgramme.use()
        glBindTexture(GL_TEXTURE_2D, font.texture)

        // Update uniforms
        shaderProgramme.addUniform(projection, "projection")
        shaderProgramme.addUniform(model, "model")

        // Upload new data
        vbo.bind()
        GL15.glBufferSubData(GL_ARRAY_BUFFER, 0, vertices)

        // Draw
        glDrawArrays (GL_TRIANGLES, 0, nVertices)
        vbo.unbind()

        vao.unbind()
    }


    fun close() {
        MemoryUtil.memFree(vertices)
        shaderProgramme.close()
        vbo.close()
        vao.close()
    }
}