package de.uniwuerzburg.omodvisualizer.graphic

import de.uniwuerzburg.omodvisualizer.Window
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL30.*
import org.lwjgl.system.MemoryStack.stackPush
import java.awt.Color
import java.awt.Font
import java.awt.Font.PLAIN
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import kotlin.math.max


/**
 * Bitmap for text rendering.
 *
 * Mostly taken from LWJGL tutorial by SilverTiger.
 */
class Font(
    private val window: Window,
    val fontSize: Int = 48,
    antiAlias: Boolean = true
) {
    val glyphs: MutableMap<Char, Glyph> = mutableMapOf()
    val texture: Int
    val textureWidth: Int
    val textureHeight: Int

    init {
        // Load Font
        val font = Font("Segoe UI", PLAIN, fontSize)

        // Create Bitmap Image
        var imageWidth = 0
        var imageHeight = 0
        for (i in 32..255) {
            if (i == 127) {
                continue
            }
            val c = i.toChar()
            val ch: BufferedImage = createCharImage(font, c, antiAlias)

            imageWidth += ch.width
            imageHeight = max(imageHeight.toDouble(), ch.height.toDouble()).toInt()
        }
        var image = BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()

        // Create glyphs
        var xOffset = 0
        for (i in 32..255) {
            if (i == 127) {
                continue
            }
            val c = i.toChar()
            val charImage = createCharImage(font, c, true)

            val charWidth = charImage.width
            val charHeight = charImage.height

            val ch = Glyph(charWidth, charHeight, xOffset, image.height - charHeight)
            g.drawImage(charImage, xOffset, 0, null)
            xOffset += ch.width
            glyphs[c] = ch
        }

        // Create buffer of pixel data
        val transform = AffineTransform.getScaleInstance(1.0, -1.0)
        transform.translate(0.0, (-image.height).toDouble())
        val operation = AffineTransformOp(transform, AffineTransformOp.TYPE_NEAREST_NEIGHBOR)
        image = operation.filter(image, null)

        val pixels = IntArray(image.width * image.height)
        image.getRGB(0, 0, image.width, image.height, pixels, 0, image.width)

        val buffer: ByteBuffer
        stackPush().use {
            buffer = BufferUtils.createByteBuffer(image.width * image.height * 4)

            for (y in 0 until image.height) {
                for (x in 0 until image.width) {
                    /* Pixel as RGBA: 0xAARRGGBB */
                    val pixel = pixels[y * image.width + x]

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
        buffer.flip()

        // Create Texture
        val texture = glGenTextures()
        glBindTexture(GL_TEXTURE_2D, texture)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, imageWidth, imageHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer)
        glGenerateMipmap(GL_TEXTURE_2D)

        // Save texture
        this.texture = texture
        textureWidth = image.width
        textureHeight = image.height
    }

    private fun createCharImage(font: Font, c: Char, antiAlias: Boolean) : BufferedImage {
        var image = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        var g = image.createGraphics()
        if (antiAlias) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        }
        g.font = font
        val metrics = g.fontMetrics
        g.dispose()

        val charWidth: Int = metrics.charWidth(c)
        val charHeight = metrics.height

        image = BufferedImage(charWidth, charHeight, BufferedImage.TYPE_INT_ARGB)
        g = image.createGraphics()
        if (antiAlias) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        }
        g.font = font
        g.paint = Color(0.9f, 0.9f, 0.9f)
        g.drawString(c.toString(), 0, metrics.ascent)
        g.dispose()
        return image
    }

    /**
     * Write text to a location on screen.
     */
    fun staticTextRenderer(text: CharSequence) : Renderer {
        val (width, height) = window.getCurrentWindowSize()
        val aspect = window.getAspect()
        return Renderer(texture = this.texture).addTextCanvas(
            text.map { glyphs[it] ?: glyphs['?']!! },
            textureWidth.toFloat(), textureHeight.toFloat(),
            width.toFloat(), height.toFloat()
        )
    }
}