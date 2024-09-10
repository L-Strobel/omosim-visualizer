package de.uniwuerzburg.omodvisualizer

import org.joml.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.glBindVertexArray
import org.lwjgl.opengl.GL31.glDrawArraysInstanced

class Renderer(private val mesh: Mesh, instances: Int) {
    private val shaderProgramme = ShaderProgram(listOf("/2D.vert", "/monochrome.frag"))

    init {
        bindVAO()
        shaderProgramme.link()
        mesh.specifyAttributeArray(shaderProgramme)
        if (instances > 1) {
            mesh.enableInstancing(instances, shaderProgramme)
        }
        shaderProgramme.use()
        unbindVAO()
    }

    fun render(model: Matrix3x2f) {
        bindVAO()
        shaderProgramme.addUniform(model, "model")
        glDrawArrays (mesh.drawMode, 0, mesh.size)
        unbindVAO()
    }

    fun renderInstanced(model: Matrix3x2f, positions: List<Pair<Float, Float>>) {
        bindVAO()
        shaderProgramme.addUniform(model, "model")
        mesh.prepareInstancedDraw(positions)
        glDrawArraysInstanced (mesh.drawMode, 0, mesh.size, mesh.instances)
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