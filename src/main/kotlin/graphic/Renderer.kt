package de.uniwuerzburg.omodvisualizer.graphic

import org.joml.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.glBindVertexArray
import org.lwjgl.opengl.GL31.glDrawArraysInstanced

class Renderer(
    private val mesh: Mesh,
    instances: Int,
    private val texture: Int?
) {
    private val shaderProgramme = if (texture == null) {
        ShaderProgram(listOf("/2D.vert", "/monochrome.frag"))
    } else {
        ShaderProgram(listOf("/2DTexture.vert", "/texture.frag"))
    }

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
                mesh.enableInstancing(instances, shaderProgramme)
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
            mesh.prepareInstancedDraw(positions)
            glDrawArraysInstanced (mesh.drawMode, 0, mesh.size, positions.size)
            mesh.cleanUpInstancedDraw()
        }
    }

    fun close() {
        shaderProgramme.close()
        mesh.close()
    }
}