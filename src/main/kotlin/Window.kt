package de.uniwuerzburg.omodvisualizer

import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWKeyCallbackI
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.NULL
import java.awt.SystemColor.window
import java.nio.IntBuffer


class Window (title: String) {
    val ref: Long

    init {
        GLFWErrorCallback.createPrint(System.err).set() // Setup an error callback.

        // Initialize GLFW
        check(glfwInit()) { "Unable to initialize GLFW" }

        // Configure GLFW
        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE)
        glfwWindowHint(GLFW_SAMPLES, 4)

        // Get the resolution of the primary monitor
        val vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor())!!

        // Create the window
        ref = glfwCreateWindow(vidMode.width(), vidMode.height(), title, glfwGetPrimaryMonitor(), NULL)
        if (ref == NULL) throw RuntimeException("Failed to create the GLFW window")

        stackPush().use { stack ->
            val pWidth: IntBuffer = stack.mallocInt(1) // int*
            val pHeight: IntBuffer = stack.mallocInt(1) // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(ref, pWidth, pHeight)

            // Center the window
            glfwSetWindowPos(
                ref,
                (vidMode.width() - pWidth[0]) / 2,
                (vidMode.height() - pHeight[0]) / 2
            )
        }

        // Make the OpenGL context current
        glfwMakeContextCurrent(ref)

        // Enable v-sync, 1 -> Enabled
        glfwSwapInterval(0)

        // Make the window visible
        glfwShowWindow(ref)
    }

    fun getCurrentWindowSize() : Pair<Int, Int> {
        val width: Int
        val height: Int

        stackPush().use { stack ->
            val pWidth: IntBuffer = stack.mallocInt(1) // int*
            val pHeight: IntBuffer = stack.mallocInt(1) // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(ref, pWidth, pHeight)

            width = pWidth[0]
            height = pHeight[0]
        }
        return Pair(width, height)
    }

    fun getAspect() : Float{
        val resolution = getCurrentWindowSize()
        return resolution.second.toFloat() / resolution.first.toFloat()
    }

    fun close() {
        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(ref)
        glfwDestroyWindow(ref)
    }
}

