package de.uniwuerzburg.omodvisualizer

import org.joml.Matrix4f
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.*
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil
import java.awt.Color
import java.awt.Font
import java.awt.Font.MONOSPACED
import java.awt.Font.PLAIN
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.lang.String
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import kotlin.Boolean
import kotlin.Char
import kotlin.CharSequence
import kotlin.Float
import kotlin.Int
import kotlin.IntArray
import kotlin.math.max
import kotlin.use


class Glyph(val width: Int, val height: Int, val x: Int, val y: Int)

class Font(
    val window: Window
) {
    val glyphs: MutableMap<Char, Glyph> = mutableMapOf()
    val fontHeight: Int
    val texture: Int
    val texWidth: Int
    val texHeight: Int
    /*val vao: Int
    val vbo: Int
    val shaderProgramme = ShaderProgram(listOf("/2DTexture.vert", "/texture.frag"))
    val vertices: FloatBuffer
    var numVertices: Int = 0
    val texWidth: Int
    val texHeight: Int*/

    init {
        /*
        vao = glGenVertexArrays()
        glBindVertexArray(vao)
        vbo = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        vertices = MemoryUtil.memAllocFloat(4096);
        val size = (vertices.capacity() * java.lang.Float.BYTES).toLong()
        glBufferData(GL_ARRAY_BUFFER, size, GL_DYNAMIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        shaderProgramme.link()
        shaderProgramme.use()

        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        val posAttrib = glGetAttribLocation(shaderProgramme.ref, "position")
        glEnableVertexAttribArray(posAttrib)
        glVertexAttribPointer(posAttrib, 2, GL_FLOAT, false, 7 * 4, 0)

        val colAttrib = glGetAttribLocation(shaderProgramme.ref, "color")
        glEnableVertexAttribArray(colAttrib)
        glVertexAttribPointer(colAttrib, 3, GL_FLOAT, false, 7 * 4, 2 * 4)

        val texAttrib = glGetAttribLocation(shaderProgramme.ref, "texcoord")
        glEnableVertexAttribArray(texAttrib)
        glVertexAttribPointer(texAttrib, 2, GL_FLOAT, false, 7 * 4, 5 * 4)
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        glBindVertexArray(0)
        */
        val font = Font(MONOSPACED, PLAIN, 48)

        var imageWidth = 0
        var imageHeight = 0

        for (i in 32..255) {
            if (i == 127) {
                continue
            }
            val c = i.toChar()
            val ch: BufferedImage = createCharImage(font, c, true)

            imageWidth += ch.width
            imageHeight = max(imageHeight.toDouble(), ch.height.toDouble()).toInt()
        }

        fontHeight = imageHeight

        var image = BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()

        var x = 0
        for (i in 32..255) {
            if (i == 127) {
                continue
            }
            val c = i.toChar()
            val charImage = createCharImage(font, c, true)

            val charWidth = charImage.width
            val charHeight = charImage.height

            val ch = Glyph(charWidth, charHeight, x, image.height - charHeight)
            g.drawImage(charImage, x, 0, null)
            x += ch.width
            glyphs[c] = ch
        }

        val transform = AffineTransform.getScaleInstance(1.0, -1.0)
        transform.translate(0.0, (-image.height).toDouble())
        val operation = AffineTransformOp(transform, AffineTransformOp.TYPE_NEAREST_NEIGHBOR)
        image = operation.filter(image, null)

        val width = image.width
        val height = image.height

        val pixels = IntArray(width * height)
        image.getRGB(0, 0, width, height, pixels, 0, width)

        val buffer: ByteBuffer
        stackPush().use { stack ->
            buffer = BufferUtils.createByteBuffer(width * height * 4)

            for (y in 0 until height) {
                for (x in 0 until width) {
                    /* Pixel as RGBA: 0xAARRGGBB */
                    val pixel = pixels[y * width + x]

                    /* Red component 0xAARRGGBB >> (4 * 4) = 0x0000AARR */
                    buffer.put(((pixel shr 16) and 0xFF).toByte())

                    /* Green component 0xAARRGGBB >> (2 * 4) = 0x00AARRGG */
                    buffer.put(((pixel shr 8) and 0xFF).toByte())

                    /* Blue component 0xAARRGGBB >> 0 = 0xAARRGGBB */
                    buffer.put((pixel and 0xFF).toByte())

                    /* Alpha component 0xAARRGGBB >> (6 * 4) = 0x000000AA */
                    buffer.put(((pixel shr 24) and 0xFF).toByte())
                }
            }
        }
        /* Do not forget to flip the buffer! */
        buffer.flip()

        texWidth = imageWidth
        texHeight = imageHeight

        // Create Texture
        val texture = glGenTextures()
        glBindTexture(GL_TEXTURE_2D, texture)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, imageWidth, imageHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer!!)
        glGenerateMipmap(GL_TEXTURE_2D);

        this.texture = texture
    }

    fun createCharImage(font: Font, c: Char, antiAlias: Boolean) : BufferedImage {
        var image = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        var g = image.createGraphics()
        if (antiAlias) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        }
        g.setFont(font)
        val metrics = g.fontMetrics
        g.dispose()

        val charWidth: Int = metrics.charWidth(c)
        val charHeight = metrics.height

        image = BufferedImage(charWidth, charHeight, BufferedImage.TYPE_INT_ARGB)
        g = image.createGraphics()
        if (antiAlias) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        }
        g.setFont(font)
        g.paint = Color.WHITE
        g.drawString(String.valueOf(c), 0, metrics.ascent)
        g.dispose()
        return image
    }

    fun staticTextMesh(text: CharSequence, llX: Float, llY: Float) : Mesh {
        val (width, height) = window.getCurrentWindowSize()
        return Mesh.textCanvas(
            text.map { glyphs[it]!! },
            llX, llY,
            texWidth.toFloat(), texHeight.toFloat(),
            width.toFloat(), height.toFloat()
        )
    }

    /*fun drawText(text: CharSequence, x: Float, y: Float, projection: Matrix4f, model: Matrix4f) {
        val lines = 1
        val textHeight: Int = lines * fontHeight

        var drawX: Float = x
        var drawY: Float = y
        if (textHeight > fontHeight) {
            drawY += (textHeight - fontHeight).toFloat()
        }

        glBindTexture(GL_TEXTURE_2D, this.texture)

        for (i in 0 until text.length) {
            val ch: Char = text.get(i)
            if (ch == '\n') {
                /* Line feed, set x and y to draw at the next line */
                drawY -= fontHeight.toFloat()
                drawX = x
                continue
            }
            if (ch == '\r') {
                /* Carriage return, just skip it */
                continue
            }
            val g = glyphs[ch]
            drawTextureRegion(drawX, drawY, g!!.x.toFloat(), g.y.toFloat(), g.width.toFloat(), g.height.toFloat(), Color.WHITE)
            drawX += g.width.toFloat()
        }


        shaderProgramme.use()
        glBindVertexArray(vao)
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
        shaderProgramme.addUniform(projection, "projection")
        shaderProgramme.addUniform(model, "model")
        glDrawArrays (GL_TRIANGLES, 0, numVertices)
        vertices.clear();
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindVertexArray(0)
        numVertices = 0
    }*/
}