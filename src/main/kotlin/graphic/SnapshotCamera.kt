package de.uniwuerzburg.omodvisualizer.graphic

import de.uniwuerzburg.omodvisualizer.Window
import org.joml.Matrix4f
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW.glfwSwapBuffers
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30.*
import org.lwjgl.stb.STBImageWrite.stbi_flip_vertically_on_write
import org.lwjgl.stb.STBImageWrite.stbi_write_png
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO

@Suppress("unused")
object SnapshotCamera {
    /**
     *  Save the rendered pixels in an .png image using a framebuffer.
     *  Allows resolutions different from the screen resolution.
     *
     *  Currently, the colors of the image are somewhat off. I blame the alpha channel but am unsure.
     */
    private fun fbSnapShot(window: Window, zoom: Float, right: Float, up: Float, renderer: Renderer) {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        // Get output image size: here 4 x the screen size
        val (widthw, heightw) = window.getCurrentWindowSize()
        val width = widthw * 4
        val height = heightw * 4

        // Create texture to draw to
        val texture = glGenTextures()
        glBindTexture(GL_TEXTURE_2D, texture)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0)

        // Create renderbuffer
        val rbo = glGenRenderbuffers()
        glBindRenderbuffer(GL_RENDERBUFFER, rbo)
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height)

        // Create framebuffer
        val fb = glGenFramebuffers()
        glBindFramebuffer(GL_FRAMEBUFFER, fb)
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0)
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, rbo)
        glBindFramebuffer(GL_FRAMEBUFFER, GL_NONE)

        // Check that framebuffer is ok
        if(glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw RuntimeException("Creating framebuffer for snapshot failed!")
        }

        // Render to framebuffer
        glBindFramebuffer(GL_FRAMEBUFFER, fb)
        glViewport(0,0, width, height) // Render on the whole framebuffer, from the lower left corner to the upper right
        val projection = Matrix4f().ortho2D(-zoom + right, zoom + right, -zoom + up, zoom + up)
        renderer.render( projection, Matrix4f()) // Draw call
        glBindFramebuffer(GL_FRAMEBUFFER, GL_NONE)

        // Get pixel data and save it in an .png
        val data = BufferUtils.createByteBuffer(width * height * 4)
        glBindTexture(GL_TEXTURE_2D, texture)
        glPixelStorei(GL_PACK_ALIGNMENT, 1) // Not sure if important
        glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA, GL_UNSIGNED_BYTE, data)
        stbi_flip_vertically_on_write(true)
        val success = stbi_write_png("snapshot.png", width, height, 4, data, width * 4)
        if (!success) {
            println("Couldn't save")
        }
        glfwSwapBuffers(window.ref) // swap the color buffers
        glDeleteFramebuffers(fb) // Untested
    }

    /**
     * Save the rendered pixels in an .png image
     */
    private fun snapshot(window: Window, zoom: Float, right: Float, up: Float, renderer: Renderer) {
        // Draw operations start
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        val projection = Matrix4f().ortho2D(-zoom + right, zoom + right, -zoom + up, zoom + up)
        renderer.render(projection, Matrix4f()) // Draw image
        glfwSwapBuffers(window.ref) // swap the color buffers
        // Draw Operations end

        // Store pixels in image
        val (width, height) = window.getCurrentWindowSize()

        val bpp = 4 // Assuming a 32-bit display with a byte each for red, green, blue, and alpha.
        val buffer = BufferUtils.createByteBuffer(width * height * bpp)
        GL11.glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buffer)

        //Save the screen image
        val file = File("snapshot.png")
        val format = "png"
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val i = (x + (width * y)) * bpp

                val r: Int = buffer.get(i).toInt() and 0xFF
                val g: Int = buffer.get(i + 1).toInt() and 0xFF
                val b: Int = buffer.get(i + 2).toInt() and 0xFF
                image.setRGB(x, height - (y + 1), (0xFF shl 24) or (r shl 16) or (g shl 8) or b)
            }
        }

        try {
            ImageIO.write(image, format, file)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}