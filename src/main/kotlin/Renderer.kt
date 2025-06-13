package de.uniwuerzburg.omodvisualizer

import org.joml.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.glBindVertexArray
import org.lwjgl.opengl.GL31.glDrawArraysInstanced

class Renderer(
    private val mesh: Mesh,
    instances: Int,
    private val textureImgFN: String? = null,
    val testFont: Boolean = false
) {
    private val shaderProgramme = if (textureImgFN == null ) {
        ShaderProgram(listOf("/2D.vert", "/monochrome.frag"))
    } else {
        ShaderProgram(listOf("/2DTexture.vert", "/texture.frag"))
    }

    init {
        bindVAO()
        shaderProgramme.link()
        if (testFont) {
            mesh.specifyAttributeArrayWTexture(shaderProgramme)
            shaderProgramme.setTextureUniform()
        } else if (textureImgFN == null) {
            mesh.specifyAttributeArray(shaderProgramme)
        } else {
            mesh.specifyAttributeArrayWTexture(shaderProgramme)
            Texture.loadTexture(textureImgFN)
            shaderProgramme.setTextureUniform()
        }

        if (instances > 1) {
            mesh.enableInstancing(instances, shaderProgramme)
        }
        shaderProgramme.use()
        unbindVAO()
    }

    fun render(projection: Matrix4f, model: Matrix4f) {
        shaderProgramme.use() // TODO Bind texture here? Maybe if I have more than one
        bindVAO()
        shaderProgramme.addUniform(projection, "projection")
        shaderProgramme.addUniform(model, "model")
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, mesh.vboIdx!!)
        glDrawElements(GL_TRIANGLES, mesh.indexSize, GL_UNSIGNED_INT,0)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
        unbindVAO()
    }

    fun renderBasic(projection: Matrix4f, model: Matrix4f) {
        shaderProgramme.use()
        bindVAO()
        shaderProgramme.addUniform(projection, "projection")
        shaderProgramme.addUniform(model, "model")
        glDrawArrays (mesh.drawMode, 0, mesh.size)
        unbindVAO()
    }

    fun renderInstanced(projection: Matrix4f, model: Matrix4f, positions: List<Pair<Float, Float>>) {
        shaderProgramme.use()
        bindVAO()
        shaderProgramme.addUniform(projection, "projection")
        shaderProgramme.addUniform(model, "model")
        mesh.prepareInstancedDraw(positions)
        glDrawArraysInstanced (mesh.drawMode, 0, mesh.size, positions.size)
        mesh.cleanUpInstancedDraw()
        unbindVAO()
    }

    private fun bindVAO() {
        glBindVertexArray(mesh.vao)
    }

    private fun unbindVAO() {
        glBindVertexArray(0)
    }

    fun close() {
        shaderProgramme.close()
        mesh.close()
    }
}