package de.uniwuerzburg.omodvisualizer

import org.joml.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.glBindVertexArray
import org.lwjgl.opengl.GL31.glDrawArraysInstanced

class Renderer(private val mesh: Mesh, instances: Int) {
    private val shaderProgramme = ShaderProgram(listOf("/2D.vert", "/monochrome.frag"))

    init {
        bind()
        glBindBuffer(GL_ARRAY_BUFFER, mesh.vbo);
        shaderProgramme.link()
        mesh.specifyAttributeArray(shaderProgramme)
        mesh.enableInstancing(instances, shaderProgramme)
        shaderProgramme.use()
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        unbind()

        /*
        shaderProgramme.addUniform(0.01f, "radius")
        shaderProgramme.addUniform(Vector2i(windowRes.first, windowRes.second), "resolution")
        */
    }

    fun render(model: Matrix3x2f, positions: List<Pair<Float, Float>>) {
        bind()
        shaderProgramme.addUniform(model, "model")
        mesh.prepareDraw(positions)
        //glDrawArrays (mesh.drawMode, 0, mesh.size)
        glDrawArraysInstanced (mesh.drawMode, 0, mesh.size, mesh.instances!!)
        mesh.cleanUpDraw()
        unbind()
    }

    private fun bind() {
        glBindVertexArray(mesh.vao)
    }

    private fun unbind() {
        glBindVertexArray(0)
    }

    fun close() {
        shaderProgramme.close()
        mesh.close()
    }
}