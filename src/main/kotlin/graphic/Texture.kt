package de.uniwuerzburg.omosimvisualizer.graphic

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.glGenerateMipmap
import org.lwjgl.stb.STBImage.*
import org.lwjgl.system.MemoryStack.stackPush
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.nio.channels.Channels

class Texture {
    companion object {
        fun loadTexture(resource: String) : Int {
            // Load image
            var width = 0
            var height = 0
            var image: ByteBuffer? = null

            // Load asset
            val res = this::class.java.classLoader.getResource(resource)?.openStream()!!
            val rBytes = res.available()
            val buffer = BufferUtils.createByteBuffer(rBytes)
            val channel = Channels.newChannel(res)
            channel.read(buffer)
            buffer.rewind()

            stackPush().use { stack ->
                val w: IntBuffer = stack.mallocInt(1)
                val h: IntBuffer = stack.mallocInt(1)
                val comp: IntBuffer = stack.mallocInt(1)
                stbi_set_flip_vertically_on_load(true)
                image = stbi_load_from_memory(buffer, w, h, comp, 4)
                    ?: throw java.lang.RuntimeException((("Failed to load a texture file!"
                            + System.lineSeparator() + stbi_failure_reason())))

                width = w.get()
                height = h.get()
            }

            // Create Texture
            val texture = glGenTextures()
            glBindTexture(GL_TEXTURE_2D, texture)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, image)
            glGenerateMipmap(GL_TEXTURE_2D)

            stbi_image_free(image!!)
            return texture
        }
    }
}