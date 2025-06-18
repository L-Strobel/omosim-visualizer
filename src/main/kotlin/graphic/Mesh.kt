package de.uniwuerzburg.omodvisualizer.graphic

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL30.*
import org.lwjgl.system.MemoryStack.stackPush
import java.io.FileInputStream
import java.io.FileOutputStream

class Mesh(
    val drawMode: Int,
    var vbo: Vbo,
    val size: Int,
    val ibo: Ibo? = null,
    val indexSize: Int = 0
) {
    fun close() {
        vbo.close()
        ibo?.close()
    }

    fun save(fn: String) {
        require(ibo != null)

        // Save vertices
        stackPush().use { _ ->
            val buffer = BufferUtils.createByteBuffer(this.size * 4)
            vbo.withBound {
                glGetBufferSubData(GL_ARRAY_BUFFER, 0, buffer)
            }
            val fos = FileOutputStream("${fn}_vertices")
            val channel = fos.channel
            channel.write(buffer)
            fos.close()
        }

        // Save indices
        stackPush().use { _ ->
            val buffer = BufferUtils.createByteBuffer(this.indexSize * 4)
            ibo.withBound {
                glGetBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, buffer)
            }
            val fos = FileOutputStream("${fn}_indices")
            val channel = fos.channel
            channel.write(buffer)
            fos.close()
        }
    }

    companion object {
        fun fromVertices(vao: Vao, vertices: FloatArray, drawMode: Int = GL_TRIANGLES): Mesh {
            val vbo = Vbo()
            vao.withBound {
                stackPush().use { _ ->
                    val buffer = BufferUtils.createFloatBuffer(vertices.size)
                    buffer.put(vertices)
                    buffer.rewind()

                    vbo.withBound {
                        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW)
                    }
                }
            }
            return Mesh(drawMode, vbo, vertices.size)
        }

        fun fromVertices(vao: Vao, vertices: FloatArray, indices: IntArray, drawMode: Int = GL_TRIANGLES): Mesh {
            val vbo = Vbo()
            val ibo = Ibo()
            vao.withBound {
                stackPush().use { _ ->
                    val buffer = BufferUtils.createFloatBuffer(vertices.size)
                    buffer.put(vertices)
                    buffer.rewind()
                    vbo.withBound {
                        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW)
                    }
                }
                stackPush().use { _ ->
                    val buffer = BufferUtils.createIntBuffer(indices.size)
                    buffer.put(indices)
                    buffer.rewind()

                    ibo.withBound {
                        glBufferData(GL_ELEMENT_ARRAY_BUFFER, buffer, GL_STATIC_DRAW)
                    }
                }
            }
            return Mesh(drawMode, vbo, vertices.size,ibo, indices.size)
        }

        fun load(vao: Vao, fn: String) : Mesh {
            val drawMode: Int = GL_TRIANGLES

            // Input sources
            val fisv = FileInputStream("${fn}_vertices")
            val vBytes = fisv.available()
            val fisi = FileInputStream("${fn}_indices")
            val iBytes = fisi.available()

            // Load to buffers
            val vbo = Vbo()
            val ibo = Ibo()
            vao.withBound {
                // Vertices
                val vBuffer = BufferUtils.createByteBuffer(vBytes)
                val vChannel = fisv.channel
                vChannel.read(vBuffer)
                vBuffer.rewind()

                vbo.withBound {
                    glBufferData(GL_ARRAY_BUFFER, vBuffer, GL_STATIC_DRAW)
                }

                // Index
                val iBuffer = BufferUtils.createByteBuffer(vBytes)
                val iChannel = fisi.channel
                iChannel.read(iBuffer)
                iBuffer.rewind()

                ibo.withBound {
                    glBufferData(GL_ELEMENT_ARRAY_BUFFER, iBuffer, GL_STATIC_DRAW)
                }
            }
            return Mesh(drawMode, vbo, (vBytes / 4), ibo, iBytes / 4)
        }
    }
}