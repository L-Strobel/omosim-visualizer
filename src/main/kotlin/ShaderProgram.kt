package de.uniwuerzburg.omodvisualizer

import org.joml.Matrix3x2f
import org.joml.Vector2i
import org.lwjgl.opengl.GL20.*
import org.lwjgl.system.MemoryStack.stackPush

class ShaderProgram(shaders: List<String>) {
    val ref: Int = glCreateProgram()
    private val shaderRefs: MutableList<Int> = mutableListOf()

    init {
        for (shader in shaders) {
            addShader(shader)
        }
    }

    fun addUniform(matrix4f: Matrix3x2f, name: String) {
        stackPush().use { stack ->
            val fb = matrix4f.get3x3(stack.mallocFloat(9))
            val uniModel = glGetUniformLocation(ref, name)
            glUniformMatrix3fv(uniModel, false, fb)
        }
    }

    private fun addShader(shaderFN: String) {
        val shaderType = when(val fileEnding = shaderFN.split(".").last()) {
            "vert" -> GL_VERTEX_SHADER
            "frag" -> GL_FRAGMENT_SHADER
            else -> throw IllegalArgumentException("Unknown shader type $fileEnding")
        }

        // Compile shader
        val shader = glCreateShader(shaderType)
        shaderRefs.add(shader)
        val shaderSrc = {}.javaClass.getResource(shaderFN)?.readText()!!
        glShaderSource(shader, shaderSrc)
        glCompileShader(shader)

        // Check if compiling worked
        if (glGetShaderi(shader, GL_COMPILE_STATUS) != GL_TRUE) {
            throw RuntimeException(glGetShaderInfoLog(shader))
        }

        glAttachShader(ref, shader)
    }

    fun link() {
        glLinkProgram(ref)

        if (glGetProgrami(ref, GL_LINK_STATUS) != GL_TRUE) {
            throw RuntimeException(glGetProgramInfoLog(ref))
        }

        glValidateProgram(ref)

        if (glGetProgrami(ref, GL_VALIDATE_STATUS) == 0) {
            throw RuntimeException(glGetProgramInfoLog(ref))
        }
    }

    fun use() {
        glUseProgram (ref)
    }

    /**
     * Delete program and its shaders
     */
    fun close() {
        for (shader in shaderRefs) {
            glDeleteShader(shader)
        }
        glDeleteProgram(ref)
    }
}