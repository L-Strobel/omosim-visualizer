package de.uniwuerzburg.omodvisualizer.graphic

import de.uniwuerzburg.omodvisualizer.Window
import org.joml.Matrix4f
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL30.*
import org.lwjgl.system.MemoryUtil

class DynTextRenderer(private val window: Window, private val font: Font) {
    private val shaderProgramme = ShaderProgram(listOf("/2DTexture.vert", "/texture.frag"))
    private val vao: Vao = Vao()
    private val vbo: Vbo = Vbo()
    private val ibo: Ibo = Ibo()
    private var nVertices: Int = 0
    private var nIndices: Int = 0
    private val vertices = MemoryUtil.memAllocFloat(4096)
    private val indices = MemoryUtil.memAllocInt(4096)
    private val width: Int
    private val height: Int
    private val aspect = window.getAspect()

    init {
        vao.withBound {
            // Reserve memory
            vbo.withBound {
                GL15.glBufferData(GL_ARRAY_BUFFER, (vertices.capacity() * 4).toLong(), GL_DYNAMIC_DRAW)
            }
            ibo.withBound {
                GL15.glBufferData(GL_ELEMENT_ARRAY_BUFFER, (indices.capacity() * 4).toLong(), GL_DYNAMIC_DRAW)
            }

            // Prepare shader
            shaderProgramme.link()
            specifyAttributeArrayWTexture(vbo, shaderProgramme)
            shaderProgramme.use()
        }

        // Start window size
        val (w, h) = window.getCurrentWindowSize()
        width = w
        height = h
    }

    fun updateTextTo(text: CharSequence) {
        // Clear old data
        vertices.clear()
        nVertices = 0
        indices.clear()
        nIndices = 0

        // Create text canvas
        val glyphs = text.map { font.glyphs[it] ?: font.glyphs['?']!! }
        val (rVertices, rIndices) = textCanvas(
            glyphs,
            font.textureWidth.toFloat(), font.textureHeight.toFloat(),
            width.toFloat(), height.toFloat()
        )

        // Store canvas in buffer
        nVertices += rVertices.size
        vertices.put(rVertices)
        vertices.rewind()

        nIndices += rIndices.size
        indices.put(rIndices)
        indices.rewind()
    }

    fun render(projection: Matrix4f = Matrix4f(), model: Matrix4f = Matrix4f()) {
        vao.withBound {
            shaderProgramme.use()
            glBindTexture(GL_TEXTURE_2D, font.texture)

            // Update uniforms
            shaderProgramme.addUniform(projection, "projection")
            shaderProgramme.addUniform(model, "model")

            vbo.withBound {
                GL15.glBufferSubData(GL_ARRAY_BUFFER, 0, vertices) // Upload new data
            }
            ibo.withBound {
                GL15.glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, indices) // Upload new data
                glDrawElements(GL_TRIANGLES, nIndices, GL_UNSIGNED_INT, 0)
            }
        }
    }

    fun close() {
        MemoryUtil.memFree(vertices)
        MemoryUtil.memFree(indices)
        shaderProgramme.close()
        vbo.close()
        ibo.close()
        vao.close()
    }
}