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
        mesh.vao.bind()
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
        mesh.vao.unbind()
    }

    fun render(projection: Matrix4f, model: Matrix4f) {
        shaderProgramme.use()
        mesh.vao.bind()

        if (texture != null) {
            glBindTexture(GL_TEXTURE_2D, texture)
        }

        shaderProgramme.addUniform(projection, "projection")
        shaderProgramme.addUniform(model, "model")
        mesh.ibo!!.bind()
        glDrawElements(GL_TRIANGLES, mesh.indexSize, GL_UNSIGNED_INT,0)
        mesh.ibo!!.unbind()
        mesh.vao.unbind()
    }

    fun renderBasic(projection: Matrix4f, model: Matrix4f) {
        shaderProgramme.use()
        mesh.vao.bind()

        if (texture != null) {
            glBindTexture(GL_TEXTURE_2D, texture)
        }

        shaderProgramme.addUniform(projection, "projection")
        shaderProgramme.addUniform(model, "model")
        glDrawArrays (mesh.drawMode, 0, mesh.size)
        mesh.vao.unbind()
    }

    fun renderInstanced(projection: Matrix4f, model: Matrix4f, positions: List<Pair<Float, Float>>) {
        shaderProgramme.use()
        mesh.vao.bind()

        if (texture != null) {
            glBindTexture(GL_TEXTURE_2D, texture)
        }

        shaderProgramme.addUniform(projection, "projection")
        shaderProgramme.addUniform(model, "model")
        mesh.prepareInstancedDraw(positions)
        glDrawArraysInstanced (mesh.drawMode, 0, mesh.size, positions.size)
        mesh.cleanUpInstancedDraw()
        mesh.vao.unbind()
    }

    fun close() {
        shaderProgramme.close()
        mesh.close()
    }
}