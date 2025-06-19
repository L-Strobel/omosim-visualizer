package de.uniwuerzburg.omodvisualizer.graphic

import org.joml.Matrix4f
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL31.glDrawArraysInstanced
import org.lwjgl.opengl.GL33.glVertexAttribDivisor
import org.lwjgl.system.MemoryUtil
import org.poly2tri.geometry.polygon.Polygon
import java.awt.Color
import java.nio.FloatBuffer

class Renderer(
    val vao: Vao = Vao(),
    private val instances: Int = 1,
    private val texture: Int? = null
) {
    lateinit var mesh: Mesh

    private val shaderProgramme = if (texture == null) {
        ShaderProgram(listOf("/2D.vert", "/monochrome.frag"))
    } else {
        ShaderProgram(listOf("/2DTexture.vert", "/texture.frag"))
    }

    // For instancing
    private var instVbo: Vbo? = null
    private var instFB: FloatBuffer? = null
    private var maxInstances: Int = 1

    fun addMesh(mesh: Mesh) {
        this.mesh = mesh

        vao.withBound {
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

    fun render(projection: Matrix4f = Matrix4f(), model: Matrix4f = Matrix4f()) {
        shaderProgramme.use()
        vao.withBound {
            if (texture != null) {
                glBindTexture(GL_TEXTURE_2D, texture)
            }

            shaderProgramme.addUniform(projection, "projection")
            shaderProgramme.addUniform(model, "model")

            // Draw
            if (mesh.ibo != null) {
                mesh.ibo!!.withBound {
                    glDrawElements(GL_TRIANGLES, mesh.indexSize, GL_UNSIGNED_INT,0)
                }
            } else {
                glDrawArrays (mesh.drawMode, 0, mesh.size)
            }
        }
    }

    fun renderInstanced(
        projection: Matrix4f = Matrix4f(), model: Matrix4f = Matrix4f(),
        positions: List<Pair<Float, Float>>
    ) {
        shaderProgramme.use()
        vao.withBound {
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

fun Renderer.addCircleMesh(color: Color, nSegments: Int = 20) : Renderer {
    val vertices = circle(color, nSegments)
    val mesh = Mesh.fromVertices(this.vao, vertices, GL_TRIANGLE_FAN)
    this.addMesh(mesh)
    return this
}

fun Renderer.addRectangleMesh(color: Color) : Renderer {
    val (vertices, indices) = rectangle(color)
    val mesh = Mesh.fromVertices(this.vao, vertices, indices)
    this.addMesh(mesh)
    return this
}

fun Renderer.addRoundedCornerRectangle(
    color: Color, roundness: Float = 0.2f, nSegments: Int = 20, width: Float = 1f, height: Float = 1f
) : Renderer {
    val (vertices, indices) = roundedCornerRectangle(color, roundness, nSegments, width, height)
    val mesh = Mesh.fromVertices(this.vao, vertices, indices)
    this.addMesh(mesh)
    return this
}

fun Renderer.addTextCanvas(
    glyphs: List<Glyph>,
    texWidth: Float, texHeight: Float,
    windowWidth: Float, windowHeight: Float
) : Renderer {
    val (vertices, indices) = textCanvas(glyphs, texWidth, texHeight, windowWidth, windowHeight)
    val mesh = Mesh.fromVertices(this.vao, vertices, indices)
    this.addMesh(mesh)
    return this
}

fun Renderer.addTextureCanvas(): Renderer {
    val (vertices, indices) = textureCanvas()
    val mesh = Mesh.fromVertices(this.vao, vertices, indices)
    this.addMesh(mesh)
    return this
}

fun Renderer.addMeshFrom2DPolygons(polygons: List<Polygon>, colors: List<Color>) : Renderer {
    val (vertices, indices) = from2DPolygons(polygons, colors)
    val mesh = Mesh.fromVertices(this.vao, vertices, indices)
    this.addMesh(mesh)
    return this
}