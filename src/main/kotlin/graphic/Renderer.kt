package de.uniwuerzburg.omodvisualizer.graphic

import org.joml.Matrix4f
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL31.glDrawArraysInstanced
import org.lwjgl.opengl.GL33.glVertexAttribDivisor
import org.lwjgl.system.MemoryUtil
import java.nio.FloatBuffer

class Renderer(
    private val mesh: Mesh,
    instances: Int = 1,
    private val texture: Int? = null
) {
    private val shaderProgramme = if (texture == null) {
        ShaderProgram(listOf("/2D.vert", "/monochrome.frag"))
    } else {
        ShaderProgram(listOf("/2DTexture.vert", "/texture.frag"))
    }

    // For instancing
    private var instVbo: Vbo? = null
    private var instFB: FloatBuffer? = null
    private var maxInstances: Int = 1

    init {
        mesh.vao.withBound {
            shaderProgramme.link()
            if (texture != null) {
                specifyAttributeArrayWTexture(mesh.vbo, shaderProgramme)
                shaderProgramme.setTextureUniform()
            } else {
                specifyAttributeArray(mesh.vbo, shaderProgramme)
            }

            if (instances > 1) {
                enableInstancing(instances, shaderProgramme)
            }
            shaderProgramme.use()
        }
    }

    fun render(projection: Matrix4f, model: Matrix4f) {
        shaderProgramme.use()
        mesh.vao.withBound {
            if (texture != null) {
                glBindTexture(GL_TEXTURE_2D, texture)
            }

            shaderProgramme.addUniform(projection, "projection")
            shaderProgramme.addUniform(model, "model")
            mesh.ibo!!.withBound {
                glDrawElements(GL_TRIANGLES, mesh.indexSize, GL_UNSIGNED_INT,0)
            }
        }
    }

    fun renderBasic(projection: Matrix4f, model: Matrix4f) {
        shaderProgramme.use()
        mesh.vao.withBound {
            if (texture != null) {
                glBindTexture(GL_TEXTURE_2D, texture)
            }

            shaderProgramme.addUniform(projection, "projection")
            shaderProgramme.addUniform(model, "model")
            glDrawArrays (mesh.drawMode, 0, mesh.size)
        }
    }

    fun renderInstanced(projection: Matrix4f, model: Matrix4f, positions: List<Pair<Float, Float>>) {
        shaderProgramme.use()
        mesh.vao.withBound {
            if (texture != null) {
                glBindTexture(GL_TEXTURE_2D, texture)
            }

            shaderProgramme.addUniform(projection, "projection")
            shaderProgramme.addUniform(model, "model")
            prepareInstancedDraw(positions)
            glDrawArraysInstanced (mesh.drawMode, 0, mesh.size, positions.size)
            cleanUpInstancedDraw()
        }
    }

    private fun prepareInstancedDraw(positions: List<Pair<Float, Float>>) {
        require(positions.size <= maxInstances)

        // Fill buffer with new instance locations
        for ((x, y) in positions) {
            instFB!!.put(x)
            instFB!!.put(y)
        }
        instFB!!.rewind()
        instVbo?.withBound {
            glBufferSubData(GL_ARRAY_BUFFER, 0, instFB!!)
        }
    }

    private fun cleanUpInstancedDraw() {
        instFB!!.clear()
    }

    private fun enableInstancing(maxInstances: Int, shaderProgram: ShaderProgram) {
        if(maxInstances <= 1) {
            throw IllegalStateException("Don't enable instancing with maximum one object!")
        }
        this.maxInstances = maxInstances
        instVbo = Vbo()

        val offAttrib = glGetAttribLocation(shaderProgram.ref, "offset")
        instVbo?.withBound {
            instFB = MemoryUtil.memAllocFloat(maxInstances * 2)
            glBufferData(GL_ARRAY_BUFFER, (maxInstances * 2 * 4).toLong(), GL_DYNAMIC_DRAW)
            glEnableVertexAttribArray(offAttrib)
            glVertexAttribPointer(offAttrib, 2, GL_FLOAT, false, 2 * 4, 0)
        }
        glVertexAttribDivisor(offAttrib, 1)
    }

    fun close() {
        MemoryUtil.memFree(instFB)
        shaderProgramme.close()
        mesh.close()
    }
}